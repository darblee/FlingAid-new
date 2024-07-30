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
 * The UI state of the solver screen
 *
 * These UI state data needs to be preserved in the event there is a configuration change
 * (e.g. screen size change, screen rotation).
 *
 * It will be managed in a observable flow called "StateFlow" Android composable will listen for it.
 *
 * [Solver State Machine](https://github.com/darblee/FlingAid-new/blob/master/README.md)
 *
 * @param _mode The public field is [mode] (read-only access). The current game mode. Possible game
 * mode is defined at [SolverMode]
 * @param _thinkingProgressLevel  The public field is [thinkingProgressLevel] (read-only access).
 * Percentage of thinking process
 * @param _winningDirection The public field is [winningDirection] (read-only access). Direction
 * on the current to move that will lead toward a win
 * @param _winningMovingChain The public field is [winningMovingChain] (read-only). Ball movements
 * is tracked in list of multiple move for each ball in the chain.
 */
data class SolverUiState(
    private var _mode : SolverMode = SolverMode.NoMoveAvailable,
    var _thinkingProgressLevel: Float = 0.0f,
    var _winningDirection: Direction = Direction.NO_WINNING_DIRECTION,
    val _winningMovingChain: List<MovingRec> = listOf()
) {
    var mode = _mode
        private set
    var thinkingProgressLevel = _thinkingProgressLevel
        private set
    var winningDirection = _winningDirection
        private set
    var winningMovingChain = _winningMovingChain
        private set

    /**
     * Various modes for UI solver game
     *
     * @property Thinking Computer to look for solution
     * @property ReadyToFindSolution Waiting for user action.
     * @property ReadyToMove Found a solution. Now waiting for user action
     * @property AnnounceNoPossibleSolution Could not find a solution. Need to inform user there is no solution
     * before going back to [ReadyToFindSolution]
     */
    sealed class SolverMode {
        data object NoMoveAvailable : SolverMode()   // Equivalent to game won condition
        data object Thinking : SolverMode()
        data object ReadyToFindSolution : SolverMode()
        data object ReadyToMove: SolverMode()
        data object AnnounceNoPossibleSolution: SolverMode()
        data object AnnounceVictory : SolverMode()
        data object MoveBall : SolverMode()
    }
}