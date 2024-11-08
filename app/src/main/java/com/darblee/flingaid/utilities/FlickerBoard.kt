package com.darblee.flingaid.utilities

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darblee.flingaid.BallMoveSet
import com.darblee.flingaid.Direction
import com.darblee.flingaid.Global
import com.darblee.flingaid.R
import com.darblee.flingaid.gAudio_swish
import com.darblee.flingaid.gAudio_victory
import com.darblee.flingaid.gDisplayBallImage
import com.darblee.flingaid.ui.MovingRec
import com.darblee.flingaid.ui.Particle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

/**
 * Position on the game board
 * - row
 * - column
 */
@Serializable
data class Pos(
    val row: Int,
    val col: Int,
)

typealias boardSnapshot = MutableList<Pos>

/**
 * Ball Position Management
 *
 * Common routine to manage the ball positions
 *
 * [ballList] List of all the ball positions. It is in mutableStateListOf<Pos> format. Intended
 * to be used in composable
 *
 * [getBallCount] Get the number of active balls in the grid
 * [setGameFile] Setup file pointer file
 * [saveBallListToFile]  Save the list of position in the file
 * [loadBallListFromFile] Load the list of ball positions from the file
 * [printPositions] Print all the ball positions. It is intended for debugging purposes.
 *
 */
class FlickerBoard {
    /**
     * List of all the balls and its position on the game board
     *
     * _Developer's Note_
     * Use of mutableStateListOf to preserve the composable state of the ball position. The state
     * are kept appropriately isolated and can be performed in a safe manner without race condition
     * when they are used in multiple threads (e.g. LaunchEffects).
     */
    var ballList = mutableStateListOf<Pos>()

    /**
     * Return the number of active ball
     *
     * @return Number of active balls
     */
    fun getBallCount(): Int {
        return ballList.count()
    }

    /**
     * File pointer to game file
     */
    private var _gameFile: File? = null

    /**
     * Set the game file
     *
     * @param file game file
     */
    fun setGameFile(file: File) {
        _gameFile = file
    }

    /**
     * Reset the game
     * Delete the game file
     */
    fun clearGame() {
        _gameFile?.delete()
        ballList.clear()
    }

