package com.romme.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONArray
import org.json.JSONObject

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

class GameSocket(private val serverUrl: String, private val socketPath: String = "/socket.io") {
    private var socket: Socket? = null
    private val gson = Gson()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    fun connect(token: String? = null, nextcloudUsername: String? = null, nextcloudPassword: String? = null) {
        val options = IO.Options().apply {
            path = socketPath
            auth = mutableMapOf<String, String>().apply {
                token?.let { put("token", it) }
                nextcloudUsername?.let { put("nextcloudUsername", it) }
                nextcloudPassword?.let { put("nextcloudPassword", it) }
            }
            forceNew = true
            reconnection = true
            reconnectionAttempts = 5
        }

        _connectionState.value = ConnectionState.CONNECTING
        socket = IO.socket(serverUrl, options)

        socket?.on(Socket.EVENT_CONNECT) {
            Log.d("GameSocket", "Verbunden")
            _connectionState.value = ConnectionState.CONNECTED
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.d("GameSocket", "Getrennt")
            _connectionState.value = ConnectionState.DISCONNECTED
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e("GameSocket", "Verbindungsfehler: ${args.firstOrNull()}")
            _connectionState.value = ConnectionState.ERROR
        }

        socket?.connect()
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    // --- Auth ---

    fun onTokenReceived(callback: (String) -> Unit) {
        socket?.on("auth:token") { args ->
            val data = when (val raw = args[0]) {
                is JSONObject -> raw
                else -> JSONObject(raw.toString())
            }
            callback(data.getString("token"))
        }
    }

    fun onUserInfo(callback: (String, String) -> Unit) {
        socket?.on("auth:user") { args ->
            val data = when (val raw = args[0]) {
                is JSONObject -> raw
                else -> JSONObject(raw.toString())
            }
            callback(data.getString("id"), data.getString("displayName"))
        }
    }

    // --- Lobby ---

    fun createRoom(name: String, maxPlayers: Int, callback: (JSONObject) -> Unit) {
        val options = JSONObject().apply {
            put("name", name)
            put("maxPlayers", maxPlayers)
        }
        socket?.emit("lobby:create", arrayOf(options)) { args ->
            callback(args[0] as JSONObject)
        }
    }

    fun joinRoom(roomId: String, callback: (JSONObject) -> Unit) {
        socket?.emit("lobby:join", arrayOf(roomId)) { args ->
            callback(args[0] as JSONObject)
        }
    }

    fun leaveRoom(roomId: String) {
        socket?.emit("lobby:leave", arrayOf(roomId)) { /* noop */ }
    }

    fun listRooms(callback: (JSONArray) -> Unit) {
        socket?.emit("lobby:list", Ack { args ->
            callback(args[0] as JSONArray)
        })
    }

    fun onRoomsUpdated(callback: (JSONArray) -> Unit) {
        socket?.on("lobby:rooms") { args ->
            callback(args[0] as JSONArray)
        }
    }

    fun onRoomUpdated(callback: (JSONObject) -> Unit) {
        socket?.on("room:updated") { args ->
            callback(args[0] as JSONObject)
        }
    }

    // --- Spiel ---

    fun startGame(roomId: String, callback: (JSONObject) -> Unit) {
        socket?.emit("game:start", arrayOf(roomId)) { args ->
            callback(args[0] as JSONObject)
        }
    }

    fun drawFromDeck(callback: (JSONObject) -> Unit) {
        socket?.emit("game:drawDeck", Ack { args ->
            callback(args[0] as JSONObject)
        })
    }

    fun drawFromDiscard(callback: (JSONObject) -> Unit) {
        socket?.emit("game:drawDiscard", Ack { args ->
            callback(args[0] as JSONObject)
        })
    }

    fun layDownMeld(cardIds: List<String>, callback: (JSONObject) -> Unit) {
        val arr = JSONArray(cardIds)
        socket?.emit("game:meld", arrayOf(arr)) { args ->
            callback(args[0] as JSONObject)
        }
    }

    fun appendToMeld(cardId: String, meldId: Int, side: String, callback: (JSONObject) -> Unit) {
        val data = JSONObject().apply {
            put("cardId", cardId)
            put("meldId", meldId)
            put("side", side)
        }
        socket?.emit("game:append", arrayOf(data)) { args ->
            callback(args[0] as JSONObject)
        }
    }

    fun replaceJoker(meldId: Int, jokerId: String, callback: (JSONObject) -> Unit) {
        val data = JSONObject().apply {
            put("meldId", meldId)
            put("jokerId", jokerId)
        }
        socket?.emit("game:replaceJoker", arrayOf(data)) { args ->
            callback(args[0] as JSONObject)
        }
    }

    fun discardCard(cardId: String, callback: (JSONObject) -> Unit) {
        socket?.emit("game:discard", arrayOf(cardId)) { args ->
            callback(args[0] as JSONObject)
        }
    }

    fun abandonGame(callback: (JSONObject) -> Unit) {
        socket?.emit("game:abandon", Ack { args ->
            callback(args[0] as JSONObject)
        })
    }

    fun onGameAbandoned(callback: (JSONObject) -> Unit) {
        socket?.on("game:abandoned") { args ->
            callback(args[0] as JSONObject)
        }
    }

    fun onGameStarted(callback: (JSONObject) -> Unit) {
        socket?.on("game:started") { args ->
            callback(args[0] as JSONObject)
        }
    }

    fun onGameState(callback: (JSONObject) -> Unit) {
        socket?.on("game:state") { args ->
            callback(args[0] as JSONObject)
        }
    }

    fun onGameAction(callback: (JSONObject) -> Unit) {
        socket?.on("game:action") { args ->
            callback(args[0] as JSONObject)
        }
    }

    fun onRoundEnd(callback: (JSONObject) -> Unit) {
        socket?.on("game:roundEnd") { args ->
            callback(args[0] as JSONObject)
        }
    }
}
