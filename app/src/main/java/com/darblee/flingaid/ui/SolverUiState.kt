package com.darblee.flingaid.ui

import com.darblee.flingaid.Direction
import kotlinx.serialization.Serializable

@Serializable
data class SolverGridPos(
    val row: Int,
    val col: Int,
)

@Serializable
data class MovingRec(
    val pos: SolverGridPos,
    val distance: Int
)

/**
 * These UI state data needs to be preserved in the event there is a configuration change
 * (e.g. screen size change, screen rotation).
 *
 * It will be managed in a observable flow called "StateFlow" Android composable will listen for it.
 *
 * @param thinkingStatus Whether it is in active thinking mode or just idle
 * @param winningPosition The position of ball to move that will lead to a win
 * @param foundWinningDirection The direction of the ball to move that will lead to a win
 * @param needToDisplayNoWinnableToastMessage Indicate whether we need to send a toast message
 * indicating there is no winnable move in the current ball positions
 * @param movingDirection While moving the ball, this is the direction
 * @param movingChain In a ball movement, it may involve multiple balls that needs to be moved,
 */
data class SolverUiState (
    var thinkingStatus: ThinkingMode = ThinkingMode.Idle,
    var winningPosition: SolverGridPos =  SolverGridPos(-1, -1),
    var foundWinningDirection : Direction = Direction.NO_WINNING_DIRECTION,
    val needToDisplayNoWinnableToastMessage: Boolean = false,
    val movingDirection: Direction = Direction.INCOMPLETE,
    val movingChain: List<MovingRec> = listOf()
) {
    sealed class ThinkingMode {
        data object Active : ThinkingMode() {
            var progressLevel : Float = 0.0f
        }
        data object Idle : ThinkingMode()
    }
}