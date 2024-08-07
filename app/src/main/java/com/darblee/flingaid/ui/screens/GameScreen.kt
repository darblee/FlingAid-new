package com.darblee.flingaid.ui.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.darblee.flingaid.BackPressHandler
import com.darblee.flingaid.Direction
import com.darblee.flingaid.Global
import com.darblee.flingaid.R
import com.darblee.flingaid.gAudio_doink
import com.darblee.flingaid.ui.GameUIState
import com.darblee.flingaid.ui.GameViewModel
import com.darblee.flingaid.ui.Particle
import com.darblee.flingaid.utilities.AnimateBallMovementsSetup
import com.darblee.flingaid.utilities.AnimateVictoryMessageSetup
import com.darblee.flingaid.utilities.Pos
import com.darblee.flingaid.utilities.animateShadowBallMovementsPerform
import com.darblee.flingaid.utilities.animateBallMovementsPerform
import com.darblee.flingaid.utilities.animateVictoryMsgPerform
import com.darblee.flingaid.utilities.generateExplosionParticles
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

/**
 *  **The main Game Screen**
 *
 *  @param modifier Pass in modifier elements that decorate or add behavior to the compose UI
 *  elements
 *  @param navController Central coordinator for managing navigation between destination screens,
 *  managing the back stack, and more
 */
@Composable
fun GameScreen(modifier: Modifier = Modifier, navController: NavHostController) {

    val gameViewModel: GameViewModel = viewModel()
    var moveBallRec : GameUIState.GameMode.MoveBall? = null // null means it is NOT moving the ball

    // null means it is NOT IndicateInvalidMoveByShowingShadowMove mode.
    var shadowBallRec : GameUIState.GameMode.IndicateInvalidMoveByShowingShadowMove? = null

    LoadGameFileOnlyOnce(gameViewModel)

    var announceVictory = false


    val gameUIState by gameViewModel.gameUIState.collectAsStateWithLifecycle()

    when (gameUIState.mode) {
        GameUIState.GameMode.WonGame -> {
            Log.i("Game Recompose: ", "${gameUIState.mode} : Announce Victory")
            announceVictory = true
        }

        GameUIState.GameMode.WaitingOnUser -> {
            Log.i("Game Recompose: ", "${gameUIState.mode} : Do nothing")
        }

        GameUIState.GameMode.NoAvailableMove -> {
            Log.i("Game Recompose: ", "${gameUIState.mode} : Do nothing")
        }

        GameUIState.GameMode.MoveBall -> {
            Log.i("Game Recompose: ", "${gameUIState.mode} : Process moving ball")
            moveBallRec = gameUIState.mode.let { GameUIState.GameMode.MoveBall }
        }

        GameUIState.GameMode.IndicateInvalidMoveByShowingShadowMove -> {
            Log.i("Game Recompose: ", "${gameUIState.mode} : Do shadow ball move plus error sound")
            shadowBallRec = gameUIState.mode.let { GameUIState.GameMode.IndicateInvalidMoveByShowingShadowMove }
        }

        GameUIState.GameMode.LookingForHint -> {
            Log.i("Game Recompose: ", "${gameUIState.mode} : Do nothing")
        }
    }

    HandleBackPressKeyForGameScreen(gameUIState.mode, navController, announceVictory)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InstructionLogo()
        GameControlButtonsForGame(gameViewModel)

        DrawGameBoard(
            modifier = Modifier.fillMaxSize(),
            gameViewModel = gameViewModel,
            announceVictory = announceVictory,
            moveBallRec = moveBallRec,
            shadowBallRec = shadowBallRec
        )
    }
}

/**
 * Populate the solver board by loading content from the solver game file
 *
 * @param gameViewModel View Model that manage business logic for Game Screen. For more
 * details, see [GameViewModel]
 */
@Composable
private fun LoadGameFileOnlyOnce(gameViewModel: GameViewModel) {
    /**
     * Need to ensure we only load the file once when we start the the solver game screen.
     * If we load the file everytime we go down this path during recompose, it will trigger more
     * recompose. This will cause unnecessary performance overload.
     *
     * __Developer's Note:__  Use [rememberSaveable] instead of [remember] because we want to
     * preserve this even after config change (e.g. screen rotation)
     */
    var needToLoadGameFile by remember { mutableStateOf(true) }

    // Load the game file only once. This is done primarily for performance reason.
    // Loading game file will trigger non-stop recomposition.
    // Also need to minimize the need to do expensive time consuming file i/o operation.
    val gameBoardFile = File(LocalContext.current.filesDir, Global.GAME_BOARD_FILENAME)
    if (needToLoadGameFile) {
        gameViewModel.loadGameFile(gameBoardFile)
        Log.i(Global.DEBUG_PREFIX, "Loading from game file")
        needToLoadGameFile = false
    }
}


/**
 *  Handle Back Press Key. Intercept backPress key while on Game  screen.
 *
 *  When doing back press on the current screen, confirm with the user whether it should exit
 *  this screen or not if it is middle of thinking. Do not exit this screen when:
 *  - It is middle of thinking for hint
 *  - It is in middle of announcing victory message
 *
 *  @param mode Current Game mode
 *  @param navController Use to navigate to the previous screen
 *  @param announceVictory Determine whether we are in middle of announcing victory message
 */
