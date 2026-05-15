package com.romme.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.romme.game.Card
import com.romme.game.GameState
import com.romme.game.Meld
import com.romme.game.TurnSummary
import com.romme.ui.GameViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.roundToInt

private val CARD_W = 56.dp
private val CARD_H = 80.dp
private val CELL_MARGIN = 4.dp
private val CELL_W = CARD_W + CELL_MARGIN   // 60dp — one grid slot
private val CELL_H = CARD_H + CELL_MARGIN   // 84dp — one grid slot
private val LABEL_H = 28.dp

@Composable
fun GameScreen(viewModel: GameViewModel, onGameEnd: () -> Unit) {
    val gameState by viewModel.gameState.collectAsState()
    val selectedCards by viewModel.selectedCards.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val turnSummary by viewModel.turnSummary.collectAsState()
    val changedMeldIds by viewModel.changedMeldIds.collectAsState()
    val gameAbandoned by viewModel.gameAbandoned.collectAsState()
    val isMyTurn = gameState.currentPlayerId == viewModel.myPlayerId

    var sortTrigger  by remember { mutableStateOf(0) }
    var appendMode   by remember { mutableStateOf(false) }
    var showAbandonDialog by remember { mutableStateOf(false) }

    LaunchedEffect(gameAbandoned) {
        if (gameAbandoned) onGameEnd()
    }

    BackHandler {
        showAbandonDialog = true
    }

    // Cancel append mode whenever selection changes or turn ends
    LaunchedEffect(selectedCards, isMyTurn) {
        if (selectedCards.size != 1 || !isMyTurn) appendMode = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OpponentBar(gameState)

            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(error, color = Color.White, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        TextButton(onClick = { viewModel.clearError() }) { Text("OK", color = Color.White) }
                    }
                }
            }

            turnSummary?.let { summary ->
                TurnSummaryBanner(summary = summary, onDismiss = { viewModel.acknowledgeTurnSummary() })
            }

            TableArea(
                gameState = gameState,
                isMyTurn = isMyTurn,
                appendMode = appendMode,
                appendCardId = if (appendMode) selectedCards.firstOrNull() else null,
                changedMeldIds = changedMeldIds,
                onDrawDeck = { viewModel.drawFromDeck() },
                onDrawDiscard = { viewModel.drawFromDiscard() },
                onAppendToMeld = { cardId, meldId, side ->
                    viewModel.appendToMeld(cardId, meldId, side)
                    appendMode = false
                },
                onReplaceJoker = { meldId, jokerId -> viewModel.replaceJoker(meldId, jokerId) },
                onSort = { sortTrigger++ }
            )

            if (isMyTurn) {
                ActionBar(
                    selectedCards = selectedCards,
                    phase = gameState.phase,
                    hasInitialMeld = gameState.hasInitialMeld,
                    appendMode = appendMode,
                    onMeld = { viewModel.layDownMeld() },
                    onAppend = { appendMode = true },
                    onDiscard = { cardId -> viewModel.discardCard(cardId) },
                    onClearSelection = {
                        viewModel.clearSelection()
                        appendMode = false
                    }
                )
            }

            HandArea(
                hand = gameState.hand,
                selectedCards = selectedCards,
                isMyTurn = isMyTurn,
                phase = gameState.phase,
                sortTrigger = sortTrigger,
                onCardClick = { card ->
                    if (isMyTurn && gameState.phase != "draw") {
                        viewModel.toggleCardSelection(card.id)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (gameState.isFinished) {
            RoundEndOverlay(gameState = gameState, onContinue = onGameEnd)
        }

        if (showAbandonDialog) {
            AlertDialog(
                onDismissRequest = { showAbandonDialog = false },
                title = { Text("Spiel verlassen?") },
                text = { Text("Das Spiel wird für alle Spieler beendet.") },
                confirmButton = {
                    TextButton(onClick = {
                        showAbandonDialog = false
                        viewModel.abandonGame()
                    }) { Text("Verlassen") }
                },
                dismissButton = {
                    TextButton(onClick = { showAbandonDialog = false }) { Text("Weiter spielen") }
                }
            )
        }
    }
}

@Composable
fun HandArea(
    hand: List<Card>,
    selectedCards: List<String>,
    isMyTurn: Boolean,
    phase: String,
    sortTrigger: Int,
    onCardClick: (Card) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1B5E20))
    ) {
        val cellWPx      = with(density) { CELL_W.toPx() }
        val cellHPx      = with(density) { CELL_H.toPx() }
        val halfMarginPx = with(density) { (CELL_MARGIN / 2).toPx() }
        val labelHPx     = with(density) { LABEL_H.toPx() }
        val maxCols = (constraints.maxWidth / cellWPx).toInt().coerceAtLeast(1)
        val maxRows = ((constraints.maxHeight - labelHPx) / cellHPx).toInt().coerceAtLeast(1)

        // Ordered list of card IDs — the visual sequence
        val cardOrder = remember { mutableStateListOf<String>() }

        // Drag state
        var draggingId     by remember { mutableStateOf<String?>(null) }
        var dragPos        by remember { mutableStateOf(Offset.Zero) }  // finger in container coords
        var insertionIndex by remember { mutableStateOf<Int?>(null) }
        val fp      = remember { floatArrayOf(0f, 0f) }   // raw finger position (non-State)
        val grabOff = remember { floatArrayOf(0f, 0f) }   // grab offset within card

        // ── helpers ──────────────────────────────────────────────────────────

        // Returns insertion point (0..cardOrder.size) in full cardOrder space
        fun computeInsertionIndex(fx: Float, fy: Float): Int {
            val n = cardOrder.size
            if (n == 0) return 0
            val col = (fx / cellWPx).roundToInt().coerceIn(0, maxCols)
            val row = ((fy - labelHPx) / cellHPx).roundToInt().coerceIn(0, maxRows)
            return (row * maxCols + col).coerceIn(0, n)
        }

        // ── order maintenance ─────────────────────────────────────────────────

        val handKey = hand.joinToString { it.id }
        LaunchedEffect(handKey) {
            val handIds = hand.map { it.id }.toSet()
            cardOrder.removeAll { it !in handIds }
            val knownIds = cardOrder.toSet()
            hand.forEach { card -> if (card.id !in knownIds) cardOrder.add(card.id) }
        }

        LaunchedEffect(sortTrigger) {
            if (sortTrigger > 0) {
                val suitOrder = mapOf("kreuz" to 0, "pik" to 1, "herz" to 2, "karo" to 3)
                val rankOrder = mapOf(
                    "2" to 0, "3" to 1, "4" to 2, "5" to 3, "6" to 4, "7" to 5,
                    "8" to 6, "9" to 7, "10" to 8, "bube" to 9, "dame" to 10,
                    "koenig" to 11, "ass" to 12, "joker" to 13
                )
                val sorted = hand.sortedWith(compareBy({ suitOrder[it.suit] ?: 4 }, { rankOrder[it.rank] ?: 0 }))
                cardOrder.clear()
                cardOrder.addAll(sorted.map { it.id })
            }
        }

        // ── rendering ────────────────────────────────────────────────────────

        Text(
            "Deine Hand (${hand.size})",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
        )

        // Insertion line — vertical yellow bar between two cards
        insertionIndex?.let { idx ->
            val lineX = (idx % maxCols) * cellWPx
            val lineY = labelHPx + (idx / maxCols) * cellHPx
            Box(
                modifier = Modifier
                    .offset { IntOffset(lineX.roundToInt(), lineY.roundToInt()) }
                    .width(3.dp)
                    .height(CELL_H)
                    .zIndex(998f)
                    .background(Color(0xFFFFEB3B), RoundedCornerShape(2.dp))
            )
        }

        // Cards — each rendered at its sequential position; dragging card shown as ghost
        cardOrder.forEachIndexed { i, cardId ->
            val card = hand.find { it.id == cardId } ?: return@forEachIndexed
            val isDraggingThis = draggingId == cardId
            val cardX = (i % maxCols) * cellWPx + halfMarginPx
            val cardY = labelHPx + (i / maxCols) * cellHPx + halfMarginPx
            val selectionIndex = selectedCards.indexOf(cardId)  // -1 if not selected
            val isSelected = selectionIndex >= 0

            key(cardId) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(cardX.roundToInt(), cardY.roundToInt()) }
                        .size(CARD_W, CARD_H)
                        .zIndex(if (isDraggingThis) 0f else 1f)
                        .graphicsLayer { alpha = if (isDraggingThis) 0.3f else 1f }
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFFFFD700) else Color.White)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color  = if (isSelected) Color(0xFFFFD700) else Color.Gray,
                            shape  = RoundedCornerShape(6.dp)
                        )
                        .pointerInput(cardId, i, maxCols, maxRows) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)

                                val slopChange = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                                    change.consume()
                                }

                                if (slopChange != null) {
                                    // ── DRAG ─────────────────────────────────────────────
                                    grabOff[0] = down.position.x
                                    grabOff[1] = down.position.y
                                    fp[0] = cardX + down.position.x
                                    fp[1] = cardY + down.position.y
                                    draggingId     = cardId
                                    dragPos        = Offset(fp[0], fp[1])
                                    insertionIndex = computeInsertionIndex(fp[0], fp[1])

                                    drag(slopChange.id) { change ->
                                        val delta = change.positionChange()
                                        change.consume()
                                        fp[0] += delta.x
                                        fp[1] += delta.y
                                        dragPos        = Offset(fp[0], fp[1])
                                        insertionIndex = computeInsertionIndex(fp[0], fp[1])
                                    }

                                    val idx     = insertionIndex
                                    val fromIdx = cardOrder.indexOf(cardId)
                                    if (idx != null && idx != fromIdx && idx != fromIdx + 1) {
                                        cardOrder.removeAt(fromIdx)
                                        val dest = (if (idx > fromIdx) idx - 1 else idx)
                                            .coerceIn(0, cardOrder.size)
                                        cardOrder.add(dest, cardId)
                                    }
                                    draggingId     = null
                                    insertionIndex = null
                                } else if (draggingId == null) {
                                    // ── TAP ──────────────────────────────────────────────
                                    onCardClick(card)
                                }
                            }
                        }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(card.rankDisplay, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = if (card.isRed) Color.Red else Color.Black)
                        Text(card.suitSymbol, fontSize = 14.sp,
                            color = if (card.isRed) Color.Red else Color.Black)
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(16.dp)
                                .background(Color(0xFFE65100), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${selectionIndex + 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Floating card — follows the finger while dragging
        val draggingCard = draggingId?.let { id -> hand.find { it.id == id } }
        if (draggingCard != null) {
            val isSelected = selectedCards.contains(draggingCard.id)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (dragPos.x - grabOff[0]).roundToInt(),
                            (dragPos.y - grabOff[1]).roundToInt()
                        )
                    }
                    .size(CARD_W, CARD_H)
                    .zIndex(999f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(0xFFFFD700) else Color.White)
                    .border(2.dp, Color(0xFFFFEB3B), RoundedCornerShape(6.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(draggingCard.rankDisplay, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = if (draggingCard.isRed) Color.Red else Color.Black)
                    Text(draggingCard.suitSymbol, fontSize = 14.sp,
                        color = if (draggingCard.isRed) Color.Red else Color.Black)
                }
            }
        }
    }
}

