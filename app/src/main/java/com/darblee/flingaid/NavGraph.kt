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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.darblee.flingaid.ui.screens.GameScreen
import com.darblee.flingaid.ui.screens.HomeScreen
import com.darblee.flingaid.ui.screens.SolverScreen
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable
    data object Home: Screen()

    @Serializable
    data object Game : Screen()

    @Serializable
    data object Solver : Screen()
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

        composable<Screen.Game>{
            onScreenChange.invoke(stringResource(id = R.string.game_title))
            GameScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = Global.TopAppBarHeight))  // Height of the TopAppBar
            {
                navController.popBackStack()
            }
        }

        composable<Screen.Solver>{
            onScreenChange.invoke(stringResource(id = R.string.solver_title))
            SolverScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = Global.TopAppBarHeight))  // Height of the TopAppBar
        }
    }
}

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