package com.darblee.flingaid.ui

import kotlinx.serialization.Serializable

enum class SolverState {
    Thinking,
    NotThinking,
}

enum class Direction { NO_WINNING_DIRECTION, UP, DOWN, LEFT, RIGHT, INCOMPLETE }

@Serializable
data class SolverGridPos(
    val row: Int,
    val col: Int,
)

// These UI state data needs to be preserved in the event there is a configuration change
// (e.g. screen size change, screen rotation).
//
// It will be managed in a observable flow called "STateFlow"
// Android composable will listen for it.

data class SolverUiState (
    var state: SolverState = SolverState.NotThinking,
    var winningPosition: SolverGridPos =  SolverGridPos(-1, -1),
    var foundWinningDirection : Direction = Direction.NO_WINNING_DIRECTION,
    val needToDisplayNoWinnableToastMessage: Boolean = false,
    val thinkingProgress: Float = 0.0F
)