package com.darblee.flingaid

import com.darblee.flingaid.ui.Direction

object Global {
    var task2_WinningDirection = Direction.NO_WINNING_DIRECTION
    var task1_WinningDirection = Direction.NO_WINNING_DIRECTION
    const val MaxColSize = 7
    const val MaxRowSize = 8
    var ThinkingProgress = 0
    var totalProcessCount : Float = 0.0F
    const val debugPrefix = "Flinfo:"
    const val boardFileName = "Board.txt"
    var gameMusicOn = false
}
