package com.darblee.flingaid.ui


import kotlinx.serialization.Serializable

enum class GameState {
    Thinking,
    NotThinking,
}

enum class Direction { NO_WINNING_DIRECTION, UP, DOWN, LEFT, RIGHT, INCOMPLETE }

@Serializable
data class Pos(
    val row: Int,
    val col: Int,
)

data class GameUiState (
    var state: GameState = GameState.NotThinking,
    var winningPosition: Pos =  Pos(-1, -1),
    var foundWinningDirection : Direction = Direction.NO_WINNING_DIRECTION,
    val needToDisplayNoWinnableToastMessage: Boolean = false,
    val thinkingProgress: Float = 0.0F
)