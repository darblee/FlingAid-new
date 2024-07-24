package com.darblee.flingaid.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.animation.core.Animatable
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
import com.darblee.flingaid.ui.GameState
import com.darblee.flingaid.ui.GameUIState
import com.darblee.flingaid.ui.GameViewModel
import com.darblee.flingaid.utilities.gameToast
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
    val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()

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
    val onBallMovementAnimationChange =
        { enableBallMovements: Boolean -> showBallMovementAnimation = enableBallMovements }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var animateBallSwipe by remember {
            mutableStateOf(false)
        }
        InstructionLogo()
        GameControlButtonsForGame(
            gameViewModel,
            uiState,
            onBallMovementAnimationChange,
            announceVictory
        )

        DrawGameBoard(
            modifier = Modifier.fillMaxSize(),
            gameViewModel,
            uiState,
            onSwipeBall = { needToAnimate -> animateBallSwipe = needToAnimate },
            onEnableVictoryMsg
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
    uiState: GameUIState,
    onSwipeBall: (animateBalls: Boolean) -> Unit,
    onEnableVictoryMsg: (Boolean) -> Unit
) {
    val context = LocalContext.current

    /**
     * Create textMeasurer instance, which is responsible for measuring a text in its entirety so
     * that it can be drawn on the canvas (drawScope). This is used to draw animated victory message
     */
    val textMeasurer = rememberTextMeasurer()

    if (gameViewModel.ballCount() == 1) {
        if (uiState.state == GameState.IdleFoundSOlution) {
            onEnableVictoryMsg(true)
        }
    }

    /**
     * Launch the animation only once when it enters the composition. It will animate infinitely
     * until it is removed from the composition
     */
    val animate1 = remember { Animatable(initialValue = 0f) }
    val animate2 = remember { Animatable(initialValue = 0f) }
    val animate3 = remember { Animatable(initialValue = 0f) }

    var processSwipe by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animate1.animateTo(
            targetValue = 1f, animationSpec =
            repeatable(
                iterations = 1,
                animation = tween(1000, easing = FastOutSlowInEasing),
            )
        )
        animate2.animateTo(
            targetValue = 1f, animationSpec =
            repeatable(
                iterations = 1,
                animation = tween(1000, easing = FastOutSlowInEasing),
            )
        )
        animate3.animateTo(
            targetValue = 1f, animationSpec =
            repeatable(
                iterations = 1,
                animation = tween(1000, easing = FastOutSlowInEasing),
            )
        )
    }

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
                            Log.i(Global.DEBUG_PREFIX, "Offset is row: $dragRow, col: $dragCol")
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
                                    Log.i(
                                        Global.DEBUG_PREFIX,
                                        "Swipe left from $dragRow, $dragCol for length $offsetX"
                                    )

                                    // Check if there is a valid move, If there is, then set-up
                                    // the move.
                                    if (gameViewModel.validMove(dragRow, dragCol, Direction.LEFT)) {
                                        onSwipeBall.invoke(true)
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

                                    // Check if there is a valid move, If there is, then set-up
                                    // the move.
                                    if (gameViewModel.validMove(dragRow, dragCol, Direction.RIGHT)) {
                                        onSwipeBall.invoke(true)
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
                                    // Check if there is a valid move, If there is, then set-up
                                    // the move.
                                    if (gameViewModel.validMove(dragRow, dragCol, Direction.UP)) {
                                        onSwipeBall.invoke(true)
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

                                    // Check if there is a valid move, If there is, then set-up
                                    // the move.
                                    if (gameViewModel.validMove(dragRow, dragCol, Direction.DOWN)) {
                                        onSwipeBall.invoke(true)
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

            drawGameBalls(gridDrawScope, gameViewModel, gridSize, displayBallImage)
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

// Draw all the balls in the provided canvas
private fun drawGameBalls(
    drawScope: DrawScope,
    gameViewModel: GameViewModel,
    gridSize: Float,
    displayBallImage: ImageBitmap
) {
    // Draw all the balls
    with(drawScope) {
        // Draw all the balls
        //
        // NOTE: ballPositionList is a SnapshotList type. It is observation system that will trigger a recompose
        // to redraw the grid.
        gameViewModel.ballPositionList().forEach { curPosition ->
            drawImage(
                image = displayBallImage,
                topLeft = Offset(
                    (curPosition.col * gridSize),
                    (curPosition.row * gridSize)
                )
            )
        }
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