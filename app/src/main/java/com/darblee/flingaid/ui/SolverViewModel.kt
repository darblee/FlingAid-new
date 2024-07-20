package com.darblee.flingaid.ui

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darblee.flingaid.Direction
import com.darblee.flingaid.Global
import com.darblee.flingaid.utilities.Pos
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
 * **View Model for the Solver Game**
 *
 * - It manage the business logic for the game. This includes the thinking activity.
 * - It is the sole source of truth for the solve game state.
 * - It prepare data for the UI. All information flow one direction to the UI
 * - It has a longer lifetime than the composable
 *
 * [State Machine](https://github.com/darblee/FlingAid-new/blob/master/README.md)
 *
 * There can only be one SolverViewModel instance. Hence, use the singleton class (object)
 *
 * [SolverUiState] State of UI
 *
 * **Ball Management**
 * Managing the ball on the game board
 * - [loadGameFile] : Define the file to store the ball position information
 * - [toggleBallPosition]
 * - [ballCount]
 * - [ballPositionList]
 *
 * **Game Play Functions**
 * - [reset]
 * - [setIDLEstate]
 * - [findWinningMove]
 * - [getWinningMoveCount]
 * - [makeWinningMove]
 */
object SolverViewModel : ViewModel() {

    /**
     * _Developer's note:_ `internal` means it will only be visible within that module. A module
     * is a set of Kotlin files that are compiled together e.g. a library or application. It provides real
     * encapsulation for the implementation details. In this case, it is shared wit the SolverEngine class.
     */
    internal var gThinkingProgress = 0

    private var gGameFile: File? = null

    /**
     * Store the winning direction for each corresponding task. Only 1 task will have the winning move
     * but we do not know which ones.
     * - Winning Direction from thread #1
     * - Winning Direction from thread #2
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
    private var _totalProcessCount: Float = 0.0F
    private var _totalBallInCurrentMove = 0

    /**
     * The direction of the winning move obtained from the tasks calculation
     */
    private var _winningDirection_from_tasks = Direction.NO_WINNING_DIRECTION

    /********************************* BALL MANAGEMENT ****************************/

    /**
     * List of all the balls and its position on the game board
     *
     * _Developer's Note_
     * Use of mutableStateListOf to preserve the composable state of the ball position. The state
     * are kept appropriately isolated and can be performed in a safe manner without race condition
     * when they are used in multiple threads (e.g. LaunchEffects).
     */
    private var _ballPositionList = mutableStateListOf<Pos>()

    /**
     * Get the list of balls and its position.
     *
     * @return Balls list in SnapshotList property type.
     *
     * _Developer's note_ : SnapshotStateList is chosen instead of MutableStateList<T>. SnapshotStateList
     * is a type of mutable list that integrates with the state observation system. When the contents of
     * a SnapshotStateList change, Compose will recreate any composable functions that depends on it,
     * which updates the UI. Only the respective item in the list will be (re)compose. When a change is
     * made to a SnapshotStateList, a new snapshot is created instead of directly modifying the original
     * list. This snapshot is a separate, immutable collection that represents the list's state at a specific
     * moment. For more info, see [Compose Snapshot System](https://dev.to/zachklipp/introduction-to-the-compose-snapshot-system-19cn)
     */
    fun ballPositionList(): SnapshotStateList<Pos> {
        return (_ballPositionList)
    }

    /**
     * Return the number of active ball
     *
     * @return Number of active balls
     */
    fun ballCount(): Int {
        return _ballPositionList.count()
    }

    /**
     * Save the solver game board to a file
     */
    private fun saveBallPositions() {
        val format = Json { prettyPrint = true }
        val ballList = mutableListOf<Pos>()

        for (currentPos in _ballPositionList) {
            ballList += currentPos
        }
        val output = format.encodeToString(ballList)

        try {
            val writer = FileWriter(gGameFile)
            writer.write(output)
            writer.close()
        } catch (e: Exception) {
            Log.i(Global.DEBUG_PREFIX, "${e.message}")
        }
    }

    /**
     * Set the game file
     *
     * @param file game file
     */
    fun loadGameFile(file: File) {
        gGameFile = file
        loadBallPositions()
    }

    /**
     * Load the saved game board from file
     **/
    private fun loadBallPositions() {
        try {
            val reader = FileReader(gGameFile)
            val data = reader.readText()
            reader.close()

            val list = Json.decodeFromString<List<Pos>>(data)

            _ballPositionList.clear()
            for (pos in list) {
                _ballPositionList.add(pos)
            }
        } catch (e: Exception) {
            Log.i(Global.DEBUG_PREFIX, "An error occurred while reading the file: ${e.message}")
        }
    }

    /**
     * Print the ball positions. Used for debugging purposes
     */
    private fun printPositions() {

        Log.i(Global.DEBUG_PREFIX, "============== Ball Listing ================)")

        for ((index, value) in _ballPositionList.withIndex()) {
            Log.i(Global.DEBUG_PREFIX, "Ball $index: (${value.row}, ${value.col})")
        }

    }

    /********************************* SOLVER GAME MANAGEMENT ****************************/

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
    internal var uiState: StateFlow<SolverUiState> = _uiState.asStateFlow()
        private set

    /**
     * Initialize the SolverViewModel
     */
    init {
        reset()
        gThinkingProgress = 0
    }

    /**
     * Reset the entire solver game
     * - Clear the board
     * - Remove any saved game file
     */
    fun reset() {
        _ballPositionList.clear()
        gGameFile?.delete()

        setIDLEstate()
    }

    /**
     * If the position is blank, then place the ball.
     * If ball already exist on that location, then remove the ball
     *
     * @param solverGridPos The position of the ball
     */
    fun toggleBallPosition(solverGridPos: Pos) {
        if (_ballPositionList.contains(solverGridPos)) {
            _ballPositionList.remove(solverGridPos)
        } else {
            _ballPositionList.add(solverGridPos)
        }

        saveBallPositions()
        setIDLEstate()
    }

    /**
     * Volatile is used to ensure each thread is reading exact same [gMultipleThread] value
     * from memory instead of from cpu-cache.
     */
    @Volatile
    var gMultipleThread = false

    private lateinit var gThinkingThread1: Thread
    private lateinit var gThinkingThread2: Thread
    private lateinit var gShowProgressThread: Thread

    private var task1WinningRow = -1
    private var task1WinningCol = -1
    private var task2WinningRow = -1
    private var task2WinningCol = -1

    /**
     * Find the winning move.
     *
     * @param solverViewModel The ViewModel instance
     */
    fun findWinningMove(solverViewModel: SolverViewModel) {
        gThinkingProgress = 0

        _uiState.update { currentStatus ->
            currentStatus.copy(
                solverGameState = SolverUiState.SolverGameMode.Thinking,
                thinkingProgressLevel = 0.0f
            )
        }
        viewModelScope.launch {

            // For explanation on the formula, see the description for _totalProcessCount
            _totalProcessCount =
                (((_totalBallInCurrentMove - 1) * 4) * (_totalBallInCurrentMove * 4)).toFloat()

            _winningDirection_from_tasks = Direction.NO_WINNING_DIRECTION
            val gTotalBallCount = solverViewModel.ballCount()

            task1_WinningDirection = Direction.INCOMPLETE
            task2_WinningDirection = Direction.INCOMPLETE

            gThinkingProgress = 0
            _totalBallInCurrentMove = solverViewModel.ballCount()

            lateinit var cyclicBarrier: CyclicBarrier

            if (gTotalBallCount > 12) {
                gMultipleThread = true
                _totalProcessCount =
                    (((_totalBallInCurrentMove - 1) * 4) * (_totalBallInCurrentMove * 4)).toFloat()
                cyclicBarrier = CyclicBarrier(2) {
                    Log.i(Global.DEBUG_PREFIX, "Reached a converged point between 2 parallel tasks")
                    recordThinkingResult()
                }
            } else {
                gMultipleThread = false
                cyclicBarrier = CyclicBarrier(1) {
                    Log.i(Global.DEBUG_PREFIX, "Reached a converged point. One thread")
                    recordThinkingResult()
                }
            }

            gThinkingThread1 = Thread {
                processTask1(gTotalBallCount)
                Log.i(
                    "${Global.DEBUG_PREFIX} Task 1",
                    "Task #1 is completed. Now waiting for all threads to complete."
                )
                cyclicBarrier.await()
            }
            gThinkingThread2 = Thread {
                processTask2(gTotalBallCount)
                Log.i(
                    "${Global.DEBUG_PREFIX}: Task 2",
                    "Task #2 is completed. Now waiting for all threads to complete."
                )
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
    private fun recordThinkingResult() {
        if ((task1_WinningDirection != Direction.NO_WINNING_DIRECTION) &&
            (task1_WinningDirection != Direction.INCOMPLETE)
        ) {
            // Task1 has the winning move
            Log.i(
                Global.DEBUG_PREFIX,
                "Task #1 has winning move with direction : $task1_WinningDirection"
            )
            val winningSolverGridPos = Pos(task1WinningRow, task1WinningCol)
            val winningDir = task1_WinningDirection

            _winningDirection_from_tasks = task1_WinningDirection

            val movingChain =
                buildMovingChain(winningSolverGridPos.row, winningSolverGridPos.col, winningDir)

            setIDLEstate(
                winningDirection = winningDir,
                winningMovingChain = movingChain,
                mode = SolverUiState.SolverGameMode.IdleFoundSolution)

        } else {

            // Task1 does not have the winning result. NOw check on task #2
            if ((task2_WinningDirection != Direction.NO_WINNING_DIRECTION) &&
                (task2_WinningDirection != Direction.INCOMPLETE)
            ) {
                Log.i(
                    Global.DEBUG_PREFIX,
                    "Task #2 has winning move with direction: $task2_WinningDirection"
                )
                // Task2 has the winning move
                val winningSolverGridPos = Pos(task2WinningRow, task2WinningCol)
                val winningDir = task2_WinningDirection

                _winningDirection_from_tasks = task2_WinningDirection

                val movingChain =
                    buildMovingChain(winningSolverGridPos.row, winningSolverGridPos.col, winningDir)

                setIDLEstate(
                    winningDirection = winningDir,
                    winningMovingChain = movingChain,
                    mode = SolverUiState.SolverGameMode.IdleFoundSolution
                )

            } else {

                // Neither Task #1 nor Task #2 has winning result
                _winningDirection_from_tasks = Direction.NO_WINNING_DIRECTION

                _uiState.update { currentState ->
                    currentState.copy(
                        winningDirection = Direction.NO_WINNING_DIRECTION,
                        winningMovingChain = mutableStateListOf(),
                        solverGameState = SolverUiState.SolverGameMode.IdleNoSolution
                    )
                }
            }
        }
        gThinkingProgress = 0
    }

    /**
     * Update [uiState]  thinking status to Idle state.
     *
     * @param winningDirection If this is idle with winning move, then this is direction of winning move.
     * Otherwise it defaults to "No winning direction"
     * @param winningMovingChain If this is idle with winning move, then this describe the movement
     * details.
     * @param mode Solver game state
     */
    fun setIDLEstate(
        winningDirection: Direction = Direction.NO_WINNING_DIRECTION,
        winningMovingChain: List<MovingRec> = mutableListOf(),
        mode: SolverUiState.SolverGameMode = SolverUiState.SolverGameMode.Idle
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                winningDirection = winningDirection,
                winningMovingChain = winningMovingChain,
                solverGameState = mode
            )
        }
        gThinkingProgress = 0
    }

    /**
     * Process the first task.
     *
     * It is tracked using the hardcoded task #1 meta data
     *
     * @param totalBallCnt Used to determine whether it has winnable move or not
     */
    private fun processTask1(totalBallCnt: Int) {
        try {
            Log.i("${Global.DEBUG_PREFIX} Task 1", "Task 1 has started")
            val game1 = SolverEngine()
            game1.populateGrid(_ballPositionList)

            val (direction, finalRow, finalCol) = game1.foundWinningMove(
                totalBallCnt, 1, 1
            )

            Log.i(
                "${Global.DEBUG_PREFIX} Finish Task #1",
                "Conclusion: Direction = $direction, row=$finalRow, col=$finalCol"
            )

            task1_WinningDirection = direction

            when (direction) {

                Direction.INCOMPLETE -> {
                    Log.i(
                        Global.DEBUG_PREFIX,
                        "Task #1 got incomplete. It expect task2 has deterministic result"
                    )
                }

                Direction.NO_WINNING_DIRECTION -> {
                    Log.i(Global.DEBUG_PREFIX, "Task #1 concluded there is no winning move")
                    if (gMultipleThread) gThinkingThread2.interrupt()
                }

                else -> {
                    // Task #1 got winning move
                    task1_WinningDirection = direction
                    task1WinningRow = finalRow
                    task1WinningCol = finalCol
                    task1_WinningDirection = direction

                    Log.i(Global.DEBUG_PREFIX, "Attempting to interrupt task #2")
                    if (gMultipleThread) gThinkingThread2.interrupt()
                }
            }
        } catch (e: InterruptedException) {
            Log.i("${Global.DEBUG_PREFIX} 1", "Interruption detected")
        }
    }

    /**
     * Process the 2nd task.
     *
     * It is tracked using the hardcoded task #2 meta data
     *
     * @param totalBallCnt Used to determine whether it has winnable move or not
     */
    private fun processTask2(totalBallCnt: Int) {
        try {
            Log.i("${Global.DEBUG_PREFIX} Task 2", "Task 2 has started")
            val game2 = SolverEngine()
            game2.populateGrid(_ballPositionList)
            task2_WinningDirection = Direction.INCOMPLETE
            val (direction, finalRow, finalCol) = game2.foundWinningMove(
                totalBallCnt, 1, -1
            )

            Log.i(
                "${Global.DEBUG_PREFIX} Finish Task #2",
                "Conclusion: Direction = $direction, row=$finalRow, col=$finalCol"
            )

            task2_WinningDirection = direction

            when (direction) {

                Direction.INCOMPLETE -> {
                    Log.i(
                        Global.DEBUG_PREFIX,
                        "Task #2 got incomplete. It expect task1 has deterministic result"
                    )
                }

                Direction.NO_WINNING_DIRECTION -> {
                    Log.i(Global.DEBUG_PREFIX, "Task #2 concluded there is no winning move")
                    gThinkingThread1.interrupt()
                }

                else -> {
                    // Task #2 got the winning move
                    task2_WinningDirection = direction
                    task2WinningRow = finalRow
                    task2WinningCol = finalCol
                    task2_WinningDirection = direction

                    Log.i(Global.DEBUG_PREFIX, "Attempting to interrupt task #1")
                    gThinkingThread1.interrupt()
                }
            }
        } catch (e: InterruptedException) {
            Log.i("${Global.DEBUG_PREFIX} Task 2", "Interruption detected")
        }
    }

    /**
     * Update the [uiState] to latest accurate progress level while it is still searching for
     * winnable move.
     */
    private fun showProcessingActivity() {
        var currentValue = 0.0F
        _totalProcessCount =
            (((_totalBallInCurrentMove - 1) * 4) * (_totalBallInCurrentMove * 4)).toFloat()

        while (_uiState.value.solverGameState == SolverUiState.SolverGameMode.Thinking) {
            // We track two level processing = level #1: 4 direction x level 2: 4 directions = 16
            val newValue: Float =
                (gThinkingProgress.toFloat() / (_totalProcessCount) * 100.0).toFloat()

            if (newValue > currentValue) {
                currentValue = newValue
                _uiState.update { currentState ->
                    currentState.copy(
                        thinkingProgressLevel = currentValue,
                        solverGameState = SolverUiState.SolverGameMode.Thinking
                    )
                }
            }

            // Wait 1.5 seconds. The reason why we split into three 500ms calls is to allow sooner
            // loop breakout when it has finished thinking
            if (_uiState.value.solverGameState == SolverUiState.SolverGameMode.Thinking) Thread.sleep(500)
            if (_uiState.value.solverGameState == SolverUiState.SolverGameMode.Thinking) Thread.sleep(500)
            if (_uiState.value.solverGameState == SolverUiState.SolverGameMode.Thinking) Thread.sleep(500)
        }
        Log.i(Global.DEBUG_PREFIX, "Finished thinking")
    }

    /**
     * Calculate number of boxes to move the ball as it go toward a win
     *
     * @param uiState Current game UI state
     *
     * @return number of boxes to move the ball
     */
    fun getWinningMoveCount(uiState: SolverUiState): Int {
        val game = SolverEngine()
        game.populateGrid(_ballPositionList)

        var winningMoveCount = 0

        var targetRow: Int
        var targetCol: Int

        if (uiState.winningMovingChain.isEmpty()) {
            assert(true) { "Got unexpected empty list moving chain." }
        }

        val winningRow = uiState.winningMovingChain[0].pos.row
        val winningCol = uiState.winningMovingChain[0].pos.col

        if (uiState.winningDirection == Direction.UP) {
            targetRow = game.findTargetRowOnMoveUp(winningRow, winningCol)
            winningMoveCount = winningRow - targetRow
        }

        if (uiState.winningDirection == Direction.DOWN) {
            targetRow = game.findTargetRowOnMoveDown(winningRow, winningCol)
            winningMoveCount = targetRow - winningRow
        }

        if (uiState.winningDirection == Direction.RIGHT) {
            targetCol = game.findTargetColOnMoveRight(winningRow, winningCol)
            winningMoveCount = targetCol - winningCol
        }

        if (uiState.winningDirection == Direction.LEFT) {
            targetCol = game.findTargetColOnMoveLeft(winningRow, winningCol)
            winningMoveCount = winningCol - targetCol
        }

        return (winningMoveCount)
    }

    /**
     * Move the ball toward a win
     *
     * @param uiState Current game UI state
     */
    fun makeWinningMove(uiState: SolverUiState) {
        val game = SolverEngine()
        game.populateGrid(_ballPositionList)

        var targetRow: Int
        var targetCol: Int

        if (uiState.winningMovingChain.isEmpty()) {
            assert(true) { "Got unexpected empty list moving chain." }
        }

        val winningRow = uiState.winningMovingChain[0].pos.row
        val winningCol = uiState.winningMovingChain[0].pos.col

        if (uiState.winningDirection == Direction.UP) {
            targetRow = game.findTargetRowOnMoveUp(winningRow, winningCol)
            game.moveUp(winningRow, targetRow, winningCol)
            _ballPositionList.clear()
            _ballPositionList = game.updateBallList()
        }

        if (uiState.winningDirection == Direction.DOWN) {
            targetRow = game.findTargetRowOnMoveDown(winningRow, winningCol)
            game.moveDown(winningRow, targetRow, winningCol)
            _ballPositionList.clear()
            _ballPositionList = game.updateBallList()
        }

        if (uiState.winningDirection == Direction.RIGHT) {
            targetCol = game.findTargetColOnMoveRight(winningRow, winningCol)
            game.moveRight(winningCol, targetCol, winningRow)
            _ballPositionList.clear()
            _ballPositionList = game.updateBallList()
        }

        if (uiState.winningDirection == Direction.LEFT) {
            targetCol = game.findTargetColOnMoveLeft(winningRow, winningCol)
            game.moveLeft(winningCol, targetCol, winningRow)
            _ballPositionList.clear()
            _ballPositionList = game.updateBallList()
        }

        _uiState.value.winningDirection = Direction.NO_WINNING_DIRECTION

        saveBallPositions()
    }

    /**
     * Build a list of move records on this single move. If only one ball is involved, then this
     * will be a chain of 1 ball record movement.
     *
     * @param initialRow Row of position to move from
     * @param initialCol Column of position to move from
     * @param direction Direction to move to
     *
     * @return List of movement records, If this only has 1 move, then this will be a chain of
     * one movement record. It will return an empty list if there is no movement needed.
     */
    private fun buildMovingChain(
        initialRow: Int,
        initialCol: Int,
        direction: Direction
    ): List<MovingRec> {
        val movingList = mutableListOf<MovingRec>()

        var chainRecInfo = findFreeSpaceCount(initialRow, initialCol, direction, firstMove = true)
        var movingDistance = chainRecInfo.first
        var nextSourcePos = chainRecInfo.second

        // For the first ball in chain, we must have at least one free space to move
        if (movingDistance == 0) return movingList

        movingList.add(MovingRec(Pos(initialRow, initialCol), movingDistance))

        // For subsequent ball in the chain, it is fine to have zero distance
        var fallenOffEdge = false
        while (!fallenOffEdge) {
            if (nextSourcePos == null) {
                fallenOffEdge = true
            } else {
                val currentSourcePos = nextSourcePos
                chainRecInfo =
                    findFreeSpaceCount(
                        currentSourcePos.row,
                        currentSourcePos.col,
                        direction,
                        firstMove = false
                    )
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
     *
     *  @return Pair<Int, SolverGridPos?> where the first element is the number of free space and
     *  second element is the position of the next ball
     *
     */
    private fun findFreeSpaceCount(
        row: Int,
        col: Int,
        direction: Direction,
        firstMove: Boolean
    ): Pair<Int, Pos?> {
        var xOffset = 0
        var yOffset = 0
        var sourceRow = row
        var sourceCol = col

        when (direction) {
            Direction.RIGHT -> yOffset = 1
            Direction.LEFT -> yOffset = -1
            Direction.UP -> xOffset = -1
            Direction.DOWN -> xOffset = 1
            else -> assert(true) { "Got unexpected direction value" }
        }

        if ((direction == Direction.LEFT) && (col == 0)) {
            if (firstMove) return (Pair(0, null))
            return (Pair(2, null))
        }

        if ((direction == Direction.RIGHT) && (col == Global.MAX_COL_SIZE)) {
            if (firstMove) return (Pair(0, null))
            return (Pair(2, null))
        }

        if ((direction == Direction.UP) && (row == 0)) {
            if (firstMove) return (Pair(0, null))
            return (Pair(2, null))
        }

        if ((direction == Direction.DOWN) && (row == Global.MAX_ROW_SIZE)) {
            if (firstMove) return (Pair(0, null))
            return (Pair(2, null))
        }

        var newRow: Int
        var newCol: Int

        var distance = 0
        var hitWall = false

        var nextSourcePos: Pos? = null

        while (!hitWall) {
            newRow = sourceRow + xOffset
            newCol = sourceCol + yOffset

            if ((newRow == -1) || (newRow == Global.MAX_ROW_SIZE) ||
                (newCol == -1) || (newCol == Global.MAX_COL_SIZE)
            ) {
                distance++ // Add two to make it fall off the edge
                distance++
                hitWall = true
            } else {
                if (_ballPositionList.contains(Pos(newRow, newCol))) {
                    nextSourcePos = Pos(newRow, newCol)
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
}

