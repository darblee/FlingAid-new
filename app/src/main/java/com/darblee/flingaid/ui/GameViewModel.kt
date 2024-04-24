package com.darblee.flingaid.ui

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darblee.flingaid.Global
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.concurrent.CyclicBarrier

var gWINNING_DIRECTION_from_tasks = Direction.NO_WINNING_DIRECTION
var gTotalBallInCurrentMove = 0

class GameViewModel : ViewModel() {

    private var _ballPositionList = mutableStateListOf<Pos>()

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
        A StateFlow can be exposed from the GameUiState so that the composable can listen for UI state
        updates and make the screen state survive configuration changes.

        In the GameViewModel class, add the following _uiState property.
     */
    internal var uiState : StateFlow<GameUiState> = _uiState.asStateFlow()
        private set

    init {
        val file : File? = null
        reset(file)
    }

    fun saveBallPositions(file: File) {
        val format = Json { prettyPrint = true }
        val ballList = mutableListOf<Pos>()

        for (currentPos in _ballPositionList) {
            ballList += currentPos
        }
        val output = format.encodeToString(ballList)

        try {
            val writer = FileWriter(file)
            writer.write(output)
            writer.close()
        } catch (e:Exception) {
            Log.i(Global.debugPrefix, "${e.message}" )
        }
    }

    fun loadBallPositions(file: File) {
        try {
            val reader = FileReader(file)
            val data = reader.readText()
            reader.close()

            val list = Json.decodeFromString<List<Pos>>(data)

            _ballPositionList.clear()
            for (pos in list) {
                _ballPositionList.add(pos)
            }
        } catch (e: Exception) {
            Log.i(Global.debugPrefix, "An error occurred while reading the file: ${e.message}")
        }
    }

    fun getThinkingStatus(): GameState
    {
        return (_uiState.value.state)
    }

    fun reset(file: File?) {
        _uiState.update {currentState ->
            currentState.copy(
                state = GameState.NotThinking
            )
        }
        _ballPositionList.clear()
        file?.delete()
        
        uiState.value.foundWinningDirection = Direction.NO_WINNING_DIRECTION
    }

    fun ballCount():Int
    {
        return _ballPositionList.count()
    }


    // If the position is blank, then place the ball.
    // If ball already exist on that location, then remove the ball
    fun toggleBallPosition(pos: Pos)
    {
        if (_ballPositionList.contains(pos)) {
            _ballPositionList.remove(pos)
        } else {
            _ballPositionList.add(pos)
        }

        _uiState.update {currentState ->
            currentState.copy(
                state = GameState.NotThinking,
                foundWinningDirection = Direction.NO_WINNING_DIRECTION
            )
        }
    }

    // Volatile is used to ensure each thread is reading exact same value
    // (from memory instead of from cpu-cache)
    @Volatile var gMultipleThread = false

    private lateinit var task1 : Thread
    private lateinit var task2 : Thread
    private lateinit var task3 : Thread

    private var task1WinningRow = -1
    private var task1WinningCol = -1
    private var task2WinningRow = -1
    private var task2WinningCol = -1

    fun findWinningMove(gameViewModel: GameViewModel) {
        _uiState.update {currentState ->
            currentState.copy(
                state = GameState.Thinking
            )
        }
        viewModelScope.launch {
            Global.totalProcessCount = (((gTotalBallInCurrentMove - 1) * 4) * (gTotalBallInCurrentMove * 4)).toFloat()
            gWINNING_DIRECTION_from_tasks = Direction.NO_WINNING_DIRECTION
            val gTotalBallCount = gameViewModel.ballCount()

            Global.task1_WinningDirection = Direction.INCOMPLETE
            Global.task2_WinningDirection = Direction.INCOMPLETE

            Global.ThinkingProgress = 0
            gTotalBallInCurrentMove = gameViewModel.ballCount()

            lateinit var cyclicBarrier : CyclicBarrier

            if (gTotalBallCount > 12) {
                gMultipleThread = true
                Global.totalProcessCount = (((gTotalBallInCurrentMove - 1) * 4) * (gTotalBallInCurrentMove * 4)).toFloat()
                cyclicBarrier = CyclicBarrier(2) {
                    Log.i(Global.debugPrefix, "Reached a converged point between 2 parallel tasks")
                    displayResult()
                }
            } else {
                gMultipleThread = false
                cyclicBarrier = CyclicBarrier(1) {
                    Log.i(Global.debugPrefix, "Reached a converged point. One thread")
                    displayResult()
                }
            }

            task1 = Thread {
                processTask1(gTotalBallCount)
                Log.i("${Global.debugPrefix} Task 1", "Task #1 is completed. Now waiting for all threads to complete.")
                cyclicBarrier.await()
            }
            task2 = Thread {
                processTask2(gTotalBallCount)
                Log.i("${Global.debugPrefix}: Task 2", "Task #2 is completed. Now waiting for all threads to complete.")
                cyclicBarrier.await()
            }

            task3 = Thread {
                showProcessingActivity()
            }

            task3.start()
            task1.start()

            if (gMultipleThread) {
                task2.start()
            }
        }
    }

