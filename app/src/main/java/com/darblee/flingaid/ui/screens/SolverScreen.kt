package com.darblee.flingaid.ui.screens

import android.graphics.Bitmap
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.darblee.flingaid.BackPressHandler
import com.darblee.flingaid.Direction
import com.darblee.flingaid.Global
import com.darblee.flingaid.R
import com.darblee.flingaid.ui.Particle
import com.darblee.flingaid.ui.SolverUIState
import com.darblee.flingaid.ui.SolverViewModel
import com.darblee.flingaid.utilities.AnimateBallMovementsSetup
import com.darblee.flingaid.utilities.AnimateShadowBallMovementSetup
import com.darblee.flingaid.utilities.AnimateVictoryMessageSetup
import com.darblee.flingaid.utilities.NoWinnableMoveDialog
import com.darblee.flingaid.utilities.Pos
import com.darblee.flingaid.utilities.animateBallMovementsPerform
import com.darblee.flingaid.utilities.animateShadowBallMovementsPerform
import com.darblee.flingaid.utilities.animateVictoryMsgPerform
import com.darblee.flingaid.utilities.gameToast
import com.darblee.flingaid.utilities.generateExplosionParticles
import java.io.File
import java.util.Locale
import kotlin.math.abs

/**
 *  **The main Solver Game Screen**
 *
 *  @param modifier Pass in modifier elements that decorate or add behavior to the compose UI
 *  elements
 *  @param navController Central coordinator for managing navigation between destination screens,
 *  back stack, and more
 */
@Composable
fun SolverScreen(modifier: Modifier = Modifier, navController: NavHostController) {

    LoadSolverFileOnlyOnce()

    var announceVictory = false
    var readyToFindSolution = false

    var showNoWinnableMoveDialogBox by remember { mutableStateOf(false) }

    // "null" value means these properties is in "false" (or "off") state
    var curThinkingLvl: Float? = null
    var moveBallRec: SolverUIState.SolverMode.MoveBall? = null
    var readyToMoveRec: SolverUIState.SolverMode.HasWinningMoveWaitingToMove? = null

    val solverUIState by SolverViewModel.uiState.collectAsStateWithLifecycle()

    when (solverUIState.mode) {
        SolverUIState.SolverMode.Thinking -> {
            Log.i("Solver Recompose:", "${solverUIState.mode} : Show Thinking Progress")

            val thinkingRec: SolverUIState.SolverMode.Thinking =
                solverUIState.mode.let { SolverUIState.SolverMode.Thinking }
            curThinkingLvl = thinkingRec.progress
        }

        SolverUIState.SolverMode.ReadyToFindSolution -> {
            Log.i("Solver Recompose:", "${solverUIState.mode} : Enable \"Find Solution\" button")

            readyToFindSolution = true
        }

        SolverUIState.SolverMode.HasWinningMoveWaitingToMove -> {
            Log.i("Solver Recompose:", "${solverUIState.mode} : Enable \"Move Ball\" button")

            readyToMoveRec =
                solverUIState.mode.let { SolverUIState.SolverMode.HasWinningMoveWaitingToMove }
        }

        SolverUIState.SolverMode.AnnounceNoPossibleSolution -> {
            Log.i("Solver Recompose:", "${solverUIState.mode} : Send message no winnable move")
            showNoWinnableMoveDialogBox = true

        }

        SolverUIState.SolverMode.AnnounceVictory -> {
            Log.i("Solver Recompose:", "${solverUIState.mode} : Show Victory message")

            announceVictory = true
        }

        SolverUIState.SolverMode.NoMoveAvailable -> {
            Log.i(
                "Solver Recompose:",
                "${solverUIState.mode} : Do nothing. Need to disable move/find button"
            )
        }

        SolverUIState.SolverMode.MoveBall -> {
            Log.i("Solver Recompose:", "${solverUIState.mode} : Move the ball")

            moveBallRec = solverUIState.mode.let { SolverUIState.SolverMode.MoveBall }
        }
    }

    // Need special handling of back key press events. Do not navigate when:
    // - It is in middle of thinking
    // - It is middle of announce victory message
    HandleBackPressKeyForSolverScreen(solverUIState.mode, navController, announceVictory)

    if (showNoWinnableMoveDialogBox) {
        NoWinnableMoveDialog(
            onDismissRequest = { showNoWinnableMoveDialogBox = false },
            onConfirmation = { showNoWinnableMoveDialogBox = false }
        )
        SolverViewModel.setModeToNoMoveAvailable()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SolverInstructionLogo(curThinkingLvl)
        SolverActionButtons(
            readyToFindSolution = readyToFindSolution,
            currentlyThinking = (curThinkingLvl != null),
            readyToMoveRec = readyToMoveRec
        )

        DrawSolverBoard(
            modifier = Modifier.fillMaxSize(),
            announceVictory = announceVictory,
            moveBallInfo = moveBallRec,
            currentlyThinking = (curThinkingLvl != null),
            readyToMoveRec = readyToMoveRec
        )
    }
}

