package com.darblee.flingaid.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.darblee.flingaid.Direction
import com.darblee.flingaid.Pos
import com.darblee.flingaid.ui.SolverViewModel.ballCount
import com.darblee.flingaid.ui.SolverViewModel.findWinningMove
import com.darblee.flingaid.ui.SolverViewModel.loadGameFile
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
 *
 * **Game Play Functions**
 * - [findWinningMove]
 */
object GameViewModel : ViewModel() {

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


    // Game UI state
    private val _uiState = MutableStateFlow(GameUIState())

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
    internal var uiState: StateFlow<GameUIState> = _uiState.asStateFlow()
        private set  // Public getter (read-only access from outside) and private setter (only internally modifiable)


    fun moveBallPos(row: Int, col: Int) {
        _uiState.update { currentState ->
            currentState.copy(
                state = GameState.MoveBall,
                moveFromRow = row,
                moveFromCol = col,
            )
        }
    }
}