@Composable
fun TurnSummaryBanner(summary: TurnSummary, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${summary.playerName} hat gespielt:",
                    color = Color(0xFFBBDEFB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                summary.lines.forEach { line ->
                    Text("• $line", color = Color.White, fontSize = 12.sp)
                }
            }
            TextButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("OK", color = Color(0xFF90CAF9), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun OpponentBar(gameState: GameState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B5E20))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        gameState.otherPlayers.forEach { (_, player) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(player.id.take(10), fontSize = 12.sp, color = Color.White)
                Text("${player.handCount} Karten", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun TableArea(
    gameState: GameState,
    isMyTurn: Boolean,
    appendMode: Boolean,
    appendCardId: String?,
    changedMeldIds: Set<Int> = emptySet(),
    onDrawDeck: () -> Unit,
    onDrawDiscard: () -> Unit,
    onAppendToMeld: (String, Int, String) -> Unit,
    onReplaceJoker: (Int, String) -> Unit,
    onSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canDrawDiscard = isMyTurn && gameState.phase == "draw_optional"

    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        // Top row: sort button + draw deck
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            SortButton(onClick = onSort)
            Spacer(modifier = Modifier.width(16.dp))
            CardBack(
                label = "Stapel\n(${gameState.deckCount})",
                onClick = if (isMyTurn && gameState.phase == "draw") onDrawDeck else null
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Discard pile: all cards visible, only top card is drawable (in draw_optional phase)
        DiscardPileRow(
            pile = gameState.discardPile,
            canDraw = canDrawDiscard,
            onDrawTop = onDrawDiscard
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = when {
                !isMyTurn -> "Warte auf Mitspieler..."
                gameState.phase == "draw" -> "Vom Stapel ziehen (Pflicht)"
                gameState.phase == "draw_optional" -> "Vom Ablegestapel ziehen (optional, mehrfach möglich)"
                gameState.phase == "play" -> "Auslegen oder Ablegen"
                else -> ""
            },
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        if (gameState.tableMelds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (appendMode) "Auslage auswählen:" else "Auslagen:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (appendMode) Color(0xFF90CAF9) else Color.Unspecified
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(gameState.tableMelds) { meld ->
                    val onLeft: (() -> Unit)? =
                        if (appendMode && appendCardId != null) ({ onAppendToMeld(appendCardId, meld.id, "left") })
                        else null
                    val onRight: (() -> Unit)? =
                        if (appendMode && appendCardId != null) ({ onAppendToMeld(appendCardId, meld.id, "right") })
                        else null
                    MeldView(
                        meld = meld,
                        highlight = appendMode,
                        isChanged = meld.id in changedMeldIds,
                        onAppendLeft = onLeft,
                        onAppendRight = onRight,
                        onJokerClick = if (isMyTurn && (gameState.phase == "play" || gameState.phase == "draw_optional") && !appendMode)
                            { jokerId -> onReplaceJoker(meld.id, jokerId) }
                        else null
                    )
                }
            }
        }
    }
}

@Composable
fun DiscardPileRow(
    pile: List<Card>,
    canDraw: Boolean,
    onDrawTop: () -> Unit
) {
    if (pile.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CARD_H)
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Ablage leer", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
        }
        return
    }

    val reversed = pile.asReversed()
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(reversed) { index, card ->
            val isTop = index == 0
            CardView(
                card = card,
                isSelected = false,
                onClick = if (isTop && canDraw) onDrawTop else null,
                extraBorder = if (isTop) Pair(2.dp, Color(0xFFFFEB3B)) else null
            )
        }
    }
}