/**
 * Populate the solver board by loading content from the solver game file
 */
@Composable
private fun LoadSolverFileOnlyOnce() {
    /**
     * Need to ensure we only load the file once when we start the the solver game screen.
     * If we load the file everytime we go down this path during recompose, it will trigger more
     * recompose. This will cause unnecessary performance overload.
     *
     * __Developer's Note:__  Use [rememberSaveable] instead of [remember] because we want to
     * preserve this even after config change (e.g. screen rotation)
     */
    var needToLoadSolverFile by rememberSaveable { mutableStateOf(true) }

    // Load the game file only once. This is done primarily for performance reason.
    // Loading game file will trigger non-stop recomposition.
    // Also need to minimize the need to do expensive time consuming file i/o operation.
    val solverBoardFile = File(LocalContext.current.filesDir, Global.SOLVER_BOARD_FILENAME)
    if (needToLoadSolverFile) {
        SolverViewModel.loadGameFile(solverBoardFile)
        Log.i(Global.DEBUG_PREFIX, "Loading from solver file")
        needToLoadSolverFile = false
    }
}

/**
 *  Handle Back Press Key. Intercept backPress key while on Game Solver screen.
 *
 *  When doing back press on the current screen, confirm with the user whether it should exit
 *  this screen or not if it is middle of thinking. Do not exit this screen when:
 *  - It is middle of thinking
 *  - It is in middle of announcing victory message
 *
 *  @param mode Current Solver mode
 *  @param navController Use to navigate to the previous screen
 *  @param announceVictory Determine whether we are in middle of announcing victory message
 */
@Composable
private fun HandleBackPressKeyForSolverScreen(
    mode: SolverUIState.SolverMode,
    navController: NavHostController,
    announceVictory: Boolean,
) {
    var backPressed by remember { mutableStateOf(false) }
    BackPressHandler(onBackPressed = { backPressed = true })
    if (backPressed) {
        backPressed = false

        if (announceVictory) return

        if (mode == SolverUIState.SolverMode.Thinking) {
            SolverViewModel.stopThinking()
        }

        navController.popBackStack()
    }
}

/**
 * Provide instruction on how to play Solver Screen.
 *
 * Display the game logo. Game logo also change to animation
 * when it is searching for the solution.
 *
 * @param curThinkingLvl Thinking progress level. Value of null means it is not thinking.
 */
