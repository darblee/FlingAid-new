package com.darblee.flingaid

import android.annotation.SuppressLint
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.darblee.flingaid.ui.screens.GameScreen
import com.darblee.flingaid.ui.screens.HomeScreen
import com.darblee.flingaid.ui.screens.SolverScreen
import kotlinx.serialization.Serializable

/**
 * All the possible screen objects
 */
@Serializable
sealed class Screen(val stringTitleResourceID: Int){
    @Serializable
    data object Home: Screen(R.string.homepage_title)

    @Serializable
    data object Game : Screen(R.string.game_title)

    @Serializable
    data object Solver : Screen(R.string.solver_title)
}

/**
 * Navigation set-up
 *
 * @param navController Central coordinator for managing navigation between destination screens,
 * managing the back stack, and more
 * @param innerPadding Inner padding
 * @param currentScreen The current active screen
 */
@SuppressLint("RestrictedApi")
@Composable
fun SetUpNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    currentScreen: MutableState<Screen>
) {
    // A surface container using the 'background' color from the theme
    NavHost(
        navController = navController,
        startDestination = Screen.Home
    ) {
        composable<Screen.Home>{
            currentScreen.value = Screen.Home
            HomeScreen (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                navController = navController)
        }

        composable<Screen.Game>{
            currentScreen.value = Screen.Game
            GameScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = Global.TopAppBarHeight),
                navController = navController
                )
        }

        composable<Screen.Solver>{
            currentScreen.value = Screen.Solver
            SolverScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = Global.TopAppBarHeight),
                navController)  // Height of the TopAppBar
        }
    }
}

/**
 * BackPressHandler is used to intercept back press
 *
 * When doing back press on main screen, need to confirm with the user whether
 * it should exit the app or not. It uses the [BackPressHandler] function.
 * See code [MainViewImplementation]
 *
 * We created [OnBackPressedCallback] and add it to the onBackPressDispatcher
 * that controls dispatching system back presses. We enable the callback whenever
 * our Composable is recomposed, which disables other internal callbacks responsible
 * for back press handling. The callback is added on any lifecycle owner change and removed
 * on dispose.
 */
@Composable
fun BackPressHandler(
    backPressedDispatcher: OnBackPressedDispatcher? =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher,
    onBackPressed: () -> Unit
) {
    val currentOnBackPressed by rememberUpdatedState(newValue = onBackPressed)

    val backCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                currentOnBackPressed()
            }
        }
    }

    DisposableEffect(key1 = backPressedDispatcher) {
        backPressedDispatcher?.addCallback(backCallback)
        onDispose { backCallback.remove() }
    }
}
