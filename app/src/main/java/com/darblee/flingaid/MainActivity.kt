package com.darblee.flingaid

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darblee.flingaid.ui.Direction
import com.darblee.flingaid.ui.GameUiState
import com.darblee.flingaid.ui.GameViewModel
import com.darblee.flingaid.ui.pos
import com.darblee.flingaid.ui.theme.FlingAidTheme

// Declare these bitmaps once as it will be reused on every recompose
private lateinit var upArrowBitmap : Bitmap
private lateinit var downArrowBitmap : Bitmap
private lateinit var leftArrowBitmap : Bitmap
private lateinit var rightArrowBitmap : Bitmap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        var keepSplashOnScreen = true
        val delay = 2000L
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the splashscreen on-screen for specific period
        splashScreen.setKeepOnScreenCondition{ keepSplashOnScreen }
        Handler(Looper.getMainLooper()).postDelayed({ keepSplashOnScreen = false }, delay)

        setContent {
            FlingAidTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainViewImplementation()
                }
            }
        }
    }
}

@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun MainViewImplementation(
        gameViewModel: GameViewModel = viewModel(),
        modifier: Modifier = Modifier
) {
    val uiState by gameViewModel.uiState.collectAsState()
    Log.i(Constants.debugPrefix, "MainViewImplementation invoke")

    // Force to be in portrait mode all the time
    val activity = LocalContext.current as Activity
    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        TopControlButtons(gameViewModel)
        Grid(gameViewModel, modifier, uiState)
        ResetGameButton(gameViewModel)
    } // Column
}

@Composable
fun TopControlButtons(
    gameViewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
)
{
    val contextForToast = LocalContext.current.applicationContext
    val view = LocalView.current

    Row(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val haptic = LocalHapticFeedback.current
        Button(
            onClick = {
                Toast.makeText(contextForToast, "Next Move", Toast.LENGTH_SHORT).show()
                gameViewModel.findWinningMove()
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) },
            shape = RoundedCornerShape(5.dp),
            elevation = ButtonDefaults.buttonElevation(5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Green,
                contentColor = Color.Black
            )
        ) {
            val iconWidth = Icons.Filled.Refresh.defaultWidth
            Icon(imageVector = Icons.Filled.Search, contentDescription = "Find Winning Move",
                modifier = Modifier.size(iconWidth))
            Text("Find winning move")
        }

        Button(
            onClick = {
                Toast.makeText(contextForToast, "Prev Move", Toast.LENGTH_SHORT).show()
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) },
            shape = RoundedCornerShape(5.dp),
            elevation = ButtonDefaults.buttonElevation(5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Cyan,
                contentColor = Color.Black
            )
        ) {
            val iconWidth = Icons.Filled.Refresh.defaultWidth
            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Undo last move",
                modifier = Modifier.size(iconWidth))
            Text("Undo")
        }
    }
}

@Composable
fun Grid(
    gameViewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier,
    uiState: GameUiState
) {
    Log.i(Constants.debugPrefix, "Grid Recompose has been triggered")

    var gridSize = 0f
    val view = LocalView.current

    val matrix = Matrix()
    matrix.postRotate(90F)

    upArrowBitmap = ImageBitmap.imageResource(R.drawable.up).asAndroidBitmap()
    rightArrowBitmap = Bitmap.createBitmap(upArrowBitmap, 0, 0, upArrowBitmap.width, upArrowBitmap.height, matrix, true )
    downArrowBitmap = Bitmap.createBitmap(rightArrowBitmap, 0, 0, rightArrowBitmap.width, rightArrowBitmap.height, matrix, true )
    leftArrowBitmap = Bitmap.createBitmap(downArrowBitmap, 0, 0, downArrowBitmap.width, downArrowBitmap.height, matrix, true )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(Constants.MaxColSize.toFloat() / Constants.MaxRowSize.toFloat())
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Gray),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .padding(15.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val pos = pos(
                                row = (tapOffset.y / gridSize).toInt(),
                                col = (tapOffset.x / gridSize).toInt()
                            )
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            gameViewModel.toggleBallPosition(pos)
                        }
                    )
                }
        ) {
            Log.i(Constants.debugPrefix, "Canvas Recompose has been triggered")
            val canvasWidth = size.width
            val canvasHeight = size.height

            val gridSizeWidth = (canvasWidth / (Constants.MaxColSize))
            val gridSizeHeight = (canvasHeight / (Constants.MaxRowSize))

            gridSize = if (gridSizeWidth > gridSizeHeight) gridSizeHeight else gridSizeWidth

            drawGrid(this, gridSize)
            drawBalls(this, gameViewModel, gridSize)

            // Draw the winning arrow if there is a winning move identified
            if (gameViewModel.winningMoveExist()) {
                drawWinningMoveArrow(this, gridSize, uiState)
            }
        }
    }
}

