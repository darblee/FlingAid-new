package com.darblee.flingaid

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.darblee.flingaid.R.raw
import com.darblee.flingaid.ui.Direction
import com.darblee.flingaid.ui.GameState
import com.darblee.flingaid.ui.GameUiState
import com.darblee.flingaid.ui.GameViewModel
import com.darblee.flingaid.ui.Pos
import com.darblee.flingaid.ui.PreferenceStore
import com.darblee.flingaid.ui.theme.SetColorTheme
import com.darblee.flingaid.ui.theme.ColorThemeOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess

// Declare these bitmaps once as it will be reused on every recompose
private lateinit var gUpArrowBitmap : Bitmap
private lateinit var gDownArrowBitmap : Bitmap
private lateinit var gLeftArrowBitmap : Bitmap
private lateinit var gRightArrowBitmap : Bitmap
private lateinit var gDisplayBallImage : ImageBitmap

private lateinit var gGameAudio : MediaPlayer

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var keepSplashOnScreen = true
        val delay = 1000L
        val splashScreen = installSplashScreen()

        // Keep the splashscreen on-screen for specific period
        splashScreen.setKeepOnScreenCondition{ keepSplashOnScreen }
        Handler(Looper.getMainLooper()).postDelayed({ keepSplashOnScreen = false }, delay)

        setContent {
            var currentColorThemeSetting by remember {
                mutableStateOf(ColorThemeOption.System)
            }

            SetColorTheme(currentColorThemeSetting) {
                SetupAllBitMapImages()
                SetUpGameAudio()
                ForcePortraitMode()

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorScheme.background
                ) {
                    MainViewImplementation(onColorThemeUpdated = { newColorThemeSetting ->
                        currentColorThemeSetting = newColorThemeSetting
                    })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Jetpack compose does not change theme for status bar
        when (this.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_NO -> {
                window.statusBarColor = resources.getColor(R.color.white, null)
            } else -> {
                window.statusBarColor = resources.getColor(R.color.black, null)
            }
        }
    }

    @Composable
    fun SetupAllBitMapImages() {

        Log.i(Global.debugPrefix, "Initializing all the images")

        gUpArrowBitmap = ImageBitmap.imageResource(R.drawable.up).asAndroidBitmap()

        val matrix = Matrix()
        matrix.postRotate(90F)

        gRightArrowBitmap = Bitmap.createBitmap(
            gUpArrowBitmap,
            0,
            0,
            gUpArrowBitmap.width,
            gUpArrowBitmap.height,
            matrix,
            true
        )
        gDownArrowBitmap = Bitmap.createBitmap(
            gRightArrowBitmap,
            0,
            0,
            gRightArrowBitmap.width,
            gRightArrowBitmap.height,
            matrix,
            true
        )
        gLeftArrowBitmap = Bitmap.createBitmap(
            gDownArrowBitmap,
            0,
            0,
            gDownArrowBitmap.width,
            gDownArrowBitmap.height,
            matrix,
            true
        )

        val ballImage = ImageBitmap.imageResource(id = R.drawable.ball)
        gDisplayBallImage = Bitmap.createScaledBitmap(ballImage.asAndroidBitmap(),
            160, 160, false).asImageBitmap()
    }

    @Composable
    fun SetUpGameAudio() {
        gGameAudio =  MediaPlayer.create(applicationContext, raw.music)
        gGameAudio.isLooping = true

        // We are passing Unit as a parameter that means we only want to call this suspend
        // block when the first time a user enters the screen
        LaunchedEffect(key1 = Unit) {
            Global.gameMusicOn = PreferenceStore(applicationContext).getGameMusicOnFlag()
            if (Global.gameMusicOn) gGameAudio.start()
        }

        val lifecycleOwner = LocalLifecycleOwner.current

        // DisposableEffect is a tool that allows you to perform side effects in your composable
        // functions that need to be cleaned up when the composable leaves the composition.
        // Keys is used to control when the callback function is called.
        DisposableEffect(key1 = lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        if (Global.gameMusicOn) gGameAudio.start()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        if (Global.gameMusicOn) gGameAudio.start()
                    }
                    Lifecycle.Event.ON_STOP -> {
                        gGameAudio.pause()
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

    @SuppressLint("SourceLockedOrientationActivity")
    @Composable
    fun ForcePortraitMode() {
        val activity = LocalContext.current as Activity
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

private lateinit var gBoardFile : File

@Composable
fun MainViewImplementation(
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel = viewModel(),
    onColorThemeUpdated: (colorThemeSetting: ColorThemeOption) -> Unit) {
    val uiState by gameViewModel.uiState.collectAsState()
    Log.i(Global.debugPrefix, "MainViewImplementation : Recompose Thinking status: ${uiState.state}")
    gBoardFile = File(LocalContext.current.filesDir, Global.boardFileName)

    gameViewModel.loadBallPositions(gBoardFile)  // Load balls from previous game save

    Scaffold (
        topBar = { FlingAidTopAppBar(onColorThemeUpdated = onColorThemeUpdated) }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
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

            DrawUpperBoxLogo(uiState)
            ControlButtons(
                gameViewModel,
                findWinnableMoveButtonEnabled,
                showWinnableMoveToUser,
                uiState
            )
            DrawFlingBoard(modifier, gameViewModel, uiState)
        } // Column
    }
}

@Composable
fun DisplayNoWinnableMoveToast() {
    val contextForToast = LocalContext.current

    Toast.makeText(
        contextForToast,
        "There is no winnable move",
        Toast.LENGTH_LONG
    ).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlingAidTopAppBar(onColorThemeUpdated: (colorThemeSetting: ColorThemeOption) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showAboutDialogBox by remember { mutableStateOf(false) }
    var showSettingDialogBox by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorScheme.primaryContainer,
            titleContentColor = colorScheme.primary,
        ),

        modifier = Modifier.height(40.dp),
        title =
        {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Fling Aid")
            }
        },
        actions = {
            IconButton(
                onClick = { menuExpanded = !menuExpanded },
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(text = "About") },
                    onClick = {
                        showAboutDialogBox = true
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = "Setting...") },
                    onClick = {
                        showSettingDialogBox = true
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Exit") },
                    onClick = {
                        menuExpanded = false

                        // Android will eventually kill the entire process when it gets around to it.
                        // You have no control over this (and that is intentional).
                        exitProcess(1)
                    }
                )
            } // DropdownMenu
        }
    )

    if (showAboutDialogBox) {
        AboutDialogPopup(
            onDismissRequest = { showAboutDialogBox = false },
            onConfirmation = { showAboutDialogBox = false },
        )
    }

    if (showSettingDialogBox) {
        SettingPopup(
            onDismissRequest = { showSettingDialogBox = false },
            onColorThemeUpdated = onColorThemeUpdated
        )
    }
}

