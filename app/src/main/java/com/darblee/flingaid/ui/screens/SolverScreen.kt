package com.darblee.flingaid.ui.screens

import android.graphics.Bitmap
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.darblee.flingaid.utilities.AnimateVictoryMessageSetup
import com.darblee.flingaid.utilities.Pos
import com.darblee.flingaid.utilities.animateBallMovementsPerform
import com.darblee.flingaid.utilities.animateShadowBallMovementsPerform
import com.darblee.flingaid.utilities.animateVictoryMsgPerform
import com.darblee.flingaid.utilities.ballMovementKeyframeSpec
import com.darblee.flingaid.utilities.gameToast
import com.darblee.flingaid.utilities.generateExplosionParticles
import com.darblee.flingaid.utilities.particleExplosionAnimatedSpec
import com.darblee.flingaid.utilities.wiggleBallAnimatedSpec
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.abs

/**
 *  **The main Solver Game Screen**
 *
 *  @param modifier Pass in modifier elements that decorate or add behavior to the compose UI
 *  elements
 *  @param navController Central coordinator for managing navigation between destination screens,
 *  managing the back stack, and more
 */
@Composable
fun SolverScreen(modifier: Modifier = Modifier, navController: NavHostController) {

    Log.i(Global.DEBUG_PREFIX, "Solver Screen - recompose")

    var announceVictory by remember { mutableStateOf(false) }

    /**
     * Need to ensure we only load the file once when we start the the solver game screen.
     * If we load the file everytime we go down this path during recompose, it will trigger more
     * recompose. This will cause unnecessary performance overload.
     *
     * __Developer's Note:__  Use [rememberSaveable] instead of [remember] because we want to
     * preserve this even after config change (e.g. screen rotation)
     */
    var needToLoadSolverGameFile by rememberSaveable { mutableStateOf(true) }

    val solverViewModel: SolverViewModel = viewModel()
    val solverUIState by solverViewModel.uiState.collectAsStateWithLifecycle()

    /**
     * Intercept backPress key while on Game Solver screen.
     * When doing back press on the current screen, confirm with the user whether it should exit
     * this screen or not if it is middle of thinking. Do not exit this screen when:
     * - It is middle of thinking
     * - It is in middle of announcing victory message
     */
    var backPressed by remember { mutableStateOf(false) }
    BackPressHandler(onBackPressed = { backPressed = true })
    if (backPressed) {
        backPressed = false
        if (announceVictory) return
        if (solverUIState.mode == SolverUiState.SolverMode.Thinking) return

        navController.popBackStack()
        return
    }



    val solverBoardFile = File(LocalContext.current.filesDir, Global.SOLVER_BOARD_FILENAME)

    val onEnableVictoryMsg = { setting: Boolean -> announceVictory = setting }

    // Load the game file only once. This is done primarily for performance reason.
    // Loading game file will trigger non-stop recomposition.
    // Also need to minimize the need to do expensive time consuming file i/o operation.
    if (needToLoadSolverGameFile) {
        solverViewModel.loadGameFile(solverBoardFile)
        needToLoadSolverGameFile = false
    }

    var findWinnableMoveButtonEnabled by remember { mutableStateOf(false) }
    findWinnableMoveButtonEnabled =
        ((solverViewModel.ballCount() > 1) && (!hasFoundWinnableMove(solverUIState)))

    var showWinnableMoveToUser by remember { mutableStateOf(false) }
    showWinnableMoveToUser =
        (solverUIState.winningDirection != Direction.NO_WINNING_DIRECTION)

    /**
     * Keep track of when to do the ball movement animation
     */
    var showBallMovementAnimation by remember { mutableStateOf(false) }
    val onBallMovementAnimationChange =
        { enableBallMovements: Boolean -> showBallMovementAnimation = enableBallMovements }

    if (solverUIState.mode == SolverUiState.SolverMode.IdleNoSolution) {
            gameToast(LocalContext.current, "There is no winnable move", displayLonger = false)
            solverViewModel.solverSetIDLE()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Instruction_DynamicLogo(solverUIState)
        ControlButtonsForSolver(
            solverViewModel = solverViewModel,
            findWinnableMoveButtonEnabled = findWinnableMoveButtonEnabled,
            showWinnableMoveToUser = showWinnableMoveToUser,
            uiState = solverUIState,
            onBallMovementAnimationChange = onBallMovementAnimationChange,
            announceVictory
        )

        DrawSolverBoard(
            modifier = Modifier.fillMaxSize(),
            solverViewModel = solverViewModel,
            solverUIState = solverUIState,
            showBallMovementAnimation = showBallMovementAnimation,
            onBallMovementAnimationEnablement = onBallMovementAnimationChange,
            announceVictory,
            onEnableVictoryMsg
        )
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
            if (uiState.mode != SolverUiState.SolverMode.Thinking) {
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
                val percentComplete =
                    String.format(Locale.getDefault(), "%.1f%%", uiState.thinkingProgressLevel)
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
 * @param onBallMovementAnimationChange Determine whether the ball movement animation is done or not
 * @param announceVictory Indicate whether we need to announce victory message or not
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
                    solverViewModel.findWinningMove()
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
                    (uiState.mode != SolverUiState.SolverMode.Thinking))
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
                if (uiState.mode == SolverUiState.SolverMode.Thinking) {
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
            enabled = ((uiState.mode != SolverUiState.SolverMode.Thinking) || announceVictory)
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
 *  - Draw the Grid
 *  - Place all the balls
 *  - Handle all the input (drag, click on grid to place the ball)
 *  - Handle all the ball animations and dynamic animated messages
 *
 *  @param modifier Pass in modifier elements that decorate or add behavior to the compose UI
 *  elements
 *  @param solverViewModel Solver Game view model
 *  @param solverUIState Current UI state of the solver game
 *  @param showBallMovementAnimation Indicate whether it need to do ball movement animation
 *  @param onBallMovementAnimationEnablement Enable/disable ball movement animation
 *  @param announceVictory Indicate whether it need to show animated victory message or not
 *  @param onEnableVictoryMsg Indicate whether it need to show animated victory message or not
 */
@Composable
private fun DrawSolverBoard(
    modifier: Modifier = Modifier,
    solverViewModel: SolverViewModel = viewModel(),
    solverUIState: SolverUiState,
    showBallMovementAnimation: Boolean,
    onBallMovementAnimationEnablement: (enableBallMovementAnimation: Boolean) -> Unit,
    announceVictory: Boolean,
    onEnableVictoryMsg: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val victoryMsgColor = MaterialTheme.colorScheme.onPrimaryContainer


    /**
     * Create textMeasurer instance, which is responsible for measuring a text in its entirety so
     * that it can be drawn on the canvas (drawScope). This is used to draw animated victory message
     */
    val textMeasurer = rememberTextMeasurer()

    if (solverViewModel.ballCount() == 1) {
        if (solverUIState.mode == SolverUiState.SolverMode.IdleFoundSolution) {
            onEnableVictoryMsg(true)
        }
    }

    /**
     * Animation control that handle showing a preview (shadow) of winning move
     */
    val animatePreviewWinningMove = remember { Animatable(initialValue = 0f) }
    SolverAnimateShadowBallMovementSetup(animatePreviewWinningMove)

    val animateBallMovementChain = mutableListOf<Animatable<Float, AnimationVector1D>>()

    val animateParticleExplosion = remember { Animatable(initialValue = 0f) }
    var particles = mutableListOf<Particle>()

    val animateVictoryMessage = remember { Animatable(initialValue = 0f) }
    if (announceVictory) {
        AnimateVictoryMessageSetup(
            { solverViewModel.solverSetIDLE() },
            animateCtl = animateVictoryMessage,
            onAnimationChange = onEnableVictoryMsg
        )
    }

    // Ball movement must have at least 2 balls in the movement chain
    if ((showBallMovementAnimation) && (solverUIState.winningMovingChain.size > 1)) {

        // Set-up the particles, which is used for the explosion animated effect
        particles = remember {
            generateExplosionParticles(solverUIState.winningMovingChain, solverUIState.winningDirection)
        }.toMutableList()

        SolverAnimateBallMovementsSetup(
            solverViewModel = solverViewModel,
            movingChain = solverUIState.winningMovingChain,
            direction = solverUIState.winningDirection,
            animateBallMovementChainCtlList = animateBallMovementChain,
            animateParticleExplosionCtl = animateParticleExplosion,
            onAnimationChange = onBallMovementAnimationEnablement
        )
    } else {
        // Else we longer need ball movement animation. So clear animation set-up
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

                            if (solverUIState.mode == SolverUiState.SolverMode.Thinking) {
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
                                    solverViewModel.toggleBallPosition(Pos(row, col))
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
            val drawScope = this
            val canvasWidth = size.width
            val canvasHeight = size.height

            val gridSizeWidth = (canvasWidth / (Global.MAX_COL_SIZE))
            val gridSizeHeight = (canvasHeight / (Global.MAX_ROW_SIZE))

            gridSize = if (gridSizeWidth > gridSizeHeight) gridSizeHeight else gridSizeWidth

            drawGrid(drawScope, gridSize, lineColor)

            val ballSize = (gridSize * 1.2).toInt()
            val displayBallImage = Bitmap.createScaledBitmap(
                ballImage.asAndroidBitmap(),
                ballSize, ballSize, false
            ).asImageBitmap()
            displayBallImage.prepareToDraw()   // cache it

            if (showBallMovementAnimation) {
                // The animation routine already show the ball in its starting position. We need
                // to erase it from normal draw ball
                val ballsToErase = solverUIState.winningMovingChain
                solverViewModel.drawSolverBallsOnGrid(drawScope, gridSize, displayBallImage, ballsToErase)
            } else {
                // No need to animate ball movement, but now need to check if we need to show
                // preview of next winning ball movement
                solverViewModel.drawSolverBallsOnGrid(drawScope, gridSize, displayBallImage)
                if (solverViewModel.ballCount() > 1) {
                    // If there is a known winnable move, then provide user a review of the next
                    // winnable move by showing the animated ball movement in a shadow (transparent)
                    if (hasFoundWinnableMove(solverUIState)) {
                        if (solverUIState.winningMovingChain.isNotEmpty()) {
                            val moveCount = solverViewModel.getWinningMoveCount(
                                pos = solverUIState.winningMovingChain[0].pos,
                                direction = solverUIState.winningDirection)
                            animateShadowBallMovementsPerform(
                                drawScope = drawScope,
                                gridSize = gridSize,
                                animateCtl = animatePreviewWinningMove,
                                displayBallImage = displayBallImage,
                                distance = moveCount,
                                direction = solverUIState.winningDirection,
                                pos = solverUIState.winningMovingChain[0].pos
                            )
                        }
                    }
                }
            }

            if (showBallMovementAnimation) {
                animateBallMovementsPerform(
                    drawScope = drawScope,
                    gridSize = gridSize,
                    displayBallImage = displayBallImage,
                    animateBallMovementChainCtlList = animateBallMovementChain,
                    animateParticleExplosionCtl = animateParticleExplosion,
                    particles = particles,
                    direction = solverUIState.winningDirection,
                    movingChain = solverUIState.winningMovingChain
                )
            }

            if (announceVictory) {
                animateVictoryMsgPerform(
                    drawScope, animateVictoryMessage,
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



/************************** Animation Routines **************************/

/**
 * Setup ball movement animation and the particle explosion effect
 * - After movement, it will do the actual move
 * - THen start to look for the next move unless this is the last ball
 *
 * @param solverViewModel Solver Game View model
 * @param movingChain List of ball movements in the chain
 * @param direction Direction of ball movement
 * @param animateBallMovementChainCtlList Animate Object that control animation state of all the ball movement in this chain
 * @param animateParticleExplosionCtl Animate Object that control animation state of particle explosion effect
 * @param onAnimationChange Change the state on whether to perform the animation or not
 */
@Composable
private fun SolverAnimateBallMovementsSetup(
    solverViewModel: SolverViewModel,
    movingChain: List<MovingRec>,
    direction: Direction,
    animateBallMovementChainCtlList: MutableList<Animatable<Float, AnimationVector1D>>,
    animateParticleExplosionCtl: Animatable<Float, AnimationVector1D>,
    onAnimationChange: (enableBallMovementAnimation: Boolean) -> Unit
) {
    movingChain.forEach { _ ->
        val animateBallMovement = remember { Animatable(initialValue = 0f) }
        animateBallMovementChainCtlList.add(animateBallMovement)
    }

    // Time ratio of total time when the ball makes contact to the neighboring ball
    val whenBallMakeContactRatio = 0.97f

    // Launch two animations in parallel
    // Only start the explosion when the ball makes an impact to the other ball

    LaunchedEffect(Unit) {
        // Use coroutine to ensure both launch animations get completed in the same  co-routine scope
        coroutineScope {
            launch {  // one or more ball movements in serial fashion
                movingChain.forEachIndexed { index, currentMovingRec ->
                    if (currentMovingRec.distance > 0) {
                        val totalTimeLength = (currentMovingRec.distance * 100) + 100
                        animateBallMovementChainCtlList[index].animateTo(
                            targetValue = 1f,
                            animationSpec = ballMovementKeyframeSpec(
                                totalTimeLength,
                                whenBallMakeContactRatio
                            )
                        )
                    } else {
                        // Distance is zero. Need to set-up the "wiggle" effect
                        animateBallMovementChainCtlList[index].animateTo(
                            targetValue = 0f, animationSpec = wiggleBallAnimatedSpec
                        )
                    } // if-else currentMovingRec.distance
                } // movingChain forEach
                animateBallMovementChainCtlList.clear()

                onAnimationChange(false)

                if (movingChain.isNotEmpty()) {
                    solverViewModel.moveBallToWin(movingChain[0].pos, direction)

                    if (solverViewModel.ballCount() > 1) {
                        Log.i(Global.DEBUG_PREFIX, ">>> Looking for next winnable move")
                        solverViewModel.findWinningMove()
                    }
                }
            } // Launch

            launch {  // Animate explosion on the first ball only
                val totalTimeLength = (movingChain[0].distance * 100) + 100
                animateParticleExplosionCtl.animateTo(
                    targetValue = 0.5f,
                    animationSpec = particleExplosionAnimatedSpec(
                        totalTimeLength,
                        whenBallMakeContactRatio
                    )
                )
            } // launch
        } // coroutine
    }
}

//
//  Animating Winning Move Preview
//

/**
 * Show Winning Move. Animate the shadowed ball movement. This is a set-up
 *
 * @param animateCtl Animate Object that control animation state of shadowed movement, which loop
 * forever.
 */
@Composable
private fun SolverAnimateShadowBallMovementSetup(animateCtl: Animatable<Float, AnimationVector1D>) {
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

@Composable
@Preview(showBackground = true)
private fun SolverScreenPreview() {
}