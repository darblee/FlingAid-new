package com.darblee.flingaid.ui

import com.darblee.flingaid.Direction

/**
 * The state of game  UI state
 *
 * It will be managed in a observable flow. Android composable will listen for it.
 *
 *  [Solver State Machine](https://github.com/darblee/FlingAid-new/blob/master/README.md)
 *
 * @param mode The state of the game
 * @param movingDirection Direction to move the ball from
 * @param movingChain Movement chain in the current turn
 */
data class GameUIState(
    var mode: GameMode = GameMode.WaitingOnUser,
    var movingDirection: Direction = Direction.NO_WINNING_DIRECTION,
    val movingChain: List<MovingRec> = listOf()
) {
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
