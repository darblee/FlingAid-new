package com.darblee.flingaid.ui

import com.darblee.flingaid.BallMoveSet
import com.darblee.flingaid.Direction

/**
 * The UI state of game screen
 *
 * For details, see [GameViewModel]
 *
 * @param _mode The public field is [mode] (read-only access). The current game mode. Possible game
 * mode is defined at [GameMode]
 */
data class GameUIState(
    private var _mode: GameMode = GameMode.WaitingOnUser,
) {
    var mode = _mode
        private set

    /**
     * Various game modes for UI state
     *
     * @property WaitingOnUser Waiting for user next move
     * @property MoveBall Processing ball movement
     * @property IndicateInvalidMoveByShowingShadowMove Processing the ball shadow movement
     * @property LookingForHint Computer to look for solution and provide it to user
     * @property WonGame One ball remaining. User has won the game
     * @property NoAvailableMove There is no available move.
     */
    sealed class GameMode {
        data object WaitingOnUser : GameMode()
        data object MoveBall : GameMode() {
            var moveDirection : Direction = Direction.NO_WINNING_DIRECTION
            var movingChain : BallMoveSet = listOf()
        }
        data object IndicateInvalidMoveByShowingShadowMove : GameMode() {
            var shadowMoveDirection : Direction = Direction.NO_WINNING_DIRECTION
            var shadowMovingChain : BallMoveSet = listOf()
        }
        data object LookingForHint : GameMode()
        data object WonGame : GameMode()
        data object NoAvailableMove : GameMode()
    }

}
