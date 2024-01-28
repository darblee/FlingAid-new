package com.darblee.flingaid.ui

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darblee.flingaid.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class pos(
    val row: Int,
    val col: Int,
)

class GameViewModel : ViewModel() {

    private val _ballPositionList = mutableStateListOf<pos>()

    /*
    Developer's notes
     "internal" means any client inside this module- who see this class will see this member

     SnapshotList:
        - The composables will automatically update when that state changes
        - Values can change
        - Other functions will be notified of the changes
        Ref: https://dev.to/zachklipp/introduction-to-the-compose-snapshot-system-19cn
     */
    internal val ballPositionList: SnapshotStateList<pos> = _ballPositionList

    // Game UI state
    private val _uiState = MutableStateFlow(GameUiState())
    internal var uiState : StateFlow<GameUiState> = _uiState.asStateFlow()
        private set

    private val _foundWinningMove = mutableStateOf<Boolean>(false)

    internal var foundWinningMove : MutableState<Boolean> = _foundWinningMove

    fun reset() {
        _uiState.update {currentState ->
            currentState.copy(
                state = GameState.not_thinking
            )
        }
        _ballPositionList.clear()
    }

    fun count():Int
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

    fun findWinningMove() {
        viewModelScope.launch {

            val winningMove = pos(
                row = 3,
                col = 5
            )

            Log.i(Constants.debugPrefix, "FOund a winning move. Set winning to true")

            _uiState.update { currentState ->
                currentState.copy(
                    winningDirection = Direction.DOWN,
                    winningPosition = winningMove
                )
            }
        }
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
}