@Composable
private fun HandleBackPressKeyForGameScreen(
    mode: GameUIState.GameMode,
    navController: NavHostController,
    announceVictory: Boolean
) {
    var backPressed by remember { mutableStateOf(false) }
    BackPressHandler(onBackPressed = { backPressed = true })
    if (backPressed) {
        backPressed = false
        if (announceVictory) return
        if (mode == GameUIState.GameMode.LookingForHint) return

        navController.popBackStack()
        return
    }
}


/**
 * Provide instruction on how to play Game Screen.
 *
 * Display the game logo. Game logo also change to animation
 * when it is searching for the solution.
 */
@Composable
private fun InstructionLogo() {
    val logoSize = 125.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Box {
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
        }
        Column(Modifier.padding(5.dp)) {
            Text(
                text = "Instruction:",
                style = MaterialTheme.typography.titleSmall
            )

            val bullet = "\u2022"
            val messages = listOf(
                "Start game",
                "Select \"undo\" : Undo the most recent move",
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
        }
    }
}

/**
 * Show all the control buttons on top of the screen. These buttons
 * are "find the solution" button and "reset" button
 *
 * @param gameViewModel View Model that manage business logic for Game Screen. For more
 * details, see [GameViewModel]
 */
@Composable
private fun GameControlButtonsForGame(
    gameViewModel: GameViewModel = viewModel()
) {
    val iconWidth = Icons.Filled.Refresh.defaultWidth

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Button(
            onClick = { gameViewModel.generateNewGame(1) },
            shape = RoundedCornerShape(5.dp),
            elevation = ButtonDefaults.buttonElevation(5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier
                .weight(5F)
                .padding(2.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Create, contentDescription = "New Game",
                modifier = Modifier.size(iconWidth)
            )
            Text(text = "New Game")
        }

        Button(
            onClick = { },
            shape = RoundedCornerShape(5.dp),
            elevation = ButtonDefaults.buttonElevation(5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier
                .weight(4F)
                .padding(2.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh, contentDescription = "Undo",
                modifier = Modifier.size(iconWidth)
            )
            Text("Undo")
        }

        Button(
            onClick = { },
            shape = RoundedCornerShape(5.dp),
            elevation = ButtonDefaults.buttonElevation(5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            modifier = Modifier
                .weight(4F)
                .padding(2.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Info, contentDescription = "Hint",
                modifier = Modifier.size(iconWidth)
            )
            Text("Hint")
        }

    }
}

/**
 *  Draw the Game Board:
 *  - Draw the Grid
 *  - Place all the balls
 *  - Handle all the input (drag, click on grid to place the ball)
 *  - Handle all the ball animations and dynamic animated messages
 *
 * @param modifier Pass in modifier elements that decorate or add behavior to the compose UI
 *  elements
 * @param gameViewModel View Model that manage business logic for Game Screen. For more
 * details, see [GameViewModel]
 * @param gameUIState Current UI state of the game
 * @param announceVictory Indicate whether it need to show animated victory message or not
 */
@Composable
private fun DrawGameBoard(
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel = viewModel(),
    announceVictory: Boolean,
    moveBallRec: GameUIState.GameMode.MoveBall?,
    shadowBallRec: GameUIState.GameMode.IndicateInvalidMoveByShowingShadowMove?,
) {
    val victoryMsgColor = MaterialTheme.colorScheme.onPrimaryContainer

    /**
     * Create textMeasurer instance, which is responsible for measuring a text in its entirety so
     * that it can be drawn on the canvas (drawScope). This is used to draw animated victory message
     */
    val textMeasurer = rememberTextMeasurer()

    /**
     * Animation control that handle ball movement
     */
    val animateBallMovementChain = mutableListOf<Animatable<Float, AnimationVector1D>>()

    val animateParticleExplosion = remember { Animatable(initialValue = 0f) }
    var particles = mutableListOf<Particle>()

    val animateVictoryMessage = remember { Animatable(initialValue = 0f) }
    if (announceVictory) {
        AnimateVictoryMessageSetup(
            { gameViewModel.gameSetModeWaitingOnUser() },
            animateCtl = animateVictoryMessage
        )
    } else {
        LaunchedEffect(true) {
            animateVictoryMessage.stop()
            animateVictoryMessage.snapTo(0F)
        }
    }

    if (moveBallRec != null) {

        // Set-up the particles, which is used for the explosion animated effect
        particles = remember {
            generateExplosionParticles(moveBallRec.movingChain, moveBallRec.moveDirection)
        }.toMutableList()

        /**
         * The following lambda functions are used in [AnimateBallMovementsSetup] routine
         */
        val gameMoveBallTask =
            { pos: Pos, direction: Direction -> gameViewModel.moveBall(pos, direction) }

        AnimateBallMovementsSetup(
            movingChain = moveBallRec.movingChain,
            direction = moveBallRec.moveDirection,
            animateBallMovementCtlList = animateBallMovementChain,
            animateParticleExplosionCtl = animateParticleExplosion,
            moveBallTask = gameMoveBallTask
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

    /**
     * Animation control that handle shadow ball movement. Used to show invalid move to user
     */
    val animateShadowMovement = remember { Animatable(initialValue = 0f) }

    if (shadowBallRec != null)
        GameAnimateShadowBallMovementSetup(animateShadowMovement, gameViewModel)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(Global.MAX_COL_SIZE.toFloat() / Global.MAX_ROW_SIZE.toFloat())
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
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
                    detectDragGestures(
                        onDragStart = { offset: Offset ->
                            dragRow = (offset.y / gridSize).toInt()
                            dragCol = (offset.x / gridSize).toInt()
                        },
                        onDrag = { change, dragAmount ->
                            //  Consume this event, so that parent will not react to it anymore.
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        },
                        onDragEnd = {
                            when {
                                (offsetX < 0F && abs(offsetX) > minSwipeOffset) ->
                                    gameViewModel.setupNextMove(dragRow, dragCol, Direction.LEFT)

                                (offsetX > 0F && abs(offsetX) > minSwipeOffset) ->
                                    gameViewModel.setupNextMove(dragRow, dragCol, Direction.RIGHT)

                                (offsetY < 0F && abs(offsetY) > minSwipeOffset) ->
                                    gameViewModel.setupNextMove(dragRow, dragCol, Direction.UP)

                                (offsetY > 0F && abs(offsetY) > minSwipeOffset) ->
                                    gameViewModel.setupNextMove(dragRow, dragCol, Direction.DOWN)
                            }
                            offsetX = 0F
                            offsetY = 0F
                            dragRow = -1
                            dragCol = -1
                        }
                    ) // detectDragGestures
                } // .pointerInput
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val gridSizeWidth = (canvasWidth / (Global.MAX_COL_SIZE))
            val gridSizeHeight = (canvasHeight / (Global.MAX_ROW_SIZE))

            gridSize = if (gridSizeWidth > gridSizeHeight) gridSizeHeight else gridSizeWidth

            val drawScope = this   // DrawScope of this grid
            drawGridForGame(drawScope, gridSize, lineColor)

            val ballSize = (gridSize * 1.10).toInt()
            val displayBallImage = Bitmap.createScaledBitmap(
                ballImage.asAndroidBitmap(),
                ballSize, ballSize, false
            ).asImageBitmap()

            displayBallImage.prepareToDraw()   // cache it

            if (moveBallRec != null) {
                val ballsToErase = moveBallRec.movingChain

                gameViewModel.drawGameBallsOnGrid(
                    drawScope,
                    gridSize,
                    displayBallImage,
                    ballsToErase
                )
                animateBallMovementsPerform(
                    drawScope = drawScope,
                    gridSize = gridSize,
                    displayBallImage = displayBallImage,
                    animateBallMovementChainCtlList = animateBallMovementChain,
                    animateParticleExplosionCtl = animateParticleExplosion,
                    particles = particles,
                    direction = moveBallRec.moveDirection,
                    movingChain = moveBallRec.movingChain
                )
            } else {
                gameViewModel.drawGameBallsOnGrid(drawScope, gridSize, displayBallImage)

                if (shadowBallRec != null) {
                    animateShadowBallMovementsPerform(
                        drawScope = drawScope,
                        gridSize = gridSize,
                        displayBallImage = displayBallImage,
                        animateCtl = animateShadowMovement,
                        distance = shadowBallRec.shadowMovingChain[0].distance,
                        direction = shadowBallRec.shadowMoveDirection,
                        pos = shadowBallRec.shadowMovingChain[0].pos
                    )
                }
            }

            if (announceVictory) {
                animateVictoryMsgPerform(
                    drawScope, animateVictoryMessage,
                    textMeasurer, victoryMsgColor
                )
            }

        }
    }
}

private fun drawGridForGame(
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

/******************************* Animation routines **********************************************/

/**
 *  Set up animation for shadow ball movement
 *
 * @param animateCtl Animate object that control the shadow ball movement for the game
 * @param gameViewModel View Model that manage business logic for Game Screen. For more
 * details, see [GameViewModel]
 */
@Composable
fun GameAnimateShadowBallMovementSetup(
    animateCtl: Animatable<Float, AnimationVector1D>,
    gameViewModel: GameViewModel
) {
    LaunchedEffect(Unit) {
        // Use coroutine to ensure both animation and sound happen in parallel
        coroutineScope {
            launch {
                animateCtl.animateTo(
                    targetValue = 1f,
                    animationSpec = repeatable(
                        iterations = 1,
                        animation = tween(1000, easing = FastOutSlowInEasing)
                    )
                )
                animateCtl.stop()
                animateCtl.snapTo(0f)
                gameViewModel.gameSetModeWaitingOnUser()
            }  // launch

            launch {
                gAudio_doink.start()
            }
        } // coroutine scope
    }  // LaunchedEffect
}