fun drawWinningMoveArrow(drawScope: DrawScope, gridSize: Float, uiState: GameUiState, ) {
    with (drawScope) {
        Log.i(Constants.debugPrefix, "Winning Move exist with winning direction:  ${uiState.winningDirection}")
        Log.i(Constants.debugPrefix, "Winning Move position is :  row = ${uiState.winningPosition.row}, col = ${uiState.winningPosition.col}")

        val displayArrowBitMap = when (uiState.winningDirection) {
            Direction.UP -> upArrowBitmap
            Direction.DOWN -> downArrowBitmap
            Direction.LEFT -> leftArrowBitmap
            Direction.RIGHT -> rightArrowBitmap

            //NOTE: bitmap configuration describes how pixels are stored. This affects the quality (color depth) as well as the ability to display transparent/translucent colors.
            // "Bitmap.Config.ARGB_8888" indicates the maximum flexibility
            else -> {
                Log.e(Constants.debugPrefix, "Got unexpected Direction value")
                assert(true)
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }
        }

        // Reduce size of arrow to fit inside the grid
        val displayArrow = Bitmap.createScaledBitmap(displayArrowBitMap, gridSize.toInt()-10, gridSize.toInt()-10, false).asImageBitmap()

        drawImage(displayArrow, topLeft = Offset(x = ((uiState.winningPosition.row) * gridSize) + 5f, y = ((uiState.winningPosition.col * gridSize) + 5f)))
    }
}

fun drawBalls(drawScope: DrawScope, gameViewModel: GameViewModel, gridSize: Float) {
    // Draw all the balls
    with (drawScope) {
        gameViewModel.ballPositionList.forEach { pos ->
            drawCircle(
                Color.Red,
                radius = (gridSize / 2) - 10f,
                center = Offset(
                    (pos.col * gridSize) + (gridSize / 2f),
                    (pos.row * gridSize) + (gridSize / 2f)
                )
            )
        }
    }
}

fun drawGrid(DrawScope: DrawScope, gridSize: Float, ) {
    with (DrawScope) {
        // Draw horizontal lines
        var currentY = 0F
        val gridWidth = gridSize * Constants.MaxColSize
        repeat(Constants.MaxRowSize + 1) { index ->
            val lineWidth = if (index == 4) 5 else 2
            drawLine(
                start = Offset(x = 0.dp.toPx(), y = currentY),
                end = Offset(x = gridWidth, y = currentY),
                color = Color.Black,
                strokeWidth = lineWidth.dp.toPx() // instead of 5.dp.toPx() , you can also pass 5f
            )
            currentY += gridSize
        }

        // Draw vertical lines
        var currentX = 0F
        val gridHeight = gridSize * Constants.MaxRowSize
        repeat(Constants.MaxColSize + 1) { index ->
            drawLine(
                start = Offset(x = currentX, y = 0.dp.toPx()),
                end = Offset(x = currentX, y = gridHeight),
                color = Color.Black,
                strokeWidth = 2.dp.toPx() // instead of 5.dp.toPx() , you can also pass 5f
            )
            currentX += gridSize
        }
    }
}

@Composable
fun ResetGameButton(
    gameViewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
)
{
    val view = LocalView.current

    Row(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text("Welcome", fontSize = 24.sp)

        Button(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                gameViewModel.reset()
            },
            shape = RoundedCornerShape(5.dp),
            elevation = ButtonDefaults.buttonElevation(5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            )
        ) {
            val iconWidth = Icons.Filled.Refresh.defaultWidth
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Reset",
                modifier = Modifier.size(iconWidth))
            Text("Reset Board")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FlingAidTheme {
        MainViewImplementation()
    }
}