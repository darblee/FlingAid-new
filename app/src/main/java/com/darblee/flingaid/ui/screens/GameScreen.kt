package com.darblee.flingaid.ui.screens

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darblee.flingaid.Global
import com.darblee.flingaid.R
import com.darblee.flingaid.ui.GameUIState
import com.darblee.flingaid.ui.GameViewModel
import kotlin.math.abs

@Composable
fun GameScreen(modifier: Modifier = Modifier,
        onNavigateBack: () -> Unit)
{
    val gameViewModel: GameViewModel = viewModel()
    Column (
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
          val uiState by gameViewModel.uiState.collectAsState()
    //    gBoardFile = File(LocalContext.current.filesDir, Global.boardFileName)

    //    gameViewModel.loadBallPositions(gBoardFile)  // Load balls from previous game save

        InstructionLogo(onNavigateBack)
        GameControlButtonsForGame(gameViewModel, uiState)

        DrawGameBoard(modifier = Modifier.fillMaxSize(), gameViewModel, uiState)
    }
}

/*
 * Provide instruction on how to play Solver Screen.
 *
 * Display the game logo. Game logo also change to animation
 * when it is searching for the solution.
 */
@Composable
private fun InstructionLogo(onNavigateBack: () -> Unit)
{
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
            Text(text = "Instruction:",
                style = MaterialTheme.typography.titleSmall)

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
private fun GameControlButtonsForGame(
    gameViewModel: GameViewModel = viewModel(),
    uiState: GameUIState
)
{
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Button(
            onClick = { }, // OnClick
            shape = RoundedCornerShape(5.dp),
            elevation = ButtonDefaults.buttonElevation(5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier
                .weight(3F)
                .padding(5.dp)
        ) { Text(text = "New Game") }

        Button(
            onClick = { },
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
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Undo",
                modifier = Modifier.size(iconWidth))
            Text("Undo")
        }
    }
}

/*
 *  Draw the Fling Game Board:
 *       - Grid
 *       - all the balls
 *       - winning arrow (if there is solution after "find winnable movable" submission
 */
@Composable
private fun DrawGameBoard(
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel = viewModel(),
    uiState: GameUIState
)
{
    var gridSize by rememberSaveable {
        mutableFloatStateOf(0f)
    }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var dragRow by remember { mutableIntStateOf(-1) }
    var dragCol by remember { mutableIntStateOf(-1) }

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
        val lineColor = MaterialTheme.colorScheme.outline

        val ballImage = ImageBitmap.imageResource(id = R.drawable.ball)

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
                            Log.i(Global.debugPrefix, "Offset is row: $dragRow, col: $dragCol")
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
                                    Log.i(Global.debugPrefix, "Swipe left from $dragRow, $dragCol for length $offsetX")
                                    offsetX = 0F
                                    offsetY = 0F
                                    dragRow = -1
                                    dragCol = -1
                                }
                                (offsetX > 0F && abs(offsetX) > minSwipeOffset) -> {
                                    Log.i(Global.debugPrefix, "Swipe right from $dragRow, $dragCol for length $offsetX")
                                    offsetX = 0F
                                    offsetY = 0F
                                    dragRow = -1
                                    dragCol = -1
                                }
                                (offsetY < 0F && abs(offsetY) > minSwipeOffset) -> {
                                    Log.i(Global.debugPrefix, "Swipe Up from $dragRow, $dragCol for length $offsetY")
                                    offsetX = 0F
                                    offsetY = 0F
                                    dragRow = -1
                                    dragCol = -1
                                }
                                (offsetY > 0F && abs(offsetY) > minSwipeOffset) -> {
                                    Log.i(Global.debugPrefix, "Swipe down from $dragRow, $dragCol for length $offsetY")
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

            drawGridForGame(this, gridSize, lineColor)

            val ballSize =  (gridSize * 1.10).toInt()
            val displayBallImage = Bitmap.createScaledBitmap(ballImage.asAndroidBitmap(),
                ballSize, ballSize, false).asImageBitmap()

            drawBallsForGame(this, gameViewModel, gridSize, displayBallImage)
        }
    }
}

private fun drawGridForGame(
    drawScope: DrawScope,
    gridSize: Float,
    lineColor: Color
)
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

// Draw all the balls in the provided canvas
private fun drawBallsForGame(
    drawScope: DrawScope,
    gameViewModel: GameViewModel,
    gridSize: Float,
    displayBallImage: ImageBitmap)
{
    // Draw all the balls
    with (drawScope) {
        gameViewModel.ballPositionList().forEach { pos ->
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

