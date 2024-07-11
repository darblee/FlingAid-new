package com.darblee.flingaid

import android.media.MediaPlayer
import androidx.compose.ui.unit.dp

enum class Direction { NO_WINNING_DIRECTION, UP, DOWN, LEFT, RIGHT, INCOMPLETE }

lateinit var gAudio_gameMusic : MediaPlayer
lateinit var gAudio_youWon : MediaPlayer
var gSoundOn = false

internal object Global {
    const val MaxColSize = 7
    const val MaxRowSize = 8
    const val debugPrefix = "Flinfo:"
    const val boardFileName = "Board.txt"
    val TopAppBarHeight = 48.0.dp
}
