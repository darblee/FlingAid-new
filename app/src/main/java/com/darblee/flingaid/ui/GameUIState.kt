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
     * @property UpdateGameBoardWithNoSolution There is an updated game board, which we know there
     * is a known winnable move for it. NOw waiting for user to make a move. AT the same time, we can
     * inform user there is a hint available
     * @property MoveBall Processing ball movement
     * @property IndicateInvalidMoveByShowingShadowMove Processing the ball shadow movement
     * @property ShowHint Find a hint and now need to show the user with animation
     * @property WonGame One ball remaining. User has won the game
     * @property NoWinnnableMoveWithDialog There is no winning move. It will remain this way until there is a new
     * game or when user undo a move
     */
    sealed class GameMode {
        data object UpdatedGameBoard : GameMode()

        data object UpdateGameBoardWithNoSolution : GameMode()

        data object MoveBall : GameMode() {
            var moveDirection: Direction = Direction.NO_WINNING_DIRECTION
            var movingChain: BallMoveSet = listOf()
        }

        data object IndicateInvalidMoveByShowingShadowMove : GameMode() {
            var shadowMoveDirection: Direction = Direction.NO_WINNING_DIRECTION
            var shadowMovingChain: BallMoveSet = listOf()
        }

        data object ShowHint : GameMode() {
            var shadowMoveDirection: Direction = Direction.NO_WINNING_DIRECTION
            var shadowMovingChain: BallMoveSet = listOf()
        }
        data object WonGame : GameMode()

        data object NoWinnnableMoveWithDialog : GameMode()

        data object  NoWinnableMove : GameMode()
    }

}
