package com.darblee.flingaid

import android.media.MediaPlayer
import androidx.compose.ui.unit.dp

enum class Direction { NO_WINNING_DIRECTION, UP, DOWN, LEFT, RIGHT, INCOMPLETE }

lateinit var gAudio_gameMusic : MediaPlayer
lateinit var gAudio_youWon : MediaPlayer
var gSoundOn = false

object Global {
    var task2_WinningDirection = Direction.NO_WINNING_DIRECTION
    var task1_WinningDirection = Direction.NO_WINNING_DIRECTION
    const val MaxColSize = 7
    const val MaxRowSize = 8
    var ThinkingProgress = 0
    var totalProcessCount : Float = 0.0F
    const val debugPrefix = "Flinfo:"
    const val boardFileName = "Board.txt"
    val TopAppBarHeight = 48.0.dp
}
