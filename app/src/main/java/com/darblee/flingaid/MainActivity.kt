package com.darblee.flingaid

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.pm.ActivityInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.darblee.flingaid.ui.theme.SetColorTheme
import com.darblee.flingaid.ui.theme.ColorThemeOption
import com.darblee.flingaid.utilities.click
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess


private lateinit var gAudio_gameMusic: MediaPlayer

lateinit var gAudioManager: AudioManager
lateinit var gFocusRequest: AudioFocusRequest

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val playbackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        // Media player is handled according to the change in the focus which Android system grants for
        val audioFocusChangeListener =
            AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    // Permanent loss of audio focus
                    // Pause playback immediately
                    mediaController.transportControls.pause()
                    gAudio_gameMusic.release()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    // Pause playback
                    gAudio_gameMusic.pause()
                    gAudio_gameMusic.seekTo(0)
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    gAudio_gameMusic.pause()
                    gAudio_gameMusic.seekTo(0)
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // Your app has been granted audio focus again
                    gAudio_gameMusic.start()
                }
            }
        }

        gFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(playbackAttributes!!)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()

        setContent {
            ForcePortraitMode()

            val splashScreen = installSplashScreen()

            SetUpGameAudioOnAppStart(playbackAttributes)

            var colorTheme by remember {
                mutableStateOf(ColorThemeOption.System)
            }

            // LaunchedEffect is a mechanism to perform async tasks (suspend function) in a composable function,
            // It fetch data from setting persistent store without blocking the main UI thread.
            //
            // As soon as data is fetched. then 'currentColorThemeSetting' is updated, it will do
            // a recompose automatically
            //
            // This tasks is cancelled automatically when composable is removed from the composition
            //
            // "true" in "LaunchEffect(true)" means run this once.
            //
            LaunchedEffect(Unit) {
                var keepSplashOnScreen = true
                splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

                colorTheme = PreferenceStore(applicationContext).readColorModeFromSetting()

                gSoundOn = PreferenceStore(applicationContext).readGameMusicOnFlagFromSetting()
                Log.i(Global.DEBUG_PREFIX, "Read sound setting from file: gSoundOn = $gSoundOn")

                keepSplashOnScreen = false // End the splash screen

                if (gSoundOn) {
                    Log.i(
                        Global.DEBUG_PREFIX,
                        "MAIN: Request system to get permission to play music"
                    )
                    val res = gAudioManager.requestAudioFocus(gFocusRequest)
                    if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        Log.i(Global.DEBUG_PREFIX, "Permission granted")

                        gAudio_gameMusic.start()
                    }
                }
            }

            SetColorTheme(colorTheme)
            {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorScheme.background
                ) {
                    MainViewImplementation(
                        currentTheme = colorTheme,

                        onColorThemeUpdated = { newColorThemeSetting ->
                            colorTheme = newColorThemeSetting

                            // Save the Color Theme setting
                            CoroutineScope(Dispatchers.IO).launch {
                                PreferenceStore(applicationContext).saveColorModeToSetting(
                                    newColorThemeSetting
                                )
                            }
                        }  // onColorThemeUpdated
                    )  // MainViewImplementation
                }  // Surface
            }  // SetColorTheme()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gAudio_gameMusic.release()
        gAudio_doink.release()
        gAudio_victory.release()
        gAudio_swish.release()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Composable
    fun ForcePortraitMode() {
        val activity = LocalContext.current as Activity
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

@Composable
private fun MainViewImplementation(
    onColorThemeUpdated: (colorThemeSetting: ColorThemeOption) -> Unit,
    currentTheme: ColorThemeOption
) {
    val currentScreen = remember { mutableStateOf<Screen>(Screen.Home) }

    val navController = rememberNavController()

    Scaffold(
        topBar = {
            FlingAidTopAppBar(
                onColorThemeUpdated,
                currentTheme,
                navController,
                currentScreen
            )
        }
    ) { contentPadding ->
        SetUpNavGraph(navController = navController, contentPadding, currentScreen)
    }
}

@Composable
private fun SetUpGameAudioOnAppStart(playbackAttributes: AudioAttributes) {

    gAudio_doink = MediaPlayer.create(LocalContext.current, R.raw.doink)
    gAudio_doink.setAudioAttributes(playbackAttributes)

    gAudio_victory = MediaPlayer.create(LocalContext.current, R.raw.victory)
    gAudio_victory.setAudioAttributes(playbackAttributes)

    gAudio_gameMusic = MediaPlayer.create(LocalContext.current, R.raw.music2)
    gAudio_gameMusic.setAudioAttributes(playbackAttributes)

    gAudio_swish = MediaPlayer.create(LocalContext.current, R.raw.swish)
    gAudio_swish.setAudioAttributes(playbackAttributes)

    gAudio_gameMusic.isLooping = true

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // DisposableEffect is a tool that allows you to perform side effects in your composable
    // functions that need to be cleaned up when the composable leaves the composition.
    // Keys is used to control when the callback function is called.
    DisposableEffect(key1 = lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    Log.i(Global.DEBUG_PREFIX, "Detected Lifecycle Event - ON_START")

                    if (gSoundOn) {
                        val res = gAudioManager.requestAudioFocus(gFocusRequest)
                        Log.i(Global.DEBUG_PREFIX, "Request permission to play music")
                        if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                            Log.i(Global.DEBUG_PREFIX, "Permission granted")
                            gAudio_gameMusic.start()
                        }
                        Log.i(Global.DEBUG_PREFIX, "Detected Lifecycle Event - ON_START : Music started")
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    Log.i(Global.DEBUG_PREFIX, "Detected Lifecycle Event. gSoundOn - $gSoundOn - ON_RESUME")

                    if (gSoundOn) {
                        val res = gAudioManager.requestAudioFocus(gFocusRequest)
                        Log.i(Global.DEBUG_PREFIX, "Request permission to play music")
                        if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                            Log.i(Global.DEBUG_PREFIX, "Permission granted")

                            gAudio_gameMusic.start()
                        }
                        Log.i(Global.DEBUG_PREFIX, "Detected Lifecycle Event - ON_RESUME : Music started")

                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    gAudio_gameMusic.pause()
                    Log.i(Global.DEBUG_PREFIX, "Detected Lifecycle Event - ON_STOP : Music paused")
                }

                Lifecycle.Event.ON_PAUSE -> {
                    gAudio_gameMusic.pause()
                    Log.i(Global.DEBUG_PREFIX, "Detected Lifecycle Event - ON_PAUSE : Music paused")
                }

                else -> {
                    Log.i(Global.DEBUG_PREFIX, "Detected Lifecycle Event $event -  ignored")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlingAidTopAppBar(
    onColorThemeUpdated: (colorThemeSetting: ColorThemeOption) -> Unit,
    currentTheme: ColorThemeOption,
    navController: NavHostController,
    currentScreen: MutableState<Screen>
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showAboutDialogBox by remember { mutableStateOf(false) }
    var showSettingDialogBox by remember { mutableStateOf(false) }
    var currentPlayerName by remember { mutableStateOf("") }
    var loadPlayerNameOnce by remember { mutableStateOf( false) }

    val screenTitle = stringResource(id = currentScreen.value.stringTitleResourceID)

    val context = LocalContext.current
    val preference = PreferenceStore(context)

    if (!loadPlayerNameOnce) {
        LaunchedEffect(Unit) {
            currentPlayerName = preference.readPlayerNameFomSetting()
        }
        loadPlayerNameOnce = true
    }

    CenterAlignedTopAppBar(
        navigationIcon = {
            when (currentScreen.value) {
                Screen.Home -> {
                    IconButton(onClick = { /* Do nothing */ })
                    { Icon(Icons.Filled.Home, contentDescription = "Home screen") }
                }

                Screen.Solver -> {
                    IconButton(onClick =
                    {
                        Log.i(
                            Global.DEBUG_PREFIX,
                            "Detect back-press in Solver Screen. Mode is ${gSolverViewModel.uiState.value.mode}"
                        )
                        if (gSolverViewModel.canExitSolverScreen()) navController.popBackStack()
                    }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back to home screen"
                        )
                    }
                }

                Screen.Game -> {
                    IconButton(onClick =
                    {
                        Log.i(
                            Global.DEBUG_PREFIX,
                            "Detect back-press in Game Screen. Mode is ${gGameViewModel.gameUIState.value.mode}"
                        )
                        if (gGameViewModel.canExitGameScreen()) navController.popBackStack()
                    }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back to home screen"
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorScheme.primaryContainer,
            titleContentColor = colorScheme.primary,
        ),
        modifier = Modifier.height(Global.TopAppBarHeight),
        title = {
            val titleText =
                if (currentPlayerName == "") screenTitle else "$screenTitle : $currentPlayerName"
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(titleText, style = MaterialTheme.typography.titleLarge)
            }
        },
        actions = {
            IconButton(
                onClick = { menuExpanded = !menuExpanded },
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Info, contentDescription = "About")
                    },
                    text = { Text(text = "About") },
                    onClick = {
                        showAboutDialogBox = true
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    text = { Text(text = "Setting...") },
                    onClick = {
                        showSettingDialogBox = true
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Exit"
                        )
                    },
                    text = { Text("Exit") },
                    onClick = {
                        menuExpanded = false

                        // Android will eventually kill the entire process when it gets around to it.
                        // You have no control over this (and that is intentional).
                        exitProcess(1)
                    }
                )
            } // DropdownMenu
        }  // action
    )  // CenterAlignTopBar

    if (showAboutDialogBox) {
        AboutDialogPopup(
            onDismissRequest = { showAboutDialogBox = false },
            onConfirmation = { showAboutDialogBox = false },
        )
    }

    if (showSettingDialogBox) {

        SettingPopup(
            onDismissRequest = { showSettingDialogBox = false },
            onConfirmation = { showSettingDialogBox = false },
            onColorThemeUpdated = onColorThemeUpdated,
            currentTheme,
            onPlayerNameUpdated = { newPlayerName ->
                currentPlayerName = newPlayerName
            },
            currentPlayerName,
            onSoundSettingUpdated = { newSoundSetting ->
                gSoundOn = newSoundSetting

                setGameMusic(newSoundSetting)

                CoroutineScope(Dispatchers.IO).launch {
                    preference.saveGameMusicFlagToSetting(gSoundOn)
                }
            },
            currentSoundSetting = gSoundOn
        )
    }
}

@Composable
private fun AboutDialogPopup(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit
) {
    Dialog(
        onDismissRequest = { onDismissRequest() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .width(200.dp)
                .padding(0.dp)
                .height(IntrinsicSize.Min)
                .border(0.dp, color = colorScheme.outline, shape = RoundedCornerShape(20.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Column(
                Modifier.fillMaxWidth(),
            ) {
                Row {
                    Column(
                        Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically)) {
                        Image(
                            painter = painterResource(id = R.drawable.ball),
                            contentDescription = "Game",
                            contentScale = ContentScale.Fit
                        )
                    }
                    Column(Modifier.weight(3f)) {
                        Text(
                            text = stringResource(id = R.string.version),
                            color = colorScheme.primary,
                            modifier = Modifier
                                .padding(2.dp, 4.dp, 2.dp, 0.dp)
                                .align(Alignment.CenterHorizontally)
                                .fillMaxWidth(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = BuildConfig.BUILD_TIME,
                            color = colorScheme.primary,
                            modifier = Modifier
                                .padding(4.dp, 0.dp, 4.dp, 2.dp)
                                .align(Alignment.CenterHorizontally)
                                .fillMaxWidth(),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .width(1.dp), color = colorScheme.outline
                )
                Row(Modifier.padding(top = 0.dp)) {
                    TextButton(
                        onClick = { onConfirmation() },
                        Modifier
                            .fillMaxWidth()
                            .padding(0.dp)
                            .weight(1F)
                            .border(0.dp, color = Color.Transparent)
                            .height(48.dp),
                        elevation = ButtonDefaults.elevatedButtonElevation(0.dp, 0.dp),
                        shape = RoundedCornerShape(0.dp),
                        contentPadding = PaddingValues()
                    ) {
                        Text(
                            text = stringResource(id = R.string.OK),
                            color = colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingPopup(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    onColorThemeUpdated: (colorThemeType: ColorThemeOption) -> Unit,
    currentTheme: ColorThemeOption,
    onPlayerNameUpdated: (newPlayerName: String) -> Unit,
    currentPlayerName: String,
    onSoundSettingUpdated: (soundOn: Boolean) -> Unit,
    currentSoundSetting: Boolean
) {
    Dialog(
        onDismissRequest = { onDismissRequest() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(350.dp)
                .wrapContentHeight()
                .padding(25.dp)
                .border(
                    0.dp,
                    color = colorScheme.outline,
                    shape = RoundedCornerShape(16.dp)
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
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
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = { onConfirmation() })
                    {
                        Text(
                            text = stringResource(id = R.string.OK),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } // ColumnScope
    }
}

@Composable
private fun PlayerNameSetting(
    currentPlayerName: String,
    onPlayerNameUpdated: (newPlayerName: String) -> Unit
) {
    val preference = PreferenceStore(LocalContext.current)
    Row {
        var rawText by remember {
            mutableStateOf(currentPlayerName)
        }
        OutlinedTextField(
            value = rawText,
            onValueChange =
            { newRawText ->
                rawText = newRawText.trimStart() // Remove leading spaces
                onPlayerNameUpdated(rawText) // Call the lambda function to update new player name

                CoroutineScope(Dispatchers.IO).launch {
                    preference.savePlayerNameToSetting(rawText)
                }
            },
            label = { Text(text = stringResource(id = R.string.player_name)) },
            singleLine = true,
            leadingIcon =
            {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = stringResource(id = R.string.player_name)
                )
            },
            trailingIcon =
            {
                IconButton(
                    onClick = {
                        rawText = ""
                        onPlayerNameUpdated("")
                        CoroutineScope(Dispatchers.IO).launch {
                            preference.savePlayerNameToSetting("")
                        }
                    })
                {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(id = R.string.clear_name)
                    )
                }
            }
        )
    }
}

@Composable
private fun MusicSetting(
    onSoundSettingUpdated: (soundOn: Boolean) -> Unit,
    currentSoundSetting: Boolean
) {
    val view = LocalView.current

    // Declare an audio manager
    val audioManager = LocalContext.current.getSystemService(AUDIO_SERVICE) as AudioManager

    var musicSwitch by remember {
        mutableStateOf(currentSoundSetting)
    }

    Row(
        modifier = Modifier.
            wrapContentWidth()
            .border(1.dp, colorScheme.outline, shape = RoundedCornerShape(5.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(stringResource(R.string.music),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(10.dp))

        // add weight modifier to the row composable to ensure
        // that the composable is measured after the other
        // composable is measured. This create space between
        // first item (left side) and second item (right side)
        Spacer(modifier = Modifier.weight(1f))

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
                view.click()
                musicSwitch = isCheckStatus
                onSoundSettingUpdated(isCheckStatus)
            },
            thumbContent = icon
        )
    }
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .border(1.dp, colorScheme.outline, shape = RoundedCornerShape(5.dp)),
        verticalAlignment = Alignment.CenterVertically,
        )
    {
        val color = if (musicSwitch) MaterialTheme.typography.bodyMedium.color else Color.Gray
        Text("Volume",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            modifier = Modifier.padding(10.dp))
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = {
                view.click()
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND) },
            enabled = musicSwitch,
            modifier = Modifier.padding(5.dp)
            )
        {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Increase volume")
        }
        IconButton(
            onClick = {
                view.click()
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND) },
            enabled = musicSwitch
        )
        {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Decrease volume")
        }
    }
}

@Composable
private fun ColorThemeSetting(
    onColorThemeUpdated: (colorThemeType: ColorThemeOption) -> Unit,
    currentTheme: ColorThemeOption
) {
    val view = LocalView.current

    Row(
        modifier = Modifier
            .border(1.dp, colorScheme.outline, shape = RoundedCornerShape(5.dp))
            .wrapContentWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val colorThemeOptionsStringValues = listOf(
            ColorThemeOption.System.toString(),
            ColorThemeOption.Light.toString(),
            ColorThemeOption.Dark.toString()
        )

        val (selectedOption, onOptionSelected) = remember {
            // Make the initial selection match the global color theme
            // at the start of opening the Theme setting dialog box
            mutableStateOf(currentTheme.toString())
        }
        Text(
            text = "Color Theme",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(5.dp)
                .wrapContentWidth()
        )

        // add weight modifier to the row composable to ensure
        // that the composable is measured after the other
        // composable is measured. This create space between
        // first item (left side) and second item (right side)
        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .wrapContentWidth()
                .selectableGroup()
                .padding(5.dp)
        ) {
            colorThemeOptionsStringValues.forEach { curColorString ->
                Row(
                    Modifier
                        .selectable(
                            selected = (curColorString == selectedOption),
                            onClick = {
                                view.click()
                                onOptionSelected(curColorString)  // This make this button get selected
                                val newSelectedTheme =
                                    when (curColorString) {
                                        ColorThemeOption.System.toString() -> ColorThemeOption.System
                                        ColorThemeOption.Light.toString() -> ColorThemeOption.Light
                                        else -> ColorThemeOption.Dark
                                    }

                                // Calls the lambda function that does the actual Color theme change to the app
                                onColorThemeUpdated(newSelectedTheme)
                            },
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = (curColorString == selectedOption),
                        onClick = null  // null recommended for accessibility with ScreenReaders
                    )
                    Text(
                        text = curColorString,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .wrapContentWidth()
                    )
                }
            }
        }
    }
}

private fun setGameMusic(on: Boolean) {
    if (on) {
        if (!gAudio_gameMusic.isPlaying) {
            val res = gAudioManager.requestAudioFocus(gFocusRequest)
            Log.i(Global.DEBUG_PREFIX, "SetGameMusic: Request permission to play music")
            if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.i(Global.DEBUG_PREFIX, "Permission granted")
                gAudio_gameMusic.start()
            }
        }
    } else {
        gAudio_gameMusic.pause()
    }
}

@Composable
@Preview(showBackground = true)
private fun ScreenPreview() {
    SettingPopup(
        onDismissRequest = { /* TODO */ }, { }, {},
        ColorThemeOption.Dark, {}, "David", {}, true
        )

}
