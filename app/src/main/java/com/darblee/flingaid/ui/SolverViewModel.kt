package com.darblee.flingaid.ui

import android.util.Log
import android.widget.Space
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darblee.flingaid.Direction
import com.darblee.flingaid.Global
import com.darblee.flingaid.ui.screens.gBoardFile
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

class SolverViewModel : ViewModel() {

    private var _ballPositionList = mutableStateListOf<SolverGridPos>()

    /*
        SnapshotList:
        - The composable will automatically update when that state changes
        - Values can change
        - Other functions will be notified of the changes
        Ref: https://dev.to/zachklipp/introduction-to-the-compose-snapshot-system-19cn
     */

    // Game UI state
    private val _uiState = MutableStateFlow(SolverUiState())

    /*
        Developer's note:
        StateFlow is a data holder observable flow that emits the current and new state updates.
        Its value property reflects the current state value. To update state and send it to the flow,
        assign a new value to the value property of the MutableStateFlow class.

        In Android, StateFlow works well with classes that must maintain an observable immutable state.
        A StateFlow can be exposed from the GameUiState so that the composable can listen for UI state
        updates and make the screen state survive configuration changes.

        In the GameViewModel class, add the following _uiState property.

        "internal" means it will only be visible within that module. A module is a set of Kotlin
        files that are compiled together e.g. a library or application. It provides real
        encapsulation for the implementation details

     */
    internal var uiState : StateFlow<SolverUiState> = _uiState.asStateFlow()
        private set  // Public getter (read-only access from outside) and private setter (only internally modifiable)



    init {
        val file : File? = null
        reset(file)
    }

