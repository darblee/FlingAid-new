package com.darblee.flingaid.utilities

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darblee.flingaid.Direction
import com.darblee.flingaid.Global
import com.darblee.flingaid.gAudio_victory
import com.darblee.flingaid.ui.MovingRec
import com.darblee.flingaid.ui.Particle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileReader
import java.io.FileWriter

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
 * [removeGameFile] Delete the file
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
     * Delete the game file
     */
    fun removeGameFile() {
        _gameFile?.delete()
    }

    /**
     * Save the game board to a file
     */
    fun saveBallListToFile() {
        val format = Json { prettyPrint = true }
        val ballList = mutableListOf<Pos>()

        for (currentPos in this.ballList) {
            ballList += currentPos
        }
        val output = format.encodeToString(ballList)

        try {
            val writer = FileWriter(_gameFile)
            writer.write(output)
            writer.close()
        } catch (e: Exception) {
            Log.i(Global.DEBUG_PREFIX, "${e.message}")
        }
    }

    /**
     * Load the saved game board from file
     **/
    fun loadBallListFromFile() {
        try {
            val reader = FileReader(_gameFile)
            val data = reader.readText()
            reader.close()

            val list = Json.decodeFromString<List<Pos>>(data)

            ballList.clear()
            for (pos in list) {
                ballList.add(pos)
            }
        } catch (e: Exception) {
            Log.i(Global.DEBUG_PREFIX, "An error occurred while reading the file: ${e.message}")
        }
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
    ): List<MovingRec> {
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
     *  second element is the position of the next ball. If there is no possible move, it will
     *  return <0, null>
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
                distance++ // Add two to make it fall off the edge
                distance++
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
     * @param displayBallImage Actual image of ball o display
     * @param eraseAnimatedBallPositions Used during ball animation. We need to temporarily
     * erase the animation ball as the animation routine will display it
     */
    fun drawAllBalls(
        drawScope: DrawScope,
        gridSize: Float,
        displayBallImage: ImageBitmap,
        eraseAnimatedBallPositions: List<MovingRec> = listOf()
    ) {
        // Draw all the balls
        //
        // NOTE: ballPositionList is a SnapshotList type. It is observation system that will trigger a recompose
        // to redraw the grid.
        ballList.forEach { pos ->
            var skipDraw = false
            eraseAnimatedBallPositions.forEach { eraseRec ->
                if ((eraseRec.pos.col == pos.col) && (eraseRec.pos.row == pos.row))
                    skipDraw = true
            }

            if (!skipDraw) drawBallOnGrid(drawScope, gridSize, pos, displayBallImage)
        }
    }
}

/**
 * Draw the ball in a specific position (e,g, row, col) on the board grid
 *
 * @param drawScope
 * @param gridSize Size the dimension of the grid
 * @param pos Specific position (row, column)
 * @param displayBallImage Actual image bitmap of the ball
 * @param alpha amount of transparency
 */
fun drawBallOnGrid(
    drawScope: DrawScope,
    gridSize: Float,
    pos: Pos,
    displayBallImage: ImageBitmap,
    alpha: Float = 1.0F
) {
    val offsetAdjustment = (gridSize / 2) - (displayBallImage.width / 2)

    with(drawScope) {
        drawImage(
            image = displayBallImage,
            topLeft = Offset(
                (pos.col * gridSize) + offsetAdjustment,
                (pos.row * gridSize) + offsetAdjustment
            ),
            alpha = alpha
        )
    }
}

/*************************** Animation Routines ************************************************/

/*************** Shadow Ball Movement *******************************/

/* The set-up routine is done on each screen - GameScreen & SolverScreen */


/**
 * Animate the shadowed ball movement.
 *
 * @param drawScope Scope to do the drawing on
 * @param gridSize width or height of the grid in dp unit
 * @param animateCtl Animate object controller that manage shadow ball movement
 * @param displayBallImage The image of the ball
 * @param distance number of blocks in this ball movement
 * @param direction Direction of the ball movement
 * @param pos Position of the ball to move from
 */
