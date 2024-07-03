package com.darblee.flingaid.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.darblee.flingaid.R
import com.darblee.flingaid.Screen
import com.darblee.flingaid.ui.SolverViewModel
import kotlin.system.exitProcess

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    solverViewModel: SolverViewModel = viewModel(),
    navController: NavHostController,
    innerPadding: PaddingValues)
{
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .wrapContentSize()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,

    ) {
        Card(
            modifier = Modifier
                .wrapContentSize()
                .align(alignment = Alignment.CenterHorizontally)
                .defaultMinSize(minWidth = 200.dp)
                .clickable {
                    navController.navigate(Screen.Game)
                },
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.onSecondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.wrapContentSize()
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ball), contentDescription = "Game",
                    contentScale = ContentScale.Fit)
                Text(text = "Game", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 20.dp))
            }
        }
        Card(
            modifier = Modifier
                .wrapContentSize()
                .defaultMinSize(minWidth = 200.dp)
                .align(alignment = Alignment.CenterHorizontally)
                .clickable {
                    navController.navigate(Screen.Solver)
                },
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.onTertiary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.wrapContentSize()
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ball), contentDescription = "Game",
                    contentScale = ContentScale.Fit)
                Text(text = "Solution Finder", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 20.dp))
            }
        }

        Button(
            onClick = { exitProcess(1) },
            modifier = Modifier.padding(10.dp)
        )
        {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Exit",
                modifier = Modifier.padding(end = 5.dp))
            Text(text = "Exit", style = MaterialTheme.typography.titleLarge)
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
