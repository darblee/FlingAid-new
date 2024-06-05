package com.darblee.flingaid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun GameScreen(screenRouteParams: Screen.Game, onNavigateBack: () -> Unit)
{
    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Game; Sound On = ${screenRouteParams.soundOn}, player = ${screenRouteParams.playerName}")
        Button(onClick = { onNavigateBack.invoke() })
        {
            Text(text = "Go Back")
        }
    }
}