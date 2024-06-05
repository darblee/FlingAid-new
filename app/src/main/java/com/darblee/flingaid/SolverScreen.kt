package com.darblee.flingaid

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.util.Log
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.darblee.flingaid.ui.Direction
import com.darblee.flingaid.ui.GameState
import com.darblee.flingaid.ui.GameUiState
import com.darblee.flingaid.ui.GameViewModel
import com.darblee.flingaid.ui.Pos
import com.darblee.flingaid.ui.theme.ColorThemeOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@Composable
fun SolverScreen(
    modifier: Modifier = Modifier,
    screenRouteParams: Screen.Solver, onNavigateBack: () -> Unit)
{
    val gameViewModel: GameViewModel = viewModel()
    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),  // Height of the TopAppBar
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val uiState by gameViewModel.uiState.collectAsState()
        Log.i(Global.debugPrefix, "MainViewImplementation : Thinking status: ${uiState.state}")
        gBoardFile = File(LocalContext.current.filesDir, Global.boardFileName)

        gameViewModel.loadBallPositions(gBoardFile)  // Load balls from previous game save

        var findWinnableMoveButtonEnabled by remember { mutableStateOf(false) }
        findWinnableMoveButtonEnabled =
            ((gameViewModel.ballCount() > 1) && (!gameViewModel.foundWinnableMove()))

        var showWinnableMoveToUser by remember { mutableStateOf(false) }
        showWinnableMoveToUser =
            (uiState.foundWinningDirection != Direction.NO_WINNING_DIRECTION)

        if (uiState.needToDisplayNoWinnableToastMessage) {
            DisplayNoWinnableMoveToast()
            gameViewModel.noNeedToDisplayNoWinnableToastMessage()
        }

        DrawUpperBoxLogo(uiState, onNavigateBack)
        ControlButtons(
            gameViewModel,
            findWinnableMoveButtonEnabled,
            showWinnableMoveToUser,
            uiState
        )
        DrawFlingBoard(modifier, gameViewModel, uiState)
    }
}

@Composable
fun SetUpGameAudioOnAppStart()
{
//    gGameAudio =  MediaPlayer.create(applicationContext, R.raw.music)
    gGameAudio =  MediaPlayer.create(LocalContext.current, R.raw.music)
    gGameAudio.isLooping = true

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // DisposableEffect is a tool that allows you to perform side effects in your composable
    // functions that need to be cleaned up when the composable leaves the composition.
    // Keys is used to control when the callback function is called.
    DisposableEffect(key1 = lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (gSoundOn) gGameAudio.start()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (gSoundOn) gGameAudio.start()
                }
                Lifecycle.Event.ON_STOP -> {
                    gGameAudio.pause()
                    Log.i(Global.debugPrefix, "Music paused")
                }
                else -> {
                    Log.i(Global.debugPrefix, "$event event ignored")
                }
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }  // DisposableEffect
}

@Composable
fun ControlButtons(
    gameViewModel: GameViewModel = viewModel(),
    findWinnableMoveButtonEnabled: Boolean,
    showWinnableMoveToUser: Boolean,
    uiState: GameUiState
)
{
    val audio : MediaPlayer = MediaPlayer.create(LocalContext.current, R.raw.you_won)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        val context = LocalContext.current
        Button(
            onClick = {
                Log.i(Global.debugPrefix, ">>> Starting thinking : Button Pressed")
                if (showWinnableMoveToUser) {
                    // Make the actual move before find the next winnable move
                    gameViewModel.makeWinningMove(uiState)
                    gameViewModel.saveBallPositions(gBoardFile)
                }
                if (gameViewModel.ballCount() == 1) {
                    audio.start()
                    Toast.makeText(context, "You won!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.i(Global.debugPrefix, ">>> Looking for winnable move")
                    gameViewModel.findWinningMove(gameViewModel)
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
                    (uiState.state == GameState.NotThinking))
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
                gameViewModel.reset(gBoardFile)
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
fun DisplayNoWinnableMoveToast()
{
    val contextForToast = LocalContext.current

    Toast.makeText(
        contextForToast,
        "There is no winnable move",
        Toast.LENGTH_LONG
    ).show()
}

@Composable
fun DrawUpperBoxLogo(uiState: GameUiState,
                     onNavigateBack: () -> Unit)
{
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        horizontalArrangement = Arrangement.Center

    ) {
        Text(text = "Instruction")
        Box {
            if (uiState.state == GameState.NotThinking) {
                val imageModifier = Modifier
                    .size(150.dp)
                    .border(BorderStroke(1.dp, Color.Black))
                    .background(Color.Black)
                Image(
                    painter = painterResource(id = R.drawable.fling),
                    contentDescription = stringResource(id = R.string.app_name),
                    contentScale = ContentScale.Fit,
                    modifier = imageModifier
                )
            } else {
                PlaySearchAnimation(
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.Center)
                )

                // We track two level processing = level #1: 4 direction x level 2: 4 directions = 16
                val newPercentageValue: Float = (Global.ThinkingProgress.toFloat() /
                        (Global.totalProcessCount) * 100.0).toFloat()
                val percentComplete =
                    String.format(Locale.getDefault(), "%.1f%%", newPercentageValue)
                Text("Searching $percentComplete")
            }
        }
        Button(
            modifier = Modifier.width(75.dp),
            onClick = { onNavigateBack.invoke() }
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(id = R.string.back )
            )
        }
    }
}

/*
    Draw the Fling Game Board:
        - Grid
        - all the balls
        - winning arrow (if there is solution after "find winnable movable" submission
 */
@Composable
fun DrawFlingBoard(
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel = viewModel(),
    uiState: GameUiState)
{
    var gridSize by rememberSaveable {
        mutableFloatStateOf(0f)
    }

    val animate = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(Unit){
        animate.animateTo(targetValue = 1f, animationSpec =
        infiniteRepeatable(
            animation = tween(1000,easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
        )
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

        val ballImage = ImageBitmap.imageResource(id = R.drawable.ball)

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .padding(15.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            // For unknown reason, we can not use uiState.state
                            val thinkingStatus = gameViewModel.getThinkingStatus()
                            if (thinkingStatus == GameState.Thinking) {
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
                                    gameViewModel.toggleBallPosition(Pos(row, col))
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    gameViewModel.saveBallPositions(gBoardFile)
                                }
                            } // if thinkingStatus != GameState.thinking
                        }, // onTap
                    ) // detectTapGestures
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

            drawBalls(this, gameViewModel, gridSize, displayBallImage)

            if (gameViewModel.ballCount() > 1) {
                // Draw the winning arrow if there is a winning move identified
                if (gameViewModel.foundWinnableMove()) {
                    val moveCount = gameViewModel.getWinningMoveCount(uiState)
                    showWinningMove(this, gridSize, uiState, animate, moveCount, displayBallImage)
                }
            }
        }
    }
}

