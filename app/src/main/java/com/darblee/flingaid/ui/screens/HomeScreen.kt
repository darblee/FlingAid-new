package com.darblee.flingaid.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.darblee.flingaid.Screen
import com.darblee.flingaid.ui.GameViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel = viewModel(),
    navController: NavHostController,
    innerPadding: PaddingValues)
{
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {

        Button (onClick = {
            navController.navigate(Screen.Game)
        }) {
            Text(text = "Test - Go to game")
        }

        Button (onClick = {
            navController.navigate(Screen.Solver)
        }) {
            Text(text = "Test - Go to solver game")
        }

    } // Column
}


@SuppressLint("UnrememberedMutableState")
@Preview(name="Custom Dialog")
@Composable
private fun MyDialogUIPreview(){
}
