package com.darblee.flingaid.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel : ViewModel() {
    private var _ballPositionList = mutableStateListOf<Pos>()

    /*

    SnapshotList:
    - The composable will automatically update when that state changes
    - Values can change
    - Other functions will be notified of the changes

    Ref: https://dev.to/zachklipp/introduction-to-the-compose-snapshot-system-19cn

    */

    // Game UI state
    private val _uiState = MutableStateFlow(GameUIState())

    /*
    Developer's note:
    StateFlow is a data holder observable flow that emits the current and new state updates.
    Its value property reflects the current state value. To update state and send it to the flow,
    assign a new value to the value property of the MutableStateFlow class.

    In Android, StateFlow works well with classes that must maintain an observable immutable state.
    A StateFlow can be exposed from the GameUiState so that the composable can listen for UI state
    updates and make the screen state survive configuration changes.

    In the GameViewModel class, add the following _uiState property.

    "internal" means it will only be visible within that module. A module is a set of Kotlin
    files that are compiled together e.g. a library or application. It provides real
    encapsulation for the implementation details

 */
    internal var uiState : StateFlow<GameUIState> = _uiState.asStateFlow()
        private set  // Public getter (read-only access from outside) and private setter (only internally modifiable)


    fun ballPositionList() : SnapshotStateList<Pos>
    {
        return (_ballPositionList)
    }

}