@Composable
fun SortButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(CARD_W, CARD_H)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF2E7D32))
            .border(1.dp, Color(0xFF388E3C), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⇅", fontSize = 22.sp, color = Color.White)
            Text("Sort", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun ActionBar(
    selectedCards: List<String>,
    phase: String,
    hasInitialMeld: Boolean,
    appendMode: Boolean,
    onMeld: () -> Unit,
    onAppend: () -> Unit,
    onDiscard: (String) -> Unit,
    onClearSelection: () -> Unit
) {
    if (selectedCards.isEmpty() && !appendMode) return
    val bgColor = if (appendMode) Color(0xFF1565C0) else Color(0xFF2E7D32)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (appendMode) {
            Text("Auslage antippen zum Anlegen", color = Color.White, fontSize = 13.sp,
                modifier = Modifier.weight(1f))
        } else {
            Text("${selectedCards.size} gewählt", color = Color.White, fontSize = 13.sp)
        }

        val isPlayPhase = phase == "play" || phase == "draw_optional"

        if (!appendMode && isPlayPhase && selectedCards.size >= 3) {
            Button(
                onClick = onMeld,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) { Text("Auslegen", fontSize = 13.sp) }
        }

        if (!appendMode && isPlayPhase && selectedCards.size == 1 && hasInitialMeld) {
            Button(
                onClick = onAppend,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) { Text("Anlegen", fontSize = 13.sp) }
        }

        if (!appendMode && isPlayPhase && selectedCards.size == 1) {
            Button(
                onClick = { onDiscard(selectedCards.first()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) { Text("Ablegen", fontSize = 13.sp) }
        }

        OutlinedButton(
            onClick = onClearSelection,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) { Text("✕", fontSize = 13.sp, color = Color.White) }
    }
}

@Composable
fun MeldView(
    meld: Meld,
    highlight: Boolean = false,
    isChanged: Boolean = false,
    onAppendLeft: (() -> Unit)? = null,
    onAppendRight: (() -> Unit)? = null,
    onJokerClick: ((String) -> Unit)? = null
) {
    val borderMod = when {
        highlight -> Modifier.border(2.dp, Color(0xFF90CAF9), RoundedCornerShape(8.dp))
        isChanged -> Modifier.border(3.dp, Color(0xFFF57C00), RoundedCornerShape(8.dp))
        else -> Modifier
    }
    Card(
        modifier = Modifier
            .padding(4.dp)
            .then(borderMod),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) Color(0xFF1565C0) else Color(0xFF388E3C)
        )
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            meld.cards.forEachIndexed { index, card ->
                val isFirst = index == 0
                val isLast = index == meld.cards.size - 1
                val click: (() -> Unit)? = when {
                    isFirst && onAppendLeft != null -> onAppendLeft
                    isLast && onAppendRight != null -> onAppendRight
                    card.rank == "joker" && onJokerClick != null -> { { onJokerClick(card.id) } }
                    else -> null
                }
                MiniCardView(
                    card = card,
                    onClick = click,
                    isAppendTarget = (isFirst && onAppendLeft != null) || (isLast && onAppendRight != null)
                )
            }
        }
    }
}