@Composable
private fun SolverInstructionLogo(
    curThinkingLvl: Float?
) {
    val logoSize = 125.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        val notThinking = (curThinkingLvl == null)

        Box {
            if (notThinking) {
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
                    String.format(Locale.getDefault(), "%.1f%%", curThinkingLvl)
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
                stringResource(R.string.clear_the_board_with_reset_button),
                stringResource(R.string.add_new_balls_on_the_grid),
                stringResource(R.string.solve_the_game_with_find_next_button)
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
 * @param readyToFindSolution Determine whether it is ready to start looking for a solution or not
 * @param currentlyThinking Determine whether it is currently in thinking mode or not
 * and winning moving chain
 */
@Composable
private fun SolverActionButtons(
    readyToFindSolution: Boolean,
    currentlyThinking: Boolean,
    readyToMoveRec: SolverUIState.SolverMode.HasWinningMoveWaitingToMove?,
) {
    val view = LocalView.current

    val readyToMove = (readyToMoveRec != null)

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
                if (readyToMove) {

                    SolverViewModel.setModeToShowBallMovement(
                        readyToMoveRec!!.winningDirectionPreview,
                        readyToMoveRec.winingMovingChainPreview
                    )
                }

                if (readyToFindSolution) {
                    SolverViewModel.findWinningMove()
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
            enabled = (readyToMove || readyToFindSolution)
        ) {
            val iconWidth = Icons.Filled.Refresh.defaultWidth
            Icon(
                imageVector = Icons.Filled.Search, contentDescription = "Find Winning Move",
                modifier = Modifier.size(iconWidth)
            )

            if (readyToMove) {
                Text("Move and Find next")
            } else {
                Text("Find next")
            }
        }  // "Find winning move" Button

        Button(
            onClick = {
                view.let { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }

                if (currentlyThinking) SolverViewModel.stopThinking()

                // Reset the board game and set it back to idle state
                SolverViewModel.reset()
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
 * @param modifier Pass in modifier elements that decorate or add behavior to the compose UI
 *  elements
 * @param announceVictory Indicate whether it need to show animated victory message or not
 * @param moveBallInfo Info to process moving the ball. A null means there is no need to move ball
 * @param currentlyThinking Indicate whether it is currently thinking or not
 */
@Composable
private fun DrawSolverBoard(
    modifier: Modifier = Modifier,
    announceVictory: Boolean,
    moveBallInfo: SolverUIState.SolverMode.MoveBall?,
    currentlyThinking: Boolean,
    readyToMoveRec: SolverUIState.SolverMode.HasWinningMoveWaitingToMove?
) {

    val showBallMovementAnimation = (moveBallInfo != null)
    val showPreviewMovementAnimation = (readyToMoveRec != null)

    val context = LocalContext.current

    val victoryMsgColor = MaterialTheme.colorScheme.onPrimaryContainer

    /**
     * Create textMeasurer instance, which is responsible for measuring a text in its entirety so
     * that it can be drawn on the canvas (drawScope). This is used to draw animated victory message
     */
    val textMeasurer = rememberTextMeasurer()

    /**
     * Animation control that handle showing a preview (shadow) of winning move
     */
    val animatePreviewWinningMove = remember { Animatable(initialValue = 0f) }
    if (showPreviewMovementAnimation) {
        AnimateShadowBallMovementSetup(animatePreviewWinningMove)
    }

    val animateBallMovementChain = mutableListOf<Animatable<Float, AnimationVector1D>>()

    val animateParticleExplosion = remember { Animatable(initialValue = 0f) }
    var particles = mutableListOf<Particle>()

    val animateVictoryMessage = remember { Animatable(initialValue = 0f) }
    if (announceVictory) {
        AnimateVictoryMessageSetup(
            { SolverViewModel.setModeToNoMoveAvailable() },
            animateCtl = animateVictoryMessage,
        )
    } else {
        LaunchedEffect(true) {
            animateVictoryMessage.stop()
            animateVictoryMessage.snapTo(0f)
        }
    }

    // Ball movement must have at least 2 balls in the movement chain
    if (showBallMovementAnimation) {

        // Set-up the particles, which is used for the explosion animated effect
        particles = remember {
            generateExplosionParticles(
                moveBallInfo!!.winingMovingChainMoveBall,
                moveBallInfo.winningDirMoveBall
            )
        }.toMutableList()


        /**
         * The following lambda functions are used in [AnimateBallMovementsSetup] routine
         */
        val solverMoveBallTask =
            { pos: Pos, direction: Direction -> SolverViewModel.moveBallToWin(pos, direction) }

        AnimateBallMovementsSetup(
            movingChain = moveBallInfo!!.winingMovingChainMoveBall,
            direction = moveBallInfo.winningDirMoveBall,
            animateBallMovementCtlList = animateBallMovementChain,
            animateParticleExplosionCtl = animateParticleExplosion,
            moveBallTask = solverMoveBallTask
        )

    } else {
        particles.clear()
        LaunchedEffect(true) {
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
                            val row = (tapOffset.y / gridSize).toInt()
                            val col = (tapOffset.x / gridSize).toInt()
                            if ((row >= Global.MAX_ROW_SIZE) || (col >= Global.MAX_COL_SIZE)) return@detectTapGestures

                            view.let { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }

                            if (currentlyThinking) SolverViewModel.stopThinking()

                            SolverViewModel.toggleBallPosition(Pos(row, col))
                            view.playSoundEffect(SoundEffectConstants.CLICK)

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
                                if (SolverViewModel.ballCount() > 1) {
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
                val ballsToErase = moveBallInfo!!.winingMovingChainMoveBall
                SolverViewModel.drawSolverBallsOnGrid(
                    drawScope,
                    gridSize,
                    displayBallImage,
                    ballsToErase
                )
            } else {
                // No need to animate ball movement, but now need to check if we need to show
                // preview of next winning ball movement
                SolverViewModel.drawSolverBallsOnGrid(drawScope, gridSize, displayBallImage)

                if (showPreviewMovementAnimation) {

                    if (readyToMoveRec!!.winingMovingChainPreview.isNotEmpty()) {

                        val moveCount = SolverViewModel.getWinningMoveCount(
                            pos = readyToMoveRec.winingMovingChainPreview[0].pos,
                            direction = readyToMoveRec.winningDirectionPreview,
                        )
                        animateShadowBallMovementsPerform(
                            drawScope = drawScope,
                            gridSize = gridSize,
                            animateCtl = animatePreviewWinningMove,
                            displayBallImage = displayBallImage,
                            distance = moveCount,
                            direction = readyToMoveRec.winningDirectionPreview,
                            pos = readyToMoveRec.winingMovingChainPreview[0].pos
                        )
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
                    direction = moveBallInfo!!.winningDirMoveBall,
                    movingChain = moveBallInfo.winingMovingChainMoveBall
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

@Composable
@Preview(showBackground = true)
private fun SolverScreenPreview() {
}