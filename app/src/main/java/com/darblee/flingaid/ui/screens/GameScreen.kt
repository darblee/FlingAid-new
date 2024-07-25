package com.darblee.flingaid.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
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
import androidx.compose.ui.graphics.drawscope.translate
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
import com.darblee.flingaid.ui.GameState
import com.darblee.flingaid.ui.GameUIState
import com.darblee.flingaid.ui.GameViewModel
import com.darblee.flingaid.ui.MovingRec
import com.darblee.flingaid.utilities.Pos
import com.darblee.flingaid.utilities.drawBallOnGrid
import com.darblee.flingaid.utilities.gameToast
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

    Log.i(Global.DEBUG_PREFIX, "Game Screen - recompose")

    var announceVictory by remember { mutableStateOf(false) }

    /**
     * Need to ensure we only load the file once when we start the the solver game screen.
     * If we load the file everytime we go down this path during recompose, it will trigger more
     * recompose. This will cause unnecessary performance overload.
     *
     * __Developer's Note:__  Use [rememberSaveable] instead of [remember] because we want to
     * preserve this even after config change (e.g. screen rotation)
     */
    var needToLoadGameFile by remember { mutableStateOf(true) }

    /**
     * Intercept backPress key while on Game screen.
     *
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
        gameScreenBackPressed(LocalContext.current, navController)
        return
    }

    val gameViewModel: GameViewModel = viewModel()
    val gameUIState by gameViewModel.gameUIState.collectAsStateWithLifecycle()

    val boardFile = File(LocalContext.current.filesDir, Global.GAME_BOARD_FILENAME)

    val onEnableVictoryMsg = { setting: Boolean -> announceVictory = setting }
    val victoryMsgColor = MaterialTheme.colorScheme.onPrimaryContainer

    // Load the game file only once. This is done primarily for performance reason.
    // Loading game file will trigger non-stop recomposition.
    // Also need to minimize the need to do expensive time consuming file i/o operation.
    if (needToLoadGameFile) {
        gameViewModel.loadGameFile(boardFile)
        needToLoadGameFile = false
    }

    /**
     * Keep track of when to do the ball movement animation
     */
    var showBallMovementAnimation by remember { mutableStateOf(false) }
    val onBallMovementAnimationEnablement =
        { enableBallMovements: Boolean -> showBallMovementAnimation = enableBallMovements }

    /**
     * Show shadow movement only. This is used to show invalid move
     */
    var showShadowMovement by remember { mutableStateOf(false) }
    val onShadowMovementAnimationEnablement =
        { enableShadowMovement: Boolean -> showShadowMovement = enableShadowMovement }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        InstructionLogo()
        GameControlButtonsForGame(
            gameViewModel,
            gameUIState,
            onBallMovementAnimationEnablement,
            announceVictory
        )

        DrawGameBoard(
            modifier = Modifier.fillMaxSize(),
            gameViewModel = gameViewModel,
            gameUIState = gameUIState,
            showBallMovementAnimation = showBallMovementAnimation,
            onBallMovementAnimationEnablement = onBallMovementAnimationEnablement,
            onEnableVictoryMsg = onEnableVictoryMsg,
            showShadowMovement = showShadowMovement,
            onShadowMovementAnimationEnablement = onShadowMovementAnimationEnablement
        )
    }
}

/**
 * Provide instruction on how to play Solver Screen.
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
 * @param gameViewModel  Game View model
 * @param uiState Current UI state of the solver game
 * @param onBallMovementAnimationChange Determine whether the ball movement animation is done or not
 * @param announceVictory Indicate whether we need to announce victory message or not
 */
