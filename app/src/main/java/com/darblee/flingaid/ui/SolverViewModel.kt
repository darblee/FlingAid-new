package com.darblee.flingaid.ui

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darblee.flingaid.BallMoveSet
import com.darblee.flingaid.Direction
import com.darblee.flingaid.Global
import com.darblee.flingaid.utilities.FlickerBoard
import com.darblee.flingaid.utilities.Pos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CyclicBarrier

/**
 * **View Model for the Solver Game**
 *
 * - It manage the business logic for the game. This includes the thinking activity.
 * - It is the sole source of truth for the solve game state.
 * - It prepare data for the UI. All information flow one direction to the UI
 * - It has a longer lifetime than the composable.
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
 * - [findWinningMove]
 * - [getWinningMoveCount]
 * - [moveBallToWin]
 */
object SolverViewModel : ViewModel() {

    /**
     * Track the thinking process to find the winning move.
     *
     * _Developer's note:_ `internal` means it will only be visible within that module. A module
     * is a set of Kotlin files that are compiled together e.g. a library or application. It provides real
     * encapsulation for the implementation details. In this case, it is shared wit the SolverEngine class.
     */
    internal var gThinkingProgress = 0

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

    private var _solverBallPos = FlickerBoard()

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
    private fun ballPositionList(): SnapshotStateList<Pos> {
        return (_solverBallPos.ballList)
    }

    /**
     * Return the number of active ball
     *
     * @return Number of active balls
     */
    fun ballCount(): Int {
        return (_solverBallPos.getBallCount())
    }

    /**
     * Load the game file
     *
     * @param file game file
     */
    fun loadGameFile(file: File) {
        _solverBallPos.setGameFile(file)
        _solverBallPos.loadBallListFromFile()

        setModeBaseOnBoard()
    }

    /********************************* SOLVER GAME MANAGEMENT ****************************/

    /**
     * Contain the UI state of the solver game. This is used by the game screens to display
     * proper UI elements. Various composable will automatically update when that state changes
     *
     * For reference, see [ https://dev.to/zachklipp/introduction-to-the-compose-snapshot-system-19cn ]
     */
    private val _uiSolverState = MutableStateFlow(SolverUiState())

    /**
     * Holds the [_uiSolverState] as a state flow.
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
     * In the SolverViewModel class, add the following [_uiSolverState] property.
     *
     * `private set` this is internally modifiable and read-only access from the outside.
     * This ensure information flow in one direction from the view model to the UI
     */
    internal var uiState: StateFlow<SolverUiState> = _uiSolverState.asStateFlow()
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
        _solverBallPos.ballList.clear()
        _solverBallPos.removeGameFile()

