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
 * @param moveFromRow Row position to move the ball from
 * @param moveFromCol Column position to move the ball from
 * @param movingDirection Direction to move the ball from
 * @param movingChain Movement chain in the current turn
 */
data class GameUIState(
    var mode: GameMode = GameMode.WaitingOnUser,
    var moveFromRow: Int = 0,
    var moveFromCol: Int = 0,
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
     */
    sealed class GameMode {
        data object WaitingOnUser : GameMode()
        data object MoveBall : GameMode()
        data object ShowShadowMovement : GameMode()
        data object LookingForHint : GameMode()
    }

}