    /**
     * Save the game board to a file
     */
    fun saveBallListToFile() {

        if (_gameFile == null) return

        try {
            val format = Json { prettyPrint = true }
            val ballList = mutableListOf<Pos>()

            for (currentPos in this.ballList) { ballList += currentPos }

            val output = format.encodeToString(ballList)
            val writer = FileWriter(_gameFile)
            writer.write(output)
            writer.close()
        } catch (e: SerializationException) {
            Log.i(Global.DEBUG_PREFIX, "Serialization error. Unable to encode ball list information. Reason: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.i(Global.DEBUG_PREFIX, "Serialization error Detected non-compliant format while saving ball list to file. Reason: ${e.message}")
        } catch (e: IOException) {
            Log.i(Global.DEBUG_PREFIX, "Unable to write to ball list file. Reason: ${e.message}")
        } catch (e: Exception) {
            Log.i(Global.DEBUG_PREFIX, "Unable to save to game file. Reason: ${e.message}")
        }
    }

    /**
     * Load the saved game board from file
     **/
    fun loadBallListFromFile() {

        if (_gameFile == null) return

        try {
            val reader = FileReader(_gameFile)
            val data = reader.readText()
            reader.close()

            val list = Json.decodeFromString<List<Pos>>(data)

            ballList.clear()
            for (pos in list) { ballList.add(pos) }

        } catch (e: FileNotFoundException) {
            Log.i(Global.DEBUG_PREFIX, "Missing file. Reason: ${e.message}")
        } catch (e: SerializationException) {
            Log.i(Global.DEBUG_PREFIX, "Serialization error. Unable to decode. File may have been corrupted. Reason:  ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.i(Global.DEBUG_PREFIX, "Decoded data from file could not be converted to Pos structure. Reason: ${e.message}")
        } catch (e: Exception) {
            Log.i(Global.DEBUG_PREFIX, "An error occurred while trying to load the ball list file. Reason: ${e.message}")
        }
    }

    /**
     * File pointer to reject ball file
     */
    private var _rejectBallFile: File? = null

    /**
     * Set the reject ball file pointer
     */
    fun setRejectBallFile(file: File)
    {
        _rejectBallFile = file
    }

    /**
     * Save the reject balls to file
     */
    fun saveRejectBallsToFile(rejectedBalls: List<Pos>)
    {
        if (_rejectBallFile == null) return

        try {
            val format = Json { prettyPrint = true }
            val output = format.encodeToString(rejectedBalls)
            val writer = FileWriter(_rejectBallFile)
            writer.write(output)
            writer.close()
        } catch (e: SerializationException) {
            Log.i(Global.DEBUG_PREFIX, "Serialization error. Unable to encode reject ball list information. Reason: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.i(Global.DEBUG_PREFIX, "Serialization error Detected non-compliant format while saving rejected balls. Reason: ${e.message}")
        } catch (e: IOException) {
            Log.i(Global.DEBUG_PREFIX, "Unable to write to reject list file. Reason: ${e.message}")
        } catch (e: Exception) {
            Log.i(Global.DEBUG_PREFIX, "Unable to save to reject ball file. Reason: ${e.message}")
        }
    }

    /**
     * Load the reject balls from file
     *
     * @return
     * - On success: return rejected ball list
     * - On any failure, return an empty list
     */
    fun loadRejectBallsFromFile() : List<Pos>
    {
        if (_rejectBallFile == null) return listOf()

        try {
            val reader = FileReader(_rejectBallFile)
            val data = reader.readText()
            reader.close()

            return (Json.decodeFromString<List<Pos>>(data))
        } catch (e: Exception) {
            return listOf()
        }
    }

    /**
     * Delete the reject file
     */
    fun deleteRejectFile()
    {
        _rejectBallFile?.delete()
    }

    /**
     * Print the ball positions. Used for debugging purposes
     */
    fun printPositions() {

        Log.i(Global.DEBUG_PREFIX, "============== Ball Listing ================)")

        for ((index, value) in ballList.withIndex()) {
            Log.i(Global.DEBUG_PREFIX, "Ball $index: (${value.row}, ${value.col})")
        }
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
    fun buildMovingChain(
        initialRow: Int,
        initialCol: Int,
        direction: Direction
    ): BallMoveSet {
        val movingList = mutableListOf<MovingRec>()

        var chainRecInfo =
            findFreeSpaceCount(initialRow, initialCol, direction, firstMove = true)
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
                val curSourcePos = nextSourcePos
                chainRecInfo =
                    findFreeSpaceCount(
                        curSourcePos.row,
                        curSourcePos.col,
                        direction,
                        firstMove = false
                    )
                movingDistance = chainRecInfo.first
                nextSourcePos = chainRecInfo.second

                movingList.add(MovingRec(curSourcePos, movingDistance))
            }
        }
        return (movingList)
    }

    /**
     *  Find the number of free space in front of the ball doing on a specific direction. If the
     *  ball is on the edge, then automatically provide 2 free spaces. The caller [buildMovingChain]
     *  will send the ball off the grid. We need to see it fall off the edge of the phone screen,
     *  which is why two space is provided instead of one space.
     *
     *  @param row Row of existing ball
     *  @param col Column of existing ball
     *  @param direction Direction to look for free space
     *  @param firstMove If this is the first move in the moving chain, we need to do special check
     *  when the ball is on the edge of the grid. Ball on edge can not move off edge of the board.
     *  If this is not the first ball on the chain, then it can fall off the edge
     *
     *  @return Pair<Int, SolverGridPos?> where the first element is the number of free space and
     *  second element is the position of the next ball.  If the second element is null, then
     *  the ball has fallen off the edge.
     *
     *  If there is no possible move at all, it will return <0, null>
     *
     *  @see buildMovingChain
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
                distance += 2  // Add two to make it fall off the edge
                hitWall = true
            } else {
                if (ballList.contains(Pos(newRow, newCol))) {
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

    /**
     * Draw all the balls in the provided canvas grid
     *
     * @param drawScope The drawing canvas of the grid
     * @param gridSize The size of the grid
     * @param eraseAnimatedBallPositions Used during ball animation. We need to temporarily
     * erase the animation ball as the animation routine will display it
     */
    fun drawAllBalls(
        drawScope: DrawScope,
        gridSize: Float,
        eraseAnimatedBallPositions: BallMoveSet = listOf()
    ) {
        // Draw all the balls
        //
        // NOTE: ballPositionList is a SnapshotList type. It is observation system that will trigger a recompose
        // to redraw the grid.
        ballList.forEach { curBallListPosition ->
            var skipDraw = false
            run innerLoop@{
                eraseAnimatedBallPositions.forEach { eraseRec ->
                    if (eraseRec.pos == curBallListPosition) {
                        skipDraw = true
                        return@innerLoop
                    }
                }
            }

            if (!skipDraw) drawBallOnGrid(drawScope, gridSize, curBallListPosition)
        }
    }

    /********************************* Board history management *****************/

    /**
     * [_moveHistory] data structure to keep history of all recent moves for the entire game
     */
    private var _moveHistory: MutableList<boardSnapshot> = mutableListOf()

    /**
     * Make a snapshot copy of the board
     *
     * @return Board snapshot, which is a list of ball positions
     */
    private fun createBoardSnapshot(): boardSnapshot {
        val s: MutableList<Pos> = mutableListOf()
        ballList.forEach { curPos ->
            s.add(curPos)
        }
        return s
    }

    /**
     * Clear the move history
     */
    fun clearMoveHistory()
    {
        _historyFile?.delete()
        _moveHistory.clear()
    }

    /**
     * Create a new snapshot based on current state of the board, then add it to the
     * history records.
     */
    fun addSnapshotToHistory() {
        val curSnapshot = createBoardSnapshot()
        _moveHistory.add(curSnapshot)

        saveHistoryToFile()
    }

    /**
     * Perform the actual undo.
     * - Remove the move recent entry of snapshot and throw it away
     * - Take the second most recent snapshot and activate it
     */
    fun undo() {
        if (_moveHistory.isEmpty()) return

        _moveHistory.removeLast()

        val i = _moveHistory.size
        val snapshot = _moveHistory[i - 1]
        ballList.clear()
        snapshot.forEach { curPos ->
            ballList.add(curPos)
        }
    }

    /**
     * Indicate whether there is enough history to do undo operation
     */
    fun canPerformUndo(): Boolean
    {
        return (_moveHistory.size > 1)
    }

    /**
     * File pointer to game history file
     */
    private var _historyFile: File? = null

    /**
     * Set the history file
     */
    fun setHistoryFile(file: File)
    {
        _historyFile = file
    }

    /**
     * Save the history to file
     */
    fun saveHistoryToFile()
    {
        if (_historyFile == null) return

        try {
            val format = Json { prettyPrint = true }
            val output = format.encodeToString(_moveHistory)
            val writer = FileWriter(_historyFile)
            writer.write(output)
            writer.close()
        } catch (e: SerializationException) {
            Log.i(Global.DEBUG_PREFIX, "Serialization error. Unable to encode history data. Reason: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.i(Global.DEBUG_PREFIX, "Serialization error. Detected non-compliant format. Reason: ${e.message}")
        } catch (e: IOException) {
            Log.i(Global.DEBUG_PREFIX, "Unable to write to history. Reason: ${e.message}")
        } catch (e: Exception) {
            Log.i(Global.DEBUG_PREFIX, "Unable to save history to file. Reason: ${e.message}")
        }
    }

    /**
     * Load the history from file
     */
    fun loadHistoryFromFile()
    {
        if (_historyFile == null) return

        try {
            val reader = FileReader(_historyFile)
            val data = reader.readText()
            reader.close()

            val historyList = Json.decodeFromString<List<List<Pos>>>(data)
            _moveHistory.clear()

            historyList.forEach { curSnapshot ->
                val newSnapshot: boardSnapshot = mutableListOf()
                curSnapshot.forEach { curPos ->
                    newSnapshot.add(curPos)
                }
                _moveHistory.add(newSnapshot)
            }
        } catch (e: FileNotFoundException) {
            Log.i(Global.DEBUG_PREFIX, "Missing file. Reason: ${e.message}")
        } catch (e: SerializationException) {
            Log.i(Global.DEBUG_PREFIX, "Serialization error. Unable to decode history content. File may have been corrupted. Reason: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.i(Global.DEBUG_PREFIX, "Serialization error. Decoded data could not be converted to history format. Reason: ${e.message}")
        } catch (e: Exception) {
            Log.i(Global.DEBUG_PREFIX, "Unable to load history from file. Reason: ${e.message}")
        }
    }
}

/**
 * Draw the ball in a specific position (e,g, row, col) on the board grid
 *
 * @param drawScope
 * @param gridSize Size the dimension of the grid
 * @param pos Specific position (row, column)
 * @param alpha amount of transparency
 */
fun drawBallOnGrid(
    drawScope: DrawScope,
    gridSize: Float,
    pos: Pos,
    alpha: Float = 1.0F
) {
    val offsetAdjustment = (gridSize / 2) - (gDisplayBallImage.width / 2)

    with(drawScope) {
        drawImage(
            image = gDisplayBallImage,
            topLeft = Offset(
                (pos.col * gridSize) + offsetAdjustment,
                (pos.row * gridSize) + offsetAdjustment
            ),
            alpha = alpha
        )
    }
}

/****************************** Animation Routines ************************************************/

/******************* Shadow Ball Movement Animation Routines **************************************/

/* The set-up routine is done on each screen - GameScreen & SolverScreen */


/**
 * Animate the shadowed ball movement.
 *
 * @param drawScope Scope to do the drawing on
 * @param gridSize width or height of the grid in dp unit
 * @param animateCtl Object controller that manage animation state of shadow ball movement
 * @param distance number of blocks in this ball movement
 * @param direction Direction of the ball movement
 * @param pos Position of the ball to move from
 */
fun animateShadowBallMovementsPerform(
    drawScope: DrawScope,
    gridSize: Float,
    animateCtl: Animatable<Float, AnimationVector1D>,
    distance: Int,
    direction: Direction,
    pos: Pos
) {
    with(drawScope) {
        var xOffset = 0
        var yOffset = 0

        when (direction) {
            Direction.UP -> {
                yOffset = -1 * gridSize.toInt() * distance
            }

            Direction.DOWN -> {
                yOffset = 1 * gridSize.toInt() * distance
            }

            Direction.LEFT -> {
                xOffset = -1 * gridSize.toInt() * distance
            }

            Direction.RIGHT -> {
                xOffset = 1 * gridSize.toInt() * distance
            }

            else -> {
                assert(true) { "Got unexpected Direction value: $direction" }
            }
        }

        translate(
            (xOffset) * animateCtl.value,
            (yOffset) * animateCtl.value
        ) {
            drawBallOnGrid(drawScope, gridSize, pos, alpha = 0.4F)
        }
    }
}

/*************************** Ball Movement Animation Routines *************************************/

/* The set-up routine is done on each screen - GameScreen & SolverScreen */

/**
 * Animate the shadowed ball movement endlessly in a loop. This is a set-up.
 *
 * @param animateCtl Object that control animation state of shadowed movement, which loops forever.
 */
@Composable
fun AnimateShadowBallMovementSetup(animateCtl: Animatable<Float, AnimationVector1D>) {

    // Run the side effect only once
    LaunchedEffect(Unit) {
        animateCtl.snapTo(0f)
        animateCtl.animateTo(
            targetValue = 1f, animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }
}


/**
 * Create all particles, which is used for the explosion animated effect
 *
 * @param movingChain Moving chain. __NOTE:__ This function assume the [movingChain] have at
 * least 2 movements.
 * @param direction Ball movement direction
 *
 * @return List of all the particles
 */
fun generateExplosionParticles(
    movingChain: BallMoveSet,
    direction: Direction
): List<Particle> {
    val sizeDp = 200.dp
    val sizePx = sizeDp.toPx()
    val explosionPos = movingChain[1].pos
    val explosionX = explosionPos.row
    val explosionY = explosionPos.col
    val particles =
        List(5) {
            Particle(
                color = Color(
                    listOf(
                        0xffea4335,
                        0xff4285f4,
                        0xfffbbc05,
                        0xff34a853
                    ).random()
                ),
                startRow = explosionX,
                startCol = explosionY,
                direction = direction,
                maxHorizontalDisplacement = sizePx * randomInRange(-0.3f, 0.3f),
                maxVerticalDisplacement = sizePx * randomInRange(0.05f, 0.07f)
            )
        }

    return (particles)
}

/**
 * Define the animation KeyframeSpec for the straight ball movement.
 * Ensure the most of time is spend linear speed with slow start
 * and a bounce effect at the end.
 *
 * @param totalTimeLength Total time to run the straight ball movement
 * @param whenBallMakeContactRatio Time ratio of total time when the ball makes contact to the neighboring ball. It is used
 * in conjunction with [particleExplosionAnimatedSpec] routine
 *
 * @return Animation state specification
 */
fun ballMovementKeyframeSpec(
    totalTimeLength: Int,
    whenBallMakeContactRatio: Float
): KeyframesSpec<Float> {

    val spec: KeyframesSpec<Float> = keyframes {
        durationMillis = totalTimeLength
        0f.at((0.05 * totalTimeLength).toInt()) using LinearOutSlowInEasing
        1.04f.at((whenBallMakeContactRatio * totalTimeLength).toInt()) using FastOutLinearInEasing   // Overrun the ball slightly to hit the neighboring ball
        1.0f.at(totalTimeLength) using EaseOut   // Roll back to the destination
    }
    return (spec)
}

/**
 * Define the animation KeyframeSpec for the particle explosion. Explosion will occur at the tail
 * end of the ball movement by delaying the start of explosion effect.
 *
 *  @param totalTimeLength Total time to run the straight ball movement
 *  @param whenBallMakeContactRatio Time ratio of total time when the ball makes contact to the neighboring ball. It is used
 *  in conjunction with [ballMovementKeyframeSpec] routine
 *
 *  @return The animation state specification
 */
fun particleExplosionAnimatedSpec(
    totalTimeLength: Int,
    whenBallMakeContactRatio: Float
): KeyframesSpec<Float> {
    val spec: KeyframesSpec<Float> = keyframes {
        durationMillis = totalTimeLength + 250
        delayMillis = (whenBallMakeContactRatio * totalTimeLength - 10).toInt()
        0.5f.at(totalTimeLength + 250) using LinearOutSlowInEasing
    }
    return (spec)
}

/**
 * Statically defined animated Keyframe specification to wiggle the ball
 */
val wiggleBallAnimatedSpec = keyframes {
    durationMillis = 80
    0f.at(10) using LinearEasing   // from 0 ms to 10 ms
    5f.at(20) using LinearEasing    // from 10 ms to 20 ms
}

/**
 * Put in the appropriate xOffset and yOffset values.
 * If the distance is zero, then just set the offset to 1 distance so that it will
 * do a small bump movement
 *
 * @return Pair <xOffset, yOffset>
 * - row length
 * - column length
 */
private fun setOffsets(direction: Direction, distance: Int, gridSize: Float): Pair<Float, Float> {
    var xOffset = 0f
    var yOffset = 0f

    // If the distance is zero, then just set the offset to 1 distance so that it will
    // do a small bump movement
    if (distance == 0) {
        when (direction) {
            Direction.UP -> yOffset = -1f
            Direction.DOWN -> yOffset = 1f
            Direction.LEFT -> xOffset = -1f
            Direction.RIGHT -> xOffset = 1f
            else -> assert(true) { "Unexpected direction value on animate ball movement" }
        }

        return (Pair(xOffset, yOffset))
    }

    when (direction) {
        Direction.UP -> yOffset = -1 * distance * gridSize
        Direction.DOWN -> yOffset = distance * gridSize
        Direction.LEFT -> xOffset = -1 * distance * gridSize
        Direction.RIGHT -> xOffset = distance * gridSize
        else -> assert(true) { "Got unexpected direction during animation" }
    }

    return (Pair(xOffset, yOffset))
}

/**
 * Setup ball movement animation and the particle explosion effect
 * - After movement, it will if there is the last ball. If so, announce victory
 *
 * @param movingChain List of ball movements in the chain
 * @param direction Direction of ball movement
 * @param animateBallMovementCtlList  Object that control animation state of all the ball
 * movement in this chain
 * @param animateParticleExplosionCtl Object that control animation state of particle
 * explosion effect
 * @param moveBallTask Lambda function - Move the ball
 */
@Composable
fun AnimateBallMovementsSetup(
    movingChain: BallMoveSet,
    direction: Direction,
    animateBallMovementCtlList: MutableList<Animatable<Float, AnimationVector1D>>,
    animateParticleExplosionCtl: Animatable<Float, AnimationVector1D>,
    moveBallTask: (pos: Pos, direction: Direction) -> Unit,
) {
    if (movingChain.isEmpty()) return

    movingChain.forEach { _ ->
        val animateBallMovement = remember { Animatable(initialValue = 0f) }
        animateBallMovementCtlList.add(animateBallMovement)
    }

    // Time ratio of total time when the ball makes contact to the neighboring ball
    val whenBallMakeContactRatio = 0.9f
    val overtime = 200

    // Run the side effect only once
    LaunchedEffect(Unit) {
        // Use coroutine to ensure both launch animation get completed in the same co-routine scope
        coroutineScope {
            val timeLengthMultiplier = 150
            launch(Dispatchers.Main) { // One or more ball movements in serial fashion
                movingChain.forEachIndexed { index, currentMovingRec ->
                    if (currentMovingRec.distance > 0) {
                        val totalTimeLength = (currentMovingRec.distance * timeLengthMultiplier) + overtime
                        animateBallMovementCtlList[index].animateTo(
                            targetValue = 1f,
                            animationSpec = ballMovementKeyframeSpec(
                                totalTimeLength,
                                whenBallMakeContactRatio
                            )
                        )
                    } else {
                        // Distance is zero. Need to set-up the "wiggle" effect
                        animateBallMovementCtlList[index].animateTo(
                            targetValue = 0f, animationSpec = wiggleBallAnimatedSpec
                        )
                    } // if-else currentMovingRec.distance
                }   // movingChain forEach
                animateBallMovementCtlList.clear()

                // Move the ball. We use lambda function to perform the corresponding move
                // based the view model, either gameViewModel or solverViewModel
                moveBallTask.invoke(movingChain[0].pos, direction)

            } // Launch

            launch(Dispatchers.Main) {  // Animate explosion on the first ball only
                val totalTimeLength = (movingChain[0].distance * timeLengthMultiplier) + overtime
                animateParticleExplosionCtl.animateTo(
                    targetValue = 0.5f,
                    animationSpec = particleExplosionAnimatedSpec(
                        totalTimeLength,
                        whenBallMakeContactRatio
                    )
                )
            } // launch

            launch (Dispatchers.Main) {
                val numMoves = movingChain.size
                movingChain.forEachIndexed { index, currentMovingRec ->
                    Log.i(Global.DEBUG_PREFIX, "Checking swish sound at index $index")
                    if ((currentMovingRec.distance > 0) && (index < (numMoves - 1))) {
                        val totalTimeLength = (movingChain[index].distance * timeLengthMultiplier + overtime)
                        delay((totalTimeLength * whenBallMakeContactRatio).toLong())
                        Log.i(Global.DEBUG_PREFIX, "Distance: ${currentMovingRec.distance}   Play swish sound at index $index")
                        if (gAudio_swish.isPlaying) {
                            gAudio_swish.seekTo(0)
                        } else {
                            gAudio_swish.start()
                        }
                    }
                }
            }
        }   // coroutineScope
    } // LaunchedEffect
}


/**
 * Stop and reset the ball movement animation control
 *
 * @param animateParticleExplosionCtl Object controller that manage and define animation state and
 * specification of the ball movement
 */
@Composable
fun AnimateBallMovementsReset(animateParticleExplosionCtl: Animatable<Float, AnimationVector1D>,
)
{
    LaunchedEffect(Unit) {
        animateParticleExplosionCtl.stop()
        animateParticleExplosionCtl.snapTo(0f)
    }
}

/**
 * Perform the actual animation of ball movements (including its chain) and
 * the particle explosion effect.
 *
 * @param drawScope Canvas to draw the grid and balls
 * @param gridSize Size of grid. This used to do various computation on animation effect
 * @param animateBallMovementChainCtlList Chain list of objects that control animation
 * state of all the ball movements in this chain
 * @param animateParticleExplosionCtl  Object that control animation state of particle explosion effect
 * @param direction Direction of the ball movement
 * @param movingChain Chain list of ball movements
 */
fun animateBallMovementsPerform(
    drawScope: DrawScope,
    gridSize: Float,
    animateBallMovementChainCtlList: MutableList<Animatable<Float, AnimationVector1D>>,
    animateParticleExplosionCtl: Animatable<Float, AnimationVector1D>,
    particles: MutableList<Particle>,
    direction: Direction,
    movingChain: BallMoveSet
) {
    if (movingChain.isEmpty()) return
    if (direction == Direction.NO_WINNING_DIRECTION) return
    if (animateBallMovementChainCtlList.isEmpty()) return

    with(drawScope) {
        var movingSourcePos: Pos
        var offset: Pair<Float, Float>
        var xOffset: Float
        var yOffset: Float

        // Ball movements including multiple ball movements (if there is multiple balls in the chain)
        for ((index, currentMovement) in movingChain.withIndex()) {
            movingSourcePos = currentMovement.pos

            offset = setOffsets(direction, movingChain[index].distance, gridSize)
            xOffset = offset.first
            yOffset = offset.second

            // Perform animation by adjusting both the xOffset and yOffset amount respectively
            // to move the ball
            translate(
                (xOffset) * ((animateBallMovementChainCtlList[index]).value),
                (yOffset) * ((animateBallMovementChainCtlList[index]).value)
            ) {
                drawBallOnGrid(drawScope, gridSize, movingSourcePos)
            } // translate
        }  // for

        // Particle explosion effect
        particles.forEach { curParticle ->
            curParticle.updateProgress(animateParticleExplosionCtl.value, gridSize)
            drawCircle(
                alpha = curParticle.alpha,
                color = curParticle.color,
                radius = curParticle.currentRadius,
                center = Offset(curParticle.currentXPosition, curParticle.currentYPosition)
            )
        }
    }   // drawScope
}

/************************ Victory Announcement Animation Routines *********************************/

/**
 * Perform the actual victory message animation
 *
 * @param drawScope Canvas to draw the animation on
 * @param animateCtl  Object that control animation state of the victory message
 * @param textMeasurer Responsible for measuring a text in its entirety so that it can be drawn
 * on the canvas (drawScope).
 * @param victoryMsgColor Color of the message. Color will differ depending on the current theme
 */
fun animateVictoryMsgPerform(
    drawScope: DrawScope,
    animateCtl: Animatable<Float, AnimationVector1D>,
    textMeasurer: TextMeasurer,
    victoryMsgColor: Color
) {
    val animationValue = animateCtl.value
    with(drawScope) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val text = "You won!"
        val animatedTextSize = (50 * animationValue) + 10
        val textStyle = TextStyle(
            color = victoryMsgColor,
            fontWeight = FontWeight.Bold,
            fontSize = animatedTextSize.sp
        )
        val textLayoutResult: TextLayoutResult =
            textMeasurer.measure(text = AnnotatedString(text), style = textStyle)
        val textSize = textLayoutResult.size
        drawText(
            textMeasurer = textMeasurer, text = text,
            topLeft = Offset(
                x = (canvasWidth - textSize.width) * 0.5f,  // in center
                y = (canvasHeight * 0.25f)
            ),
            style = textStyle
        )
    }
}

/**
 * Setup animated victory message. Define animation spec.
 *
 * @param setModeAfterVictoryMsg Lambda function to set UI to "WaitingOnUser" mode after announcing victory message
 * @param animateCtl  Object that control animation state of the victory message
 */
@Composable
fun AnimateVictoryMessageSetup(
    setModeAfterVictoryMsg: () -> Unit,
    animateCtl: Animatable<Float, AnimationVector1D>,
) {
    // Run the side effect only once
    LaunchedEffect(Unit) {
        // Use coroutine to ensure both animation and sound happen in parallel
        coroutineScope {
            launch (Dispatchers.Main) {
                animateCtl.snapTo(0f)
                animateCtl.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1500,
                        easing = LinearOutSlowInEasing
                    )
                )
                setModeAfterVictoryMsg.invoke()

                animateCtl.snapTo(0f)
                animateCtl.stop()

                // Pause for 0.5 second to allow user see victory message before it disappear
                delay(500)

            }  // launch

            launch (Dispatchers.Main) { gAudio_victory.start() }

        }  // coroutineScope
    } // LaunchedEffect
}

/**
 * Stop and reset the victory message animation control
 *
 * @param animateCtl  Object that control animation state of the victory message
 */
@Composable
fun AnimateVictoryMessageReset(animateCtl: Animatable<Float, AnimationVector1D>)
{
    // Run the side effect only once
    LaunchedEffect(Unit) {
        animateCtl.stop()
        animateCtl.snapTo(0F)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoWinnableMoveDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit
) {
    Dialog(
        onDismissRequest = { onDismissRequest() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(200.dp)
                .padding(0.dp)
                .height(IntrinsicSize.Min)
                .border(
                    0.dp, color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(16.dp)
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Column(
                Modifier.fillMaxWidth()
            ) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Image(
                            painter = painterResource(id = R.drawable.ball),
                            contentDescription = "Game",
                            contentScale = ContentScale.Fit
                        )
                    }
                    Column(Modifier.weight(3f)) {
                        Text(
                            text = stringResource(R.string.there_is_no_winnable_move),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp, 8.dp, 8.dp, 2.dp)
                                .align(Alignment.CenterHorizontally)
                                .fillMaxWidth(),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outline
                )
                Row(Modifier.padding(top = 0.dp)) {
                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentEnforcement provides false,
                    ) {
                        TextButton(
                            onClick = { onConfirmation() },
                            Modifier
                                .fillMaxWidth()
                                .padding(0.dp)
                                .weight(1F)
                                .border(0.dp, color = Color.Transparent)
                                .height(48.dp),
                            elevation = ButtonDefaults.elevatedButtonElevation(0.dp, 0.dp),
                            shape = RoundedCornerShape(0.dp),
                            contentPadding = PaddingValues()
                        ) {
                            Text(
                                text = stringResource(id = R.string.OK),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Display "Loading data..." message on the board grid
 *
 * It is very unlikely user will see this display as app will load the data very quickly
 *
 *  @param drawScope Canvas to draw the text on
 *  @param textMeasurer Responsible for measuring a text in its entirety so that it can be drawn
 *  on the canvas (drawScope).
 *  @param msgColor Color of the message. Color will differ depending on the current theme
 */
fun displayLoadingMessage(
    drawScope: DrawScope,
    textMeasurer: TextMeasurer,
    msgColor: Color
) {
    with(drawScope) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val text = "Loading data ...."
        val textStyle = TextStyle(
            color = msgColor,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        val textLayoutResult: TextLayoutResult =
            textMeasurer.measure(text = AnnotatedString(text), style = textStyle)
        val textSize = textLayoutResult.size
        drawText(
            textMeasurer = textMeasurer, text = text,
            topLeft = Offset(
                x = (canvasWidth - textSize.width) * 0.5f,  // in center
                y = (canvasHeight * 0.25f)
            ),
            style = textStyle
        )
    }
}