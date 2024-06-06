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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    data object Home: Screen()

    @Serializable
    data class Game(
        val soundOn: Boolean,
        val playerName: String
    ): Screen()

    @Serializable
    data class Solver(
        val soundOn: Boolean,
        val playerName: String
    ): Screen()
}

@SuppressLint("RestrictedApi")
@Composable
fun SetUpNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    onScreenChange: (screenTitle: String) -> Unit
) {
    // A surface container using the 'background' color from the theme
    NavHost(
        navController = navController,
        startDestination = Screen.Home
    ) {
        composable<Screen.Home>{
            val appName = stringResource(id = R.string.app_name)
            onScreenChange.invoke(appName)
            HomeScreen (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                navController = navController,
                innerPadding = innerPadding
            )
        }

        composable<Screen.Game>{navBackStackEntry ->
            // Need to pass parameters to Game screen
            val screenRouteParams = navBackStackEntry.toRoute<Screen.Game>()
            onScreenChange.invoke("Game")
            GameScreen(screenRouteParams) {
                navController.popBackStack()
            }
        }

        composable<Screen.Solver>{navBackStackEntry ->
            val screenRouteParams = navBackStackEntry.toRoute<Screen.Solver>()
            // Need to pass parameters to Solver screen
            onScreenChange.invoke("Solver")
            SolverScreen(
                modifier = Modifier
                    .fillMaxSize(),
                screenRouteParams) {
                navController.popBackStack()
            }
        }
    }
}

// Need to verify that user can interact with app and return true. Use this before you can popBackStack.
// For more detailed explanation,
// see https://medium.com/@daniel.atitienei/do-not-make-this-navigation-mistake-in-jetpack-compose-8ba2a1732357
val NavHostController.canGoBack: Boolean
    get() = this.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED

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

        onDispose {
            backCallback.remove()
        }
    }
}