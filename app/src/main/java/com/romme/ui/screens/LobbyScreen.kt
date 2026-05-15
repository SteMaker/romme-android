package com.romme.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romme.game.RoomInfo
import com.romme.ui.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    viewModel: GameViewModel,
    onGameStarted: () -> Unit,
    onLogout: () -> Unit
) {
    val rooms by viewModel.rooms.collectAsState()
    val currentRoom by viewModel.currentRoom.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    BackHandler { onLogout() }

    // Wenn ein Spiel startet, zum Spielbildschirm wechseln
    LaunchedEffect(gameState.hand, gameState.isFinished) {
        if (gameState.hand.isNotEmpty() && !gameState.isFinished) {
            onGameStarted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lobby") },
                navigationIcon = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ArrowBack, "Abmelden")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (currentRoom == null) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Add, "Raum erstellen")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Fehlermeldung
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(error, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (currentRoom != null) {
                // In einem Raum - Wartebildschirm
                RoomWaitingView(
                    room = currentRoom!!,
                    isHost = currentRoom!!.hostId == viewModel.myPlayerId,
                    onStartGame = { viewModel.startGame() },
                    onLeave = { viewModel.leaveRoom() }
                )
            } else {
                // Raum-Liste
                Text(
                    "Offene Tische",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (rooms.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Keine offenen Tische.\nErstelle einen neuen!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(rooms) { room ->
                            RoomCard(room = room, onClick = { viewModel.joinRoom(room.id) })
                        }
                    }
                }
            }
        }
    }

    // Dialog: Neuen Raum erstellen
    if (showCreateDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, maxPlayers ->
                viewModel.createRoom(name, maxPlayers)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun RoomCard(room: RoomInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(room.name, fontWeight = FontWeight.Bold)
                Text(
                    "${room.playerCount}/${room.maxPlayers} Spieler",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(onClick = onClick) {
                Text("Beitreten")
            }
        }
    }
}

@Composable
fun RoomWaitingView(
    room: RoomInfo,
    isHost: Boolean,
    onStartGame: () -> Unit,
    onLeave: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            room.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Spieler:", fontWeight = FontWeight.Bold)
        room.players.forEach { player ->
            Text("  • ${player.displayName}")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (room.playerCount < 2) {
            Text("Warte auf weitere Spieler...")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onLeave) {
                Text("Verlassen")
            }
            if (isHost && room.playerCount >= 2) {
                Button(
                    onClick = onStartGame,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Spiel starten")
                }
            }
        }
    }
}

@Composable
fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var maxPlayers by remember { mutableIntStateOf(4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuen Tisch erstellen") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tisch-Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Max. Spieler: $maxPlayers")
                Slider(
                    value = maxPlayers.toFloat(),
                    onValueChange = { maxPlayers = it.toInt() },
                    valueRange = 2f..6f,
                    steps = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name, maxPlayers) }) {
                Text("Erstellen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
