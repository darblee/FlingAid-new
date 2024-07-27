package com.darblee.flingaid

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.darblee.flingaid.ui.screens.solverScreenBackPressed
import com.darblee.flingaid.ui.theme.SetColorTheme
import com.darblee.flingaid.ui.theme.ColorThemeOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ForcePortraitMode()

            val splashScreen = installSplashScreen()

            SetUpGameAudioOnAppStart()

            var colorTheme by remember {
                mutableStateOf(ColorThemeOption.System)
            }

            // LaunchedEffect is a mechanism to perform async tasks (suspend function) in a composable function,
            // It fetch data from setting persistent store without blocking the main UI thread.
            //
            // As soon as data is fetched. then 'currentColorThemeSetting' is updated, it will do
            // a recompose automatically
            //
            // THis tasks is cancelled automatically when composable is removed from the composition
            //
            // "true" in "LaunchEffect(true)" means run this once.
            //
            LaunchedEffect(true) {
                var keepSplashOnScreen = true
                splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

                colorTheme = PreferenceStore(applicationContext).readColorModeFromSetting()

                gSoundOn = PreferenceStore(applicationContext).readGameMusicOnFlagFromSetting()

                if (gSoundOn) {
                    gAudio_gameMusic.start()
                }

                keepSplashOnScreen = false // End the splash screen
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

    override fun onResume() {
        super.onResume()

        // Jetpack compose does not change theme for status bar
        when (this.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_NO -> {
                window.statusBarColor = resources.getColor(R.color.white, null)
            }

            else -> {
                window.statusBarColor = resources.getColor(R.color.black, null)
            }
        }
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
private fun SetUpGameAudioOnAppStart() {
    // Initiate the media player instance with the media file from the raw folder
    gAudio_doink = MediaPlayer.create(LocalContext.current, R.raw.doink)
    gAudio_victory = MediaPlayer.create(LocalContext.current, R.raw.victory)
    gAudio_gameMusic = MediaPlayer.create(LocalContext.current, R.raw.music)

    gAudio_gameMusic.isLooping = true

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // DisposableEffect is a tool that allows you to perform side effects in your composable
    // functions that need to be cleaned up when the composable leaves the composition.
    // Keys is used to control when the callback function is called.
    DisposableEffect(key1 = lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (gSoundOn) {
                        gAudio_gameMusic.start()
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (gSoundOn) {
                        gAudio_gameMusic.start()
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    gAudio_gameMusic.pause()
                    Log.i(Global.DEBUG_PREFIX, "Music paused")
                }

                else -> {
                    Log.i(Global.DEBUG_PREFIX, "$event event ignored")
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

    val screenTitle = stringResource(id = currentScreen.value.stringTitleResourceID)

    val context = LocalContext.current
    val preference = PreferenceStore(context)
    LaunchedEffect(key1 = true) {
        currentPlayerName = preference.readPlayerNameFomSetting()
    }

    CenterAlignedTopAppBar(
        navigationIcon = {
            when (currentScreen.value) {
                Screen.Home -> {
                    IconButton(onClick = { /* Do nothing */ })
                    { Icon(Icons.Filled.Home, contentDescription = "Home screen") }
                }

                Screen.Solver -> {
                    IconButton(onClick = { solverScreenBackPressed(context, navController) })
                    {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back to home screen"
                        )
                    }
                }

                Screen.Game -> {
                    IconButton(onClick = { navController.popBackStack() })
                    {
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
            gSoundOn
        )
    }
}

@Composable
private fun AboutDialogPopup(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .padding(25.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ball),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                )
                Text(
                    text = stringResource(id = R.string.version) + " : " + BuildConfig.BUILD_TIME,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 10.dp, start = 10.dp, end = 10.dp)
                )
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(
                        onClick = { onConfirmation() },
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.padding(end = 5.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.back)
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
    Dialog(onDismissRequest = { onDismissRequest() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .width(350.dp)
                .wrapContentHeight()
                .padding(25.dp),
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
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.Center
                )
                {
                    Button(onClick = { onConfirmation() })
                    {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.padding(end = 5.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.back),
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
    Row(
        modifier = Modifier.wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text("Music")

        // add weight modifier to the row composable to ensure
        // that the composable is measured after the other
        // composable is measured. This create space between
        // first item (left side) and second item (right side)
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

@Composable
private fun ColorThemeSetting(
    onColorThemeUpdated: (colorThemeType: ColorThemeOption) -> Unit,
    currentTheme: ColorThemeOption
) {
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
            text = "Color Theme", modifier = Modifier
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

private fun setGameMusic(on: Boolean) {
    if (on) {
        if (!gAudio_gameMusic.isPlaying) {
            gAudio_gameMusic.start()
        }
    } else {
        gAudio_gameMusic.pause()
    }
}

@Composable
@Preview(showBackground = true)
private fun ScreenPreview() {
    //   ExitAlertDialog(onDismiss = { /* TODO */ }, onExit = { /* TODO */ })
}