package com.darblee.flingaid.ui

enum class GameState {
    thinking,
    not_thinking,
}

enum class Direction { NO_WINNING_DIRECTION, UP, DOWN, LEFT, RIGHT, INCOMPLETE }

data class pos(
    val row: Int,
    val col: Int,
)

data class GameUiState (
    val state: GameState = GameState.not_thinking,
    var winningPosition: pos =  pos(0, 0),
    var winningDirection : Direction = Direction.NO_WINNING_DIRECTION
)