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
    private var _mode: GameMode = GameMode.UpdatedGameBoard,
) {
    var mode = _mode
        private set

    /**
     * Various game modes for UI state
     *
     * @property UpdatedGameBoard There is an updated (or new) game board. Now waiting for user to
     * make a move
     * @property MoveBall Processing ball movement
     * @property IndicateInvalidMoveByShowingShadowMove Processing the ball shadow movement
     * @property LookingForHint Computer to look for solution and provide it to user
     * @property ShowHint Find a hint and now need to show the user with animation
     * @property WonGame One ball remaining. User has won the game
     * @property NoWinnnableMoveWithDiaglog There is no winning move. It will remain this way until there is a new
     * game or when user undo a move
     */
    sealed class GameMode {
        data object UpdatedGameBoard : GameMode()
        data object MoveBall : GameMode() {
            var moveDirection: Direction = Direction.NO_WINNING_DIRECTION
            var movingChain: BallMoveSet = listOf()
        }

        data object IndicateInvalidMoveByShowingShadowMove : GameMode() {
            var shadowMoveDirection: Direction = Direction.NO_WINNING_DIRECTION
            var shadowMovingChain: BallMoveSet = listOf()
        }

        data object LookingForHint : GameMode()
        data object ShowHint : GameMode() {
            var shadowMoveDirection: Direction = Direction.NO_WINNING_DIRECTION
            var shadowMovingChain: BallMoveSet = listOf()
        }
        data object WonGame : GameMode()
        data object NoWinnnableMoveWithDiaglog : GameMode()
        data object  NoWinnableMove : GameMode()
    }

}