@Composable
fun MiniCardView(card: Card, onClick: (() -> Unit)? = null, isAppendTarget: Boolean = false) {
    val isJoker = card.rank == "joker"
    val borderWidth = when {
        isAppendTarget -> 2.dp
        isJoker && onClick != null -> 2.dp
        else -> 0.dp
    }
    val borderColor = when {
        isAppendTarget -> Color(0xFF4CAF50)
        isJoker && onClick != null -> Color(0xFFFFEB3B)
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .size(32.dp, 44.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isJoker) Color(0xFF7B1FA2) else Color.White)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .border(borderWidth, borderColor, RoundedCornerShape(3.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (isJoker) "★\nJ" else "${card.rankDisplay}\n${card.suitSymbol}",
            fontSize = 9.sp,
            color = if (isJoker) Color.White else if (card.isRed) Color.Red else Color.Black,
            textAlign = TextAlign.Center,
            lineHeight = 10.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardView(
    card: Card,
    isSelected: Boolean,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    extraBorder: Pair<Dp, Color>? = null
) {
    val borderWidth = when {
        isSelected -> 2.dp
        extraBorder != null -> extraBorder.first
        else -> 1.dp
    }
    val borderColor = when {
        isSelected -> Color(0xFFFFD700)
        extraBorder != null -> extraBorder.second
        else -> Color.Gray
    }
    Box(
        modifier = Modifier
            .size(CARD_W, CARD_H)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFFFFD700) else Color.White)
            .then(
                if (onClick != null || onLongClick != null)
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = { onLongClick?.invoke() }
                    )
                else Modifier
            )
            .border(borderWidth, borderColor, RoundedCornerShape(6.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(card.rankDisplay, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                color = if (card.isRed) Color.Red else Color.Black)
            Text(card.suitSymbol, fontSize = 14.sp,
                color = if (card.isRed) Color.Red else Color.Black)
        }
    }
}

@Composable
fun CardBack(label: String, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .size(CARD_W, CARD_H)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1565C0))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .border(1.dp, Color(0xFF0D47A1), RoundedCornerShape(6.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 10.sp, color = Color.White, textAlign = TextAlign.Center)
    }
}

@Composable
fun CardPlaceholder(label: String) {
    Box(
        modifier = Modifier
            .size(CARD_W, CARD_H)
            .clip(RoundedCornerShape(6.dp))
            .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
fun RoundEndOverlay(gameState: GameState, onContinue: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Runde beendet!", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Gewinner: ${gameState.winner ?: "?"}", fontSize = 20.sp, color = Color.White)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) { Text("Zurück zur Lobby") }
            }
        }
    }
}