    fun saveBallPositions(file: File)
    {
        val format = Json { prettyPrint = true }
        val ballList = mutableListOf<SolverGridPos>()

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

    fun loadBallPositions(file: File)
    {
        try {
            val reader = FileReader(file)
            val data = reader.readText()
            reader.close()

            val list = Json.decodeFromString<List<SolverGridPos>>(data)

            _ballPositionList.clear()
            for (pos in list) {
                _ballPositionList.add(pos)
            }
        } catch (e: Exception) {
            Log.i(Global.debugPrefix, "An error occurred while reading the file: ${e.message}")
        }
    }

    fun getThinkingStatus(): SolverState
    {
        return (_uiState.value.state)
    }

    fun reset(file: File?)
    {
        _uiState.update {currentState ->
            currentState.copy(
                state = SolverState.NotThinking
            )
        }
        _ballPositionList.clear()
        file?.delete()
        
        _uiState.value.foundWinningDirection = Direction.NO_WINNING_DIRECTION
    }

    fun ballCount():Int
    {
        return _ballPositionList.count()
    }

    // If the position is blank, then place the ball.
    // If ball already exist on that location, then remove the ball
    fun toggleBallPosition(solverGridPos: SolverGridPos)
    {
        if (_ballPositionList.contains(solverGridPos)) {
            _ballPositionList.remove(solverGridPos)
        } else {
            _ballPositionList.add(solverGridPos)
        }

        _uiState.update {currentState ->
            currentState.copy(
                state = SolverState.NotThinking,
                foundWinningDirection = Direction.NO_WINNING_DIRECTION
            )
        }
    }

    // Volatile is used to ensure each thread is reading exact same value
    // (from memory instead of from cpu-cache)
    @Volatile var gMultipleThread = false

    private lateinit var gThinkingThread1 : Thread
    private lateinit var gThinkingThread2 : Thread
    private lateinit var gShowProgressThread : Thread

    private var task1WinningRow = -1
    private var task1WinningCol = -1
    private var task2WinningRow = -1
    private var task2WinningCol = -1

    fun findWinningMove(solverViewModel: SolverViewModel)
    {
        _uiState.update {currentState ->
            currentState.copy(
                state = SolverState.Thinking
            )
        }
        viewModelScope.launch {
            Global.totalProcessCount = (((gTotalBallInCurrentMove - 1) * 4) * (gTotalBallInCurrentMove * 4)).toFloat()
            gWINNING_DIRECTION_from_tasks = Direction.NO_WINNING_DIRECTION
            val gTotalBallCount = solverViewModel.ballCount()

            Global.task1_WinningDirection = Direction.INCOMPLETE
            Global.task2_WinningDirection = Direction.INCOMPLETE

            Global.ThinkingProgress = 0
            gTotalBallInCurrentMove = solverViewModel.ballCount()

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

            gThinkingThread1 = Thread {
                processTask1(gTotalBallCount)
                Log.i("${Global.debugPrefix} Task 1", "Task #1 is completed. Now waiting for all threads to complete.")
                cyclicBarrier.await()
            }
            gThinkingThread2 = Thread {
                processTask2(gTotalBallCount)
                Log.i("${Global.debugPrefix}: Task 2", "Task #2 is completed. Now waiting for all threads to complete.")
                cyclicBarrier.await()
            }

            gShowProgressThread = Thread {
                showProcessingActivity()
            }

            gShowProgressThread.start()
            gThinkingThread1.start()

            if (gMultipleThread) {
                gThinkingThread2.start()
            }
        }
    }

    private fun displayResult()
    {
        Log.i(Global.debugPrefix, "DisplayResult after processing....")
        uiState.value.state = SolverState.NotThinking

        if ((Global.task1_WinningDirection != Direction.NO_WINNING_DIRECTION) &&
            (Global.task1_WinningDirection != Direction.INCOMPLETE)) {
            // Task1 has the winning move
            Log.i(Global.debugPrefix,
                "Task #1 has winning move with direction : ${Global.task1_WinningDirection}")
            val winningSolverGridPos = SolverGridPos(task1WinningRow, task1WinningCol)
            val winningDir = Global.task1_WinningDirection

            gWINNING_DIRECTION_from_tasks = Global.task1_WinningDirection

            _uiState.update {currentState ->
                currentState.copy(
                    state = SolverState.NotThinking,
                    winningPosition = winningSolverGridPos,
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
                val winningSolverGridPos = SolverGridPos(task2WinningRow, task2WinningCol)
                val winningDir = Global.task2_WinningDirection
                gWINNING_DIRECTION_from_tasks = Global.task2_WinningDirection
                _uiState.update {currentState ->
                    currentState.copy(
                        state = SolverState.NotThinking,
                        winningPosition = winningSolverGridPos,
                        foundWinningDirection = winningDir
                    )
                }
            } else {
                Log.i(Global.debugPrefix, "Neither Task #1 nor Task #2 has winning result")
                gWINNING_DIRECTION_from_tasks = Direction.NO_WINNING_DIRECTION
                _uiState.update {currentState ->
                    currentState.copy(
                        state = SolverState.NotThinking,
                        winningPosition = SolverGridPos(-1, -1),
                        foundWinningDirection = Direction.NO_WINNING_DIRECTION,
                        needToDisplayNoWinnableToastMessage = true
                    )
                }
            }
        }
    }

    fun noNeedToDisplayNoWinnableToastMessage()
    {
        _uiState.update {currentState ->
            currentState.copy(
                needToDisplayNoWinnableToastMessage = false
            )
        }
    }

    private fun processTask1(totalBallCnt :  Int)
    {
        try {
            Log.i("${Global.debugPrefix} Task 1", "Task 1 has started")
            val game1 = SolverEngine()
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
                    if (gMultipleThread) gThinkingThread2.interrupt()
                }

                else -> {
                    // Task #1 got winning move
                    Global.task1_WinningDirection = direction
                    task1WinningRow = finalRow
                    task1WinningCol = finalCol
                    Global.task1_WinningDirection = direction

                    Log.i(Global.debugPrefix, "Attempting to interrupt task #2")
                    if (gMultipleThread) gThinkingThread2.interrupt()
                }
            }
        } catch (e: InterruptedException) {
            Log.i("${Global.debugPrefix} 1", "Interruption detected")
        }
    }

