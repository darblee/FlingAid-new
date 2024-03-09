package com.darblee.flingaid

import com.darblee.flingaid.ui.Direction

object Global {
    var task2_WinningDirection= Direction.NO_WINNING_DIRECTION
    var task1_WinningDirection = Direction.NO_WINNING_DIRECTION
    val MaxColSize = 7
    val MaxRowSize = 8
    var ThinkingProgress = 0
    var totalProcessCount : Float = 0.0F
    val debugPrefix = "Flinfo:"
    const val boardFileName = "Board.txt"
}
