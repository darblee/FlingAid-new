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
 * @param winningDirection The direction of the ball to move that will lead to a win. This is also
 * used to do ball animation as animation always move toward winning.
 * A value of "Direction.NO_WINNING_DIRECTION" means there is no winning direction found in the
 * current board layout.
 * @param winningMovingChain In a ball movement, it may involve multiple balls that needs to be moved,
 * The chain is used primarily to animate all the ball movements
 */
data class SolverUiState (
    var thinkingStatus: ThinkingMode = ThinkingMode.Idle,
    var winningPosition: SolverGridPos =  SolverGridPos(-1, -1),
    var winningDirection: Direction = Direction.NO_WINNING_DIRECTION,
    val winningMovingChain: List<MovingRec> = listOf()
) {
    /**
     * There are two different thinking modes:
     * - Active : Actively searching for solution with 'progressLevel' thinking progress
     * - Idle : With one of the following idle mode:
     *      - Waiting on the user
     *      - No Solution is found
     *      - Solution is found
     */
    sealed class ThinkingMode {
        data object Active : ThinkingMode() {
            var progressLevel : Float = 0.0f
        }
        data object Idle : ThinkingMode() {
            var IdleMode = IdleType.WaitingOnUser
            enum class IdleType {
                WaitingOnUser,
                NoSolutionFound,
                SolutionFound
            }
        }
    }
}