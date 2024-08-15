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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

/**
 * **View Model for the  Game**
 *
 * - It Manage the business logic for the game. This includes the thinking activity.
 * - It holds the source of truth for the solve game state.
 * - It prepare data for the UI and follow UDF (Unidirectional Data FLow) to the UI
 * - It has a longer lifetime than the composable.
 * - All fields in UI state [GameUIState] is stateless. It can be re-created after reading the game board ball
 * positions. The application can survive process death. No need to save state to persistent
 * storage.
 * - There can only be one [GameViewModel] instance. Hence, use the singleton class (object)
 * - There is only one instance of this object.
 *
 * - Here is the overall state machine:
 * [State Machine](https://github.com/darblee/FlingAid-new/blob/master/README.md)
 *
 * [GameUIState] State of UI
 *
 * **Ball Management**
 *
 * Managing the ball on the game board
 * - [loadGameFiles] : Define the file to store the ball position information
 * - [ballCount]
 * - [ballPositionList]
 * - [printBalls] Print all the ball positions. Used for debugging purposes.
 * *
 * **Game Play Functions**
 * - [generateNewGame]  Generate a new game based on provided level
 */
object GameViewModel : ViewModel() {

    /********************************* BALL MANAGEMENT ********************************************/

    private var _gameBallPos = FlickerBoard()
    private var _gameLevel = 1

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
     * Load the game files - Game file and history file
     *
     * @param gameFile File that contain the board ball positions
     * @param historyFile File to contain history list of moves
     */
    fun loadGameFiles(gameFile: File, historyFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            _gameBallPos.setGameFile(gameFile)
            _gameBallPos.loadBallListFromFile()

            _gameBallPos.setHistoryFile(historyFile)
            _gameBallPos.loadHistoryFromFile()
            setModeUpdatedGameBoard()
        }
    }

    /**
     * Set the game level
     *
     * @param newLevel The new game level
     */
    fun setGameLevel(newLevel: Int)
    {
        _gameLevel = if (newLevel > Global.MAX_GAME_LEVEL)
            Global.MAX_GAME_LEVEL
        else
            newLevel
    }

    /********************************* GAME MANAGEMENT ********************************************/

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
    internal var gameUIState: StateFlow<GameUIState> = _uiGameState.asStateFlow()
        private set

    /**
     * Initialize the SolverViewModel
     */
    init {
        reset()
    }

    /**
     * Determine whether it can safely exit the GameViewModel
     *
     * @return
     * - true Clean-up is done. It is safe to exit the view model
     * - false Unable to clean-up or in a middle of doing something. Do not exit the view model
     */
    fun canExitGameScreen(): Boolean {
        when (gameUIState.value.mode) {
            GameUIState.GameMode.WonGame -> return false
            GameUIState.GameMode.UpdatedGameBoard -> return true
            GameUIState.GameMode.UpdateGameBoardWithNoSolution -> return true
            GameUIState.GameMode.NoWinnnableMoveWithDialog -> return false
            GameUIState.GameMode.NoWinnableMove -> return true
            GameUIState.GameMode.MoveBall -> return true
            GameUIState.GameMode.IndicateInvalidMoveByShowingShadowMove -> return false
            GameUIState.GameMode.ShowHint -> {
                setModeUpdatedGameBoard()
                return true
            }
        }
    }

    /**
     * Reset the entire solver game
     * - Clear the board
     * - Remove any saved game files
     */
    private fun reset() {
        _gameBallPos.clearGame()
        _gameBallPos.clearMoveHistory()

        setModeUpdatedGameBoard()
    }

    /**
     * Generate a new game based on level.
     *
     * It will add balls to the ball list
     */
    fun generateNewGame() {
        val tempBoard = FlickerBoard()

        val curRow = Random.nextInt(1, (Global.MAX_ROW_SIZE - 1))
        val curCol = Random.nextInt(1, (Global.MAX_COL_SIZE - 1))

        tempBoard.ballList.add(Pos(curRow, curCol))

        val game = FlickerEngine()
        game.populateGrid(tempBoard.ballList)

        repeat(_gameLevel) {
            game.moveBack()
        }

        viewModelScope.launch(Dispatchers.IO) {
            _gameBallPos.ballList.clear()
            _gameBallPos.ballList = game.updateBallList()
            _gameBallPos.clearMoveHistory()
            _gameBallPos.addSnapshotToHistory()
            _gameBallPos.saveBallListToFile()

            setModeUpdatedGameBoard()
        }
    }

    enum class MoveResult {
        Valid,    // This is a valid move
        InvalidNoBump,   // Not a valid move as move require you to bump the ball
        InvalidNoBall,   // Not a valid move as there is no ball in the provided position
        InvalidNoRoom,   // Not a valid move as there is no room to move the ball
    }

    /**
     * Attempt to set up the ball move based on input, which is from user's swipe action.
     * If first check if the upcoming move is valid move or not. It will return the appropriate
     * move result type and set the UI state accordingly.
     *
     * @param initialRow Row position to move the ball from
     * @param initialCol Column position to move the ball from
     * @param direction Direction of the ball movement
     *
     * @return
     * - [MoveResult.Valid] This is a valid move. Set to mode to [GameUIState.GameMode.MoveBall]
     * - [MoveResult.InvalidNoRoom] This is not a valid move. There is no room to move the ball
     * - [MoveResult.InvalidNoBall] This is not a valid move. There is no ball in the provided position
     * - [MoveResult.InvalidNoBump] This is not a valid move as it moves the ball of the edge. It
     * set the game mode to [GameUIState.GameMode.IndicateInvalidMoveByShowingShadowMove]
     */
    fun setupNextMove(initialRow: Int, initialCol: Int, direction: Direction): MoveResult {
        if (!(_gameBallPos.ballList.contains(Pos(initialRow, initialCol))))
            return MoveResult.InvalidNoBall

        val movingChain = _gameBallPos.buildMovingChain(initialRow, initialCol, direction)

        if (movingChain.isEmpty())
            return MoveResult.InvalidNoRoom

        // If only one ball is moving, then it did not bump another ball. Hence, this is
        // an invalid move. We need to inform user this is invalid by showing shadow move.
        if (movingChain.size == 1) {
            val shadowMoveBallRec = GameUIState.GameMode.IndicateInvalidMoveByShowingShadowMove
            shadowMoveBallRec.shadowMoveDirection = direction
            shadowMoveBallRec.shadowMovingChain = movingChain
            _uiGameState.update { curState ->
                curState.copy(
                    _mode = shadowMoveBallRec
                )
            }
            return MoveResult.InvalidNoBump
        }

        val moveBallRec = GameUIState.GameMode.MoveBall
        moveBallRec.moveDirection = direction
        moveBallRec.movingChain = movingChain
        _uiGameState.update { curState ->
            curState.copy(
                _mode = moveBallRec
            )
        }
        return MoveResult.Valid
    }

    /**
     * Indicate whether it can do undo operation or not
     */
    fun canUndo(): Boolean
    {
        return (_gameBallPos.canPerformUndo())
    }

    /**
     * Perform the actual undo
     * - Do the undo operation
     * - Save the snapshot to file
     * - Notify Game Screen there is a updated game board
     */
    fun undo()
    {
        _gameBallPos.undo()

        viewModelScope.launch(Dispatchers.Main) {

            // Now that board got updated, check to see if there is a winnable move. This will help
            // us determine what mode to set to.
            gTask1WinningDirection = Direction.INCOMPLETE
            gTask2WinningDirection = Direction.INCOMPLETE
            gThinkingProgress = 0

            val game = FlickerEngine()
            game.populateGrid(_gameBallPos.ballList)

            val (direction, _, _) = game.foundWinningMove(
                _gameBallPos.ballList.size, 1, 1
            )

            if ((direction == Direction.INCOMPLETE) || (direction == Direction.NO_WINNING_DIRECTION)) {
                setModeUpdatedGameBoardWIthNoSolution()
            } else {
                setModeUpdatedGameBoard()
            }

            withContext(Dispatchers.IO) {
                _gameBallPos.saveHistoryToFile()
                _gameBallPos.saveBallListToFile()
            }
        }
    }

    /**
     * Move the ball.
     * - Save the ball position to file.
     * - After ball movement, change the state to no direction.
     *
     * @param pos Position of the ball to move from
     * @param direction Direction of ball movement
     */
    fun moveBall(pos: Pos, direction: Direction) {
        val game = FlickerEngine()
        game.populateGrid(_gameBallPos.ballList)

        var targetRow: Int
        var targetCol: Int

        if (direction == Direction.UP) {
            targetRow = game.findTargetRowOnMoveUp(pos.row, pos.col)
            game.moveUp(pos.row, targetRow, pos.col)
            _gameBallPos.ballList.clear()
            _gameBallPos.ballList = game.updateBallList()
        }

        if (direction == Direction.DOWN) {
            targetRow = game.findTargetRowOnMoveDown(pos.row, pos.col)
            game.moveDown(pos.row, targetRow, pos.col)
            _gameBallPos.ballList.clear()
            _gameBallPos.ballList = game.updateBallList()
        }

        if (direction == Direction.RIGHT) {
            targetCol = game.findTargetColOnMoveRight(pos.row, pos.col)
            game.moveRight(pos.col, targetCol, pos.row)
            _gameBallPos.ballList.clear()
            _gameBallPos.ballList = game.updateBallList()
        }

        if (direction == Direction.LEFT) {
            targetCol = game.findTargetColOnMoveLeft(pos.row, pos.col)
            game.moveLeft(pos.col, targetCol, pos.row)
            _gameBallPos.ballList.clear()
            _gameBallPos.ballList = game.updateBallList()
        }

        _gameBallPos.addSnapshotToHistory()

        viewModelScope.launch(Dispatchers.IO) {
            _gameBallPos.saveBallListToFile()

            _uiGameState.update { curState ->
                curState.copy(
                    _mode = if (ballCount() == 1) {
                        GameUIState.GameMode.WonGame
                    } else {
                        GameUIState.GameMode.UpdatedGameBoard
                    }
                )
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
    fun drawGameBallsOnGrid(
        drawScope: DrawScope,
        gridSize: Float,
        displayBallImage: ImageBitmap,
        ballsToErase: BallMoveSet = listOf()
    ) {
        _gameBallPos.drawAllBalls(drawScope, gridSize, displayBallImage, ballsToErase)
    }

    /**
     * Print all the balls.
     *
     * Used primarily for debugging purposes
     */
    private fun printBalls() {
        _gameBallPos.printPositions()
    }

    /**
     * Check if there is a winnable move or not
     */
    fun hasWinnableMove() : Boolean
    {

        var winningDirection: Direction = Direction.NO_WINNING_DIRECTION

        viewModelScope.launch (Dispatchers.Default) {
            gTask1WinningDirection = Direction.INCOMPLETE
            gTask2WinningDirection = Direction.INCOMPLETE
            gThinkingProgress = 0

            val game = FlickerEngine()
            game.populateGrid(_gameBallPos.ballList)

            val (direction, _, _) = game.foundWinningMove(
                _gameBallPos.ballList.size, 1, 1
            )
            winningDirection = direction
        }

        return !((winningDirection == Direction.INCOMPLETE) || (winningDirection == Direction.NO_WINNING_DIRECTION))
    }

    /**
     * Find hint
     */
    fun getHint() {
        viewModelScope.launch (Dispatchers.Default) {
            gTask1WinningDirection = Direction.INCOMPLETE
            gTask2WinningDirection = Direction.INCOMPLETE
            gThinkingProgress = 0

            val game = FlickerEngine()
            game.populateGrid(_gameBallPos.ballList)

            val (direction, row, col) = game.foundWinningMove(
                _gameBallPos.ballList.size, 1, 1
            )

            Log.i(
                "${Global.DEBUG_PREFIX} Finish Task",
                "Conclusion: Direction = $direction, row=$row, col=$col"
            )

            if ((direction == Direction.INCOMPLETE) || (direction == Direction.NO_WINNING_DIRECTION)) {
                Log.i(Global.DEBUG_PREFIX, "No winnable move found.")
                setModeToNoWinnableMoveWithDialog()
                return@launch
            }

            // We have a hint. Now get the distance
            var distance = 0
            when (direction) {
                Direction.UP -> {
                    distance = row - game.findTargetRowOnMoveUp(row, col)
                }

                Direction.DOWN -> {
                    distance = game.findTargetRowOnMoveDown(row, col) - row
                }

                Direction.LEFT -> {
                    distance = col - game.findTargetColOnMoveLeft(row, col)
                }

                Direction.RIGHT -> {
                    distance = game.findTargetColOnMoveRight(row, col) - col
                }

                else -> {
                    Log.i(Global.DEBUG_PREFIX, "Got unexpected direction value")
                }
            }

            setModeToShowHint(Pos(row, col), direction, distance)
        }
    }

    /********************************  Set mode routines ******************************************/

    /**
     * Set mode to "Updated Game Board"
     */
    fun setModeUpdatedGameBoard() {
        _uiGameState.update { curState ->
            curState.copy(
                _mode = GameUIState.GameMode.UpdatedGameBoard
            )
        }
    }

    /**
     * Set mode to "Updated Game Board with no solution"
     */
    private fun setModeUpdatedGameBoardWIthNoSolution() {
        _uiGameState.update { curState ->
            curState.copy(
                _mode = GameUIState.GameMode.UpdateGameBoardWithNoSolution
            )
        }
    }

    /**
     * Set mode to "Show Hint"
     *
     * Update the [_uiGameState] state flow with this "Show Hint" mode information
     * and send it to Game Screen
     *
     * @param pos Ball position to start shadow movement
     * @param direction Direction of the shadow movement
     * @param distance Distance of the shadow movement
     */
    private fun setModeToShowHint(pos: Pos, direction: Direction, distance: Int) {
        val move = MovingRec(pos, distance)
        val movingChain = mutableListOf<MovingRec>()
        movingChain.add(move)

        val showHintRec = GameUIState.GameMode.ShowHint
        showHintRec.shadowMovingChain = movingChain
        showHintRec.shadowMoveDirection = direction

        _uiGameState.update { curState ->
            curState.copy(
                _mode = showHintRec
            )
        }
    }

    /**
     * Set mode to "No Winnable Move with dialog". Basically, to indicate the game could no longer be winnable
     * without reset or ga back to previous moves. ALso informing game screen to show the dialog informing
     * user there is no winnable move
     *
     * Update the [_uiGameState] state flow with this "Show Hint" mode information
     * and send it to Game Screen
     *
     */
    private fun setModeToNoWinnableMoveWithDialog() {
        _uiGameState.update { curState ->
            curState.copy(
                _mode = GameUIState.GameMode.NoWinnnableMoveWithDialog
            )
        }
    }

    /**
     * Set mode to "No Winnable Move". Basically, to indicate the game could no longer be winnable
     * without reset or ga back to previous moves.
     *
     * Update the [_uiGameState] state flow with this "Show Hint" mode information
     * and send it to Game Screen
     *
     */
    fun setModeToNoWinnableMove() {
        _uiGameState.update { curState ->
            curState.copy(
                _mode = GameUIState.GameMode.NoWinnableMove
            )
        }
    }
}