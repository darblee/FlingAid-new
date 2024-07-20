package com.darblee.flingaid.ui

import kotlinx.serialization.Serializable

enum class GameState {
    Idle,
    IdleFoundSOlution,
    Won,
    Playing,
    MoveBall,
}

/**
 * These UI state data needs to be preserved in the event there is a configuration change
 * (e.g. screen size change, screen rotation).
 *
 * It will be managed in a observable flow called "STateFlow" Android composable will listen for it.
 */
data class GameUIState(
    var state: GameState = GameState.Playing,
    var moveFromRow: Int = 0,
    var moveFromCol: Int = 0,
)
