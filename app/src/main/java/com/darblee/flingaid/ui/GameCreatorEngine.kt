package com.darblee.flingaid.ui

import com.darblee.flingaid.Global

class GameCreatorEngine {
    private var flickerGrid = Array(Global.MaxRowSize) { BooleanArray(Global.MaxColSize) }

    init {
        reset()
    }

    private fun reset()
    {
        repeat (Global.MaxRowSize) { row ->
            repeat(Global.MaxColSize) { col ->
                flickerGrid[row][col] = false
            }
        }
    }
}