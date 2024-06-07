package com.darblee.flingaid.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.darblee.flingaid.R
import com.darblee.flingaid.Screen
import com.darblee.flingaid.ui.GameViewModel
import kotlin.system.exitProcess

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel = viewModel(),
    navController: NavHostController,
    innerPadding: PaddingValues)
{
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,

    ) {
        Card(
            modifier = Modifier
                .width(200.dp)
                .height(200.dp),
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.onSecondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(painter = painterResource(id = R.drawable.ball), contentDescription = "Game")
                Button(onClick = {
                    navController.navigate(Screen.Game)
                }) {
                    Text(text = "Game")
                }
            }
        }
        Card(
            modifier = Modifier
                .width(200.dp)
                .height(200.dp),
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.onTertiary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(painter = painterResource(id = R.drawable.ball), contentDescription = "Game")
                Button(onClick = {
                    navController.navigate(Screen.Solver)
                }) {
                    Text(text = "Solution Finder")
                }
            }
        }

        Button(onClick = {
            exitProcess(1)
        }) {
            Text(text = "Exit")
        }
    }
}


@SuppressLint("UnrememberedMutableState")
@Preview(name="Custom Dialog")
@Composable
private fun MyDialogUIPreview(){
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Card (
            modifier = Modifier
                .width(200.dp)
                .height(200.dp),
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Green),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painter = painterResource(id = R.drawable.ball), contentDescription = "Game")
                Button(onClick = {
                }) {
                    Text(text = "Game")
                }
            }
        }
        Card (
            modifier = Modifier
                .width(200.dp)
                .height(200.dp),
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Cyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painter = painterResource(id = R.drawable.ball), contentDescription = "Game")
                Button(onClick = {
                }) {
                    Text(text = "Solution Finder")
                }
            }
        }

        Button(onClick = {
        }) {
            Text(text = "Exit")
        }
    } // Column
}