    private fun processTask2(totalBallCnt : Int)
    {
        try {
            Log.i("${Global.debugPrefix} Task 2", "Task 2 has started")
            val game2 = SolverEngine()
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
                    gThinkingThread1.interrupt()
                }

                else -> {
                    // Task #2 got the winning move
                    Global.task2_WinningDirection = direction
                    task2WinningRow = finalRow
                    task2WinningCol = finalCol
                    Global.task2_WinningDirection = direction

                    Log.i(Global.debugPrefix, "Attempting to interrupt task #1")
                    gThinkingThread1.interrupt()
                }
            }
        } catch (e: InterruptedException) {
            Log.i("${Global.debugPrefix} Task 2", "Interruption detected")
        }
    }

    private fun showProcessingActivity()
    {
        var i = 0
        var currentValue = 0.0F
        Global.totalProcessCount = (((gTotalBallInCurrentMove - 1) * 4) * (gTotalBallInCurrentMove * 4)).toFloat()

        while (_uiState.value.state == SolverState.Thinking) {

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
            if (_uiState.value.state == SolverState.Thinking) Thread.sleep(500)
            if (_uiState.value.state == SolverState.Thinking) Thread.sleep(500)
            if (_uiState.value.state == SolverState.Thinking) Thread.sleep(500)
            i++
        }
        Log.i(Global.debugPrefix, "Finished thinking")
    }
    
    fun ballPositionList() : SnapshotStateList<SolverGridPos>
    {
        return (_ballPositionList)
    }

    fun foundWinnableMove() : Boolean
    {
        return (_uiState.value.foundWinningDirection != Direction.NO_WINNING_DIRECTION)
    }

    fun getWinningMoveCount(uiState: SolverUiState): Int
    {
        val game = SolverEngine()
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

    fun makeWinningMove(uiState: SolverUiState)
    {
        val game = SolverEngine()
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

        saveBallPositions(gBoardFile)
    }

    fun makeManualMove(direction: Direction, pos: SolverGridPos) {

        val game = SolverEngine()
        game.populateGrid(_ballPositionList)

        var targetRow: Int
        var targetCol: Int

        /*
        when (direction) {
            Direction.UP -> {
                targetRow = game.findTargetRowOnMoveUp(pos.row, pos.col)
                game.moveUp(pos.row, targetRow, pos.col)
                _ballPositionList.clear()
                _ballPositionList = game.updateBallList()
            }

            Direction.DOWN -> {
                targetRow = game.findTargetRowOnMoveDown(pos.row, pos.col)
                game.moveDown(pos.row, targetRow, pos.col)
                _ballPositionList.clear()
                _ballPositionList = game.updateBallList()
            }

            Direction.LEFT -> {
                targetCol = game.findTargetColOnMoveLeft(pos.row, pos.col)
                game.moveLeft(pos.row, targetCol, pos.col)
                _ballPositionList.clear()
                _ballPositionList = game.updateBallList()
            }

            Direction.RIGHT -> {
                targetCol = game.findTargetColOnMoveRight(pos.row, pos.col)
                game.moveRight(pos.row, targetCol, pos.col)
                _ballPositionList.clear()
                _ballPositionList = game.updateBallList()
            }

            else -> {
                assert(true) { "Got unexpected direction during manual move"}
            }
        }

        saveBallPositions(gBoardFile)

         */

        _uiState.update {currentState ->
            currentState.copy(
                movingChain = listOf(),
                movingDirection = direction,
                state = SolverState.NotThinking
            )
        }
    }

    fun setupMovingChain(row: Int, col: Int, direction: Direction )
    {
        if (!(_ballPositionList.contains(SolverGridPos(row, col)))) return

        when (direction) {
            Direction.UP -> {
                val movingChain = buildMovingChain(row, col, direction)

                if (movingChain.isEmpty()) return

                printChain(direction, movingChain)
                _uiState.update {currentState ->
                    currentState.copy(
                        movingDirection = Direction.UP,
                        movingChain = movingChain
                    )
                }
            }
            Direction.DOWN -> {
                val movingChain = buildMovingChain(row, col, direction)

                if (movingChain.isEmpty()) return

                printChain(direction, movingChain)

                _uiState.update { currentState ->
                    currentState.copy(
                        movingDirection = Direction.DOWN,
                        movingChain = movingChain
                    )
                }
            }
            Direction.RIGHT -> {
                val movingChain = buildMovingChain(row, col, direction)
                if (movingChain.isEmpty()) return

                printChain(direction, movingChain)

                _uiState.update {currentState ->
                    currentState.copy(
                        movingDirection = Direction.RIGHT,
                        movingChain = movingChain
                    )
                }
            }
            Direction.LEFT -> {
                val movingChain = buildMovingChain(row, col, direction)
                if (movingChain.isEmpty()) return

                printChain(direction, movingChain)

                _uiState.update {currentState ->
                    currentState.copy(
                        movingDirection = Direction.LEFT,
                        movingChain = movingChain
                    )
                }
            }
            else -> {
                assert(true) {"Got unexpected direction value"}
            }
        }
    }

    fun needBallAnimation() : Boolean
    {
        return (_uiState.value.movingChain.isNotEmpty())
    }

    private fun printChain(direction: Direction, movingList: List<MovingRec>) {
        Log.i(Global.debugPrefix, "===============")
        movingList.forEach {rec ->
            val gridPos = rec.pos
            Log.i(Global.debugPrefix, "Mov Pos: ${gridPos.row}, ${gridPos.col}, distance= ${rec.distance} ${direction}")
        }
        Log.i(Global.debugPrefix, "===============")
    }

    private fun buildMovingChain(initialRow: Int, initialCol: Int, direction: Direction) : List<MovingRec>
    {
        val movingList = mutableListOf<MovingRec>()

        var chainRecInfo = findFreeSpaceCount(initialRow, initialCol, direction, firstMove = true)
        var movingDistance = chainRecInfo.first
        var nextSourcePos = chainRecInfo.second

        // For the first ball in chain, we must have at least one free space to move
        if (movingDistance == 0) return movingList

        movingList.add(MovingRec(SolverGridPos(initialRow,initialCol), movingDistance))

        // For subsequent ball in the chain, it is fine to have zero distance
        var fallenOffEdge = false
        while (!fallenOffEdge) {
            if (nextSourcePos == null) {
                fallenOffEdge = true
            } else {
                val currentSourcePos = nextSourcePos
                chainRecInfo =
                    findFreeSpaceCount(currentSourcePos.row, currentSourcePos.col, direction, firstMove = false)
                movingDistance = chainRecInfo.first
                nextSourcePos = chainRecInfo.second
                movingList.add(MovingRec(currentSourcePos, movingDistance))
            }
        }

        return (movingList)
    }

    private fun findFreeSpaceCount(row: Int, col: Int, direction: Direction, firstMove: Boolean) : Pair <Int, SolverGridPos?>
    {
        var xOffset = 0
        var yOffset = 0
        var sourceRow = row
        var sourceCol = col

        when (direction) {
            Direction.RIGHT -> yOffset = 1
            Direction.LEFT -> yOffset = -1
            Direction.UP -> xOffset = -1
            Direction.DOWN -> xOffset = 1
            else -> assert(true) {"Got unexpected direction value"}
        }

        if ((direction == Direction.LEFT) && (col == 0)) {
            if (firstMove) return (Pair(0, null ))
            return (Pair(2, null ))
        }

        if ((direction == Direction.RIGHT) && (col == Global.MaxColSize)) {
            if (firstMove) return (Pair(0, null ))
            return (Pair(2, null ))
        }

        if ((direction == Direction.UP) && (row == 0)) {
            if (firstMove) return (Pair(0, null ))
            return (Pair(2, null ))
        }

        if ((direction == Direction.DOWN) && (row == Global.MaxRowSize)) {
            if (firstMove) return (Pair(0, null ))
            return (Pair(2, null ))
        }

        var newRow : Int
        var newCol : Int

        var distance = 0
        var hitWall = false

        var nextSourcePos : SolverGridPos? = null

        while (!hitWall) {
            newRow = sourceRow + xOffset
            newCol = sourceCol + yOffset

            if ((newRow == -1) || (newRow == Global.MaxRowSize) ||
                (newCol == -1) || (newCol == Global.MaxColSize)) {
                distance++ // Add two to make it fall off the edge
                distance++
                hitWall = true
            } else {
                if (_ballPositionList.contains(SolverGridPos(newRow, newCol))) {
                    nextSourcePos = SolverGridPos(newRow, newCol)
                    hitWall = true
                } else {
                    distance++
                    sourceRow = newRow
                    sourceCol = newCol
                }
            }
        }

        return (Pair(distance, nextSourcePos))
    }

    fun ballMovementAnimationComplete()
    {
        val direction = _uiState.value.movingDirection
        val pos = _uiState.value.movingChain[0].pos

        Log.i(Global.debugPrefix, "Making manual move: $direction, ${pos.row} ${pos.col}")
        makeManualMove(direction, pos)
    }

}

