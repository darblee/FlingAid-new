package com.darblee.flingaid.ui

import android.util.Log
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

/**
 * View Model for the Solver Game
 *
 * - It manage the business logic for the game. This includes the thinking activity.
 * - It is the sole source of truth for the solve game state.
 * - It prepare data for the UI. All information flow one direction to the UI
 * - It has a longer lifetime than the composable
 *
 * There can only be one SolverViewModel instance. Hence, use the singleton class (object)
 */
object SolverViewModel : ViewModel() {

    /**
     * _Developer's note:_ `internal` means it will only be visible within that module. A module
     * is a set of Kotlin files that are compiled together e.g. a library or application. It provides real
     * encapsulation for the implementation details. In this case, it is shared wit the SolverEngine class.
     */
    internal var gThinkingProgress = 0

    /**
     * Store the winning direction for each corresponding task. Only 1 task will have the winning move
     * but we do not know which ones.
     */
    var task2_WinningDirection = Direction.NO_WINNING_DIRECTION
    var task1_WinningDirection = Direction.NO_WINNING_DIRECTION

    /**
     * [_totalProcessCount] is the total amount of thinking process involved in the current move.
     * [_totalBallInCurrentMove] total number of balls in the current move and current level
     *
     * The total is 2 levels of thinking.
     * Next level is (number of balls - 1) x 4 directions
     * Current level is the number of balls x 4 direction
     * Total = (Next level) x (current level)
     */
    private var _totalProcessCount : Float = 0.0F
    private var _totalBallInCurrentMove = 0

    /**
     * The direction of the winning move obtained from the tasks calculation
     */
    private var _winningDirection_from_tasks = Direction.NO_WINNING_DIRECTION

    /**
     * List of all the balls and its position on the game board
     */
    private var _ballPositionList = mutableStateListOf<SolverGridPos>()

    /**
     * Contain the UI state of the solver game. This is used by the game screens to display
     * proper UI elements. Various composable will automatically update when that state changes
     *
     * For reference, see [ https://dev.to/zachklipp/introduction-to-the-compose-snapshot-system-19cn ]
     */
    private val _uiState = MutableStateFlow(SolverUiState())

    /**
     * Holds the [_uiState] as a state flow.
     *
     * __Developer's note__:
     * StateFlow is a data holder observable flow that emits the current and new state updates.
     * Its value property reflects the current state value. To update state and send it to the flow,
     * assign a new value to the value property of the MutableStateFlow class.
     *
     * In Android, StateFlow works well with classes that must maintain an observable immutable state.
     * A StateFlow can be exposed from the GameUiState so that the composable can listen for UI state
     * updates and make the screen state survive configuration changes.
     *
     * In the GameViewModel class, add the following [_uiState] property.
     *
     * `private set` this is internally modifiable and read-only access from the outside.
     * This ensure information flow in one direction from the view model to the UI
     */
    internal var uiState : StateFlow<SolverUiState> = _uiState.asStateFlow()
    private set

    /**
     * Initialize the SolverViewModel
     */
    init {
        val file : File? = null
        reset(file)
        gThinkingProgress = 0
    }

    /**
     * Save the solver game board to a file
     *
     * @param file File to write to
     */
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

    /**
     * Load the saved game board from file
     *
     * @param file  File to load from
     */
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

    /**
     * Get the current thinking status
     */
    fun getThinkingStatus(): SolverUiState.ThinkingMode
    {
        return (_uiState.value.thinkingStatus)
    }

    /**
     * Reset the entire solver game
     * - Clear the board
     * - Remove any saved game file
     */
    fun reset(file: File?)
    {
        _ballPositionList.clear()
        file?.delete()

        IDLEstate()
    }

    /**
     * Return the number of active ball
     *
     * @return Number of active balls
     */
    fun ballCount():Int
    {
        return _ballPositionList.count()
    }

    /**
     * If the position is blank, then place the ball.
     * If ball already exist on that location, then remove the ball
     *
     * @param solverGridPos The position of the ball
     */
    fun toggleBallPosition(solverGridPos: SolverGridPos)
    {
        if (_ballPositionList.contains(solverGridPos)) {
            _ballPositionList.remove(solverGridPos)
        } else {
            _ballPositionList.add(solverGridPos)
        }

        IDLEstate()
    }