    private fun displayResult() {
        Log.i(Global.debugPrefix, "DisplayResult after processing....")
        uiState.value.state = GameState.NotThinking

        if ((Global.task1_WinningDirection != Direction.NO_WINNING_DIRECTION) &&
            (Global.task1_WinningDirection != Direction.INCOMPLETE)) {
            // Task1 has the winning move
            Log.i(Global.debugPrefix,
                "Task #1 has winning move with direction : ${Global.task1_WinningDirection}")
            val winningPos = Pos(task1WinningRow, task1WinningCol)
            val winningDir = Global.task1_WinningDirection

            gWINNING_DIRECTION_from_tasks = Global.task1_WinningDirection

            _uiState.update {currentState ->
                currentState.copy(
                    state = GameState.NotThinking,
                    winningPosition = winningPos,
                    foundWinningDirection = winningDir
                )
            }
        } else {
            Log.i(
                Global.debugPrefix,
                "Task #1 does not have winning result. Now check on task #2 outcome"
            )
            if ((Global.task2_WinningDirection != Direction.NO_WINNING_DIRECTION) &&
                (Global.task2_WinningDirection != Direction.INCOMPLETE)) {
                Log.i(Global.debugPrefix,
                    "Task #2 has winning move with direction: ${Global.task2_WinningDirection}")
                // Task2 has the winning move
                val winningPos = Pos(task2WinningRow, task2WinningCol)
                val winningDir = Global.task2_WinningDirection
                gWINNING_DIRECTION_from_tasks = Global.task2_WinningDirection
                _uiState.update {currentState ->
                    currentState.copy(
                        state = GameState.NotThinking,
                        winningPosition = winningPos,
                        foundWinningDirection = winningDir
                    )
                }
            } else {
                Log.i(Global.debugPrefix, "Neither Task #1 nor Task #2 has winning result")
                gWINNING_DIRECTION_from_tasks = Direction.NO_WINNING_DIRECTION
                _uiState.update {currentState ->
                    currentState.copy(
                        state = GameState.NotThinking,
                        winningPosition = Pos(-1, -1),
                        foundWinningDirection = Direction.NO_WINNING_DIRECTION,
                        needToDisplayNoWinnableToastMessage = true
                    )
                }
            }
        }
    }

    fun noNeedToDisplayNoWinnableToastMessage() {
        _uiState.update {currentState ->
            currentState.copy(
                needToDisplayNoWinnableToastMessage = false
            )
        }
    }

    private fun processTask1(totalBallCnt :  Int) {

        try {
            Log.i("${Global.debugPrefix} Task 1", "Task 1 has started")
            val game1 = GameEngine()
            game1.populateGrid(_ballPositionList)

            val (direction, finalRow, finalCol) = game1.foundWinningMove(
                totalBallCnt, 1, 1)

            Log.i("${Global.debugPrefix} Finish Task #1",
                "Conclusion: Direction = $direction, row=$finalRow, col=$finalCol")

            Global.task1_WinningDirection = direction

            when (direction) {

                Direction.INCOMPLETE -> {
                    Log.i(Global.debugPrefix,
                        "Task #1 got incomplete. It expect task2 has deterministic result")
                }

                Direction.NO_WINNING_DIRECTION -> {
                    Log.i(Global.debugPrefix, "Task #1 concluded there is no winning move")
                    if (gMultipleThread) task2.interrupt()
                }

                else -> {
                    // Task #1 got winning move
                    Global.task1_WinningDirection = direction
                    task1WinningRow = finalRow
                    task1WinningCol = finalCol
                    Global.task1_WinningDirection = direction

                    Log.i(Global.debugPrefix, "Attempting to interrupt task #2")
                    if (gMultipleThread) task2.interrupt()
                }
            }
        } catch (e: InterruptedException) {
            Log.i("${Global.debugPrefix} 1", "Interruption detected")
        }
    }