/*
 * Show Winning Move
 *   Show direction arrow
 *   Animate the ball movement
 */
fun showWinningMove(
    drawScope: DrawScope,
    gridSize: Float,
    uiState: GameUiState,
    animate: Animatable<Float, AnimationVector1D>,
    gWinningMoveCount: Int,
    displayBallImage: ImageBitmap
)
{
    with (drawScope) {
        // Log.i(Global.debugPrefix, "Winning Move exist with winning direction:  ${uiState.foundWinningDirection}")
        // Log.i(Global.debugPrefix, "Winning Move position is :  row = ${uiState.winningPosition.row}, col = ${uiState.winningPosition.col}")
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

        drawImage(displayArrow, topLeft =
        Offset(x = ((uiState.winningPosition.col) * gridSize),
            y = ((uiState.winningPosition.row * gridSize)))
        )

        translate(
            (xOffset) * animate.value,
            (yOffset) * animate.value
        ) {
            drawImage(
                image = displayBallImage, topLeft =
                Offset(
                    x = ((uiState.winningPosition.col) * gridSize),
                    y = ((uiState.winningPosition.row * gridSize))
                )
            )
        }
    }
}

// Draw all the balls in the provided canvas
fun drawBalls(
    drawScope: DrawScope,
    gameViewModel: GameViewModel,
    gridSize: Float,
    displayBallImage: ImageBitmap
)
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

fun drawGrid(drawScope: DrawScope,
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

@Composable
fun PlaySearchAnimation(modifier: Modifier)
{

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.find_animation))

    LottieAnimation(
        modifier = modifier,
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
}


@Composable
fun SettingPopup(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    onColorThemeUpdated: (colorThemeType: ColorThemeOption) -> Unit,
    currentTheme: ColorThemeOption,
    onPlayerNameUpdated: (newPlayerName: String) -> Unit,
    currentPlayerName: String,
    onSoundSettingUpdated: (soundOn : Boolean) -> Unit,
    currentSoundSetting: Boolean
)
{
    Dialog(onDismissRequest = { onDismissRequest() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .width(275.dp)
                .wrapContentHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PlayerNameSetting(currentPlayerName, onPlayerNameUpdated)
                MusicSetting(onSoundSettingUpdated, currentSoundSetting)
                ColorThemeSetting(onColorThemeUpdated, currentTheme)

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(
                        modifier = Modifier.width(125.dp),
                        onClick = { onConfirmation() },

                        ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(id = R.string.back ),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } // ColumnScope
    }
}

@Composable
fun PlayerNameSetting(
    currentPlayerName: String,
    onPlayerNameUpdated: (newPlayerName: String) -> Unit)
{
    val preference = PreferenceStore(LocalContext.current)
    Row {
        var rawText by remember {
            mutableStateOf(currentPlayerName)
        }
        OutlinedTextField(
            value = rawText,
            onValueChange =
            { newRawText ->
                rawText = newRawText
                onPlayerNameUpdated(newRawText) // Call the lambda function to update new player name

                CoroutineScope(Dispatchers.IO).launch {
                    preference.savePlayerNameToSetting(newRawText)
                }
            },
            label = { Text(text = stringResource(id = R.string.player_name))},
            singleLine = true,
            leadingIcon =
            {
                Icon(imageVector = Icons.Filled.Person, contentDescription = stringResource(id = R.string.player_name))
            },
            trailingIcon =
            {
                IconButton(onClick =
                {
                    rawText = ""
                    onPlayerNameUpdated("")
                    CoroutineScope(Dispatchers.IO).launch {
                        preference.savePlayerNameToSetting("")
                    }
                })
                {
                    Icon(imageVector = Icons.Filled.Clear, contentDescription = stringResource(id = R.string.clear_name))
                }
            }
        )
    }
}

@Composable
fun MusicSetting(onSoundSettingUpdated: (soundOn: Boolean) -> Unit, currentSoundSetting: Boolean)
{
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text("Music")

        Spacer(modifier = Modifier.weight(1f))

        var musicSwitch by remember {
            mutableStateOf(currentSoundSetting)
        }

        val icon: (@Composable () -> Unit)? = if (gSoundOn) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        } else null

        Switch(
            modifier = Modifier.padding(8.dp),
            checked = musicSwitch,
            onCheckedChange = { isCheckStatus ->
                musicSwitch = isCheckStatus
                onSoundSettingUpdated(isCheckStatus)
            },
            thumbContent = icon
        )
    }
}

fun setGameMusic(on: Boolean)
{
    if (on) {
        if (!gGameAudio.isPlaying) {
            gGameAudio.start()
        }
    } else {
        gGameAudio.pause()
    }
}

