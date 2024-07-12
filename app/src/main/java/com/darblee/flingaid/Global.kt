package com.darblee.flingaid

import android.media.MediaPlayer
import androidx.compose.ui.unit.dp

/**
 * Winning Ball direction.
 * - [NO_WINNING_DIRECTION]  If used in identifying whether there is a winnable move,
 * this indicates there is no winning direction
 * - [UP] WInning direction is up
 * - [DOWN] Winning direction is down
 * - [LEFT] Winning direction is left
 * - [RIGHT] Winning direction is right
 *  - [INCOMPLETE] : While in the middle of searching for solution (task operation), it could not
 *  reach a conclusive result whether it has a solution or not
 */
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