    private fun processTask2(totalBallCnt : Int) {

        try {
            Log.i("${Global.debugPrefix} Task 2", "Task 2 has started")
            val game2 = GameEngine()
            game2.populateGrid(_ballPositionList)
            Global.task2_WinningDirection = Direction.INCOMPLETE
            val (direction, finalRow, finalCol) = game2.foundWinningMove(
                totalBallCnt, 1, -1)

            Log.i("${Global.debugPrefix} Finish Task #2", "Conclusion: Direction = $direction, row=$finalRow, col=$finalCol")

            Global.task2_WinningDirection = direction

            when (direction) {

                Direction.INCOMPLETE -> {
                    Log.i(Global.debugPrefix, "Task #2 got incomplete. It expect task1 has deterministic result")
                }

                Direction.NO_WINNING_DIRECTION -> {
                    Log.i(Global.debugPrefix, "Task #2 concluded there is no winning move")
                    task1.interrupt()
                }

                else -> {
                    // Task #2 got the winning move
                    Global.task2_WinningDirection = direction
                    task2WinningRow = finalRow
                    task2WinningCol = finalCol
                    Global.task2_WinningDirection = direction

                    Log.i(Global.debugPrefix, "Attempting to interrupt task #1")
                    task1.interrupt()
                }
            }
        } catch (e: InterruptedException) {
            Log.i("${Global.debugPrefix} Task 2", "Interruption detected")
        }
    }

    private fun showProcessingActivity() {
        var i = 0
        var currentValue = 0.0F
        Global.totalProcessCount = (((gTotalBallInCurrentMove - 1) * 4) * (gTotalBallInCurrentMove * 4)).toFloat()

        while (_uiState.value.state == GameState.Thinking) {

            // We track two level processing = level #1: 4 direction x level 2: 4 directions = 16
            val newValue : Float = (Global.ThinkingProgress.toFloat() / (Global.totalProcessCount) * 100.0).toFloat()

            if (newValue > currentValue) {
                currentValue = newValue
                _uiState.update { currentState ->
                    currentState.copy(
                        thinkingProgress = newValue
                    )
                }
            }

            // Wait 1.5 seconds. The reason why we split into three 500ms calls is to allow sooner
            // loop breakout when it has finished thinking
            if (_uiState.value.state == GameState.Thinking) Thread.sleep(500)
            if (_uiState.value.state == GameState.Thinking) Thread.sleep(500)
            if (_uiState.value.state == GameState.Thinking) Thread.sleep(500)
            i++
        }
        Log.i(Global.debugPrefix, "Finished thinking")
    }
    
    fun ballPositionList() : SnapshotStateList<Pos> {
        return (_ballPositionList)
    }

    fun foundWinnableMove() : Boolean
    {
        return (_uiState.value.foundWinningDirection != Direction.NO_WINNING_DIRECTION)
    }

    fun getWinningMoveCount(uiState: GameUiState): Int {
        val game = GameEngine()
        game.populateGrid(_ballPositionList)

        var winningMoveCount = 0

        var targetRow: Int
        var targetCol: Int

        if (uiState.foundWinningDirection == Direction.UP) {
            targetRow = game.findTargetRowOnMoveUp(uiState.winningPosition.row, uiState.winningPosition.col)
            winningMoveCount = uiState.winningPosition.row - targetRow
        }

        if (uiState.foundWinningDirection == Direction.DOWN) {
            targetRow = game.findTargetRowOnMoveDown(uiState.winningPosition.row, uiState.winningPosition.col)
            winningMoveCount = targetRow - uiState.winningPosition.row
        }

        if (uiState.foundWinningDirection == Direction.RIGHT) {
            targetCol = game.findTargetColOnMoveRight(uiState.winningPosition.row, uiState.winningPosition.col)
            winningMoveCount = targetCol - uiState.winningPosition.col
        }

        if (uiState.foundWinningDirection == Direction.LEFT) {
            targetCol = game.findTargetColOnMoveLeft(uiState.winningPosition.row, uiState.winningPosition.col)
            winningMoveCount = uiState.winningPosition.col - targetCol
        }

        return (winningMoveCount)
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
