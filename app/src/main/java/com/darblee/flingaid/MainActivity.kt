package com.darblee.flingaid

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
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
import androidx.navigation.compose.rememberNavController
import com.darblee.flingaid.ui.theme.SetColorTheme
import com.darblee.flingaid.ui.theme.ColorThemeOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

// Declare these bitmaps once as it will be reused on every recompose
lateinit var gUpArrowBitmap : Bitmap
lateinit var gDownArrowBitmap : Bitmap
lateinit var gLeftArrowBitmap : Bitmap
lateinit var gRightArrowBitmap : Bitmap

private lateinit var gGameAudio : MediaPlayer

private var gSoundOn = false

class MainActivity : ComponentActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Log.i(Global.debugPrefix, "Recompose: SetContent")
            ForcePortraitMode()

            val splashScreen = installSplashScreen()

            SetupAllBitMapImagesOnAppStart()
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
                splashScreen.setKeepOnScreenCondition{ keepSplashOnScreen }

                colorTheme = PreferenceStore(applicationContext).readColorModeFromSetting()

                gSoundOn = PreferenceStore(applicationContext).readGameMusicOnFlagFromSetting()
                if (gSoundOn) gGameAudio.start()

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
                                PreferenceStore(applicationContext).saveColorModeToSetting(newColorThemeSetting)
                            }
                        }
                    )
                }
            }  // SetColorTheme()
        }
    }

    override fun onResume()
    {
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

    @SuppressLint("SourceLockedOrientationActivity")
    @Composable
    fun ForcePortraitMode()
    {
        val activity = LocalContext.current as Activity
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

@Composable
private fun SetupAllBitMapImagesOnAppStart()
{
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
}

@Composable
private fun SetUpGameAudioOnAppStart()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlingAidTopAppBar(
    onColorThemeUpdated: (colorThemeSetting: ColorThemeOption) -> Unit,
    currentTheme: ColorThemeOption,
    screenTitle: String
)
{
    var menuExpanded by remember { mutableStateOf(false) }
    var showAboutDialogBox by remember { mutableStateOf(false) }
    var showSettingDialogBox by remember { mutableStateOf(false) }
    var currentPlayerName by remember { mutableStateOf("") }

    val preference = PreferenceStore(LocalContext.current)
    LaunchedEffect(key1 = true) {
        currentPlayerName = preference.readPlayerNameFomSetting()
    }

    Log.i(Global.debugPrefix, "Recompose CenterAlignedTopAppBar")

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorScheme.primaryContainer,
            titleContentColor = colorScheme.primary,
        ),

        modifier = Modifier.height(Global.TopAppBarHeight),
        title =
        {
            val titleText = if (currentPlayerName == "") screenTitle else "$screenTitle : $currentPlayerName"
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
private fun MainViewImplementation(
    onColorThemeUpdated: (colorThemeSetting: ColorThemeOption) -> Unit,
    currentTheme: ColorThemeOption)
{
    var currentScreenName by remember { mutableStateOf("Fling Aid") }

    // When doing back press on main screen, confirm with the user whether
    // it should exit the app or not
    val backPressed = remember { mutableStateOf(false) }
    BackPressHandler(onBackPressed = { backPressed.value = true })

    if (backPressed.value)
        ExitAlertDialog(onDismiss = { backPressed.value = false}, onExit = { exitProcess(1)})

    val navController = rememberNavController()

    Scaffold (
        topBar = { FlingAidTopAppBar(onColorThemeUpdated, currentTheme, currentScreenName) }
    ) { contentPadding ->
        SetUpNavGraph(navController = navController, contentPadding, onScreenChange = { newScreenTitle -> currentScreenName = newScreenTitle } )
    }
}

@Composable
private fun AboutDialogPopup(onDismissRequest: () -> Unit,
                             onConfirmation: () -> Unit)
{
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
                        modifier = Modifier.width(100.dp),
                        onClick = { onConfirmation() }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(id = R.string.back )
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
private fun PlayerNameSetting(
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
private fun MusicSetting(onSoundSettingUpdated: (soundOn: Boolean) -> Unit, currentSoundSetting: Boolean)
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

@Composable
private fun ColorThemeSetting(onColorThemeUpdated: (colorThemeType: ColorThemeOption) -> Unit,
                      currentTheme: ColorThemeOption)
{
    Row (
        modifier = Modifier
            .border(1.dp, colorScheme.outline, shape = RoundedCornerShape(5.dp))
            .wrapContentWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val colorThemeOptionsStringValues
                = listOf(ColorThemeOption.System.toString(), ColorThemeOption.Light.toString(), ColorThemeOption.Dark.toString())

        val (selectedOption, onOptionSelected) = remember {
            // Make the initial selection match the global color theme
            // at the start of opening the Theme setting dialog box
            mutableStateOf(currentTheme.toString())
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
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),  // Make the entire row selectable
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

private fun setGameMusic(on: Boolean)
{
    if (on) {
        if (!gGameAudio.isPlaying) {
            gGameAudio.start()
        }
    } else {
        gGameAudio.pause()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExitAlertDialog(onDismiss: () -> Unit, onExit: () -> Unit) {
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp)
                .height(IntrinsicSize.Min),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Text(
                    text = "Logout",
                    color = Color.Black,
                    modifier = Modifier
                        .padding(8.dp, 16.dp, 8.dp, 2.dp)
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth(), fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Are you sure you want to exit?",
                    color = Color.Black,
                    modifier = Modifier
                        .padding(8.dp, 2.dp, 8.dp, 16.dp)
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .width(1.dp), color = Color.Gray
                )
                Row(Modifier.padding(top = 0.dp)) {
                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentEnforcement provides false,
                    ) {
                        TextButton(
                            onClick = { onDismiss() },
                            Modifier
                                .fillMaxWidth()
                                .padding(0.dp)
                                .weight(1F)
                                .border(0.dp, Color.Transparent)
                                .height(48.dp),
                            elevation = ButtonDefaults.elevatedButtonElevation(0.dp, 0.dp),
                            shape = RoundedCornerShape(0.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = "Not now", color = Color.Gray)
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp), color = Color.Gray
                    )
                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentEnforcement provides false,
                    ) {
                        TextButton(
                            onClick = {
                                onExit.invoke()
                            },
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
                            Text(text = "Exit", color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun ScreenPreview()
{
    SettingPopup(
        onDismissRequest = { /*TODO*/ },
        onConfirmation = { /*TODO*/ },
        onColorThemeUpdated = {},
        currentTheme = ColorThemeOption.Dark,
        onPlayerNameUpdated = {},
        currentPlayerName = "Tom",
        onSoundSettingUpdated = {},
        currentSoundSetting = true
    )
}