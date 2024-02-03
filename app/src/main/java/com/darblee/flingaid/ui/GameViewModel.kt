package com.darblee.flingaid.ui

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darblee.flingaid.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private var _ballPositionList = mutableStateListOf<pos>()

    /*
    Developer's notes
     "internal" means any client inside this module- who see this class will see this member

     SnapshotList:
        - The composable will automatically update when that state changes
        - Values can change
        - Other functions will be notified of the changes
        Ref: https://dev.to/zachklipp/introduction-to-the-compose-snapshot-system-19cn
     */
    internal var ballPositionList: SnapshotStateList<pos> = _ballPositionList

    // Game UI state
    private val _uiState = MutableStateFlow(GameUiState())
    internal var uiState : StateFlow<GameUiState> = _uiState.asStateFlow()
        private set

    private val _foundWinningMove = mutableStateOf<Boolean>(false)

    internal var foundWinningMove : MutableState<Boolean> = _foundWinningMove

    init {
        reset()
    }
    
    fun reset() {
        _uiState.update {currentState ->
            currentState.copy(
                state = GameState.not_thinking
            )
        }
        _ballPositionList.clear()
        
        uiState.value.winningDirection = Direction.NO_WINNING_DIRECTION
    }

    fun ballCount():Int
    {
        return _ballPositionList.count()
    }

    /*
        If the position is blank, then place the ball.
        If ball already exist on that location, then remove the ball
     */
    fun toggleBallPosition(pos: pos)
    {
        if (_ballPositionList.contains(pos)) {
            Log.i(Constants.debugPrefix, "Position $pos.x, $pos.y is already taken")
            _ballPositionList.remove(pos)
        } else {
            _ballPositionList.add(pos)
        }
    }

    fun findWinningMove(gameViewModel: GameViewModel) {
        viewModelScope.launch {
            val game = GameEngine()

            game.populateGrid(_ballPositionList)

            val (direction, winningRow, winningCol) = game.foundWinningMove(gameViewModel.ballCount(), 1, 1)

            val winningPos = pos (winningRow,winningCol)
            val winningDir = direction

            Log.i(Constants.debugPrefix, "Found a winning move. row = ${winningPos.row}, col = ${winningPos.col} at direction = ${winningDir.name}")
            val updatedGameState = GameUiState(
                state = GameState.not_thinking,
                winningPosition = winningPos,
                winningDirection = winningDir
            )

            Log.i(Constants.debugPrefix, "P1: ${_ballPositionList.count()} P2: ${ballPositionList.count()}")
            // Need to trigger a recompose
            _uiState.emit(updatedGameState)
        }
    }

    fun ballPositionList() {

    }
    fun winningMoveExist() : Boolean
    {
        return (_uiState.value.winningDirection != Direction.NO_WINNING_DIRECTION)
    }

    fun printGrid()
    {
        for (currentPos in _ballPositionList) {
            Log.i(Constants.debugPrefix, "$currentPos")
        }
    }

    fun makeWinningMove(uiState: GameUiState) {
        val game = GameEngine()
        game.populateGrid(_ballPositionList)

        var targetRow: Int
        var targetCol: Int

        if (uiState.winningDirection == Direction.UP) {
            targetRow = game.findTargetRowOnMoveUp(uiState.winningPosition.row, uiState.winningPosition.col)
            game.moveUp(uiState.winningPosition.row, targetRow, uiState.winningPosition.col)
            _ballPositionList.clear()
            _ballPositionList = game.updateBallList()
        }

        if (uiState.winningDirection == Direction.DOWN) {
            targetRow = game.findTargetRowOnMoveDown(uiState.winningPosition.row, uiState.winningPosition.col)
            game.moveDown(uiState.winningPosition.row, targetRow, uiState.winningPosition.col)
            _ballPositionList.clear()
            _ballPositionList = game.updateBallList()
        }

        if (uiState.winningDirection == Direction.RIGHT) {
            targetCol = game.findTargetColOnMoveRight(uiState.winningPosition.row, uiState.winningPosition.col)
            game.moveRight(uiState.winningPosition.col, targetCol, uiState.winningPosition.row)
            _ballPositionList.clear()
            _ballPositionList = game.updateBallList()
        }

        if (uiState.winningDirection == Direction.LEFT) {
            targetCol = game.findTargetColOnMoveLeft(uiState.winningPosition.row, uiState.winningPosition.col)
            game.moveLeft(uiState.winningPosition.col, targetCol, uiState.winningPosition.row)
            _ballPositionList.clear()
            _ballPositionList = game.updateBallList()
        }

        ballPositionList = _ballPositionList

        // Erase the arrow
        _uiState.value.winningDirection = Direction.NO_WINNING_DIRECTION
    }

}
