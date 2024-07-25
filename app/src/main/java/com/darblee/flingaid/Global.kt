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


lateinit var gAudio_gameMusic: MediaPlayer
lateinit var gAudio_gameWon: MediaPlayer
lateinit var gAudio_doink: MediaPlayer

var gSoundOn = false

internal object Global {
    const val MAX_COL_SIZE = 7
    const val MAX_ROW_SIZE = 8
    const val DEBUG_PREFIX = "Flicker:"
    const val SOLVER_BOARD_FILENAME = "SolverBoard.txt"
    const val GAME_BOARD_FILENAME = "MainBoard.txt"
    val TopAppBarHeight = 48.0.dp
}