@Composable
fun AboutDialogPopup(onDismissRequest: () -> Unit, onConfirmation: () -> Unit) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(275.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fling),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(100.dp)
                )
                Text(
                    text = stringResource(id = R.string.version) +
                             " : " + BuildConfig.BUILD_TIME,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(12.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(
                        modifier = Modifier.width(150.dp),
                        onClick = { onConfirmation() }
                    ) {
                        Text(
                            text = stringResource(id = R.string.confirm ),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingPopup(onDismissRequest: () -> Unit, onColorThemeUpdated: (colorThemeType: ColorThemeOption) -> Unit) {
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
                MusicSetting()
                ColorThemeSetting(onColorThemeUpdated)
            }
        } // ColumnScope
    }
}

@Composable
fun MusicSetting() {
    val preference = PreferenceStore(LocalContext.current)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text("Music")

        Spacer(modifier = Modifier.weight(1f))

        var musicSwitch by remember {
            mutableStateOf(Global.gameMusicOn)
        }

        val icon: (@Composable () -> Unit)? = if (Global.gameMusicOn) {
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
                Global.gameMusicOn = musicSwitch

                setGameMusic(Global.gameMusicOn )
                CoroutineScope(Dispatchers.IO).launch {
                    preference.saveGameMusicFlag((Global.gameMusicOn))
                }
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

@Composable
fun ColorThemeSetting(onColorThemeUpdated: (colorThemeType: ColorThemeOption) -> Unit) {
    Row (
        modifier = Modifier
            .border(1.dp, colorScheme.outline, shape = RoundedCornerShape(5.dp))
            .wrapContentWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val ColorThemeOptionsStringValues
        = listOf(ColorThemeOption.System.toString(), ColorThemeOption.Light.toString(), ColorThemeOption.Dark.toString())
        val (selectedOption, onOptionSelected) = remember {
            mutableStateOf(ColorThemeOptionsStringValues[0])
        }
        Text(text = "Color Theme", modifier = Modifier
            .padding(5.dp)
            .wrapContentWidth())

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .wrapContentWidth()
                .selectableGroup()
                .padding(5.dp)) {
            ColorThemeOptionsStringValues.forEach { text ->
                Row(
                    Modifier
                        .selectable(
                            selected = (text == selectedOption),
                            onClick = {
                                onOptionSelected(text)  // This make this button get selected
                                val newSelectedTheme =
                                    when (text) {
                                        ColorThemeOption.System.toString() -> ColorThemeOption.System
                                        ColorThemeOption.Light.toString() -> ColorThemeOption.Light
                                        else -> ColorThemeOption.Dark
                                    }
                                onColorThemeUpdated(newSelectedTheme)
                            },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),  // Make the entire row selectable
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = (text == selectedOption),
                        onClick = null  // null recommended for accessibility with screenreaders
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .wrapContentWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun DrawUpperBoxLogo(uiState: GameUiState)
{
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
            PlaySearchAnimation(modifier = Modifier
                .size(150.dp)
                .align(Alignment.Center))

            // We track two level processing = level #1: 4 direction x level 2: 4 directions = 16
            val newPercentageValue : Float = (Global.ThinkingProgress.toFloat() /
                    (Global.totalProcessCount) * 100.0).toFloat()
            val percentComplete = String.format("%.1f%%", newPercentageValue)
            Text("Searching $percentComplete")
        }
    }

}

@Composable
fun ControlButtons(
    gameViewModel: GameViewModel = viewModel(),
    findWinnableMoveButtonEnabled: Boolean,
    showWinnableMoveToUser: Boolean,
    uiState: GameUiState,
)
{
    val audio : MediaPlayer = MediaPlayer.create(LocalContext.current, raw.you_won)

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
                containerColor = colorScheme.primary,
                contentColor = colorScheme.primaryContainer
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
                containerColor = colorScheme.errorContainer,
                contentColor = colorScheme.error
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
    uiState: GameUiState,
    ) {
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
            .fillMaxWidth()
            .aspectRatio(Global.MaxColSize.toFloat() / Global.MaxRowSize.toFloat())
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        val view = LocalView.current
        val lineColor = colorScheme.outline

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .padding(15.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            // For unknown reason, we can not use uistate.state
                            val thinkingStatus =
                                gameViewModel.getThinkingStatus()
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
                        } // onTap
                    ) // detectTapGestures
                }  // .pointerInput
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val gridSizeWidth = (canvasWidth / (Global.MaxColSize))
            val gridSizeHeight = (canvasHeight / (Global.MaxRowSize))

            gridSize = if (gridSizeWidth > gridSizeHeight) gridSizeHeight else gridSizeWidth

            drawGrid(this, gridSize, lineColor)
            drawBalls(this, gameViewModel, gridSize)

            if (gameViewModel.ballCount() > 1) {
                // Draw the winning arrow if there is a winning move identified
                if (gameViewModel.foundWinnableMove()) {
                    val moveCount = gameViewModel.getWinningMoveCount(uiState)
                    showWinningMove(this, gridSize, uiState, animate, moveCount)
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
    gWinningMoveCount: Int
) {
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
            Offset(x = ((uiState.winningPosition.col) * gridSize) - 3f,
                y = ((uiState.winningPosition.row * gridSize) -3f))
        )

        translate(
            (xOffset) * animate.value,
            (yOffset) * animate.value
        ) {
            drawImage(
                image = gDisplayBallImage, topLeft =
                Offset(
                    x = ((uiState.winningPosition.col) * gridSize) - 10f,
                    y = ((uiState.winningPosition.row * gridSize) - 10f)
                )
            )
        }
    }
}

// Draw all the balls in the provided canvas
fun drawBalls(
    drawScope: DrawScope,
    gameViewModel: GameViewModel,
    gridSize: Float
) {
    // Draw all the balls
    with (drawScope) {
        gameViewModel.ballPositionList().forEach { pos ->
            drawImage(
                image = gDisplayBallImage,
                topLeft = Offset(
                    (pos.col * gridSize)-10,
                    (pos.row * gridSize)-10
                )
            )
        }
    }
}

fun drawGrid(drawScope: DrawScope, gridSize: Float, lineColor: Color) {
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
fun PlaySearchAnimation(modifier: Modifier) {

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(raw.find_animation))

    LottieAnimation(
        modifier = modifier,
        composition = composition,
        iterations = LottieConstants.IterateForever
    )
}