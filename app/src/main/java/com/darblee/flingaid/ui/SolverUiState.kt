package com.darblee.flingaid.ui

import com.darblee.flingaid.Direction
import com.darblee.flingaid.utilities.Pos
import kotlinx.serialization.Serializable

/**
 * Moving record
 *
 *  @property pos: where to move from
 *  @property distance: how far to move
 */
@Serializable
data class MovingRec(
    val pos: Pos,
    val distance: Int
)

/**
 * These UI state data needs to be preserved in the event there is a configuration change
 * (e.g. screen size change, screen rotation).
 *
 * It will be managed in a observable flow called "StateFlow" Android composable will listen for it.
 *
 * [Solver State Machine](https://github.com/darblee/FlingAid-new/blob/master/README.md)
 *
 * @param mode Solver game mode. It is whether it is in active thinking mode or one of
 * possible idle state
 * @param winningDirection The direction of the ball to move that will lead to a win. This is also
 * used to do ball animation as animation always move toward winning.
 * A value of "Direction.NO_WINNING_DIRECTION" means there is no winning direction found in the
 * current board layout.
 * @param winningMovingChain The first element in the chain is the position of ball to move that
 * will lead to a win.In a ball movement, it may involve multiple balls that needs to be moved,
 * The chain is used primarily to animate all the ball movements
 */
data class SolverUiState(
    var mode : SolverMode = SolverMode.Idle,
    var thinkingProgressLevel: Float = 0.0f,
    var winningDirection: Direction = Direction.NO_WINNING_DIRECTION,
    val winningMovingChain: List<MovingRec> = listOf()
) {
    /**
     * Various modes for UI solver game
     *
     * @property Thinking Computer to look for solution
     * @property Idle Waiting for user action.
     * @property IdleFoundSolution Found a solution. Now waiting for user action
     * @property IdleNoSolution Could not find a solution. Need to inform user there is no solution
     * before going back to [Idle]
     */
    sealed class SolverMode {
        data object Thinking : SolverMode()
        data object Idle : SolverMode()
        data object IdleFoundSolution: SolverMode()
        data object IdleNoSolution: SolverMode()
    }
}