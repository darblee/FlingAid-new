package com.darblee.flingaid.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.widget.Toast
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.darblee.flingaid.BackPressHandler
import com.darblee.flingaid.Direction
import com.darblee.flingaid.Global
import com.darblee.flingaid.R
import com.darblee.flingaid.ui.MovingRec
import com.darblee.flingaid.ui.Particle
import com.darblee.flingaid.ui.SolverUiState
import com.darblee.flingaid.ui.SolverViewModel
import com.darblee.flingaid.ui.SolverGridPos
import com.darblee.flingaid.utilities.gameToast
import com.darblee.flingaid.utilities.randomInRange
import com.darblee.flingaid.utilities.toPx
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.abs

/**
 *  The main Solver Game Screen
 *
 *  @param modifier Pass in modifier elements that decorate or add behavior to the compose UI
 *  elements
 *  @param navController Central coordinator for managing navigation between destination screens,
 *  managing the back stack, and more
 */
@Composable
fun SolverScreen(modifier: Modifier = Modifier, navController: NavHostController) {
    var announceVictory by remember { mutableStateOf(false) }
    var needToLoadGameFile by remember { mutableStateOf(true) }

    // Intercept backPress key while on Game Solver screen..
    //
    // When doing back press on the current screen, confirm with the user whether
    // it should exit this screen or not if it is middle of thinking.
    // Do not exit this screen when:
    // - It is middle of thinking
    // - It is in middle of announcing victory message
    var backPressed by remember { mutableStateOf(false) }
    BackPressHandler(onBackPressed = { backPressed = true })
    if (backPressed) {
        backPressed = false
        if (announceVictory) return
        solverScreenBackPressed(LocalContext.current, navController)
        return
    }

    val solverViewModel: SolverViewModel = viewModel()
    val uiState by solverViewModel.uiState.collectAsState()

    val boardFile = File(LocalContext.current.filesDir, Global.BOARD_FILENAME)

    val onEnableVictoryMsg = { setting: Boolean -> announceVictory = setting }
    val victoryMsgColor = MaterialTheme.colorScheme.onPrimaryContainer

    if (needToLoadGameFile) {
        solverViewModel.setFile(boardFile)
     /* TODO    needToLoadGameFile = false */
    }

    var findWinnableMoveButtonEnabled by remember { mutableStateOf(false) }
    findWinnableMoveButtonEnabled =
        ((solverViewModel.ballCount() > 1) && (!hasFoundWinnableMove(uiState)))

    var showWinnableMoveToUser by remember { mutableStateOf(false) }
    showWinnableMoveToUser =
        (uiState.winningDirection != Direction.NO_WINNING_DIRECTION)

    /**
     * Keep track of when to do the ball movement animation
     */
    var showBallMovementAnimation by remember { mutableStateOf(false) }
    val onBallMovementAnimationChange =
        { enableBallMovements: Boolean -> showBallMovementAnimation = enableBallMovements }

    if (uiState.thinkingStatus == SolverUiState.ThinkingMode.Idle) {
        val idleRec = uiState.thinkingStatus.let { SolverUiState.ThinkingMode.Idle }
        if (idleRec.IdleMode == (SolverUiState.ThinkingMode.Idle.IdleType.NoSolutionFound)) {
            gameToast(LocalContext.current, "There is no winnable move", displayLonger = true)
            solverViewModel.setIDLEstate()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Instruction_DynamicLogo(uiState)
        ControlButtonsForSolver(
            solverViewModel = solverViewModel,
            findWinnableMoveButtonEnabled = findWinnableMoveButtonEnabled,
            showWinnableMoveToUser = showWinnableMoveToUser,
            uiState = uiState,
            onBallMovementAnimationChange = onBallMovementAnimationChange,
            announceVictory
        )

        DrawSolverBoard(
            modifier = Modifier.fillMaxSize(),
            solverViewModel = solverViewModel,
            uiState = uiState,
            showPreviewBallMovementAnimation = showBallMovementAnimation,
            onBallMovementAnimationChange = onBallMovementAnimationChange,
            announceVictory,
            onEnableVictoryMsg,
            victoryMsgColor
        )
    }
}

/**
 * Perform the key back press action. CHeck if it has permission to do so.
 * BackPress is allow if:
 * - THere is no active thinking
 *
 * @param context  Current context to do a toast on
 * @param navController Navigator controller, which is used to navigate to the previous screen.
 */
fun solverScreenBackPressed(context: Context, navController: NavHostController) {
    if (SolverViewModel.uiState.value.thinkingStatus == SolverUiState.ThinkingMode.Active) {
        gameToast(context, "Unable to go back to home while it is thinking")
    } else {
        navController.popBackStack()
    }
}

/**
 * Provide instruction on how to play Solver Screen.
 *
 * Display the game logo. Game logo also change to animation
 * when it is searching for the solution.
 *
 * @param uiState Current UI state of the solver game
 */
@Composable
private fun Instruction_DynamicLogo(uiState: SolverUiState) {
    val logoSize = 125.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Box {
            if (uiState.thinkingStatus == SolverUiState.ThinkingMode.Idle) {
                val imageModifier = Modifier
                    .size(logoSize)
                    .align(Alignment.Center)
                    .background(MaterialTheme.colorScheme.background)
                Image(
                    painter = painterResource(id = R.drawable.ball),
                    contentDescription = "",
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
                val newPercentageValue =
                    (uiState.thinkingStatus.let { SolverUiState.ThinkingMode.Active }).progressLevel
                val percentComplete =
                    String.format(Locale.getDefault(), "%.1f%%", newPercentageValue)
                Text(
                    "$percentComplete Complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }  // Box
        Column(Modifier.padding(5.dp)) {
            Text(
                text = "Instruction:",
                style = MaterialTheme.typography.titleSmall
            )

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
                    }
                },
                style = MaterialTheme.typography.bodySmall
            )
        } // Column
    } // Row
}

/**
 * Show all the control buttons on top of the screen. These buttons
 * are "find the solution" button and "reset" button
 *
 * @param solverViewModel Solver Game View model
 * @param findWinnableMoveButtonEnabled Determine whether the "Find move" button needs to be
 * enabled or disabled
 * @param showWinnableMoveToUser Determine whether to show the preview of the next winnable
 * move or not
 * @param uiState Current UI state of the solver game
 */
@Composable
private fun ControlButtonsForSolver(
    solverViewModel: SolverViewModel = viewModel(),
    findWinnableMoveButtonEnabled: Boolean,
    showWinnableMoveToUser: Boolean,
    uiState: SolverUiState,
    onBallMovementAnimationChange: (enableBallMovementAnimation: Boolean) -> Unit,
    announceVictory: Boolean
) {
    val view = LocalView.current
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Button(
            onClick = {
                view.let { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
                Log.i(Global.DEBUG_PREFIX, ">>> Starting thinking : Button Pressed")
                if (showWinnableMoveToUser) {

                    //  Turn on the need to do the ball movement animation
                    onBallMovementAnimationChange(true)
                } else {
                    // In this case, we did not move the ball as we did not show hint yet.
                    // We need to find the winning move
                    Log.i(Global.DEBUG_PREFIX, ">>> Looking for next winnable move")
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
                    (uiState.thinkingStatus == SolverUiState.ThinkingMode.Idle))
        ) {
            val iconWidth = Icons.Filled.Refresh.defaultWidth
            Icon(
                imageVector = Icons.Filled.Search, contentDescription = "Find Winning Move",
                modifier = Modifier.size(iconWidth)
            )
            if (showWinnableMoveToUser)
                Text("Move and find next")
            else
                Text("Find next")
        }  // "Find winning move" Button

        Button(
            onClick = {
                view.let { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
                if (uiState.thinkingStatus == SolverUiState.ThinkingMode.Active) {
                    gameToast(context, "Unable to reset as it is currently busy finding a solution")
                } else {
                    // Reset the board game and set it back to idle state
                    solverViewModel.reset()
                }
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
            // Disable button while it is in active thinking mode or in the middle of announce
            // victory message
            enabled = ((uiState.thinkingStatus != SolverUiState.ThinkingMode.Active) ||
                    announceVictory)
        ) {
            val iconWidth = Icons.Filled.Refresh.defaultWidth
            Icon(
                imageVector = Icons.Filled.Refresh, contentDescription = "Reset",
                modifier = Modifier.size(iconWidth)
            )
            Text("Reset")
        }  // Reset Button
    }
}

/**
 *  Draw the Solver Game Board:
 *       Draw the Grid
 *       Place all the balls
 *       Handle all the input (drag, click on grid to place the ball)
 *       Handle all the ball animations (show next move, move the actual ball)
 *
 *  @param modifier Pass in modifier elements that decorate or add behavior to the compose UI
 *  elements
 *  @param solverViewModel Solver game view model
 *  @param uiState Current UI state of the solver game
 *
 *  @see drawGrid
 *  @see drawSolverBalls
 *
 */
@Composable
private fun DrawSolverBoard(
    modifier: Modifier = Modifier,
    solverViewModel: SolverViewModel = viewModel(),
    uiState: SolverUiState,
    showPreviewBallMovementAnimation: Boolean,
    onBallMovementAnimationChange: (enableBallMovementAnimation: Boolean) -> Unit,
    announceVictory: Boolean,
    onEnableVictoryMsg: (Boolean) -> Unit,
    victoryMsgColor: Color
) {
    val context = LocalContext.current

    /**
     * Create textMeasurer instance, which is responsible for measuring a text in its entirety so
     * that it can be drawn on the canvas (drawScope). This is used to draw animated victory message
     */
    val textMeasurer = rememberTextMeasurer()

    if (solverViewModel.ballCount() == 1) {
        if (uiState.thinkingStatus == SolverUiState.ThinkingMode.Idle) {
            val idleRec = uiState.thinkingStatus.let { SolverUiState.ThinkingMode.Idle }
            if (idleRec.IdleMode == (SolverUiState.ThinkingMode.Idle.IdleType.SolutionFound)) {
                onEnableVictoryMsg(true)
            }
        }
    }

    // Launch the animation only once when it enters the composition. It will animate infinitely
    // until it is removed from the composition
    val animateWinningMove = remember { Animatable(initialValue = 0f) }
    AnimatePreviewWinningMoveSetup(animateWinningMove)

    val animateBallMovementChain = mutableListOf<Animatable<Float, AnimationVector1D>>()

    val animateParticleExplosion = remember { Animatable(initialValue = 0f) }
    var particles = mutableListOf<Particle>()

    val animateVictoryMessage = remember { Animatable(initialValue = 0f) }

    if (announceVictory) {

        AnimateVictoryMessageSetup(
            solverViewModel = solverViewModel,
            animateVictoryMessage = animateVictoryMessage,
            onAnimationChange = onEnableVictoryMsg
        )
    }

    if (showPreviewBallMovementAnimation) {

        // Set-up the particles. which is used for the explosion animated effect
        particles = remember {
            generateExplosionParticles(uiState)
        }.toMutableList()

        AnimatePreviewBallMovementsSetup(
            solverViewModel, uiState, animateBallMovementChain,
            animateParticleExplosion, onBallMovementAnimationChange
        )
    } else {  // else need ball movement animation

        // Make sure we do not show any more particle explosion when ball animation is done
        particles.clear()
        LaunchedEffect(Unit) {
            animateParticleExplosion.stop()
            animateParticleExplosion.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(Global.MAX_COL_SIZE.toFloat() / Global.MAX_ROW_SIZE.toFloat())
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        val view = LocalView.current
        val lineColor = MaterialTheme.colorScheme.outline
        var gridSize by rememberSaveable { mutableFloatStateOf(0f) }

        val ballImage = ImageBitmap.imageResource(id = R.drawable.ball)

        var dragXOffset by remember { mutableFloatStateOf(0f) }
        var dragYOffset by remember { mutableFloatStateOf(0f) }
        var dragRow by remember { mutableIntStateOf(-1) }
        var dragCol by remember { mutableIntStateOf(-1) }

        val minSwipeOffset = gridSize

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .drawBehind { }  // Performance optimization. Prevent recomposing other elements outside the canvas
                .padding(15.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            // For unknown reason, we can not use uiState.state

                            view.let { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }

                            if (uiState.thinkingStatus == SolverUiState.ThinkingMode.Active) {
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
                                if ((row < Global.MAX_ROW_SIZE) && (col < Global.MAX_COL_SIZE)) {
                                    solverViewModel.toggleBallPosition(SolverGridPos(row, col))
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
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

                            // Need to consume this event, so that its parent knows not to react to
                            // it anymore. What change.consume() does is it prevent pointerInput
                            // above it to receive events by returning PointerInputChange.positionChange()
                            // Offset.Zero PointerInputChange.isConsumed true.
                            change.consume()
                            dragXOffset += dragAmount.x
                            dragYOffset += dragAmount.y
                        },
                        onDragEnd = {
                            if ((abs(dragXOffset) > minSwipeOffset) ||
                                (abs(dragYOffset) > minSwipeOffset)
                            ) {
                                if (solverViewModel.ballCount() > 1) {
                                    gameToast(context, "Use \"Find next\" button to move the ball")
                                } else {
                                    gameToast(context, "No need to move the last ball")
                                }
                            }
                        } // onDragEnd
                    ) // detectDragGestures
                } // .pointerInput
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val gridSizeWidth = (canvasWidth / (Global.MAX_COL_SIZE))
            val gridSizeHeight = (canvasHeight / (Global.MAX_ROW_SIZE))

            gridSize = if (gridSizeWidth > gridSizeHeight) gridSizeHeight else gridSizeWidth

            drawGrid(this, gridSize, lineColor)

            val ballSize = (gridSize * 1.2).toInt()
            val displayBallImage = Bitmap.createScaledBitmap(
                ballImage.asAndroidBitmap(),
                ballSize, ballSize, false
            ).asImageBitmap()
            displayBallImage.prepareToDraw()   // cache it

            if (showPreviewBallMovementAnimation) {
                val ballsToErase = uiState.winningMovingChain
                drawSolverBalls(this, solverViewModel, gridSize, displayBallImage, ballsToErase)
            } else {
                drawSolverBalls(this, solverViewModel, gridSize, displayBallImage)

                if (solverViewModel.ballCount() > 1) {

                    // If there is a known winnable move, then provide user a review of the next
                    // winnable move by showing the animated ball movement in a shadow (transparent)
                    if (hasFoundWinnableMove(uiState)) {

                        val moveCount = solverViewModel.getWinningMoveCount(uiState)
                        animatePreviewWinningMovePerform(
                            this, gridSize, uiState, animateWinningMove,
                            moveCount, displayBallImage
                        )
                    }
                }
            }

            if (showPreviewBallMovementAnimation) {
                animateBallMovementsPerform(
                    this, gridSize, displayBallImage, animateBallMovementChain,
                    animateParticleExplosion, particles, uiState
                )
            }  // if needToAnimateMovingBall

            if (announceVictory) {
                animateVictoryMsgPerform(
                    this, animateVictoryMessage,
                    textMeasurer, victoryMsgColor
                )
            }
        } // Canvas
    } // Box
}

/**
 * Determine if we have a winnable move
 */
fun hasFoundWinnableMove(uiState: SolverUiState): Boolean {
    return when (uiState.winningDirection) {
        Direction.UP,
        Direction.DOWN,
        Direction.LEFT,
        Direction.RIGHT -> true

        else -> false
    }
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
 * Create all particles. which is used for the explosion animated effect
 *
 * @param uiState The current state of the Solver Game
 *
 * @return List of all the particles
 */
private fun generateExplosionParticles(uiState: SolverUiState): List<Particle> {
    if (uiState.winningMovingChain.isEmpty()) {
        Log.i(Global.DEBUG_PREFIX, "Got empty moving chain list. Unable to process particles")
        return mutableListOf()
    }

    val sizeDp = 200.dp
    val sizePx = sizeDp.toPx()
    val explosionPos = uiState.winningMovingChain[1].pos
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
                direction = uiState.winningDirection,
                maxHorizontalDisplacement = sizePx * randomInRange(-0.3f, 0.3f),
                maxVerticalDisplacement = sizePx * randomInRange(0.05f, 0.07f)
            )
        }

    return (particles)

}

/**
 * Draw all the balls in the provided canvas
 *
 * During ball animation, we need to temporarily erase the animation ball
 */
private fun drawSolverBalls(
    drawScope: DrawScope,
    solverViewModel: SolverViewModel,
    gridSize: Float,
    displayBallImage: ImageBitmap,
    eraseAnimatedBallPositions: List<MovingRec> = listOf()
) {
    // Draw all the balls
    solverViewModel.ballPositionList().forEach { pos ->
        var skipDraw = false
        eraseAnimatedBallPositions.forEach { eraseRec ->
            if ((eraseRec.pos.col == pos.col) && (eraseRec.pos.row == pos.row))
                skipDraw = true
        }

        if (!skipDraw) drawBall(drawScope, gridSize, pos, displayBallImage)
    }
}

/**
 * Draw the ball in a specific position (e,g, row, col) in the board room
 *
 * @param drawScope
 * @param gridSize Size the dimension of the grid
 * @param pos Specific position (row, column)
 * @param displayBallImage Actual image bitmap of the ball
 * @param alpha amount of transparency
 */
private fun drawBall(
    drawScope: DrawScope,
    gridSize: Float,
    pos: SolverGridPos,
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

/**
 * Draw the game board, which is the grid and the circle in the middle of the grid
 *
 * @param drawScope
 * @param gridSize Grid size of each box
 * @param lineColor Color of the line. We are using the theme color
 */
private fun drawGrid(
    drawScope: DrawScope,
    gridSize: Float,
    lineColor: Color
) {
    with(drawScope) {
        // Draw horizontal lines
        var currentY = 0F
        val gridWidth = gridSize * Global.MAX_COL_SIZE
        repeat(Global.MAX_ROW_SIZE + 1) { index ->
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
        val gridHeight = gridSize * Global.MAX_ROW_SIZE
        repeat(Global.MAX_COL_SIZE + 1) {

            drawLine(
                start = Offset(x = currentX, y = 0.dp.toPx()),
                end = Offset(x = currentX, y = gridHeight),
                color = lineColor,
                strokeWidth = 2.dp.toPx() // instead of 5.dp.toPx() , you can also pass 5f
            )
            currentX += gridSize
        }

        // Draw the circle in the center of the grid
        val offsetX = (gridSize * ((Global.MAX_COL_SIZE / 2) + 0.5)).toFloat()
        val offsetY = (gridSize * ((Global.MAX_ROW_SIZE / 2)))
        val radiusLength = (gridSize * 0.66).toFloat()
        drawCircle(
            lineColor,
            radius = radiusLength,
            center = Offset(x = offsetX, y = offsetY),
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

/**
 * Play the search animation to indicate it is actively searching a solution. It uses Lottie animation framework.
 * For more details, see [Lottie](https://lottiefiles.com/tutorials/how-to-create-animation-in-android-studio-kotlin-lottie-files-android-development-full-course-EXR47xeo3cA)
 *
 * @param modifier Pass in modifier elements that decorate or add behavior to the compose UI
 *  elements
 */
@Composable
private fun PlaySearchAnimation(modifier: Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.find_animation))

    LottieAnimation(
        modifier = modifier,
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
}

/**
 * Draw the "You won" message on the canvas.
 *
 * @param drawScope  Scope of the canvas
 * @param canvasWidth Width of the canvas
 * @param canvasHeight Height of the canvas
 * @param textMeasurer Text measure
 * @param animationValue Between 0f and 1f. The set up font size of the message. This is used in
 * animation context to show the effect of growing text
 * @param victoryMsgColor Font color. Depending on the current theme, this color will change
 */
private fun drawVictoryMessage(
    drawScope: DrawScope,
    canvasWidth: Float, canvasHeight: Float,
    textMeasurer: TextMeasurer, animationValue: Float,
    victoryMsgColor: Color
) {
    with(drawScope) {
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

/************************** Animation Routines **************************/

//
// Animating Preview Winning Ball Movement
//

/**
 * Setup ball movement animation and the particle explosion effect
 *
 * @param solverViewModel Solver Game View model
 * @param uiState The current state of the Solver Game
 * @param animateBallMovementChain Object that control animation state of all the ball movement in this chain
 * @param animateParticleExplosion Object that control animation state of particle explosion effect
 * @param onAnimationChange Change the state on whether to perform the animation or not
 */
@Composable
fun AnimatePreviewBallMovementsSetup(
    solverViewModel: SolverViewModel,
    uiState: SolverUiState,
    animateBallMovementChain: MutableList<Animatable<Float, AnimationVector1D>>,
    animateParticleExplosion: Animatable<Float, AnimationVector1D>,
    onAnimationChange: (enableBallMovementAnimation: Boolean) -> Unit
) {
    val movingChain = uiState.winningMovingChain

    movingChain.forEach { _ ->
        val animateBallMovement = remember { Animatable(initialValue = 0f) }
        animateBallMovementChain.add(animateBallMovement)
    }

    // Time ratio of total time when the ball makes contact to the neighboring ball
    val whenBallMakeContactRatio = 0.97f

    // Launch two animations in parallel
    // Only start the explosion when the ball makes an impact to the other ball

    LaunchedEffect(Unit) {
        // Use coroutine to ensure both launch animations get completed in the same scope
        coroutineScope {
            launch {  // one or more ball movements in serial fashion
                movingChain.forEachIndexed { index, currentMovingRec ->
                    if (currentMovingRec.distance > 0) {
                        val totalTimeLength = (currentMovingRec.distance * 100) + 100
                        animateBallMovementChain[index].animateTo(
                            targetValue = 1f,
                            animationSpec = ballMovementKeyframeSpec(
                                totalTimeLength,
                                whenBallMakeContactRatio
                            )
                        )
                    } else {
                        // Distance is zero. Need to set-up the "wiggle" effect
                        animateBallMovementChain[index].animateTo(
                            targetValue = 0f, animationSpec = wiggleBallAnimatedSpec
                        )
                    } // if-else currentMovingRec.distance
                }
                animateBallMovementChain.clear()
                delay(500)

                onAnimationChange(false)
                solverViewModel.makeWinningMove(uiState)
                if (solverViewModel.ballCount() > 1) {
                    Log.i(Global.DEBUG_PREFIX, ">>> Looking for next winnable move")
                    solverViewModel.findWinningMove(solverViewModel)
                }
            }

            launch {  // If there is  multiple ball chain movements, we only animate explosion on the first ball
                val totalTimeLength = (movingChain[0].distance * 100) + 100
                animateParticleExplosion.animateTo(
                    targetValue = 0.5f,
                    animationSpec = particleExplosionAnimatedSpec(
                        totalTimeLength,
                        whenBallMakeContactRatio
                    )
                )
            }
        }
    }
}

//
// Animating Ball movements
//

/**
 * Statically defined animated Keyframe specification to wiggle the ball
 *
 * @see AnimatePreviewBallMovementsSetup
 */
private val wiggleBallAnimatedSpec = keyframes {
    durationMillis = 80
    0f.at(10) using LinearEasing   // from 0 ms to 10 ms
    5f.at(20) using LinearEasing    // from 10 ms to 20 ms
}

/**
 * Define the animation KeyframeSpec for the straight ball movement.
 * Ensure the most of time is spend linear speed with slow start
 * and a bounce effect at the end.
 *
 * The following functions all work together to create end-to-end animation of ball movement and particle explosion:
 *  - [AnimatePreviewBallMovementsSetup]
 *  - [ballMovementKeyframeSpec]
 *  - [wiggleBallAnimatedSpec]
 *  - [particleExplosionAnimatedSpec]
 *  - [animateBallMovementsPerform]
 *
 * @param totalTimeLength Total time to run the straight ball movement
 * @param whenBallMakeContactRatio Time ratio of total time when the ball makes contact to the neighboring ball. It is used
 * in conjunction with [particleExplosionAnimatedSpec] routine
 *
 * @return Animation state specification
 */
private fun ballMovementKeyframeSpec(
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
private fun particleExplosionAnimatedSpec(
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
 * Perform the actual animation of ball movements (including its chain) and
 * the particle explosion effect.
 *
 * @param drawScope
 * @param gridSize Size of grid. This used to do various computation on animation effect
 * @param displayBallImage The actual ball image to show
 * @param animateBallMovementChain Object that control animation state of all the ball movement in this chain
 * @param animateParticleExplosion Object that control animation state of particle explosion effect
 * @param particles List of each particle
 */
fun animateBallMovementsPerform(
    drawScope: DrawScope,
    gridSize: Float,
    displayBallImage: ImageBitmap,
    animateBallMovementChain: MutableList<Animatable<Float, AnimationVector1D>>,
    animateParticleExplosion: Animatable<Float, AnimationVector1D>,
    particles: MutableList<Particle>,
    uiState: SolverUiState
) {
    with(drawScope) {
        val movingDirection = uiState.winningDirection
        val movingChain = uiState.winningMovingChain

        var movingSourcePos: SolverGridPos
        var offset: Pair<Float, Float>
        var xOffset: Float
        var yOffset: Float

        // Ball movements including multiple ball movements (if there is multiple balls in the chain)
        for ((index, currentMovement) in movingChain.withIndex()) {
            movingSourcePos = currentMovement.pos

            offset = setOffsets(movingDirection, movingChain[index].distance, gridSize)
            xOffset = offset.first
            yOffset = offset.second

            // Perform animation by adjusting both the xOffset and yOffset amount respectively
            // to move the ball
            translate(
                (xOffset) * ((animateBallMovementChain[index]).value),
                (yOffset) * ((animateBallMovementChain[index]).value)
            ) {
                drawBall(drawScope, gridSize, movingSourcePos, displayBallImage)
            } // translate
        }  // for

        // Particle explosion effect
        particles.forEach { curParticle ->
            curParticle.updateProgress(animateParticleExplosion.value, gridSize)
            drawCircle(
                alpha = curParticle.alpha,
                color = curParticle.color,
                radius = curParticle.currentRadius,
                center = Offset(curParticle.currentXPosition, curParticle.currentYPosition)
            )
        }
    }   // with (DrawScope)
}

//
//  Animating Victory message
//

/**
 * Setup animated victory message. Define animation spec.
 *
 * @param solverViewModel Solver Game View model
 * @param animateVictoryMessage Object that control animation state of the victory message
 * @param onAnimationChange Change the state on whether to perform the animation or not
 */
@Composable
fun AnimateVictoryMessageSetup(
    solverViewModel: SolverViewModel,
    animateVictoryMessage: Animatable<Float, AnimationVector1D>,
    onAnimationChange: (enableVictoryMessage: Boolean) -> Unit
) {
    LaunchedEffect(Unit) {
        animateVictoryMessage.snapTo(0f)
        animateVictoryMessage.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = LinearOutSlowInEasing
            )
        )
        delay(1000)
        onAnimationChange(false)
        solverViewModel.setIDLEstate()
        animateVictoryMessage.snapTo(0f)
    }
}

/**
 * Perform the actual victory message animation
 *
 * @param drawScope Canvas to draw the animation on
 * @param animateVictoryMsg Object that control animation state of the victory message
 * @param textMeasurer Responsible for measuring a text in its entirety so that it can be drawn
 * on the canvas (drawScope).
 * @param victoryMsgColor Color of the message. Color will differ depending on the current theme
 */
fun animateVictoryMsgPerform(
    drawScope: DrawScope,
    animateVictoryMsg: Animatable<Float, AnimationVector1D>,
    textMeasurer: TextMeasurer,
    victoryMsgColor: Color
) {
    with(drawScope) {
        drawVictoryMessage(
            this,
            size.width, size.height,
            textMeasurer, animateVictoryMsg.value,
            victoryMsgColor
        )
    }
}

//
//  Animating Winning Move Preview
//

/**
 * Show Winning Move. Animate the shadowed ball movement. This is a set-up
 *
 * @param animateWinningMove Object that control animation state of shadowed movement, which loop
 * forever.
 */
@Composable
private fun AnimatePreviewWinningMoveSetup(animateWinningMove: Animatable<Float, AnimationVector1D>) {
    LaunchedEffect(Unit) {
        animateWinningMove.snapTo(0f)
        animateWinningMove.animateTo(
            targetValue = 1f, animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }
}

/**
 * Show Winning Move. Animate the shadowed ball movement. Perform the actual animation
 *
 * @param drawScope Scope to do the drawing on
 * @param gridSize width or height of the grid in dp unit
 * @param uiState The current state of the Solver Game
 * @param animate Object that control animation state of shadow ball movement. It loops endlessly
 * @param gWinningMoveCount number of ball movement for the upcoming winning move
 * @param displayBallImage The image of the ball
 */
private fun animatePreviewWinningMovePerform(
    drawScope: DrawScope,
    gridSize: Float,
    uiState: SolverUiState,
    animate: Animatable<Float, AnimationVector1D>,
    gWinningMoveCount: Int,
    displayBallImage: ImageBitmap
) {
    with(drawScope) {
        var xOffset = 0
        var yOffset = 0

        when (uiState.winningDirection) {
            Direction.UP -> {
                yOffset = -1 * gridSize.toInt() * gWinningMoveCount
            }

            Direction.DOWN -> {
                yOffset = 1 * gridSize.toInt() * gWinningMoveCount
            }

            Direction.LEFT -> {
                xOffset = -1 * gridSize.toInt() * gWinningMoveCount
            }

            Direction.RIGHT -> {
                xOffset = 1 * gridSize.toInt() * gWinningMoveCount
            }

            else -> {
                assert(true) { "Got unexpected Direction value: ${uiState.winningDirection}" }
            }
        }

        translate(
            (xOffset) * animate.value,
            (yOffset) * animate.value
        ) {
            drawBall(
                drawScope,
                gridSize,
                uiState.winningMovingChain[0].pos,
                displayBallImage,
                alpha = 0.4F
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun SolverScreenPreview() {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawVictoryMessage(
            drawScope = this,
            canvasWidth = size.width, canvasHeight = size.height,
            textMeasurer, 1.0f, Color.Cyan
        )
    }
}