fun animateShadowBallMovementsPerform(
    drawScope: DrawScope,
    gridSize: Float,
    animateCtl: Animatable<Float, AnimationVector1D>,
    displayBallImage: ImageBitmap,
    distance: Int,
    direction: Direction,
    pos: Pos
) {
    with(drawScope) {
        var xOffset = 0
        var yOffset = 0

        when (direction) {
            Direction.UP -> { yOffset = -1 * gridSize.toInt() * distance }
            Direction.DOWN -> { yOffset = 1 * gridSize.toInt() * distance }
            Direction.LEFT -> { xOffset = -1 * gridSize.toInt() * distance }
            Direction.RIGHT -> { xOffset = 1 * gridSize.toInt() * distance }
            else -> {
                assert(true) { "Got unexpected Direction value: $direction" }
            }
        }

        translate(
            (xOffset) * animateCtl.value,
            (yOffset) * animateCtl.value
        ) {
            drawBallOnGrid(drawScope, gridSize, pos, displayBallImage, alpha = 0.4F)
        }
    }
}

/*************** Ball Movement ************************************/

/* The set-up routine is done on each screen - GameScreen & SolverScreen */

/**
 * Create all particles, which is used for the explosion animated effect
 *
 * @param movingChain Moving chain. __NOTE:__ This function assume the [movingChain] have at
 * least 2 movements.
 * @param direction Ball movement direction
 *
 * @return List of all the particles
 */
fun generateExplosionParticles(movingChain: List<MovingRec>,
                               direction: Direction): List<Particle>
{
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
        1.02f.at((whenBallMakeContactRatio * totalTimeLength).toInt()) using FastOutLinearInEasing   // Overrun the ball slightly to hit the neighboring ball
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
 * Perform the actual animation of ball movements (including its chain) and
 * the particle explosion effect.
 *
 * @param drawScope Canvas to draw the grid and balls
 * @param gridSize Size of grid. This used to do various computation on animation effect
 * @param displayBallImage The actual ball image to show
 * @param animateBallMovementChainCtlList Chain list of animate objects that control animation
 * state of all the ball movements in this chain
 * @param animateParticleExplosionCtl Animate Object that control animation state of particle explosion effect
 * @param direction Direction of the ball movement
 * @param movingChain Chain list of ball movements
 */
fun animateBallMovementsPerform(
    drawScope: DrawScope,
    gridSize: Float,
    displayBallImage: ImageBitmap,
    animateBallMovementChainCtlList: MutableList<Animatable<Float, AnimationVector1D>>,
    animateParticleExplosionCtl: Animatable<Float, AnimationVector1D>,
    particles: MutableList<Particle>,
    direction: Direction,
    movingChain: List<MovingRec>
) {
    if (movingChain.isEmpty())  return
    if (direction == Direction.NO_WINNING_DIRECTION) return
    if (animateBallMovementChainCtlList.isEmpty()) return

    with (drawScope) {
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
                drawBallOnGrid(drawScope, gridSize, movingSourcePos, displayBallImage)
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

/*************** Victory Announcement ************************************/

/**
 * Perform the actual victory message animation
 *
 * @param drawScope Canvas to draw the animation on
 * @param animateCtl Animate object that control animation state of the victory message
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
        val canvasWidth =  size.width
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
 * @param setIdleFunction Lambda function to set UI to idle after announcing victory message
 * @param animateCtl Animate object that control animation state of the victory message
 * @param onAnimationChange Change the state on whether to perform the animation or not
 */
@Composable
fun AnimateVictoryMessageSetup(
    setIdleFunction: () -> Unit,
    animateCtl: Animatable<Float, AnimationVector1D>,
    onAnimationChange: (enableVictoryMessage: Boolean) -> Unit
) {
    LaunchedEffect(Unit) {
        // Use coroutine to ensure both animation and sound happen in parallel
        coroutineScope {
            launch {
                animateCtl.snapTo(0f)
                animateCtl.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 1500,
                        easing = LinearOutSlowInEasing
                    )
                )
                delay(500)  // Pause for 0.5 second to see victory message before it disappear
                onAnimationChange(false)
                animateCtl.snapTo(0f)
                setIdleFunction.invoke()
            }  // launch

            launch {
                gAudio_victory.start()
            }
        }  // coroutineScope
    } // LaunchedEffect
}