@Composable
private fun GameControlButtonsForGame(
    gameViewModel: GameViewModel = viewModel(),
    uiState: GameUIState,
    onBallMovementAnimationChange: (Boolean) -> Unit,
    announceVictory: Boolean
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


/*
 *  Draw the  Game Board:
 *       - Grid
 *       - all the balls
 */
@Composable
private fun DrawGameBoard(
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel = viewModel(),
    gameUIState: GameUIState,
    showBallMovementAnimation: Boolean,
    onEnableVictoryMsg: (Boolean) -> Unit,
    onBallMovementAnimationEnablement: (Boolean) -> Unit,
    showShadowMovement: Boolean,
    onShadowMovementAnimationEnablement: (Boolean) -> Unit
) {
    val context = LocalContext.current

    /**
     * Create textMeasurer instance, which is responsible for measuring a text in its entirety so
     * that it can be drawn on the canvas (drawScope). This is used to draw animated victory message
     */
    val textMeasurer = rememberTextMeasurer()

    if (gameViewModel.ballCount() == 1) {
        if (gameUIState.state == GameState.IdleFoundSolution) {
            onEnableVictoryMsg(true)
        }
    }

    /**
     * Animation control that handle ball movement
     */
    val animateBallMovementChain = mutableListOf<Animatable<Float, AnimationVector1D>>()

    if ((showBallMovementAnimation) && (gameUIState.state == GameState.MoveBall)) {
        GameAnimateBallMovementsSetup(
            gameUIState,
            onBallMovementAnimationEnablement,
            animateBallMovementChain,
            gameViewModel
        )
    }

    /**
     * Animation control that handle shadow ball movement. Used to show invalid move to user
     */
    val animateShadowMovement = remember { Animatable(initialValue = 0f) }
    if ((showShadowMovement) && (gameUIState.state == GameState.ShowShadowMovement))
        GameAnimateShadowBallMovementsSetup(animateShadowMovement,
            onShadowMovementAnimationEnablement, gameViewModel)

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
                            //  Need to consume this event, so that its parent knows not to react to
                            //  it anymore. What change.consume() does is it prevent pointerInput
                            //  above it to receive events
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        },
                        onDragEnd = {
                            when {
                                (offsetX < 0F && abs(offsetX) > minSwipeOffset) -> {
                                    Log.i(
                                        Global.DEBUG_PREFIX,
                                        "Swipe left from $dragRow, $dragCol for length $offsetX"
                                    )
                                    val moveResult =
                                        gameViewModel.validMove(dragRow, dragCol, Direction.LEFT)

                                    when (moveResult) {
                                        GameViewModel.MoveResult.Valid ->
                                            onBallMovementAnimationEnablement(true)

                                        GameViewModel.MoveResult.InvalidNoBump ->
                                            onShadowMovementAnimationEnablement(true)

                                        else -> {
                                            Log.i(
                                                Global.DEBUG_PREFIX,
                                                "Invalid ball movement. Ignore"
                                            )
                                        }
                                    }
                                    offsetX = 0F
                                    offsetY = 0F
                                    dragRow = -1
                                    dragCol = -1
                                }

                                (offsetX > 0F && abs(offsetX) > minSwipeOffset) -> {
                                    Log.i(
                                        Global.DEBUG_PREFIX,
                                        "Swipe right from $dragRow, $dragCol for length $offsetX"
                                    )
                                    val moveResult =
                                        gameViewModel.validMove(dragRow, dragCol, Direction.RIGHT)
                                    when (moveResult) {
                                        GameViewModel.MoveResult.Valid ->
                                            onBallMovementAnimationEnablement(true)

                                        GameViewModel.MoveResult.InvalidNoBump ->
                                            onShadowMovementAnimationEnablement(true)

                                        else -> {
                                            Log.i(
                                                Global.DEBUG_PREFIX,
                                                "Invalid ball movement. Ignore"
                                            )
                                        }
                                    }

                                    offsetX = 0F
                                    offsetY = 0F
                                    dragRow = -1
                                    dragCol = -1
                                }

                                (offsetY < 0F && abs(offsetY) > minSwipeOffset) -> {
                                    Log.i(
                                        Global.DEBUG_PREFIX,
                                        "Swipe Up from $dragRow, $dragCol for length $offsetY"
                                    )

                                    val moveResult =
                                        gameViewModel.validMove(dragRow, dragCol, Direction.UP)
                                    when (moveResult) {
                                        GameViewModel.MoveResult.Valid ->
                                            onBallMovementAnimationEnablement(true)

                                        GameViewModel.MoveResult.InvalidNoBump ->
                                            onShadowMovementAnimationEnablement(true)

                                        else -> {
                                            Log.i(
                                                Global.DEBUG_PREFIX,
                                                "Invalid ball movement. Ignore"
                                            )
                                        }
                                    }

                                    offsetX = 0F
                                    offsetY = 0F
                                    dragRow = -1
                                    dragCol = -1
                                }

                                (offsetY > 0F && abs(offsetY) > minSwipeOffset) -> {
                                    Log.i(
                                        Global.DEBUG_PREFIX,
                                        "Swipe down from $dragRow, $dragCol for length $offsetY"
                                    )
                                    val moveResult =
                                        gameViewModel.validMove(dragRow, dragCol, Direction.DOWN)
                                    when (moveResult) {
                                        GameViewModel.MoveResult.Valid ->
                                            onBallMovementAnimationEnablement(true)

                                        GameViewModel.MoveResult.InvalidNoBump ->
                                            onShadowMovementAnimationEnablement(true)

                                        else -> {
                                            Log.i(
                                                Global.DEBUG_PREFIX,
                                                "Invalid ball movement. Ignore"
                                            )
                                        }
                                    }

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

            val gridSizeWidth = (canvasWidth / (Global.MAX_COL_SIZE))
            val gridSizeHeight = (canvasHeight / (Global.MAX_ROW_SIZE))

            gridSize = if (gridSizeWidth > gridSizeHeight) gridSizeHeight else gridSizeWidth

            val gridDrawScope = this
            drawGridForGame(gridDrawScope, gridSize, lineColor)

            val ballSize = (gridSize * 1.10).toInt()
            val displayBallImage = Bitmap.createScaledBitmap(
                ballImage.asAndroidBitmap(),
                ballSize, ballSize, false
            ).asImageBitmap()

            displayBallImage.prepareToDraw()   // cache it

            if ((showBallMovementAnimation) && (gameUIState.state == GameState.MoveBall)) {
                val ballsToErase = gameUIState.movingChain
                drawGameBalls(gridDrawScope, gameViewModel, gridSize, displayBallImage, ballsToErase)
                gameAnimateBallMovementsPerform(
                    this, gridSize, displayBallImage,
                    animateBallMovementChain, gameUIState
                )
            } else {
                drawGameBalls(gridDrawScope, gameViewModel, gridSize, displayBallImage)

                if ((showShadowMovement) && (gameUIState.state == GameState.ShowShadowMovement)) {
                    if (gameUIState.movingChain.isNotEmpty()) {
                        animateShadowMovePerform(
                            drawScope = gridDrawScope,
                            gridSize = gridSize,
                            animate = animateShadowMovement,
                            displayBallImage = displayBallImage,
                            distance = gameUIState.movingChain[0].distance,
                            direction = gameUIState.movingDirection,
                            pos = gameUIState.movingChain[0].pos
                        )
                    }
                }
            }
        }
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

// Draw all the balls in the provided canvas
private fun drawGameBalls(
    drawScope: DrawScope,
    gameViewModel: GameViewModel,
    gridSize: Float,
    displayBallImage: ImageBitmap,
    eraseAnimatedBallPositions: List<MovingRec> = listOf()
) {
    // Draw all the balls
    //
    // NOTE: ballPositionList is a SnapshotList type. It is observation system that will trigger a recompose
    // to redraw the grid.
    gameViewModel.ballPositionList().forEach { pos ->
        var skipDraw = false
        eraseAnimatedBallPositions.forEach { eraseRec ->
            if ((eraseRec.pos.row == pos.row) && (eraseRec.pos.col == pos.col))
                skipDraw = true
        }

        if (!skipDraw) drawBallOnGrid(drawScope, gridSize, pos, displayBallImage)
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
fun gameScreenBackPressed(context: Context, navController: NavHostController) {
    /* TODO
     * Add logic to handle backPress.
     * - Do not backPress while it is thinking.
     * - other??
     */
    Log.i(Global.DEBUG_PREFIX, "Game: backPress detected")
    gameToast(context, "Unable to go back home screen while thinking")

    navController.popBackStack()
}

/******************************* Animation routines **********************************************/

/**
 * Statically defined animated Keyframe specification to wiggle the ball
 *
 * @see AnimateBallMovementsSetup
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
 *  - [AnimateBallMovementsSetup]
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

@Composable
private fun GameAnimateBallMovementsSetup(
    gameUIState: GameUIState,
    onAnimationChange: (enableBallMovementAnimation: Boolean) -> Unit,
    animateBallMovementChain: MutableList<Animatable<Float, AnimationVector1D>>,
    gameViewModel: GameViewModel
) {
    val movingChain = gameUIState.movingChain
    movingChain.forEach { _ ->
        val animateBallMovement = remember { Animatable(initialValue = 0f) }
        animateBallMovementChain.add(animateBallMovement)
    }

    // Time ratio of total time when the ball makes contact to the neighboring ball
    val whenBallMakeContactRatio = 0.97f

    LaunchedEffect(Unit) {
        // Use coroutine to ensure both launch animation get completed in the same co-routine scope
        coroutineScope {
            launch { // One or more ball movements in serial fashion
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
                    }
                }   // movingChain forEach
                animateBallMovementChain.clear()

                onAnimationChange(false)

                gameViewModel.moveBall(gameUIState)
            } // Launch
        }   // coroutine
    }
}

/**
 * Perform the actual animation of ball movements (including its chain) and
 * the particle explosion effect.
 *
 * @param drawScope
 * @param gridSize Size of grid. This used to do various computation on animation effect
 * @param displayBallImage The actual ball image to show
 * @param animateBallMovementChain Object that control animation state of all the ball movement in this chain
 * @param gameUIState UI state of the game View Model
 */
fun gameAnimateBallMovementsPerform(drawScope: DrawScope,
                                    gridSize: Float,
                                    displayBallImage: ImageBitmap,
                                    animateBallMovementChain: MutableList<Animatable<Float, AnimationVector1D>>,
                                    gameUIState: GameUIState
) {
    val movingDirection = gameUIState.movingDirection
    val movingChain = gameUIState.movingChain

    if (movingChain.isEmpty())  return
    if (movingDirection == Direction.NO_WINNING_DIRECTION) return
    if (animateBallMovementChain.isEmpty()) return

    with (drawScope) {
        var movingSourcePos: Pos
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
                drawBallOnGrid(drawScope, gridSize, movingSourcePos, displayBallImage)
            } // translate
        }  // for
    }
}

/**
 * This is a set-up to control shadow ball movement
 */
@Composable
fun GameAnimateShadowBallMovementsSetup(
    animateShadowMovement: Animatable<Float, AnimationVector1D>,
    onShadowMovementAnimationEnablement: (Boolean) -> Unit,
    gameViewModel: GameViewModel
)
{
    LaunchedEffect(Unit) {
        // Use coroutine to ensure both animation and sound happen in parallel
        coroutineScope {
            launch {
                animateShadowMovement.animateTo(
                    targetValue = 1f,
                    animationSpec = repeatable(
                        iterations = 1,
                        animation = tween(1000, easing = FastOutSlowInEasing)
                    )
                )
                animateShadowMovement.stop()
                animateShadowMovement.snapTo(0f)
                onShadowMovementAnimationEnablement(false)
                gameViewModel.idleState()
            }  // launch
            launch {
                gAudio_doink.start()
            }
        } // coroutine scope
    }  // LaunchedEffect
}

/**
 * Animate the shadowed ball movement.
 *
 * @param drawScope Scope to do the drawing on
 * @param gridSize width or height of the grid in dp unit
 * @param animate Object that control animation state of shadow ball movement. It loops endlessly
 * @param displayBallImage The image of the ball
 * @param distance number of blocks in this ball movement
 * @param direction Direction of the ball movement
 * @param pos Position of the ball to move from
 */
private fun animateShadowMovePerform(
    drawScope: DrawScope,
    gridSize: Float,
    animate: Animatable<Float, AnimationVector1D>,
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
                assert(true) { "Got unexpected Direction value: ${direction}" }
            }
        }

        translate(
            (xOffset) * animate.value,
            (yOffset) * animate.value
        ) {
            drawBallOnGrid(drawScope, gridSize, pos, displayBallImage, alpha = 0.4F)
        }
    }
}