    /**
     * Volatile is used to ensure each thread is reading exact same [gMultipleThread] value
     * from memory instead of from cpu-cache.
     */
    @Volatile var gMultipleThread = false

    private lateinit var gThinkingThread1 : Thread
    private lateinit var gThinkingThread2 : Thread
    private lateinit var gShowProgressThread : Thread

    private var task1WinningRow = -1
    private var task1WinningCol = -1
    private var task2WinningRow = -1
    private var task2WinningCol = -1

    /**
     * Find the winning move.
     *
     * @param solverViewModel The ViewModel instance
     * @return Unit (nothing)
     */
    fun findWinningMove(solverViewModel: SolverViewModel)
    {
        gThinkingProgress = 0
        val activeThinkingRec = SolverUiState.ThinkingMode.Active
        activeThinkingRec.progressLevel = 0.0f

        _uiState.update { currentStatus ->
            currentStatus.copy(
                thinkingStatus = activeThinkingRec
            )
        }
        viewModelScope.launch {

            // For explanation on the formula, see the description for _totalProcessCount
            _totalProcessCount = (((_totalBallInCurrentMove - 1) * 4) * (_totalBallInCurrentMove * 4)).toFloat()

            _winningDirection_from_tasks = Direction.NO_WINNING_DIRECTION
            val gTotalBallCount = solverViewModel.ballCount()

            task1_WinningDirection = Direction.INCOMPLETE
            task2_WinningDirection = Direction.INCOMPLETE

            gThinkingProgress = 0
            _totalBallInCurrentMove = solverViewModel.ballCount()

            lateinit var cyclicBarrier : CyclicBarrier

            if (gTotalBallCount > 12) {
                gMultipleThread = true
                _totalProcessCount = (((_totalBallInCurrentMove - 1) * 4) * (_totalBallInCurrentMove * 4)).toFloat()
                cyclicBarrier = CyclicBarrier(2) {
                    Log.i(Global.debugPrefix, "Reached a converged point between 2 parallel tasks")
                    recordThinkingResult()
                }
            } else {
                gMultipleThread = false
                cyclicBarrier = CyclicBarrier(1) {
                    Log.i(Global.debugPrefix, "Reached a converged point. One thread")
                    recordThinkingResult()
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

    /**
     * Update the [_uiState] with the result of the thinking task(s).
     */
    private fun recordThinkingResult()
    {
        Log.i(Global.debugPrefix, "DisplayResult after processing....")
        val idleRec = SolverUiState.ThinkingMode.Idle

        if ((task1_WinningDirection != Direction.NO_WINNING_DIRECTION) &&
            (task1_WinningDirection != Direction.INCOMPLETE))
        {
            // Task1 has the winning move
            Log.i(Global.debugPrefix,
                "Task #1 has winning move with direction : ${task1_WinningDirection}")
            val winningSolverGridPos = SolverGridPos(task1WinningRow, task1WinningCol)
            val winningDir = task1_WinningDirection

            _winningDirection_from_tasks = task1_WinningDirection

            idleRec.IdleMode = SolverUiState.ThinkingMode.Idle.IdleType.SolutionFound
            uiState.value.thinkingStatus = idleRec

            _uiState.update {currentState ->
                currentState.copy(
                    thinkingStatus = idleRec,
                    winningPosition = winningSolverGridPos,
                    winningDirection = winningDir
                )
            }
        } else {
            // Task1 does not have the winning result. NOw check on task #2
            if ((task2_WinningDirection != Direction.NO_WINNING_DIRECTION) &&
                (task2_WinningDirection != Direction.INCOMPLETE)) {
                Log.i(Global.debugPrefix,
                    "Task #2 has winning move with direction: ${task2_WinningDirection}")
                // Task2 has the winning move
                val winningSolverGridPos = SolverGridPos(task2WinningRow, task2WinningCol)
                val winningDir = task2_WinningDirection
                _winningDirection_from_tasks = task2_WinningDirection

                idleRec.IdleMode = SolverUiState.ThinkingMode.Idle.IdleType.SolutionFound
                uiState.value.thinkingStatus = idleRec

                _uiState.update {currentState ->
                    currentState.copy(
                        thinkingStatus = idleRec,
                        winningPosition = winningSolverGridPos,
                        winningDirection = winningDir
                    )
                }
            } else {
                // Neither Task #1 nor Task #2 has winning result
                _winningDirection_from_tasks = Direction.NO_WINNING_DIRECTION

                idleRec.IdleMode = SolverUiState.ThinkingMode.Idle.IdleType.NoSolutionFound
                uiState.value.thinkingStatus = idleRec

                _uiState.update {currentState ->
                    currentState.copy(
                        thinkingStatus = idleRec,
                        winningPosition = SolverGridPos(-1, -1),
                        winningDirection = Direction.NO_WINNING_DIRECTION
                    )
                }
            }
        }
        gThinkingProgress = 0
    }

    /**
     * Set to thinking status to Idle, and Waiting on User
     */
    fun IDLEstate() {

        val idleRec = SolverUiState.ThinkingMode.Idle
        idleRec.IdleMode = SolverUiState.ThinkingMode.Idle.IdleType.WaitingOnUser
        uiState.value.thinkingStatus = idleRec

        _uiState.update {currentState ->
            currentState.copy(
                thinkingStatus = idleRec,
                winningPosition = SolverGridPos(-1, -1),
                winningDirection = Direction.NO_WINNING_DIRECTION
            )
        }

        gThinkingProgress = 0
    }

    /**
     * Process the first task.
     *
     * It is tracked using the hardcoded task #1 meta data
     */
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

            task1_WinningDirection = direction

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
                    task1_WinningDirection = direction
                    task1WinningRow = finalRow
                    task1WinningCol = finalCol
                    task1_WinningDirection = direction

                    Log.i(Global.debugPrefix, "Attempting to interrupt task #2")
                    if (gMultipleThread) gThinkingThread2.interrupt()
                }
            }
        } catch (e: InterruptedException) {
            Log.i("${Global.debugPrefix} 1", "Interruption detected")
        }
    }

