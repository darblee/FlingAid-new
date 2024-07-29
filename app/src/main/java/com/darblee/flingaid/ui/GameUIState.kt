package com.darblee.flingaid.ui

import com.darblee.flingaid.Direction

/**
 * The state of game  UI state
 *
 * It will be managed in a observable flow. Android composable will listen for it.
 *
 *  [Solver State Machine](https://github.com/darblee/FlingAid-new/blob/master/README.md)
 *
 * @param _mode The public field is [mode] (read-only access). The current game mode. Possible game
 * mode is defined at [GameMode]
 * @param _movingDirection The public field is [movingDirection] (read-only access). Direction to
 * move the ball from
 * @param _movingChain The public field is [movingChain] (read-only access). Movement chain in the current turn
 */
data class GameUIState(
    private var _mode : GameMode = GameMode.WaitingOnUser,
    private var _movingDirection: Direction = Direction.NO_WINNING_DIRECTION,
    private val _movingChain: List<MovingRec> = listOf()
) {
    // Ensure private setter for all fields
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
     * @property ShowShadowMovement Processing the ball shadow movement
     * @property LookingForHint Computer to look for solution and provide it to user
     * @property WonGame One ball remaining. User has won the game
     * @property NoAvailableMove There is no available move.
     */
    sealed class GameMode {
        data object WaitingOnUser : GameMode()
        data object MoveBall : GameMode()
        data object ShowShadowMovement : GameMode()
        data object LookingForHint : GameMode()
        data object WonGame : GameMode()
        data object NoAvailableMove : GameMode()
    }

}
