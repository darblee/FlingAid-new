package com.darblee.flingaid.ui.screens

import android.graphics.Bitmap
import android.media.AudioManager
import android.util.Log
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.darblee.flingaid.Direction
import com.darblee.flingaid.Global
import com.darblee.flingaid.R
import com.darblee.flingaid.gAudioFocusRequest
import com.darblee.flingaid.gAudio_youWon
import com.darblee.flingaid.gDownArrowBitmap
import com.darblee.flingaid.gLeftArrowBitmap
import com.darblee.flingaid.gRightArrowBitmap
import com.darblee.flingaid.gUpArrowBitmap
import com.darblee.flingaid.ui.MovingRec
import com.darblee.flingaid.ui.SolverState
import com.darblee.flingaid.ui.SolverUiState
import com.darblee.flingaid.ui.SolverViewModel
import com.darblee.flingaid.ui.SolverGridPos
import java.io.File
import java.util.Locale
import kotlin.math.abs

lateinit var gBoardFile : File

@Composable
fun SolverScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit)
{
    val solverViewModel: SolverViewModel = viewModel()
    Column (
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val uiState by solverViewModel.uiState.collectAsState()

        gBoardFile = File(LocalContext.current.filesDir, Global.boardFileName)

        solverViewModel.loadBallPositions(gBoardFile)  // Load balls from previous game save

        var findWinnableMoveButtonEnabled by remember { mutableStateOf(false) }
        findWinnableMoveButtonEnabled =
            ((solverViewModel.ballCount() > 1) && (!solverViewModel.foundWinnableMove()))

        var showWinnableMoveToUser by remember { mutableStateOf(false) }
        showWinnableMoveToUser =
            (uiState.foundWinningDirection != Direction.NO_WINNING_DIRECTION)

        if (uiState.needToDisplayNoWinnableToastMessage) {
            DisplayNoWinnableMoveToast()
            solverViewModel.noNeedToDisplayNoWinnableToastMessage()
        }

        Instruction_DynamicLogo(uiState, onNavigateBack)
        ControlButtonsForSolver(
            solverViewModel,
            findWinnableMoveButtonEnabled,
            showWinnableMoveToUser,
            uiState
        )

        DrawSolverBoard(
            modifier = Modifier.fillMaxSize(),
            solverViewModel = solverViewModel,
            uiState = uiState
        )
    }
}

/*
 * Provide instruction on how to play Solver Screen.
 *
 * Display the game logo. Game logo also change to animation
 * when it is searching for the solution.
 */
@Composable
private fun Instruction_DynamicLogo(uiState: SolverUiState,
                                    onNavigateBack: () -> Unit)
{
    val logoSize = 125.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Box {
            if (uiState.state == SolverState.NotThinking) {
                val imageModifier = Modifier
                    .size(logoSize)
                    .align(Alignment.Center)
                    .background(MaterialTheme.colorScheme.background)
                Image(
                    painter = painterResource(id = R.drawable.ball),
                    contentDescription = stringResource(id = R.string.app_name),
                    contentScale = ContentScale.Fit,
                    modifier = imageModifier
                )
            } else {
                PlaySearchAnimation(
                    modifier = Modifier
                        .size(logoSize)
                        .align(Alignment.Center)
                )

                //  Track two level processing = level #1: 4 direction x level 2: 4 directions = 16
                val newPercentageValue: Float = (Global.ThinkingProgress.toFloat() /
                        (Global.totalProcessCount) * 100.0).toFloat()
                val percentComplete =
                    String.format(Locale.getDefault(), "%.1f%%", newPercentageValue)
                Text("$percentComplete Complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopCenter))
            }
        }
        Column(Modifier.padding(5.dp)) {
            Text(text = "Instruction:",
                style = MaterialTheme.typography.titleSmall)

            val bullet = "\u2022"
            val messages = listOf(
                "Clear the board with \"Reset\" button",
                "Add new balls on the grid",
                "Solve the game with \"Find next\" button"
            )
            val paragraphStyle = ParagraphStyle(textIndent = TextIndent(restLine = 10.sp))
            Text(
                text =
                buildAnnotatedString {
                    messages.forEach {
                        withStyle(style = paragraphStyle) {
                            append(bullet)
                            append("\t")
                            append(it)
                        }
                    }},
                style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.padding(10.dp))

            Button(
                onClick = { onNavigateBack.invoke() },
                Modifier
                    .defaultMinSize()
                    .align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.primaryContainer
                ),
            ) {
                Icon(painterResource(id = R.drawable.home_image), "Home")
                Text(
                    text = "Back to Home"
                )
            }
        }
    }
}

