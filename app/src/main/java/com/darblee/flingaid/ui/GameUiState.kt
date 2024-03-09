package com.darblee.flingaid.ui


import kotlinx.serialization.Serializable

enum class GameState {
    thinking,
    not_thinking,
}

enum class Direction { NO_WINNING_DIRECTION, UP, DOWN, LEFT, RIGHT, INCOMPLETE }

@Serializable
data class pos(
    val row: Int,
    val col: Int,
    val status : Int = 0  // Not used
)

data class GameUiState (
    var state: GameState = GameState.not_thinking,
    var winningPosition: pos =  pos(-1, -1),
    var foundWinningDirection : Direction = Direction.NO_WINNING_DIRECTION,
    val NeedToDIsplayNoWinnableToastMessage: Boolean = false,
    val thinkingProgress: Float = 0.0F
)