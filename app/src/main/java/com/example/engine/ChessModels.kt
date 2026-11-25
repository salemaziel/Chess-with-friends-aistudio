package com.example.engine

import kotlinx.serialization.Serializable

@Serializable
enum class PieceColor(val fenChar: Char, val displayName: String) {
    WHITE('w', "White"),
    BLACK('b', "Black");

    val opposite: PieceColor
        get() = if (this == WHITE) BLACK else WHITE
}

@Serializable
enum class PieceType(
    val fenCharWhite: Char,
    val fenCharBlack: Char,
    val sanLetter: String,
    val materialValue: Int,
    val unicodeSymbolWhite: String,
    val unicodeSymbolBlack: String
) {
    KING('K', 'k', "K", 0, "♔", "♚"),
    QUEEN('Q', 'q', "Q", 9, "♕", "♛"),
    ROOK('R', 'r', "R", 5, "♖", "♜"),
    BISHOP('B', 'b', "B", 3, "♗", "♝"),
    KNIGHT('N', 'n', "N", 3, "♘", "♞"),
    PAWN('P', 'p', "", 1, "♙", "♟");

    companion object {
        fun fromFenChar(ch: Char): Pair<PieceType, PieceColor>? {
            val upper = ch.uppercaseChar()
            val color = if (ch.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            val type = when (upper) {
                'K' -> KING
                'Q' -> QUEEN
                'R' -> ROOK
                'B' -> BISHOP
                'N' -> KNIGHT
                'P' -> PAWN
                else -> return null
            }
            return type to color
        }
    }
}

@Serializable
data class ChessPiece(
    val type: PieceType,
    val color: PieceColor
) {
    val fenChar: Char
        get() = if (color == PieceColor.WHITE) type.fenCharWhite else type.fenCharBlack

    val unicodeSymbol: String
        get() = if (color == PieceColor.WHITE) type.unicodeSymbolWhite else type.unicodeSymbolBlack
}

/**
 * BoardSquare uses 0-indexed coordinates where:
 * row 0 = Rank 8 (top of standard board, Black's home rank)
 * row 7 = Rank 1 (bottom of standard board, White's home rank)
 * col 0 = File 'a'
 * col 7 = File 'h'
 */
@Serializable
data class BoardSquare(
    val row: Int,
    val col: Int
) {
    val isValid: Boolean
        get() = row in 0..7 && col in 0..7

    val fileChar: Char
        get() = ('a'.code + col).toChar()

    val rankNumber: Int
        get() = 8 - row

    fun toAlgebraic(): String = "${fileChar}${rankNumber}"

    companion object {
        fun fromAlgebraic(algebraic: String?): BoardSquare? {
            if (algebraic == null || algebraic.length != 2 || algebraic == "-") return null
            val col = algebraic[0].lowercaseChar().code - 'a'.code
            val rank = algebraic[1].digitToIntOrNull() ?: return null
            val row = 8 - rank
            val sq = BoardSquare(row, col)
            return if (sq.isValid) sq else null
        }
    }
}

@Serializable
data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
) {
    fun toFen(): String {
        val sb = StringBuilder()
        if (whiteKingside) sb.append('K')
        if (whiteQueenside) sb.append('Q')
        if (blackKingside) sb.append('k')
        if (blackQueenside) sb.append('q')
        return if (sb.isEmpty()) "-" else sb.toString()
    }

    companion object {
        fun fromFen(token: String): CastlingRights {
            if (token == "-") return CastlingRights(false, false, false, false)
            return CastlingRights(
                whiteKingside = token.contains('K'),
                whiteQueenside = token.contains('Q'),
                blackKingside = token.contains('k'),
                blackQueenside = token.contains('q')
            )
        }
    }
}

@Serializable
data class ChessMove(
    val from: BoardSquare,
    val to: BoardSquare,
    val piece: ChessPiece,
    val capturedPiece: ChessPiece? = null,
    val promotion: PieceType? = null,
    val isCastlingKingside: Boolean = false,
    val isCastlingQueenside: Boolean = false,
    val isEnPassant: Boolean = false,
    val san: String = ""
) {
    val isCapture: Boolean
        get() = capturedPiece != null || isEnPassant
}

/**
 * Represents a visual path drawn by the Beginner Mode Visual Engine on the 2D Compose Canvas.
 */
data class BeginnerMovementRoute(
    val origin: BoardSquare,
    val waypoints: List<BoardSquare>,
    val destination: BoardSquare,
    val isCapture: Boolean,
    val isCastling: Boolean = false,
    val isKnightJump: Boolean = false
)

@Serializable
enum class GameStatus(val dbValue: String, val displayLabel: String) {
    ACTIVE("active", "In Progress"),
    CHECKMATE("checkmate", "Checkmate"),
    STALEMATE("stalemate", "Stalemate"),
    DRAW_AGREEMENT("draw_agreement", "Draw by Agreement"),
    DRAW_FIFTY_MOVE("draw_fifty_move", "Draw (50-Move Rule)"),
    RESIGNED("resigned", "Resigned"),
    TIMEOUT("timeout", "Time Out");

    val isTerminal: Boolean
        get() = this != ACTIVE

    companion object {
        fun fromDbValue(value: String): GameStatus {
            return entries.firstOrNull { it.dbValue == value } ?: ACTIVE
        }
    }
}

data class ChessBoardState(
    val grid: List<List<ChessPiece?>>, // 8x8 matrix
    val turn: PieceColor = PieceColor.WHITE,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantTarget: BoardSquare? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val status: GameStatus = GameStatus.ACTIVE,
    val isCheck: Boolean = false,
    val kingInCheckSquare: BoardSquare? = null,
    val lastMoveFrom: BoardSquare? = null,
    val lastMoveTo: BoardSquare? = null,
    val capturedByWhite: List<ChessPiece> = emptyList(), // Black pieces captured by White
    val capturedByBlack: List<ChessPiece> = emptyList(), // White pieces captured by Black
    val materialAdvantageWhite: Int = 0
) {
    fun pieceAt(square: BoardSquare): ChessPiece? {
        if (!square.isValid) return null
        return grid[square.row][square.col]
    }

    fun pieceAt(row: Int, col: Int): ChessPiece? {
        if (row !in 0..7 || col !in 0..7) return null
        return grid[row][col]
    }
}
