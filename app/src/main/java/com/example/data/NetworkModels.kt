package com.example.data

import com.example.engine.AiDifficulty
import com.example.engine.GameStatus
import com.example.engine.PieceColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TimeControlOption(
    val dbKey: String,
    val label: String,
    val shortBadge: String,
    val initialTimeMs: Long,
    val incrementMs: Long,
    val isLiveClock: Boolean
) {
    UNLIMITED("unlimited", "No Clock (Off)", "Off / ∞", 0L, 0L, false),
    BLITZ_3_2("blitz_3_2", "Blitz 3+2", "3m + 2s", 180_000L, 2_000L, true),
    BLITZ_5_0("blitz_5_0", "Blitz 5+0", "5 min", 300_000L, 0L, true),
    RAPID_10_0("rapid_10_0", "Rapid 10+0", "10 min", 600_000L, 0L, true),
    CLASSICAL_15_10("classical_15_10", "Classical 15+10", "15m + 10s", 900_000L, 10_000L, true),
    DAILY_ASYNC("daily", "Daily Correspondence", "24h / Turn", 86_400_000L, 0L, false);

    companion object {
        fun fromDbKey(key: String): TimeControlOption {
            return entries.firstOrNull { it.dbKey == key } ?: RAPID_10_0
        }
    }
}

@Serializable
enum class GameModeType(val label: String) {
    ONLINE_SUPABASE("Online Match"),
    AI_OPPONENT("AI Opponent"),
    PASS_AND_PLAY("Pass & Play")
}

@Serializable
data class UserProfileDto(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_color") val avatarColor: String = "#10B981",
    @SerialName("elo_rating") val eloRating: Int = 1200,
    @SerialName("games_played") val gamesPlayed: Int = 0,
    @SerialName("games_won") val gamesWon: Int = 0
)

@Serializable
data class FriendshipDto(
    val id: String = "",
    @SerialName("requester_id") val requesterId: String,
    @SerialName("addressee_id") val addresseeId: String,
    val status: String = "pending"
)

@Serializable
data class FriendItemUi(
    val friendshipId: String,
    val profile: UserProfileDto,
    val isPendingIncoming: Boolean = false,
    val isPendingOutgoing: Boolean = false,
    val isOnline: Boolean = true
)

@Serializable
data class GameRecordDto(
    val id: String,
    @SerialName("white_player_id") val whitePlayerId: String,
    @SerialName("black_player_id") val blackPlayerId: String,
    @SerialName("white_username") val whiteUsername: String,
    @SerialName("black_username") val blackUsername: String,
    @SerialName("current_fen") val currentFen: String,
    @SerialName("turn_color") val turnColor: String = "w",
    val status: String = "active",
    @SerialName("winner_id") val winnerId: String? = null,
    @SerialName("draw_offered_by") val drawOfferedBy: String? = null,
    @SerialName("time_control") val timeControl: String = "daily",
    @SerialName("white_time_remaining_ms") val whiteTimeRemainingMs: Long = 86_400_000L,
    @SerialName("black_time_remaining_ms") val blackTimeRemainingMs: Long = 86_400_000L,
    @SerialName("last_move_from") val lastMoveFrom: String? = null,
    @SerialName("last_move_to") val lastMoveTo: String? = null,
    @SerialName("last_move_san") val lastMoveSan: String? = null,
    @SerialName("move_list_san") val moveListSan: String = "",
    @kotlinx.serialization.Transient val updatedAt: String = "Just now"
)

@Serializable
data class MoveRecordDto(
    val id: String = "",
    @SerialName("game_id") val gameId: String,
    @SerialName("move_number") val moveNumber: Int,
    @SerialName("player_id") val playerId: String,
    val piece: String,
    @SerialName("from_square") val fromSquare: String,
    @SerialName("to_square") val toSquare: String,
    val promotion: String? = null,
    val san: String,
    @SerialName("fen_after") val fenAfter: String
)

@Serializable
data class RealtimeMoveBroadcast(
    val gameId: String,
    val fenAfter: String,
    val turnColor: String,
    val status: String,
    val lastMoveFrom: String,
    val lastMoveTo: String,
    val lastMoveSan: String,
    val moveListSan: String,
    val whiteTimeRemainingMs: Long,
    val blackTimeRemainingMs: Long,
    val drawOfferedBy: String? = null,
    val winnerId: String? = null
)

data class MatchSummaryUi(
    val record: GameRecordDto,
    val modeType: GameModeType = GameModeType.ONLINE_SUPABASE,
    val aiDifficulty: AiDifficulty? = null,
    val currentUserId: String
) {
    val playerColor: PieceColor
        get() = if (record.whitePlayerId == currentUserId) PieceColor.WHITE else PieceColor.BLACK

    val opponentUsername: String
        get() = when (modeType) {
            GameModeType.PASS_AND_PLAY -> "Local Friend (Pass & Play)"
            GameModeType.AI_OPPONENT -> {
                val diff = aiDifficulty ?: AiDifficulty.fromDbKey(
                    if (playerColor == PieceColor.WHITE) record.blackPlayerId else record.whitePlayerId
                )
                "${diff.title} (${diff.eloRating} ELO)"
            }
            GameModeType.ONLINE_SUPABASE ->
                if (playerColor == PieceColor.WHITE) record.blackUsername else record.whiteUsername
        }

    val opponentAvatarInitials: String
        get() = when (modeType) {
            GameModeType.AI_OPPONENT -> "AI"
            else -> opponentUsername
                .split(" ", "_")
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifEmpty { "OP" }
        }

    val gameStatus: GameStatus
        get() = GameStatus.fromDbValue(record.status)

    val timeControlOption: TimeControlOption
        get() = TimeControlOption.fromDbKey(record.timeControl)

    val isMyTurn: Boolean
        get() {
            if (gameStatus.isTerminal) return false
            if (modeType == GameModeType.PASS_AND_PLAY) return true
            val activeTurn = if (record.turnColor == "b") PieceColor.BLACK else PieceColor.WHITE
            return activeTurn == playerColor
        }

    val sanHistoryList: List<String>
        get() = record.moveListSan.split(" ").filter { it.isNotBlank() }
}
