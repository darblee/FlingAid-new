package com.darblee.flingaid.ui

import com.darblee.flingaid.BallMoveSet
import com.darblee.flingaid.Direction

/**
 * The UI state of game screen
 *
 * It will be managed in a observable flow. Android composable will listen for it.
 *
 *  [Solver State Machine](https://github.com/darblee/FlingAid-new/blob/master/README.md)
 *
 * It uses UDF (Unidirectional Data FLow) and immutable classes to represent the UI state.
 *
 * It will be managed in a observable flow called "StateFlow" Android composable will listen for it.
 *
 * @param _mode The public field is [mode] (read-only access). The current game mode. Possible game
 * mode is defined at [GameMode]
 * @param _movingDirection The public field is [movingDirection] (read-only access). Direction to
 * move the ball from
 * @param _movingChain The public field is [movingChain] (read-only access). Movement chain in the current turn
 */
data class GameUIState(
    private var _mode: GameMode = GameMode.WaitingOnUser,
    private var _movingDirection: Direction = Direction.NO_WINNING_DIRECTION,
    private val _movingChain: BallMoveSet = listOf()
) {
    var mode = _mode
        private set
    var movingDirection = _movingDirection
        private set
    var movingChain = _movingChain
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
