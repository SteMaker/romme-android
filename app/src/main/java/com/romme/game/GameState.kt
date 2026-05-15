package com.romme.game

/**
 * Datenklassen die den Spielzustand auf dem Client abbilden.
 */

data class Card(
    val id: String,
    val suit: String,
    val rank: String,
    val value: Int
) {
    val displayName: String
        get() = when {
            rank == "joker" -> "Joker"
            else -> "${rankDisplay} ${suitSymbol}"
        }

    val suitSymbol: String
        get() = when (suit) {
            "herz" -> "♥"
            "karo" -> "♦"
            "pik" -> "♠"
            "kreuz" -> "✚"
            else -> "🃏"
        }

    val rankDisplay: String
        get() = when (rank) {
            "bube" -> "B"
            "dame" -> "D"
            "koenig" -> "K"
            "ass" -> "A"
            else -> rank.uppercase()
        }

    val isRed: Boolean
        get() = suit == "herz" || suit == "karo"
}

data class Meld(
    val id: Int,
    val cards: List<Card>,
    val type: String,
    val ownerId: String
)

data class OtherPlayer(
    val id: String,
    val handCount: Int,
    val hasInitialMeld: Boolean
)

data class GameState(
    val hand: List<Card> = emptyList(),
    val tableMelds: List<Meld> = emptyList(),
    val currentPlayerId: String = "",
    val phase: String = "draw",
    val otherPlayers: Map<String, OtherPlayer> = emptyMap(),
    val discardPile: List<Card> = emptyList(),
    val hasInitialMeld: Boolean = false,
    val deckCount: Int = 0,
    val isFinished: Boolean = false,
    val winner: String? = null,
    val round: Int = 1
)

data class TurnSummary(
    val playerId: String,
    val playerName: String,
    val lines: List<String>
)

data class RoomInfo(
    val id: String,
    val name: String,
    val hostId: String,
    val maxPlayers: Int,
    val playerCount: Int,
    val isPlaying: Boolean,
    val players: List<PlayerInfo> = emptyList()
)

data class PlayerInfo(
    val id: String,
    val displayName: String
)