    /**
     * Process the 2nd task.
     *
     * It is tracked using the hardcoded task #2 meta data
     */
    private fun processTask2(totalBallCnt : Int)
    {
        try {
            Log.i("${Global.debugPrefix} Task 2", "Task 2 has started")
            val game2 = SolverEngine()
            game2.populateGrid(_ballPositionList)
            task2_WinningDirection = Direction.INCOMPLETE
            val (direction, finalRow, finalCol) = game2.foundWinningMove(
                totalBallCnt, 1, -1)

            Log.i("${Global.debugPrefix} Finish Task #2", "Conclusion: Direction = $direction, row=$finalRow, col=$finalCol")

            task2_WinningDirection = direction

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
                    task2_WinningDirection = direction
                    task2WinningRow = finalRow
                    task2WinningCol = finalCol
                    task2_WinningDirection = direction

                    Log.i(Global.debugPrefix, "Attempting to interrupt task #1")
                    gThinkingThread1.interrupt()
                }
            }
        } catch (e: InterruptedException) {
            Log.i("${Global.debugPrefix} Task 2", "Interruption detected")
        }
    }

    /**
     * Show the thinking processing activity.
     */
    private fun showProcessingActivity()
    {
        var currentValue = 0.0F
        _totalProcessCount = (((_totalBallInCurrentMove - 1) * 4) * (_totalBallInCurrentMove * 4)).toFloat()

        while (_uiState.value.thinkingStatus == SolverUiState.ThinkingMode.Active) {

            // We track two level processing = level #1: 4 direction x level 2: 4 directions = 16
            val newValue : Float = (gThinkingProgress.toFloat() / (_totalProcessCount) * 100.0).toFloat()

            if (newValue > currentValue) {
                currentValue = newValue
                val activeThinking = SolverUiState.ThinkingMode.Active
                activeThinking.progressLevel = currentValue
                _uiState.update { currentState ->
                    currentState.copy(
                        thinkingStatus = activeThinking,
                    )
                }
            }

            // Wait 1.5 seconds. The reason why we split into three 500ms calls is to allow sooner
            // loop breakout when it has finished thinking
            if (_uiState.value.thinkingStatus == SolverUiState.ThinkingMode.Active) Thread.sleep(500)
            if (_uiState.value.thinkingStatus == SolverUiState.ThinkingMode.Active) Thread.sleep(500)
            if (_uiState.value.thinkingStatus == SolverUiState.ThinkingMode.Active) Thread.sleep(500)
        }
        Log.i(Global.debugPrefix, "Finished thinking")
    }
    
    fun ballPositionList() : SnapshotStateList<SolverGridPos>
    {
        return (_ballPositionList)
    }

    fun foundWinnableMove() : Boolean
    {
        return (_uiState.value.winningDirection != Direction.NO_WINNING_DIRECTION)
    }

    fun getWinningMoveCount(uiState: SolverUiState): Int
    {
        val game = SolverEngine()
        game.populateGrid(_ballPositionList)

        var winningMoveCount = 0

        var targetRow: Int
        var targetCol: Int

        if (uiState.winningDirection == Direction.UP) {
            targetRow = game.findTargetRowOnMoveUp(uiState.winningPosition.row, uiState.winningPosition.col)
            winningMoveCount = uiState.winningPosition.row - targetRow
        }

        if (uiState.winningDirection == Direction.DOWN) {
            targetRow = game.findTargetRowOnMoveDown(uiState.winningPosition.row, uiState.winningPosition.col)
            winningMoveCount = targetRow - uiState.winningPosition.row
        }

        if (uiState.winningDirection == Direction.RIGHT) {
            targetCol = game.findTargetColOnMoveRight(uiState.winningPosition.row, uiState.winningPosition.col)
            winningMoveCount = targetCol - uiState.winningPosition.col
        }

        if (uiState.winningDirection == Direction.LEFT) {
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

        _uiState.value.winningDirection = Direction.NO_WINNING_DIRECTION

        saveBallPositions(gBoardFile)
    }

    fun setupMovingChain(row: Int, col: Int, direction: Direction )
    {
        if (!(_ballPositionList.contains(SolverGridPos(row, col)))) return

        when (direction) {
            Direction.UP -> {
                val movingChain = buildMovingChain(row, col, direction)

                if (movingChain.isEmpty()) return

                _uiState.update {currentState ->
                    currentState.copy(
                        winningDirection = Direction.UP,
                        movingChain = movingChain
                    )
                }
            }
            Direction.DOWN -> {
                val movingChain = buildMovingChain(row, col, direction)

                if (movingChain.isEmpty()) return

                _uiState.update { currentState ->
                    currentState.copy(
                        winningDirection = Direction.DOWN,
                        movingChain = movingChain
                    )
                }
            }
            Direction.RIGHT -> {
                val movingChain = buildMovingChain(row, col, direction)
                if (movingChain.isEmpty()) return

                _uiState.update {currentState ->
                    currentState.copy(
                        winningDirection = Direction.RIGHT,
                        movingChain = movingChain
                    )
                }
            }
            Direction.LEFT -> {
                val movingChain = buildMovingChain(row, col, direction)
                if (movingChain.isEmpty()) return

                _uiState.update {currentState ->
                    currentState.copy(
                        winningDirection = Direction.LEFT,
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

    /**
     *  Find the number of free space in front of the ball doing on a specific direction. If the
     *  ball is on the edge, then automatically provide 2 free spaces. The caller [buildMovingChain]
     *  will send the ball off the grid. We need to see it fall off the edge of the phone screen, which is why
     *  two space is provided instead of one space.
     *
     *  @param row Row of existing ball
     *  @param col Column of existing ball
     *  @param direction Direction to look for free space
     *  @param firstMove If this is the first move, we need to do special check when the ball is
     *  on the edge of the grid.
     *
     *  @see buildMovingChain
     *  @return Pair<Int, SolverGridPos?> where the first element is the number of free space and
     *  second element is the position of the next ball
     *
     */
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
        _uiState.update { currentState ->
            currentState.copy(
                winningDirection = Direction.NO_WINNING_DIRECTION,
                movingChain = mutableListOf()
            )
        }
    }

    fun getMovingChain() : List<MovingRec>
    {
        return _uiState.value.movingChain
    }
}