@Composable
private fun ControlButtonsForSolver(
    solverViewModel: SolverViewModel = viewModel(),
    findWinnableMoveButtonEnabled: Boolean,
    showWinnableMoveToUser: Boolean,
    uiState: SolverUiState)
{
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Button(
            onClick = {
                Log.i(Global.debugPrefix, ">>> Starting thinking : Button Pressed")
                if (showWinnableMoveToUser) {
                    // After moving the ball, we need to find the next move and show the hint right away.
                    //
                    // When setting up the moving chain, it will start a new LaunchEffect thread
                    // that will call animation complete at the end, which will in turn
                    // make the actual ball movement
                    val winningDirection = uiState.foundWinningDirection
                    val winningPos = uiState.winningPosition
                    solverViewModel.setupMovingChain(winningPos.row, winningPos.col, winningDirection)
                } else {
                    // In this case, we did not move the ball as we did not show hint yet.
                    // We need to find the winning move.
                    Log.i(Global.debugPrefix, ">>> Looking for next winnable move")
                    solverViewModel.findWinningMove(solverViewModel)
                }
            }, // OnClick
            shape = RoundedCornerShape(5.dp),
            elevation = ButtonDefaults.buttonElevation(5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier
                .weight(3F)
                .padding(5.dp),
            enabled = ((findWinnableMoveButtonEnabled || showWinnableMoveToUser) &&
                    (uiState.state == SolverState.NotThinking))
        ) {
            val iconWidth = Icons.Filled.Refresh.defaultWidth
            Icon(imageVector = Icons.Filled.Search, contentDescription = "Find Winning Move",
                modifier = Modifier.size(iconWidth))
            if (showWinnableMoveToUser)
                Text("Move and find next")
            else
                Text("Find next")
        }

        Button(
            onClick = {
                solverViewModel.reset(gBoardFile)
            },
            shape = RoundedCornerShape(5.dp),
            elevation = ButtonDefaults.buttonElevation(5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .weight(2F)
                .padding(5.dp),
        ) {
            val iconWidth = Icons.Filled.Refresh.defaultWidth
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Reset",
                modifier = Modifier.size(iconWidth))
            Text("Reset")
        }
    }
}

@Composable
private fun DisplayNoWinnableMoveToast()
{
    val contextForToast = LocalContext.current

    Toast.makeText(
        contextForToast,
        "There is no winnable move",
        Toast.LENGTH_LONG
    ).show()
}

/*
 *  Draw the Solver Game Board:
 *       - Grid
 *       - all the balls
 *       - winning arrow (if there is solution after "find winnable movable" submission
 */
