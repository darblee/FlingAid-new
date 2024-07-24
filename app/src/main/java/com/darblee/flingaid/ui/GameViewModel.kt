package com.darblee.flingaid.ui

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.darblee.flingaid.Direction
import com.darblee.flingaid.Global
import com.darblee.flingaid.utilities.BallPosition
import com.darblee.flingaid.utilities.Pos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * **View Model for the Game**
 *
 * - It manage the business logic for the game. This includes the thinking activity.
 * - It is the sole source of truth for the solve game state.
 * - It prepare data for the UI. All information flow one direction to the UI
 * - It has a longer lifetime than the composable
 *
 * [State Machine](https://github.com/darblee/FlingAid-new/blob/master/README.md)
 *
 * There can only be one GameViewModel instance. Hence, use the singleton class (object)
 *
 * [SolverUiState] State of UI
 *
 * **Ball Management**
 * Managing the ball on the game board
 * - [loadGameFile] : Define the file to store the ball position information
 * - [ballCount]
 * - [ballPositionList]
 * - [printBalls] Print all the ball positions. Used for debugging purposes.
 * - [buildMovingChain] Moving chain to set-up ball movement animation
 *
 * **Game Play Functions**
 * - [generateNewGame]  Generate a new game based on provided level
 */
object GameViewModel : ViewModel() {

    /**
     *
     * Track the thinking process to find the winning move on the user's hint request
     *
     * _Developer's note:_ `internal` means it will only be visible within that module. A module
     * is a set of Kotlin files that are compiled together e.g. a library or application. It provides real
     * encapsulation for the implementation details. In this case, it is shared wit the SolverEngine class.
     */
    internal var gHintThinkingProgress = 0

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

    private var _gameBallPos = BallPosition()

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
        return (_gameBallPos.ballList)
    }

    /**
     * Return the number of active ball
     *
     * @return Number of active balls
     */
    fun ballCount(): Int {
        return (_gameBallPos.getBallCount())
    }

    /**
     * Load the game file
     *
     * @param file game file
     */
    fun loadGameFile(file: File) {
        _gameBallPos.setGameFile(file)
        _gameBallPos.loadBallListFromFile()
    }

    /********************************* SOLVER GAME MANAGEMENT ****************************/

    /**
     * Contain the UI state of the solver game. This is used by the game screens to display
     * proper UI elements. Various composable will automatically update when that state changes
     *
     * For reference, see [ https://dev.to/zachklipp/introduction-to-the-compose-snapshot-system-19cn ]
     */
    private val _uiGameState = MutableStateFlow(GameUIState())

    /**
     * Holds the [_uiGameState] as a state flow.
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
     * In the GameViewModel class, add the following [_uiGameState] property.
     *
     * `private set` this is internally modifiable and read-only access from the outside.
     * This ensure information flow in one direction from the view model to the UI
     */
    internal var uiState: StateFlow<GameUIState> = _uiGameState.asStateFlow()
        private set

    /**
     * Initialize the SolverViewModel
     */
    init {
        reset()
        gHintThinkingProgress = 0
    }

    /**
     * Reset the entire solver game
     * - Clear the board
     * - Remove any saved game file
     */
    fun reset() {
        _gameBallPos.ballList.clear()
        _gameBallPos.removeGameFile()

        gameSetIDLEstate()
    }

    /**
     * Update [uiState]  thinking status to Idle state.
     *
     */
    fun gameSetIDLEstate(
    ) {
        _uiGameState.update { currentState ->
            currentState.copy(
                state = GameState.Idle,
                moveFromRow = 0,
                moveFromCol = 0
            )
        }
        gHintThinkingProgress = 0
    }

    /**
     * Generate a new game based on level.
     *
     * It will add balls to the ball list
     */
    fun generateNewGame(level: Int) {
        _gameBallPos.ballList.clear()
        _gameBallPos.ballList.add(Pos(1, 2))
        _gameBallPos.ballList.add(Pos(2, 2))
        _gameBallPos.ballList.add(Pos(5, 2))
        _gameBallPos.ballList.add(Pos(3, 4))
        _gameBallPos.ballList.add(Pos(5, 6))
        _gameBallPos.saveBallListToFile()
    }

    var gMovingChain : List<MovingRec> = mutableListOf()

    /**
     * Confirm if this is a valid move to process. If not, then return false.
     * If true, then set-up moving chain, which will be used in next step
     *
     * @param initialRow Row position to move the ball from
     * @param initialCol Column position to move the ball from
     * @param direction Direction of the ball movement
     *
     * @return
     * - true - This is a valid move. Set-up moving chain
     * - false. This is not a valid move. It either has no space to move or there is no ball in the
     * provided position.
     */
    fun validMove(initialRow: Int, initialCol: Int, direction : Direction): Boolean
    {
        if (!(_gameBallPos.ballList.contains(Pos(initialRow, initialCol)))) return false

        gMovingChain = mutableListOf()

        Log.i(Global.DEBUG_PREFIX, "Initiate ball movement from $initialRow, $initialCol")
        gMovingChain = _gameBallPos.buildMovingChain(initialRow, initialCol, direction)
        return gMovingChain.isNotEmpty()
    }

    fun moveBallPos(row: Int, col: Int) {
        _uiGameState.update { currentState ->
            currentState.copy(
                state = GameState.MoveBall,
                moveFromRow = row,
                moveFromCol = col,
            )
        }
    }

    /**
     * Print all the balls.
     *
     * Used primarily for debugging purposes
     */
    fun printBalls()
    {
        _gameBallPos.printPositions()
    }
}