package com.darblee.flingaid.ui

import com.darblee.flingaid.Direction
import kotlinx.serialization.Serializable

/**
 * Position on the game board
 * - row
 * - column
 */
@Serializable
data class SolverGridPos(
    val row: Int,
    val col: Int,
)

/**
 * Moving record.
 * - pos: where to move from
 * - distance: how far to move
 */
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
 * [Solver State Machine](https://github.com/darblee/FlingAid-new/blob/master/README.md)
 *
 * @param solverGameState Solver game state. It is whether it is in active thinking mode or one of
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
    var solverGameState : SolverGameMode = SolverGameMode.Idle,
    var thinkingProgressLevel: Float = 0.0f,
    var winningDirection: Direction = Direction.NO_WINNING_DIRECTION,
    val winningMovingChain: List<MovingRec> = listOf()
) {
    sealed class SolverGameMode {
        data object Thinking : SolverGameMode()
        data object Idle : SolverGameMode()
        data object IdleFoundSolution: SolverGameMode()
        data object IdleAnnounceNoPossibleWin: SolverGameMode()
    }
}