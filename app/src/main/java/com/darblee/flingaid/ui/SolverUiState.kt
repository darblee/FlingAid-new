package com.darblee.flingaid.ui

import com.darblee.flingaid.BallMoveSet
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
 * It uses UDF (Unidirectional Data FLow) and immutable classes to represent the UI state.
 *
 * It will be managed in a observable flow called "StateFlow" Android composable will listen for it.
 *
 * [Solver State Machine](https://github.com/darblee/FlingAid-new/blob/master/README.md)
 *
 * @param _mode The public field is [mode] (read-only access). The current game mode. Possible game
 * mode is defined at [SolverMode]
 * @param _recomposeFlag Flag used to trigger a recomposition. A change in item's property
 * (e.g. progress field) did not trigger a recomposition. (Could this be a Jetpack compose bug?)
 * So this field is used to workaround this bug by manually trigger recompose by changing the
 * value of [_recomposeFlag]
 */
data class SolverUiState(
    private var _mode : SolverMode = SolverMode.NoMoveAvailable,
    var _recomposeFlag: Boolean = false
) {
    var mode = _mode
        private set

    /**
     * Various modes for UI solver game
     *
     * @property NoMoveAvailable No more move is available
     * @property Thinking Computer to look for solution
     * @property ReadyToFindSolution Waiting for user action.
     * @property ReadyToMove Found a solution. Now waiting for user action
     * @property AnnounceNoPossibleSolution Could not find a solution. Need to inform user there is no solution
     * before going back to [ReadyToFindSolution]
     * @property AnnounceVictory Need to announce the victory message
     * @property MoveBall Process moving the ball
     */
    sealed class SolverMode {
        data object NoMoveAvailable : SolverMode()   // Equivalent to game won condition
        data object Thinking : SolverMode() {
            var progress : Float = 0.0f
        }
        data object ReadyToFindSolution : SolverMode()
        data object ReadyToMove: SolverMode() {
            var winningDirectionPreview : Direction = Direction.NO_WINNING_DIRECTION
            var winingMovingChainPreview : BallMoveSet = listOf()
        }
        data object AnnounceNoPossibleSolution: SolverMode()
        data object AnnounceVictory : SolverMode()
        data object MoveBall : SolverMode() {
            var winningDirMoveBall : Direction = Direction.NO_WINNING_DIRECTION
            var winingMovingChainMoveBall : BallMoveSet = listOf()
        }
    }
}