package com.darblee.flingaid.ui

import com.darblee.flingaid.Direction

enum class GameState {
    Idle,
    IdleFoundSolution,
    Won,
    Playing,
    MoveBall,
    ShowShadowMovement, // This is used to inform user he/she he made an invalid move by
                        // moving a ball that did not bump any ball
}

/**
 * The state of game  UI state
 *
 * It will be managed in a observable flow. Android composable will listen for it.
 *
 * @param state The state of the game
 * @param moveFromRow Row position to move the ball from
 * @param moveFromCol Column position to move the ball from
 * @param movingDirection Direction to move the ball from
 * @param movingChain Movement chain in the current turn
 */
data class GameUIState(
    var state: GameState = GameState.Playing,
    var moveFromRow: Int = 0,
    var moveFromCol: Int = 0,
    var movingDirection: Direction = Direction.NO_WINNING_DIRECTION,
    val movingChain: List<MovingRec> = listOf()
)