@Composable
private fun DrawSolverBoard(
    modifier: Modifier = Modifier,
    solverViewModel: SolverViewModel = viewModel(),
    uiState: SolverUiState
    )
{
    /*
     * Launch the animation only once when it enters the composition. It will animate infinitely
     * until it is removed from the composition
     */
    val animateWinningMove = remember { Animatable(initialValue = 0f) }
    LaunchedEffect(Unit) {
        animateWinningMove.animateTo(
            targetValue = 1f, animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    val animate1 = remember { Animatable(initialValue = 0f) }
    val animate2 = remember { Animatable(initialValue = 0f) }
    val animate3 = remember { Animatable(initialValue = 0f) }

    if (solverViewModel.needBallAnimation()) {
        LaunchedEffect(Unit) {
            animate1.animateTo(
                targetValue = 1f, animationSpec =
                repeatable(
                    iterations = 1,
                    animation = tween(250, easing = FastOutLinearInEasing),
                )
            )
            animate2.animateTo(
                targetValue = 1f, animationSpec =
                repeatable(
                    iterations = 1,
                    animation = tween(250, easing = FastOutLinearInEasing),
                )
            )
            animate3.animateTo(
                targetValue = 1f, animationSpec =
                repeatable(
                    iterations = 1,
                    animation = tween(250, easing = FastOutLinearInEasing),
                )
            )
            animate1.snapTo(0F)
            animate2.snapTo(0F)
            animate3.snapTo(0F)
            solverViewModel.ballMovementAnimationComplete()
            solverViewModel.makeWinningMove(uiState)
            if (solverViewModel.ballCount() > 1) {
                Log.i(Global.debugPrefix, ">>> Looking for next winnable move")
                solverViewModel.findWinningMove(solverViewModel)
            }

            if (solverViewModel.ballCount() == 1) {
                if (gAudioFocusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    gAudio_youWon.start()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(Global.MaxColSize.toFloat() / Global.MaxRowSize.toFloat())
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        val context = LocalContext.current
        val view = LocalView.current
        val lineColor = MaterialTheme.colorScheme.outline
        var gridSize by rememberSaveable {
            mutableFloatStateOf(0f)
        }

        val ballImage = ImageBitmap.imageResource(id = R.drawable.ball)

        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        var dragRow by remember { mutableIntStateOf(-1) }
        var dragCol by remember { mutableIntStateOf(-1) }

        val minSwipeOffset = gridSize

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .padding(15.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            // For unknown reason, we can not use uiState.state
                            val thinkingStatus = solverViewModel.getThinkingStatus()
                            if (thinkingStatus == SolverState.Thinking) {
                                Toast
                                    .makeText(
                                        context,
                                        "Unable to modify board while still searching",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                            } else {
                                val row = (tapOffset.y / gridSize).toInt()
                                val col = (tapOffset.x / gridSize).toInt()
                                if ((row < Global.MaxRowSize) && (col < Global.MaxColSize)) {
                                    solverViewModel.toggleBallPosition(SolverGridPos(row, col))
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    solverViewModel.saveBallPositions(gBoardFile)
                                }
                            } // if thinkingStatus != GameState.thinking
                        }, // onTap
                    ) // detectTapGestures
                } // .pointerInput
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset: Offset ->
                            dragRow = (offset.y / gridSize).toInt()
                            dragCol = (offset.x / gridSize).toInt()
                        },
                        onDrag = { change, dragAmount ->

                            /*
                             * Need to consume this event, so that its parent knows not to react to
                             * it anymore. What change.consume() does is it prevent pointerInput
                             * above it to receive events by returning PointerInputChange.positionChange()
                             * Offset.Zero PointerInputChange.isConsumed true.
                             */
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        },
                        onDragEnd = {
                            when {
                                (offsetX < 0F && abs(offsetX) > minSwipeOffset) -> {
                                    Toast.makeText(context,
                                        "Use \"Find next\" button to move the ball",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    offsetX = 0F
                                    offsetY = 0F
                                    dragRow = -1
                                    dragCol = -1
                                }

                                (offsetX > 0F && abs(offsetX) > minSwipeOffset) -> {
                                    Toast.makeText(context,
                                        "Use \"Find next\" button to move the ball",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    offsetX = 0F
                                    offsetY = 0F
                                    dragRow = -1
                                    dragCol = -1
                                }

                                (offsetY < 0F && abs(offsetY) > minSwipeOffset) -> {
                                    Toast.makeText(context,
                                        "Use \"Find next\" button to move the ball",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    offsetX = 0F
                                    offsetY = 0F
                                    dragRow = -1
                                    dragCol = -1
                                }

                                (offsetY > 0F && abs(offsetY) > minSwipeOffset) -> {
                                    Toast.makeText(context,
                                        "Use \"Find next\" button to move the ball",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    offsetX = 0F
                                    offsetY = 0F
                                    dragRow = -1
                                    dragCol = -1
                                }
                            }
                        }
                    ) // detectDragGestures
                } // .pointerInput

        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val gridSizeWidth = (canvasWidth / (Global.MaxColSize))
            val gridSizeHeight = (canvasHeight / (Global.MaxRowSize))

            gridSize = if (gridSizeWidth > gridSizeHeight) gridSizeHeight else gridSizeWidth

            drawGrid(this, gridSize, lineColor)

            val ballSize =  (gridSize * 1.10).toInt()
            val displayBallImage = Bitmap.createScaledBitmap(ballImage.asAndroidBitmap(),
                ballSize, ballSize, false).asImageBitmap()

            if (solverViewModel.needBallAnimation()) {
                val ballsToErase = solverViewModel.uiState.value.movingChain
                drawSolverBalls(this, solverViewModel, gridSize, displayBallImage, ballsToErase)
            } else {
                drawSolverBalls(this, solverViewModel, gridSize, displayBallImage)

                if (solverViewModel.ballCount() > 1) {
                    // Draw the winning arrow if there is a winning move identified
                    if (solverViewModel.foundWinnableMove()) {
                        val moveCount = solverViewModel.getWinningMoveCount(uiState)
                        showWinningMove(
                            this,
                            gridSize,
                            uiState,
                            animateWinningMove,
                            moveCount,
                            displayBallImage
                        )
                    }
                }
            }

            if (solverViewModel.needBallAnimation()) {
                val movingDirection = solverViewModel.uiState.value.movingDirection
                val movingChain = solverViewModel.uiState.value.movingChain

                var movingSourcePos = movingChain[0].pos
                var offset = setOffsets(movingDirection, movingChain[0].distance, gridSize)
                var xOffset = offset.first
                var yOffset = offset.second

                translate(
                    (xOffset) * animate1.value,
                    (yOffset) * animate1.value
                ) {
                    drawImage(
                        image = displayBallImage,
                        topLeft = Offset(
                            x = movingSourcePos.col * gridSize,
                            y = movingSourcePos.row * gridSize
                        )
                    )
                }

                if (movingChain.size > 1) {
                    movingSourcePos = movingChain[1].pos
                    offset = setOffsets(movingDirection, movingChain[1].distance, gridSize)
                    xOffset = offset.first
                    yOffset = offset.second

                    translate(
                        (xOffset) * animate2.value,
                        (yOffset) * animate2.value
                    ) {
                        drawImage(
                            image = displayBallImage,
                            topLeft = Offset(
                                x = movingSourcePos.col * gridSize,
                                y = movingSourcePos.row * gridSize
                            )
                        ) // drawImage
                    } // DrawScope

                    if (movingChain.size > 2) {
                        movingSourcePos = movingChain[2].pos
                        offset = setOffsets(movingDirection, movingChain[2].distance, gridSize)
                        xOffset = offset.first
                        yOffset = offset.second

                        translate(
                            (xOffset) * animate3.value,
                            (yOffset) * animate3.value
                        ) {
                            drawImage(
                                image = displayBallImage,
                                topLeft = Offset(
                                    x = movingSourcePos.col * gridSize,
                                    y = movingSourcePos.row * gridSize
                                )
                            ) // drawImage
                        } // DrawScope
                    }
                }   // if (movingChain size > 1
            }  // if needToAnimateMovingBall
        }
    }
}

private fun setOffsets(direction: Direction, distance: Int, gridSize: Float): Pair<Float, Float>
{
    var xOffset = -1F
    var yOffset = -1F

    when (direction) {
        Direction.UP -> {
            xOffset = 0 * gridSize
            yOffset = -1 * distance * gridSize
        }

        Direction.DOWN -> {
            xOffset = 0 * gridSize
            yOffset = distance * gridSize
        }

        Direction.LEFT -> {
            xOffset = -1 * distance * gridSize
            yOffset = 0 * gridSize
        }

        Direction.RIGHT -> {
            xOffset = distance * gridSize
            yOffset = 0 * gridSize
        }

        else -> {
            assert(true) { "Got unexpected direction during animation" }
        }
    }

    return(Pair(xOffset, yOffset))
}

/*
 * Show Winning Move
 *   Show direction arrow
 *   Animate the ball movement
 */
private fun showWinningMove(
    drawScope: DrawScope,
    gridSize: Float,
    uiState: SolverUiState,
    animate: Animatable<Float, AnimationVector1D>,
    gWinningMoveCount: Int,
    displayBallImage: ImageBitmap)
{
    with (drawScope) {
        var xOffset = 0
        var yOffset = 0

        val displayArrowBitMap = when (uiState.foundWinningDirection) {
            Direction.UP -> {
                yOffset = -1 * gridSize.toInt() * gWinningMoveCount
                gUpArrowBitmap
            }
            Direction.DOWN -> {
                yOffset = 1 * gridSize.toInt() * gWinningMoveCount
                gDownArrowBitmap
            }
            Direction.LEFT -> {
                xOffset = -1 * gridSize.toInt() * gWinningMoveCount
                gLeftArrowBitmap
            }
            Direction.RIGHT -> {
                xOffset = 1 * gridSize.toInt() * gWinningMoveCount
                gRightArrowBitmap
            }

            // NOTE: bitmap configuration describes how pixels are stored. This affects the quality
            // (color depth) as well as the ability to display transparent/translucent colors.
            // "Bitmap.Config.ARGB_8888" indicates the maximum flexibility
            else -> {
                Log.e(Global.debugPrefix, "Got unexpected Direction value: ${uiState.foundWinningDirection}")
                assert(true)
                (Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
            }
        }

        // Reduce size of arrow to fit inside the grid
        val displayArrow = Bitmap.createScaledBitmap(displayArrowBitMap, gridSize.toInt(),
            gridSize.toInt(), false).asImageBitmap()

        drawImage(
            displayArrow,
            topLeft = Offset(
                x = ((uiState.winningPosition.col) * gridSize),
                y = ((uiState.winningPosition.row * gridSize)))
        )

        translate(
            (xOffset) * animate.value,
            (yOffset) * animate.value
        ) {
            drawImage(
                image = displayBallImage,
                topLeft = Offset(
                    x = ((uiState.winningPosition.col) * gridSize),
                    y = ((uiState.winningPosition.row * gridSize))
                )
            )
        }
    }
}

// Draw all the balls in the provided canvas
//
// During ball animation, we need to temporarily erase the animation ball
private fun drawSolverBalls(
    drawScope: DrawScope,
    solverViewModel: SolverViewModel,
    gridSize: Float,
    displayBallImage: ImageBitmap,
    eraseAnimatedBallPositions: List<MovingRec> = listOf()
)
{
    // Draw all the balls
    with (drawScope) {
        solverViewModel.ballPositionList().forEach { pos ->
            var skipDraw = false
            eraseAnimatedBallPositions.forEach { eraseRec ->
                if ((eraseRec.pos.col == pos.col) && (eraseRec.pos.row == pos.row))
                    skipDraw = true
            }

            if (!skipDraw) {
                drawImage(
                    image = displayBallImage,
                    topLeft = Offset(
                        (pos.col * gridSize),
                        (pos.row * gridSize)
                    )
                )
            }
        }
    }
}

private fun drawGrid(
    drawScope: DrawScope,
    gridSize: Float,
    lineColor: Color)
{
    with (drawScope) {
        // Draw horizontal lines
        var currentY = 0F
        val gridWidth = gridSize * Global.MaxColSize
        repeat(Global.MaxRowSize + 1) { index ->
            val lineWidth = if (index == 4) 5 else 2
            drawLine(
                start = Offset(x = 0.dp.toPx(), y = currentY),
                end = Offset(x = gridWidth, y = currentY),
                color = lineColor,
                strokeWidth = lineWidth.dp.toPx() // instead of 5.dp.toPx() , you can also pass 5f
            )
            currentY += gridSize
        }

        // Draw vertical lines
        var currentX = 0F
        val gridHeight = gridSize * Global.MaxRowSize
        repeat(Global.MaxColSize + 1) {

            drawLine(
                start = Offset(x = currentX, y = 0.dp.toPx()),
                end = Offset(x = currentX, y = gridHeight),
                color = lineColor,
                strokeWidth = 2.dp.toPx() // instead of 5.dp.toPx() , you can also pass 5f
            )
            currentX += gridSize
        }

        // Draw the circle in the center of the grid
        val offsetX = (gridSize  * ((Global.MaxColSize / 2) + 0.5)).toFloat()
        val offsetY = (gridSize  * ((Global.MaxRowSize / 2)))
        val radiusLength = (gridSize * 0.66).toFloat()
        drawCircle(lineColor, radius = radiusLength, center = Offset(x = offsetX, y= offsetY), style = Stroke(width = 4.dp.toPx()))
    }
}

@Composable
private fun PlaySearchAnimation(modifier: Modifier)
{
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.find_animation))

    LottieAnimation(
        modifier = modifier,
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
}

@Composable
@Preview(showBackground = true)
private fun SolverScreenPreview()
{
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Box {
            Image(
                painter = painterResource(id = R.drawable.ball),
                contentDescription = stringResource(id = R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(150.dp)
            )
        }
        Column(Modifier.padding(5.dp)) {
            Text(text = "Instruction:",
                style = MaterialTheme.typography.titleSmall)
            val bullet = "\u2022"
            val messages = listOf(
                "Clear the board with \"Reset\" button",
                "Add new balls on the grid",
                "Solve the game with \"Find next\" button"
            )
            val paragraphStyle = ParagraphStyle(textIndent = TextIndent(restLine = 10.sp))
            Text(
                text =
                buildAnnotatedString {
                    messages.forEach {
                        withStyle(style = paragraphStyle) {
                            append(bullet)
                            append("\t")
                            append(it)
                        }
                    }},
                style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.padding(10.dp))

            Button(
                onClick = { },
                modifier = Modifier
                    .defaultMinSize()
                    .align(Alignment.CenterHorizontally)
            ) {
                Icon(painterResource(id = R.drawable.home_image), "Home")
                Text(
                    text = "Go back to Main Menu"
                )
            }
        }
    }
}