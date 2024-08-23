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
import com.darblee.flingaid.utilities.SingleArgSingletonHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.File

/**
 * **View Model for the Solver Game**
 *
 * - It Manage the business logic for the game. This includes the thinking activity.
 * - It holds the source of truth for the solve game state.
 * - It prepare data for the UI and follow UDF (Unidirectional Data FLow) to the UI
 * - It has a longer lifetime than the composable.
 * - All fields in UI state [SolverUIState] is stateless. It can be re-created after reading the game board ball
 * positions. The application can survive process death. No need to save state to persistent
 * storage.
 * - There can only be one [SolverViewModel] instance as it use the companion object
 *
 * - Here is the overall state machine:
 * [State Machine](https://github.com/darblee/FlingAid-new/blob/master/README.md)
 *
 *
 * **Ball Management**
 *
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
class SolverViewModel(gGameFile: File) : ViewModel() {
    companion object : SingleArgSingletonHolder<SolverViewModel, File>(::SolverViewModel)

    /**
     * [_totalProcessCount] is the total amount of thinking process involved in the current move.
     *
     * The total is 2 levels of thinking.
     * Next level is (number of balls - 1) x 4 directions
     * Current level is the number of balls x 4 direction
     * Total = (Next level) x (current level)
     */
    private var _totalProcessCount: Float = 0.0F

    /**
     * The direction of the winning move obtained from the tasks calculation
     */
    private var _winningDirectionFromTasks = Direction.NO_WINNING_DIRECTION

    /********************************* BALL MANAGEMENT ********************************************/

    /**
     * Board, represented as a list of ball positions
     */
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
    private fun loadGameFile(file: File) {
        viewModelScope.launch (Dispatchers.IO){
            Log.i(Global.DEBUG_PREFIX, "Solver: Loading Game file")
            _solverBallPos.setGameFile(file)
            _solverBallPos.loadBallListFromFile()

            setModeBaseOnBoard()
        }
    }

    /********************************* SOLVER GAME MANAGEMENT *************************************/

    /**
     * Contain the UI state of the solver game. This is used by the game screens to display
     * proper UI elements. Various composable will automatically update when that state changes
     *
     * For reference, see [ https://dev.to/zachklipp/introduction-to-the-compose-snapshot-system-19cn ]
     */
    private val _uiSolverState = MutableStateFlow(SolverUIState())

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
    internal var uiState: StateFlow<SolverUIState> = _uiSolverState.asStateFlow()
        private set

    /**
     * Initialize the SolverViewModel
     */
    init {
        Log.i(Global.DEBUG_PREFIX, "SolverViewModel initialization")

        gThinkingProgress = 0
        _solverBallPos.clearGame()
        loadGameFile(gGameFile)
    }

    /**
     * Clean-up routine before exiting the SolverVIewModel
     *
     * @return
     * - true Clean-up is done. It is safe to exit the view model
     * - false Unable to clean-up or in a middle of doing something. Do not exit the view model
     */
    fun canExitSolverScreen(): Boolean {
        when (_uiSolverState.value.mode) {
            SolverUIState.SolverMode.Initialization -> { return false }
            SolverUIState.SolverMode.AnnounceNoPossibleSolution -> { return true }
            SolverUIState.SolverMode.AnnounceVictory -> { return false }  // Let it finish announce victory
            SolverUIState.SolverMode.HasWinningMoveWaitingToMove -> { return true }
            SolverUIState.SolverMode.MoveBall -> { return false } // Let it finish to move the ball
            SolverUIState.SolverMode.NoMoveAvailable -> { return true }
            SolverUIState.SolverMode.ReadyToFindSolution -> { return true }
            SolverUIState.SolverMode.Thinking -> {
                stopThinking()
                return true
            }
        }
    }

    /**
     * Reset the entire solver game
     * - Clear the board
     * - Remove any saved game file
     */
    fun reset() {
        Log.i(Global.DEBUG_PREFIX, "SolverViewModel reset")

        gThinkingProgress = 0
        _solverBallPos.clearGame()
        setModeBaseOnBoard()
    }

    /**
     * If the position is blank, then place the ball.
     * If ball already exist on that location, then remove the ball
     *
     * @param solverGridPos The position of the ball
     */
    fun toggleBallPosition(solverGridPos: Pos) {

        if (uiState.value.mode == SolverUIState.SolverMode.Thinking) {
            stopThinking()
            Thread.sleep(500)
        }

        if (_solverBallPos.ballList.contains(solverGridPos)) {
            _solverBallPos.ballList.remove(solverGridPos)
        } else {
            _solverBallPos.ballList.add(solverGridPos)
        }

        viewModelScope.launch {
            _solverBallPos.saveBallListToFile()

            setModeBaseOnBoard()
        }
    }

    /**
     * New board so we need to set mode accordingly
     */
    private fun setModeBaseOnBoard() {
        task1WinningRow = -1
        task1WinningCol = -1
        task2WinningRow = -1
        task2WinningCol = -1
        gThinkingProgress = 0

        if (ballCount() < 2) {
            setModeToNoMoveAvailable()
        } else {
            // There is more than one ball on board. User can start looking for a possible solution
            setMode(SolverUIState.SolverMode.ReadyToFindSolution)
        }
    }

    /********************************  Set mode routines ******************************************/

    private fun setMode(mode: SolverUIState.SolverMode) {
        _uiSolverState.update { curState -> curState.copy(_mode = mode) }
    }

    /**
     * Set UI state to "One Ball Left" mode
     */
    fun setModeToNoMoveAvailable() {
        setMode(SolverUIState.SolverMode.NoMoveAvailable)
    }

    /**
     * Set mode to "Show Ball Movement"
     *
     */
    fun setModeToShowBallMovement(
        winningDirection: Direction,
        winningMovingChain: BallMoveSet
    ) {
        val moveBallRec = SolverUIState.SolverMode.MoveBall
        moveBallRec.winningDirMoveBall = winningDirection
        moveBallRec.winingMovingChainMoveBall = winningMovingChain
        _uiSolverState.update { curState ->
            curState.copy(
                _mode = moveBallRec
            )
        }
    }

    /**
     * Update [SolverUIState] to "ReadyToMove" mode. Provide the associated information such
     * winning direction and winning moving chain
     *
     * @param winningDirection Direction of the move
     * @param winningMovingChain MovingCHain of the move
     */
    private fun setModeToReadyToMove(
        winningDirection: Direction,
        winningMovingChain: BallMoveSet
    ) {
        val readyToMoveRec = SolverUIState.SolverMode.HasWinningMoveWaitingToMove
        readyToMoveRec.winingMovingChainPreview = winningMovingChain
        readyToMoveRec.winningDirectionPreview = winningDirection

        _uiSolverState.update { curState ->
            curState.copy(
                _mode = readyToMoveRec
            )
        }
    }

    /**
     * Update [SolverUIState] to "Thinking" mode.
     */
    private fun setModeToThinking(thinkingRec: SolverUIState.SolverMode.Thinking)
    {
        _uiSolverState.update { currentStatus ->
            currentStatus.copy(
                _mode = thinkingRec,

                // _recomposeFlag is used to trigger recomposition manually as changing the thinkgingRec
                // field value(s) did not trigger a composition
                recomposeFlag = !_uiSolverState.value.recomposeFlag
            )
        }

    }

    /**
     * Update [SolverUIState] to "Thinking" mode. This is the initial thinking state
     */
    private fun setModeToThinkingInitialState()
    {
        val thinkingRec: SolverUIState.SolverMode.Thinking = SolverUIState.SolverMode.Thinking
        thinkingRec.progress = 0.0f
        thinkingRec.rejectedBalls = listOf()

        setModeToThinking(thinkingRec)
    }

    /**
     *  Update "Thinking" mode with updated progress
     */
    private fun setModeThinkingWithUpdatedProgressLevel(newProgressLvl : Float)
    {
        val thinkingRec: SolverUIState.SolverMode.Thinking =
            SolverUIState.SolverMode.Thinking
        thinkingRec.progress = newProgressLvl

        setModeToThinking(thinkingRec)
    }

    /**
     * Update "Thinking" mode with one more  reject ball
     */
    private fun setModeThinkingWithRejectBallAddition(row: Int, col: Int)
    {
        val thinkingRec: SolverUIState.SolverMode.Thinking =
            SolverUIState.SolverMode.Thinking

        // There is a remote chance that we try to add rejected ball that already exist.
        // This can happen when 2 coroutine threads process the same ball
        if (thinkingRec.rejectedBalls.contains(Pos(row,col))) return

        val newRejects = thinkingRec.rejectedBalls.toMutableList()

        newRejects.add(Pos(row, col))

        thinkingRec.rejectedBalls = newRejects

        setModeToThinking(thinkingRec)
    }


    /****************************** Thinking routines *********************************************/

    /**
     * First thread to search for solution
     */
    @Volatile
    private lateinit var gSearchJob1 : Job

    @Volatile
    private lateinit var gSearchJob2 : Job

    private var task1WinningRow = -1
    private var task1WinningCol = -1
    private var task2WinningRow = -1
    private var task2WinningCol = -1

    fun findWinningMove()
    {
        setModeToThinkingInitialState()
        gTask1WinningDirection = Direction.INCOMPLETE
        gTask2WinningDirection = Direction.INCOMPLETE

        _winningDirectionFromTasks = Direction.INCOMPLETE
        gThinkingProgress = 0

        viewModelScope.launch(Dispatchers.Default) {
            val gTotalBallCount = ballCount()

            gSearchJob1 = launch (Dispatchers.Default) { searchAgent1(gTotalBallCount) }
            gSearchJob2 = launch (Dispatchers.Default) { searchAgent2(gTotalBallCount) }
            launch (Dispatchers.Default){ showSearchingProgress(gTotalBallCount) }
            joinAll(gSearchJob1, gSearchJob2)

            recordThinkingResult()
        }
    }

    /**
     * Search for solution starting from top. Called by first coroutine.
     *
     * @param totalBallCnt Total number of balls in the current move
     */
    private fun searchAgent1(totalBallCnt: Int) {
        Log.i("${Global.DEBUG_PREFIX} Agent 1", "Agent 1 has started")
        val game1 = FlickerEngine()
        game1.populateGrid(_solverBallPos.ballList)

        val (direction, finalRow, finalCol) = game1.foundWinningMove(
            totalBallCnt, 1, 1,
            onBallReject =  { row, col -> setModeThinkingWithRejectBallAddition(row, col) }
        )

        Log.i(
            "${Global.DEBUG_PREFIX} Finish Task #1",
            "Conclusion: Direction = $direction, row=$finalRow, col=$finalCol"
        )

        gTask1WinningDirection = direction

        when (direction) {

            Direction.INCOMPLETE -> {
                Log.i(
                    Global.DEBUG_PREFIX,
                    "Task #1 got incomplete. It expect task2 to have solution or incomplete result"
                )
            }

            Direction.NO_WINNING_DIRECTION -> {
                Log.i(Global.DEBUG_PREFIX, "Task #1 concluded there is no winning move")
            }

            else -> {
                // Task #1 got winning move
                gTask1WinningDirection = direction
                task1WinningRow = finalRow
                task1WinningCol = finalCol
            }
        }
    }

    /**
     * Search for solution starting from bottom. Called by second coroutine.
     *
     * @param totalBallCnt Total number of balls in the current move
     */
    private fun searchAgent2(totalBallCnt: Int) {
        Log.i("${Global.DEBUG_PREFIX} Agent 2", "Agent 2 has started")
        val game2 = FlickerEngine()
        game2.populateGrid(_solverBallPos.ballList)

        val (direction, finalRow, finalCol) = game2.foundWinningMove(
            totalBallCnt, 1, -1,
            onBallReject =  { row, col -> setModeThinkingWithRejectBallAddition(row, col) }
        )

        Log.i(
            "${Global.DEBUG_PREFIX} Finish Task #2",
            "Conclusion: Direction = $direction, row=$finalRow, col=$finalCol"
        )

        gTask2WinningDirection = direction

        when (direction) {

            Direction.INCOMPLETE -> {
                Log.i(
                    Global.DEBUG_PREFIX,
                    "Task #2 got incomplete. It expect task1 to have solution or incomplete result"
                )
            }

            Direction.NO_WINNING_DIRECTION -> {
                Log.i(Global.DEBUG_PREFIX, "Task #2 concluded there is no winning move")
            }

            else -> {
                // Task #2 got winning move
                gTask2WinningDirection = direction
                task2WinningRow = finalRow
                task2WinningCol = finalCol

            }
        }
    }

    /**
     * Update the [_uiSolverState] with the result of the thinking task(s).
     */
    private fun recordThinkingResult() {
        if ((gTask1WinningDirection != Direction.NO_WINNING_DIRECTION) &&
            (gTask1WinningDirection != Direction.INCOMPLETE)
        ) {
            // Task1 has the winning move
            Log.i(
                Global.DEBUG_PREFIX,
                "Task #1 has winning move with direction : $gTask1WinningDirection"
            )
            val winningSolverGridPos = Pos(task1WinningRow, task1WinningCol)
            val winningDir = gTask1WinningDirection

            _winningDirectionFromTasks = gTask1WinningDirection

            val movingChain = _solverBallPos.buildMovingChain(
                winningSolverGridPos.row,
                winningSolverGridPos.col,
                winningDir
            )

            setModeToReadyToMove(winningDir, movingChain)

        } else {

            // Task1 does not have the winning result. Now check on task #2
            if ((gTask2WinningDirection != Direction.NO_WINNING_DIRECTION) &&
                (gTask2WinningDirection != Direction.INCOMPLETE)
            ) {
                Log.i(
                    Global.DEBUG_PREFIX,
                    "Task #2 has winning move with direction: $gTask2WinningDirection"
                )
                // Task2 has the winning move
                val winningSolverGridPos = Pos(task2WinningRow, task2WinningCol)
                val winningDir = gTask2WinningDirection

                _winningDirectionFromTasks = gTask2WinningDirection

                val movingChain = _solverBallPos.buildMovingChain(
                    winningSolverGridPos.row,
                    winningSolverGridPos.col,
                    winningDir
                )

                setModeToReadyToMove(winningDir, movingChain)

            } else {

                Log.i(Global.DEBUG_PREFIX, "We have no clear winner")
                Log.i(Global.DEBUG_PREFIX, "Need to determine if we have no winnable move or incomplete")

                // Neither Task #1 nor Task #2 has winning result
                // However, we do not know if the tasks got cancelled or both tasks concluded
                // there is no solution.
                _winningDirectionFromTasks = Direction.NO_WINNING_DIRECTION

                // Need to check if any task got cancelled by checking if it got Direction.INCOMPLETE
                // status
                if ((gTask1WinningDirection == Direction.NO_WINNING_DIRECTION) ||
                        (gTask2WinningDirection == Direction.NO_WINNING_DIRECTION))
                {
                    setMode(SolverUIState.SolverMode.AnnounceNoPossibleSolution)
                } else {
                    setMode(SolverUIState.SolverMode.ReadyToFindSolution)
                }
            }
        }

        gThinkingProgress = 0
        task1WinningRow = -1
        task1WinningCol = -1
        task2WinningRow = -1
        task2WinningCol = -1
    }

    /**
     * Quickly stop thinking progress threads.
     *
     * Make sure all threads has completed before this function exits.
     */
    fun stopThinking() {
        if (gSearchJob1.isActive) {
            Log.i(
                Global.DEBUG_PREFIX,
                "Thread 1 is alive, make it end quickly by setting direction to \"No Winning Direction\""
            )
            gTask1WinningDirection = Direction.NO_WINNING_DIRECTION
        }

        if (gSearchJob2.isActive) {
            Log.i(
                Global.DEBUG_PREFIX,
                "Thread 2 is alive, make it end quickly by setting direction to \"No Winning Direction\""
            )
            gTask2WinningDirection = Direction.NO_WINNING_DIRECTION
        }

        while (gSearchJob1.isActive || gSearchJob2.isActive) {
            Thread.sleep(250)
        }
        gThinkingProgress = 0
        task1WinningRow = -1
        task1WinningCol = -1
        task2WinningRow = -1
        task2WinningCol = -1
    }

    /**
     * Update the [uiState] to latest accurate progress level while it is still searching for
     * winnable move.
     */
    private suspend fun showSearchingProgress(totalBallCount: Int) {
        val currentValue = 0.0F
        _totalProcessCount =
           ((totalBallCount * 4)* ((totalBallCount - 1) * 4)).toFloat()

        while (_uiSolverState.value.mode == SolverUIState.SolverMode.Thinking) {
            // We track two level processing = level #1: 4 direction x level 2: 4 directions = 16
            var newValue: Float =
                (gThinkingProgress.toFloat() / (_totalProcessCount) * 100.0).toFloat()

            if (newValue > 99.9) newValue = 99.9F

            if (newValue > currentValue) {
                setModeThinkingWithUpdatedProgressLevel(newValue)
            }

            // Wait 1.5 seconds. The reason why we split into three 500ms calls is to allow sooner
            // loop breakout when it has finished thinking
            if (_uiSolverState.value.mode == SolverUIState.SolverMode.Thinking) delay(500)
            if (_uiSolverState.value.mode == SolverUIState.SolverMode.Thinking) delay(500)
            if (_uiSolverState.value.mode == SolverUIState.SolverMode.Thinking) delay(500)
        }
        Log.i(Global.DEBUG_PREFIX, "Finished thinking")
    }

    /**
     * Calculate number of spaces to move the ball as it go toward a win
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
        val game = FlickerEngine()
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
        val game = FlickerEngine()
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

        viewModelScope.launch {
            _solverBallPos.saveBallListToFile()

            // If there is more ball, then automatically look for the next winnable move
            if (ballCount() > 1) {
                findWinningMove()
            } else {
                if (ballCount() == 1) {
                    setMode(SolverUIState.SolverMode.AnnounceVictory)
                } else {
                    assert(true) { "Got unexpected ball count state." }
                }
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

