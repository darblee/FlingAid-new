package com.darblee.flingaid.ui

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darblee.flingaid.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CyclicBarrier

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

    // Game UI state
    private val _uiState = MutableStateFlow(GameUiState())

    /*
        Developer's note:
        StateFlow is a data holder observable flow that emits the current and new state updates.
        Its value property reflects the current state value. To update state and send it to the flow,
        assign a new value to the value property of the MutableStateFlow class.

        In Android, StateFlow works well with classes that must maintain an observable immutable state.
        A StateFlow can be exposed from the GameUiState so that the composables can listen for UI state
        updates and make the screen state survive configuration changes.

        In the GameViewModel class, add the following _uiState property.
     */
    internal var uiState : StateFlow<GameUiState> = _uiState.asStateFlow()
        private set

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
        
        uiState.value.foundWinningDirection = Direction.NO_WINNING_DIRECTION
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

        Log.i(Constants.debugPrefix, "Toggle ball. Need to reset the thinking")

        _uiState.update {currentState ->
            currentState.copy(
                state = GameState.not_thinking,
                foundWinningDirection = Direction.NO_WINNING_DIRECTION
            )
        }
    }

    fun findWinningMove(gameViewModel: GameViewModel) {
        _uiState.update {currentState ->
            currentState.copy(
                state = GameState.thinking
            )
        }
        viewModelScope.launch {
            gProcessing = true
            gOverallProgress = 0
            val gWINNING_DIRECTION = Direction.NO_WINNING_DIRECTION
            val gTotalBallCount = gameViewModel.ballCount()

            task1_Direction = Direction.INCOMPLETE
            task2_Direction = Direction.INCOMPLETE

            lateinit var cyclicBarrier : CyclicBarrier

            if (gTotalBallCount > 100) {
                gMultipleThread = true
                cyclicBarrier = CyclicBarrier(2) {
                    Log.i(Constants.debugPrefix, "Reached a converged point between 2 parallel tasks")
                    // TO DO
                }
            } else {
                gMultipleThread = false
                cyclicBarrier = CyclicBarrier(1) {
                    Log.i(Constants.debugPrefix, "Reached a converged point. One thread")

                    if ((task1_Direction != Direction.NO_WINNING_DIRECTION) && (task1_Direction != Direction.INCOMPLETE)) {
                        // Task1 has the winning move
                        Log.i(Constants.debugPrefix, "Task #1 has winning move")
                        val winningPos = pos(task1WinningRow, task1WinningCol)
                        val winningDir = task1_Direction
                        _uiState.update {currentState ->
                            currentState.copy(
                                state = GameState.not_thinking,
                                winningPosition = winningPos,
                                foundWinningDirection = winningDir
                            )
                        }
                    } else {
                        Log.i(
                            Constants.debugPrefix,
                            "Task #1 does not have winning result. Now check on task #2 outcome"
                        )
                        if ((task2_Direction != Direction.NO_WINNING_DIRECTION) && (task2_Direction != Direction.INCOMPLETE)) {
                            Log.i(Constants.debugPrefix, "Task #2 has winning move")
                            // Task2 has the winning move
                            val winningPos = pos(task2WinningRow, task2WinningCol)
                            val winningDir = task2_Direction
                            _uiState.update {currentState ->
                                currentState.copy(
                                    state = GameState.not_thinking,
                                    winningPosition = winningPos,
                                    foundWinningDirection = winningDir
                                )
                            }
                        } else {
                            Log.i(Constants.debugPrefix, "Neither Task #1 nor Task #2 has winning result")
                            _uiState.update {currentState ->
                                currentState.copy(
                                    state = GameState.not_thinking,
                                    winningPosition = pos(-1, -1),
                                    foundWinningDirection = Direction.NO_WINNING_DIRECTION,
                                    NeedToDIsplayNoWinnableToastMessage = true
                                )
                            }
                        }
                    }
                }
            }

            task1 = Thread {
                processTask1(gTotalBallCount)
                Log.i("${Constants.debugPrefix} Task 1", "Task #1 is completed. Now waiting for all threads to complete.")
                cyclicBarrier.await()
            }
            task2 = Thread {
                processTask2(gTotalBallCount)
                Log.i("${Constants.debugPrefix}: Task 2", "Task #2 is completed. Now waiting for all threads to complete.")
                cyclicBarrier.await()
            }

            task1.start()

            if (gMultipleThread) {
                task2.start()
            }
        }
    }

    fun NoNeedToDisplayNoWinnableToastMessage() {
        _uiState.update {currentState ->
            currentState.copy(
                NeedToDIsplayNoWinnableToastMessage = false
            )
        }
    }

    lateinit var task1_Direction: Direction
    lateinit var task2_Direction: Direction

    // Volatile is used to ensure each thread is reading exact same value
    // (from memory instead of from cpu-cache)
    @Volatile var gProcessing = false
    @Volatile var gOverallProgress = 0
    @Volatile var gMultipleThread = false


    private lateinit var task1 : Thread
    private lateinit var task2 : Thread
    private  var task1WinningRow = -1
    private  var task1WinningCol = -1
    private  var task2WinningRow = -1
    private  var task2WinningCol = -1
    private fun processTask1(totalBallCnt :  Int) {

        try {
            Log.i("${Constants.debugPrefix} Task 1", "Task 1 has started")
            val game1 = GameEngine()
            game1.populateGrid(_ballPositionList)

            val (direction, finalRow, finalCol) = game1.foundWinningMove(
                totalBallCnt, 1, 1)

            Log.i("${Constants.debugPrefix} Finish Task #1", "Conclusion: Direction = $direction, row=$finalRow, col=$finalCol")

            task1_Direction = direction

            when (direction) {

                Direction.INCOMPLETE -> {
                    Log.i(Constants.debugPrefix, "Task #1 got incomplete. It expect task2 has deterministic result")
                }

                Direction.NO_WINNING_DIRECTION -> {
                    Log.i(Constants.debugPrefix, "Task #1 concluded there is no winning move")
                    if (gMultipleThread) task2.interrupt()
                }

                else -> {
                    // Task #1 got winning move
                    task1_Direction = direction
                    task1WinningRow = finalRow
                    task1WinningCol = finalCol

                    if (gMultipleThread) task2.interrupt()
                }
            }
        } catch (e: InterruptedException) {
            Log.i("${Constants.debugPrefix} 1", "Interruption detected")
        }
    }

    private fun processTask2(totalBallCnt : Int) {

        try {
            Log.i("${Constants.debugPrefix} Task 2", "Task 2 has started")
            val game2 = GameEngine()
            game2.populateGrid(_ballPositionList)
            task2_Direction = Direction.INCOMPLETE
            val (direction, finalRow, finalCol) = game2.foundWinningMove(
                totalBallCnt, 1, -1)

            Log.i("${Constants.debugPrefix} Finish Task #2", "Conclusion: Direction = $direction, row=$finalRow, col=$finalCol")

            task2_Direction = direction

            when (direction) {

                Direction.INCOMPLETE -> {
                    Log.i(Constants.debugPrefix, "Task #2 got incomplete. It expect task2 has deterministic result")
                }

                Direction.NO_WINNING_DIRECTION -> {
                    Log.i(Constants.debugPrefix, "Task #2 concluded there is no winning move")
                    task1.interrupt()
                }

                else -> {
                    // Task #2 got the winning move
                    task2_Direction = direction
                    task2WinningRow = finalRow
                    task2WinningCol = finalCol

                    task1.interrupt()
                }
            }
        } catch (e: InterruptedException) {
            Log.i("${Constants.debugPrefix} Task 2", "Interruption detected")
        }
    }


    fun ballPositionList() : SnapshotStateList<pos> {
        return (_ballPositionList)
    }

    fun foundWinnableMove() : Boolean
    {
        return (_uiState.value.foundWinningDirection != Direction.NO_WINNING_DIRECTION)
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

        if (uiState.foundWinningDirection == Direction.UP) {
            targetRow = game.findTargetRowOnMoveUp(uiState.winningPosition.row, uiState.winningPosition.col)
            game.moveUp(uiState.winningPosition.row, targetRow, uiState.winningPosition.col)
            _ballPositionList.clear()
            _ballPositionList = game.updateBallList()
        }

        if (uiState.foundWinningDirection == Direction.DOWN) {
            targetRow = game.findTargetRowOnMoveDown(uiState.winningPosition.row, uiState.winningPosition.col)
            game.moveDown(uiState.winningPosition.row, targetRow, uiState.winningPosition.col)
            _ballPositionList.clear()
            _ballPositionList = game.updateBallList()
        }

        if (uiState.foundWinningDirection == Direction.RIGHT) {
            targetCol = game.findTargetColOnMoveRight(uiState.winningPosition.row, uiState.winningPosition.col)
            game.moveRight(uiState.winningPosition.col, targetCol, uiState.winningPosition.row)
            _ballPositionList.clear()
            _ballPositionList = game.updateBallList()
        }

        if (uiState.foundWinningDirection == Direction.LEFT) {
            targetCol = game.findTargetColOnMoveLeft(uiState.winningPosition.row, uiState.winningPosition.col)
            game.moveLeft(uiState.winningPosition.col, targetCol, uiState.winningPosition.row)
            _ballPositionList.clear()
            _ballPositionList = game.updateBallList()
        }

        // Erase the arrow
        _uiState.value.foundWinningDirection = Direction.NO_WINNING_DIRECTION
    }

}
