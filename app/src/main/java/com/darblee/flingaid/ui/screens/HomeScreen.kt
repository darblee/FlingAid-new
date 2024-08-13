package com.darblee.flingaid.ui.screens

import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import androidx.navigation.NavHostController
import com.darblee.flingaid.BackPressHandler
import com.darblee.flingaid.R
import com.darblee.flingaid.Screen
import kotlin.system.exitProcess

/**
 *  The Home Screen
 *
 *  @param modifier Pass in modifier elements that decorate or add behavior to the compose UI
 *  elements
 *  @param navController Central coordinator for managing navigation between destination screens,
 *  managing the back stack, and more
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    /**
     * Intercept backPress key while on Home screen.
     *
     * When doing back press on the current screen, confirm with the user whether it should exit
     * this screen or not.
     */
    var backPressed by remember { mutableStateOf(false) }
    BackPressHandler(onBackPressed = { backPressed = true })
    if (backPressed) {
        ExitAlertDialog(onDismiss = { backPressed = false }, onExit = { exitProcess(1) })
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .wrapContentSize()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,

        ) {
        val view = LocalView.current
        Card(
            modifier = Modifier
                .wrapContentSize()
                .align(alignment = Alignment.CenterHorizontally)
                .defaultMinSize(minWidth = 200.dp)
                .clickable {
                    view.let { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
                    navController.navigate(Screen.Game)
                },
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.onSecondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ball), contentDescription = "Game",
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "Game", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
        }
        Card(
            modifier = Modifier
                .wrapContentSize()
                .defaultMinSize(minWidth = 200.dp)
                .align(alignment = Alignment.CenterHorizontally)
                .clickable {
                    view.let { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
                    navController.navigate(Screen.Solver)
                },
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.onTertiary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ball), contentDescription = "Game",
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "Solution Finder", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
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
                modifier = Modifier.padding(end = 5.dp)
            )
            Text(text = "Exit", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Preview(name = "Custom Dialog")
@Composable
private fun MyDialogUIPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Card(
            modifier = Modifier
                .width(200.dp)
                .height(200.dp),
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Green),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(painter = painterResource(id = R.drawable.ball), contentDescription = "Game")
                Button(onClick = {
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
            colors = CardDefaults.cardColors(containerColor = Color.Cyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

/**
 * Confirm user if it needs to exit or not
 *
 * @param onDismiss lambda function to cancel the exit
 * @param onExit lambda function to perform the exit
 */
@Composable
private fun ExitAlertDialog(onDismiss: () -> Unit, onExit: () -> Unit) {
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(300.dp)
                .padding(0.dp)
                .height(IntrinsicSize.Min)
                .border(
                    0.dp, color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(16.dp)
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Column(
                Modifier.fillMaxWidth()
            ) {
                Row {
                    Column(Modifier.weight(1f).align(Alignment.CenterVertically)) {
                        Image(
                            painter = painterResource(id = R.drawable.ball),
                            contentDescription = "Game",
                            contentScale = ContentScale.Fit
                        )
                    }
                    Column(Modifier.weight(3f)) {
                        Text(
                            text = stringResource(R.string.logout),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp, 16.dp, 8.dp, 2.dp)
                                .align(Alignment.CenterHorizontally)
                                .fillMaxWidth(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.are_you_sure_you_want_to_exit),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp, 2.dp, 8.dp, 16.dp)
                                .align(Alignment.CenterHorizontally)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outline
                )
                Row(Modifier.padding(top = 0.dp)) {
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
                        Text(
                            text = stringResource(R.string.not_now),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp), color = MaterialTheme.colorScheme.outline
                    )
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
                        Text(
                            text = stringResource(R.string.exit),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