        setModeBaseOnBoard()
    }

    /**
     * If the position is blank, then place the ball.
     * If ball already exist on that location, then remove the ball
     *
     * @param solverGridPos The position of the ball
     */
    fun toggleBallPosition(solverGridPos: Pos) {
        if (_solverBallPos.ballList.contains(solverGridPos)) {
            _solverBallPos.ballList.remove(solverGridPos)
        } else {
            _solverBallPos.ballList.add(solverGridPos)
        }

        _solverBallPos.saveBallListToFile()

        setModeBaseOnBoard()
    }

    /**
     * New board so we need to set mode accordingly
     */
    private fun setModeBaseOnBoard() {
        if (ballCount() < 2) {
            setModeToNoMoveAvailable()
        } else {
            // There is more than one ball on board. User can start looking for a possible solution
            setModeToReadyToFindSolution()
        }

    }

    /******************  Set mode routines ********************************/
    /**
     * Set UI state to "Announce Victory" mode
     */
    private fun setModeToAnnounceVictory() {
        _uiSolverState.update { currentState ->
            currentState.copy(
                _mode = SolverUiState.SolverMode.AnnounceVictory,
            )
        }
    }

    /**
     * Set UI state to "One Ball Left" mode
     */
    fun setModeToNoMoveAvailable() {
        _uiSolverState.update { currentState ->
            currentState.copy(
                _mode = SolverUiState.SolverMode.NoMoveAvailable,
            )
        }
    }

    /**
     * Set UI state to "Waiting on User Next Move
     */
    private fun setModeToReadyToFindSolution() {
        _uiSolverState.update { currentState ->
            currentState.copy(
                _mode = SolverUiState.SolverMode.ReadyToFindSolution,
            )
        }
    }

    /**
     * Set mode to "Show Ball Movement"
     */
    fun setModeToShowBallMovement(
        winningDirection: Direction,
        winningMovingChain: BallMoveSet
    ) {
        val moveBallRec = SolverUiState.SolverMode.MoveBall
        moveBallRec.winningDirMoveBall = winningDirection
        moveBallRec.winingMovingChainMoveBall = winningMovingChain
        _uiSolverState.update { curState ->
            curState.copy(
                _mode = moveBallRec
            )
        }
    }

    /************************* Thinking routwines ****************/

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
     * Find the winning move.*
     */
    fun findWinningMove() {
        gThinkingProgress = 0
        val thinkingRec: SolverUiState.SolverMode.Thinking = SolverUiState.SolverMode.Thinking
        thinkingRec.progress = 0.0f

        _uiSolverState.update { currentStatus ->
            currentStatus.copy(
                _mode = thinkingRec,
            )
        }
        viewModelScope.launch {

            // For explanation on the formula, see the description for _totalProcessCount
            _totalProcessCount =
                (((_totalBallInCurrentMove - 1) * 4) * (_totalBallInCurrentMove * 4)).toFloat()

            _winningDirection_from_tasks = Direction.NO_WINNING_DIRECTION
            val gTotalBallCount = ballCount()

            task1_WinningDirection = Direction.INCOMPLETE
            task2_WinningDirection = Direction.INCOMPLETE

            gThinkingProgress = 0
            _totalBallInCurrentMove = ballCount()

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
     * Update the [_uiSolverState] with the result of the thinking task(s).
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

            val movingChain = _solverBallPos.buildMovingChain(
                winningSolverGridPos.row,
                winningSolverGridPos.col,
                winningDir
            )

            setModeToReadyToMove(winningDir, movingChain)

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

                val movingChain = _solverBallPos.buildMovingChain(
                    winningSolverGridPos.row,
                    winningSolverGridPos.col,
                    winningDir
                )

                setModeToReadyToMove(winningDir, movingChain)

            } else {

                // Neither Task #1 nor Task #2 has winning result
                _winningDirection_from_tasks = Direction.NO_WINNING_DIRECTION

                _uiSolverState.update { currentState ->
                    currentState.copy(
                        _mode = SolverUiState.SolverMode.AnnounceNoPossibleSolution
                    )
                }
            }
        }
        gThinkingProgress = 0
    }

    /**
     * Update [SolverUiState] to "ReadyToMove" mode. Provide the associated information such
     * winning direction and winning moving chain
     *
     * @param winningDirection Direction of the move
     * @param winningMovingChain MovingCHain of the move
     */
    private fun setModeToReadyToMove(
        winningDirection: Direction,
        winningMovingChain: BallMoveSet
    ) {
        val readyToMoveRec = SolverUiState.SolverMode.ReadyToMove
        readyToMoveRec.winingMovingChainPreview = winningMovingChain
        readyToMoveRec.winningDirectionPreview = winningDirection

        _uiSolverState.update { curState ->
            curState.copy(
                _mode = readyToMoveRec
            )
        }

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
            game1.populateGrid(_solverBallPos.ballList)

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
            game2.populateGrid(_solverBallPos.ballList)
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

        while (_uiSolverState.value.mode == SolverUiState.SolverMode.Thinking) {
            // We track two level processing = level #1: 4 direction x level 2: 4 directions = 16
            val newValue: Float =
                (gThinkingProgress.toFloat() / (_totalProcessCount) * 100.0).toFloat()

            if (newValue > currentValue) {
                currentValue = newValue
                val thinkingRec: SolverUiState.SolverMode.Thinking =
                    SolverUiState.SolverMode.Thinking
                thinkingRec.progress = currentValue
                _uiSolverState.update { currentState ->

                    // _recomposeFlag is used to trigger recomposition manual as changing the progress
                    // did not trigger a composition
                    currentState.copy(
                        _mode = thinkingRec,
                        _recomposeFlag = !_uiSolverState.value._recomposeFlag
                    )
                }
            }

            // Wait 1.5 seconds. The reason why we split into three 500ms calls is to allow sooner
            // loop breakout when it has finished thinking
            if (_uiSolverState.value.mode == SolverUiState.SolverMode.Thinking) Thread.sleep(500)
            if (_uiSolverState.value.mode == SolverUiState.SolverMode.Thinking) Thread.sleep(500)
            if (_uiSolverState.value.mode == SolverUiState.SolverMode.Thinking) Thread.sleep(500)
        }
        Log.i(Global.DEBUG_PREFIX, "Finished thinking")
    }

    /**
     * Calculate number of boxes to move the ball as it go toward a win
     *
     * @param pos Position to move from
     * @param direction Direction of the ball movement
     *
     * @return number of boxes to move the ball
     */
    fun getWinningMoveCount(
        pos: Pos,
        direction: Direction
    ): Int {
        val game = SolverEngine()
        game.populateGrid((_solverBallPos.ballList))

        var winningMoveCount = 0

        var targetRow: Int
        var targetCol: Int

        val winningRow = pos.row
        val winningCol = pos.col

        if (direction == Direction.UP) {
            targetRow = game.findTargetRowOnMoveUp(winningRow, winningCol)
            winningMoveCount = winningRow - targetRow
        }

        if (direction == Direction.DOWN) {
            targetRow = game.findTargetRowOnMoveDown(winningRow, winningCol)
            winningMoveCount = targetRow - winningRow
        }

        if (direction == Direction.RIGHT) {
            targetCol = game.findTargetColOnMoveRight(winningRow, winningCol)
            winningMoveCount = targetCol - winningCol
        }

        if (direction == Direction.LEFT) {
            targetCol = game.findTargetColOnMoveLeft(winningRow, winningCol)
            winningMoveCount = winningCol - targetCol
        }

        return (winningMoveCount)
    }

    /**
     * Move the ball toward a win.
     * - Save the ball position to file.
     * - After ball movement, change the state to no direction.
     *
     * @param pos Position of the ball to move from
     * @param direction Direction of ball movement
     */
    fun moveBallToWin(pos: Pos, direction: Direction) {
        val game = SolverEngine()
        game.populateGrid((_solverBallPos.ballList))

        var targetRow: Int
        var targetCol: Int

        if (direction == Direction.UP) {
            targetRow = game.findTargetRowOnMoveUp(pos.row, pos.col)
            game.moveUp(pos.row, targetRow, pos.col)
            _solverBallPos.ballList.clear()
            _solverBallPos.ballList = game.updateBallList()
        }

        if (direction == Direction.DOWN) {
            targetRow = game.findTargetRowOnMoveDown(pos.row, pos.col)
            game.moveDown(pos.row, targetRow, pos.col)
            _solverBallPos.ballList.clear()
            _solverBallPos.ballList = game.updateBallList()
        }

        if (direction == Direction.RIGHT) {
            targetCol = game.findTargetColOnMoveRight(pos.row, pos.col)
            game.moveRight(pos.col, targetCol, pos.row)
            _solverBallPos.ballList.clear()
            _solverBallPos.ballList = game.updateBallList()
        }

        if (direction == Direction.LEFT) {
            targetCol = game.findTargetColOnMoveLeft(pos.row, pos.col)
            game.moveLeft(pos.col, targetCol, pos.row)
            _solverBallPos.ballList.clear()
            _solverBallPos.ballList = game.updateBallList()
        }

        _solverBallPos.saveBallListToFile()

        // If there is more ball, then automatically look for the next winnable move
        if (ballCount() > 1) {
            findWinningMove()
        } else {
            if (ballCount() == 1) {
                setModeToAnnounceVictory()
            } else {
                assert(true) { "Got unexpected ball count state." }
            }
        }
    }

    /**
     * Draw all the balls in the provided canvas grid
     *
     * @param drawScope The drawing canvas of the grid
     * @param gridSize The grid size
     * @param displayBallImage Actual image of ball o display
     * @param ballsToErase Used during ball animation. We need to temporarily
     * erase the animation ball as the animation routine will display it
     */
    fun drawSolverBallsOnGrid(
        drawScope: DrawScope,
        gridSize: Float,
        displayBallImage: ImageBitmap,
        ballsToErase: BallMoveSet = listOf()
    ) {
        _solverBallPos.drawAllBalls(drawScope, gridSize, displayBallImage, ballsToErase)
    }
}

