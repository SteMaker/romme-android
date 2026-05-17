package com.stemaker.openromme.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.stemaker.openromme.auth.SettingsStore
import com.stemaker.openromme.auth.TokenStore
import com.stemaker.openromme.game.*
import com.stemaker.openromme.network.ConnectionState
import com.stemaker.openromme.network.GameSocket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.json.JSONObject

data class LoginSettings(
    val serverUrl: String,
    val socketPath: String,
    val nextcloudUrl: String,
    val nextcloudUsername: String,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val gson = Gson()
    private val tokenStore = TokenStore(application)
    private val settingsStore = SettingsStore(application)
    private var gameSocket: GameSocket? = null
    private var connectingWithJwt: Boolean = false

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _rooms = MutableStateFlow<List<RoomInfo>>(emptyList())
    val rooms: StateFlow<List<RoomInfo>> = _rooms

    private val _currentRoom = MutableStateFlow<RoomInfo?>(null)
    val currentRoom: StateFlow<RoomInfo?> = _currentRoom

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState

    private val _selectedCards = MutableStateFlow<List<String>>(emptyList())
    val selectedCards: StateFlow<List<String>> = _selectedCards

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _gameAbandoned = MutableStateFlow(false)
    val gameAbandoned: StateFlow<Boolean> = _gameAbandoned

    private val _turnSummary = MutableStateFlow<TurnSummary?>(null)
    val turnSummary: StateFlow<TurnSummary?> = _turnSummary

    private val _changedMeldIds = MutableStateFlow<Set<Int>>(emptySet())
    val changedMeldIds: StateFlow<Set<Int>> = _changedMeldIds

    private val pendingActionLines = mutableListOf<String>()
    private val pendingAffectedMelds = mutableSetOf<Int>()

    var myPlayerId: String = ""
        private set

    fun getLoginSettings() = LoginSettings(
        serverUrl = settingsStore.serverUrl,
        socketPath = settingsStore.socketPath,
        nextcloudUrl = settingsStore.nextcloudUrl,
        nextcloudUsername = settingsStore.nextcloudUsername,
    )

    fun tryAutoLogin() {
        val jwt = tokenStore.jwt ?: return
        val serverUrl = settingsStore.serverUrl.ifEmpty { return }
        val socketPath = settingsStore.socketPath
        connect(serverUrl = serverUrl, socketPath = socketPath, token = jwt)
    }

    fun loginWithAppPassword(
        serverUrl: String,
        socketPath: String,
        nextcloudUrl: String,
        username: String,
        password: String,
    ) {
        settingsStore.serverUrl = serverUrl
        settingsStore.socketPath = socketPath
        settingsStore.nextcloudUrl = nextcloudUrl
        settingsStore.nextcloudUsername = username
        _connectionState.value = ConnectionState.CONNECTING
        connect(
            serverUrl = serverUrl,
            socketPath = socketPath,
            nextcloudUsername = username,
            nextcloudPassword = password,
        )
    }

    private fun connect(
        serverUrl: String,
        socketPath: String,
        token: String? = null,
        nextcloudUsername: String? = null,
        nextcloudPassword: String? = null,
    ) {
        connectingWithJwt = token != null
        gameSocket = GameSocket(serverUrl, socketPath).also { socket ->
            socket.connect(token = token, nextcloudUsername = nextcloudUsername, nextcloudPassword = nextcloudPassword)

            socket.connectionState
                .onEach { state ->
                    _connectionState.value = state
                    if (state == ConnectionState.ERROR && connectingWithJwt) {
                        tokenStore.jwt = null
                        connectingWithJwt = false
                    }
                }
                .launchIn(viewModelScope)

            socket.onUserInfo { id, _ ->
                myPlayerId = id
            }

            socket.onTokenReceived { jwt ->
                tokenStore.jwt = jwt
            }

            socket.onRoomsUpdated { roomsJson ->
                val rooms = mutableListOf<RoomInfo>()
                for (i in 0 until roomsJson.length()) {
                    val obj = roomsJson.getJSONObject(i)
                    rooms.add(parseRoomInfo(obj))
                }
                _rooms.value = rooms
            }

            socket.onRoomUpdated { roomJson ->
                _currentRoom.value = parseRoomInfo(roomJson)
            }

            socket.onGameStarted { stateJson ->
                _gameState.value = parseGameState(stateJson)
                pendingActionLines.clear()
                pendingAffectedMelds.clear()
                _turnSummary.value = null
                _changedMeldIds.value = emptySet()
            }

            socket.onGameState { stateJson ->
                val prevPlayerId = _gameState.value.currentPlayerId
                val newState = parseGameState(stateJson)
                _gameState.value = newState
                val newPlayerId = newState.currentPlayerId
                if (prevPlayerId.isNotEmpty() && prevPlayerId != newPlayerId) {
                    if (prevPlayerId != myPlayerId) {
                        val playerName = _currentRoom.value?.players
                            ?.find { it.id == prevPlayerId }?.displayName
                            ?: prevPlayerId.take(8)
                        _changedMeldIds.value = pendingAffectedMelds.toSet()
                        if (pendingActionLines.isNotEmpty()) {
                            _turnSummary.value = TurnSummary(
                                playerId = prevPlayerId,
                                playerName = playerName,
                                lines = pendingActionLines.toList()
                            )
                        }
                    } else {
                        _changedMeldIds.value = emptySet()
                        _turnSummary.value = null
                    }
                    pendingActionLines.clear()
                    pendingAffectedMelds.clear()
                }
            }

            socket.onGameAction { json ->
                when (json.optString("action")) {
                    "draw" -> pendingActionLines.add(
                        if (json.optString("source") == "deck") "Vom Nachziehstapel gezogen"
                        else "Vom Ablegestapel gezogen"
                    )
                    "meld" -> {
                        val meldId = json.getInt("meldId")
                        pendingAffectedMelds.add(meldId)
                        pendingActionLines.add(
                            if (json.optString("type") == "satz") "Satz ausgelegt"
                            else "Folge ausgelegt"
                        )
                    }
                    "append" -> {
                        val meldId = json.getInt("meldId")
                        pendingAffectedMelds.add(meldId)
                        pendingActionLines.add("An Meldung ${meldId + 1} angelegt")
                    }
                    "replaceJoker" -> {
                        val meldId = json.getInt("meldId")
                        pendingAffectedMelds.add(meldId)
                        pendingActionLines.add("Joker in Meldung ${meldId + 1} getauscht")
                    }
                    "discard" -> {
                        val cardJson = json.optJSONObject("card")
                        if (cardJson != null) {
                            val c = parseCard(cardJson)
                            pendingActionLines.add("${c.rankDisplay} ${c.suitSymbol} abgelegt")
                        }
                    }
                }
            }

            socket.onRoundEnd { data ->
                _gameState.value = _gameState.value.copy(
                    isFinished = true,
                    winner = data.optString("winner")
                )
                _currentRoom.value = null
            }

            socket.onGameAbandoned {
                _gameAbandoned.value = true
            }
        }
    }

    fun abandonGame() {
        gameSocket?.abandonGame { /* fire and forget */ }
    }

    fun createRoom(name: String, maxPlayers: Int) {
        gameSocket?.createRoom(name, maxPlayers) { response ->
            if (response.getBoolean("ok")) {
                _currentRoom.value = parseRoomInfo(response.getJSONObject("room"))
            } else {
                _errorMessage.value = response.optString("error")
            }
        }
    }

    fun joinRoom(roomId: String) {
        gameSocket?.joinRoom(roomId) { response ->
            if (response.getBoolean("ok")) {
                _currentRoom.value = parseRoomInfo(response.getJSONObject("room"))
            } else {
                _errorMessage.value = response.optString("error")
            }
        }
    }

    fun leaveRoom() {
        _currentRoom.value?.let { room ->
            gameSocket?.leaveRoom(room.id)
            _currentRoom.value = null
        }
    }

    fun startGame() {
        _currentRoom.value?.let { room ->
            gameSocket?.startGame(room.id) { response ->
                if (!response.getBoolean("ok")) {
                    _errorMessage.value = response.optString("error")
                }
            }
        }
    }

    fun drawFromDeck() {
        gameSocket?.drawFromDeck { response ->
            if (!response.getBoolean("ok")) {
                _errorMessage.value = response.optString("error")
            }
        }
    }

    fun drawFromDiscard() {
        gameSocket?.drawFromDiscard { response ->
            if (!response.getBoolean("ok")) {
                _errorMessage.value = response.optString("error")
            }
        }
    }

    fun toggleCardSelection(cardId: String) {
        val current = _selectedCards.value
        _selectedCards.value = if (current.contains(cardId)) {
            current - cardId
        } else {
            current + cardId
        }
    }

    fun clearSelection() {
        _selectedCards.value = emptyList()
    }

    fun layDownMeld() {
        val cardIds = _selectedCards.value
        if (cardIds.size < 3) {
            _errorMessage.value = "Mindestens 3 Karten auswählen"
            return
        }
        gameSocket?.layDownMeld(cardIds) { response ->
            if (response.getBoolean("ok")) {
                _selectedCards.value = emptyList()
            } else {
                _errorMessage.value = response.optString("error")
            }
        }
    }

    fun appendToMeld(cardId: String, meldId: Int, side: String = "right") {
        gameSocket?.appendToMeld(cardId, meldId, side) { response ->
            if (response.getBoolean("ok")) {
                _selectedCards.value = emptyList()
            } else {
                _errorMessage.value = response.optString("error")
            }
        }
    }

    fun replaceJoker(meldId: Int, jokerId: String) {
        gameSocket?.replaceJoker(meldId, jokerId) { response ->
            if (!response.getBoolean("ok")) {
                _errorMessage.value = response.optString("error")
            }
        }
    }

    fun discardCard(cardId: String) {
        gameSocket?.discardCard(cardId) { response ->
            if (!response.getBoolean("ok")) {
                _errorMessage.value = response.optString("error")
            }
            _selectedCards.value = emptyList()
        }
    }

    fun logout() {
        tokenStore.clear()
        gameSocket?.disconnect()
        gameSocket = null
        myPlayerId = ""
        _connectionState.value = ConnectionState.DISCONNECTED
        _rooms.value = emptyList()
        _currentRoom.value = null
        _gameState.value = GameState()
        _selectedCards.value = emptyList()
        _errorMessage.value = null
        _gameAbandoned.value = false
        pendingActionLines.clear()
        pendingAffectedMelds.clear()
        _turnSummary.value = null
        _changedMeldIds.value = emptySet()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun acknowledgeTurnSummary() {
        _turnSummary.value = null
    }

    fun resetGameState() {
        _gameState.value = GameState()
        _selectedCards.value = emptyList()
        _currentRoom.value = null
        _gameAbandoned.value = false
        pendingActionLines.clear()
        pendingAffectedMelds.clear()
        _turnSummary.value = null
        _changedMeldIds.value = emptySet()
    }

    override fun onCleared() {
        gameSocket?.disconnect()
    }

    private fun parseRoomInfo(json: JSONObject): RoomInfo {
        val players = mutableListOf<PlayerInfo>()
        val playersArr = json.optJSONArray("players")
        if (playersArr != null) {
            for (i in 0 until playersArr.length()) {
                val p = playersArr.getJSONObject(i)
                players.add(PlayerInfo(p.getString("id"), p.getString("displayName")))
            }
        }
        return RoomInfo(
            id = json.getString("id"),
            name = json.getString("name"),
            hostId = json.getString("hostId"),
            maxPlayers = json.getInt("maxPlayers"),
            playerCount = json.getInt("playerCount"),
            isPlaying = json.getBoolean("isPlaying"),
            players = players
        )
    }

    private fun parseGameState(json: JSONObject): GameState {
        val hand = mutableListOf<Card>()
        val handArr = json.getJSONArray("hand")
        for (i in 0 until handArr.length()) {
            hand.add(parseCard(handArr.getJSONObject(i)))
        }

        val tableMelds = mutableListOf<Meld>()
        val meldsArr = json.getJSONArray("tableMelds")
        for (i in 0 until meldsArr.length()) {
            val m = meldsArr.getJSONObject(i)
            val meldCards = mutableListOf<Card>()
            val cardsArr = m.getJSONArray("cards")
            for (j in 0 until cardsArr.length()) {
                meldCards.add(parseCard(cardsArr.getJSONObject(j)))
            }
            tableMelds.add(Meld(m.getInt("id"), meldCards, m.getString("type"), m.getString("ownerId")))
        }

        val otherPlayers = mutableMapOf<String, OtherPlayer>()
        val othersObj = json.getJSONObject("otherPlayers")
        for (key in othersObj.keys()) {
            val keyStr = key.toString()
            val o = othersObj.getJSONObject(keyStr)
            otherPlayers[keyStr] = OtherPlayer(o.getString("id"), o.getInt("handCount"), o.getBoolean("hasInitialMeld"))
        }

        val discardPile = mutableListOf<Card>()
        val discardArr = json.optJSONArray("discardPile")
        if (discardArr != null) {
            for (i in 0 until discardArr.length()) {
                discardPile.add(parseCard(discardArr.getJSONObject(i)))
            }
        }

        return GameState(
            hand = hand,
            tableMelds = tableMelds,
            currentPlayerId = json.getString("currentPlayerId"),
            phase = json.getString("phase"),
            otherPlayers = otherPlayers,
            discardPile = discardPile,
            hasInitialMeld = json.optBoolean("hasInitialMeld", false),
            deckCount = json.getInt("deckCount"),
            isFinished = json.getBoolean("isFinished"),
            winner = json.optString("winner", null),
            round = json.getInt("round")
        )
    }

    private fun parseCard(json: JSONObject): Card {
        return Card(
            id = json.getString("id"),
            suit = json.getString("suit"),
            rank = json.getString("rank"),
            value = json.getInt("value")
        )
    }
}
