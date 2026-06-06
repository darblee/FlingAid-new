# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**FlingAid** is an Android game app built with Kotlin and Jetpack Compose. The app includes:
- A game mode where users can interactively play by moving balls on a board
- A solver mode that finds winning moves algorithmically
- Theme customization, player name settings, and background music toggle
- State management through ViewModels and persistent storage via DataStore

The codebase follows the Android MVVM architecture pattern with Jetpack Compose for UI.

## Development Commands

### Build

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Build release APK (with minification and resource shrinking)
./gradlew :app:assembleRelease

# Full build (includes all checks)
./gradlew build
```

### Run/Install

```bash
# Install and run debug build on connected device/emulator
./gradlew :app:installDebug

# Uninstall the app from device
./gradlew :app:uninstallDebug
```

### Testing

```bash
# Run all unit tests (JVM-based)
./gradlew :app:testDebug

# Run a single unit test class
./gradlew :app:testDebugUnitTest --tests "com.darblee.flingaid.ExampleUnitTest"

# Run instrumented tests (requires device/emulator, uses AndroidJUnitRunner)
./gradlew :app:connectedAndroidTest

# Run instrumented tests on specific device
./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.deviceSerial=<device-id>
```

### Gradle Cleanup

```bash
# Clean build artifacts
./gradlew clean

# Refresh dependencies (useful if Gradle cache is corrupted)
./gradlew --refresh-dependencies
```

### Logging

The app uses `Log` from Android with a global debug prefix defined in `Global.DEBUG_PREFIX` = "Flicker:". To filter logs in Logcat:
```
tag:"Flicker:"
```

## Project Structure

```
app/src/
├── main/
│   ├── java/com/darblee/flingaid/
│   │   ├── MainActivity.kt           # Entry point, Scaffold, theme setup, audio management
│   │   ├── NavGraph.kt               # Navigation between Home, Game, Solver screens
│   │   ├── Global.kt                 # Global types, constants, and state holders
│   │   ├── PreferenceStore.kt        # Persistent settings via DataStore
│   │   ├── domain/
│   │   │   ├── Error.kt              # Error domain objects
│   │   │   └── Result.kt             # Result wrapper type for API responses
│   │   ├── ui/
│   │   │   ├── GameViewModel.kt      # MVVM ViewModel for game mode
│   │   │   ├── SolverViewModel.kt    # MVVM ViewModel for solver mode
│   │   │   ├── GameUIState.kt        # UI state data class for game
│   │   │   ├── SolverUiState.kt      # UI state data class for solver
│   │   │   ├── FlickerEngine.kt      # Core game logic (ball movement, collision, etc.)
│   │   │   ├── Particles.kt          # Visual effects for game particles
│   │   │   ├── screens/
│   │   │   │   ├── HomeScreen.kt     # Home/menu screen
│   │   │   │   ├── GameScreen.kt     # Main game play screen
│   │   │   │   └── SolverScreen.kt   # Solver screen
│   │   │   └── theme/
│   │   │       ├── Theme.kt          # Material 3 theme setup
│   │   │       ├── Color.kt          # Color definitions
│   │   │       └── Type.kt           # Typography definitions
│   │   ├── utilities/
│   │   │   ├── FlickerBoard.kt       # Game board logic, file I/O for board state
│   │   │   ├── Utilities.kt          # Helper functions (click feedback, etc.)
│   │   │   └── PairArgsSingletonHolder.kt  # Singleton pattern for ViewModels
│   │   └── res/
│   │       ├── values/               # String resources, dimensions
│   │       ├── raw/                  # Audio files (music, sound effects)
│   │       └── drawable/             # Images and custom drawables
│   ├── test/
│   │   └── java/.../ExampleUnitTest.kt    # JVM unit tests
│   └── androidTest/
│       └── java/.../ExampleInstrumentedTest.kt  # Device instrumented tests
└── build.gradle.kts
```

## Architecture & State Management

### MVVM Pattern

- **ViewModels** (`GameViewModel`, `SolverViewModel`): Hold game state and business logic using Kotlin `ViewModel` from Jetpack
- **UI State** (`GameUIState`, `SolverUiState`): Data classes that hold immutable UI state; exposed as `StateFlow` for reactive updates
- **Screens**: Jetpack Compose composables that observe `StateFlow` and call ViewModel methods on user actions

### State Flow

```kotlin
// In ViewModel:
private val _uiState = MutableStateFlow(initialState)
val uiState: StateFlow<GameUIState> = _uiState.asStateFlow()

// Update state:
_uiState.update { currentState -> currentState.copy(field = newValue) }

// In Composable:
val state by viewModel.uiState.collectAsState()
```

### Global Singletons

`Global.kt` holds:
- Global media players (`gAudio_gameMusic`, `gAudio_doink`, etc.) and audio manager
- Global ViewModels (`gGameViewModel`, `gSolverViewModel`) — instantiated once and reused
- Ball display image cache (`gDisplayBallImage`)
- App-wide constants (board dimensions: 7 cols × 8 rows, max 14 levels)

The ViewModels use `PairArgsSingletonHolder` to ensure a single instance per File pair argument.

### Persistent Storage

`PreferenceStore` uses Jetpack DataStore to persist:
- Player name
- Color theme preference (System/Light/Dark)
- Music on/off setting

## Key UI Flows

1. **Theme & Settings**: Changed in `MainActivity.SettingPopup()` → calls `onColorThemeUpdated` → updates `SetColorTheme()` → recomposes entire app
2. **Audio Management**: Managed in `MainActivity` via Android `AudioManager`, respects system audio focus (pauses music if another app takes focus)
3. **Navigation**: 3-screen app (Home, Game, Solver) using Jetpack Navigation Compose; back button behavior differs per screen
4. **Game State Machine**: Described in README.md; screens validate state before allowing actions (e.g., "Find Solution" button disabled while solving)

## Testing Notes

- **Unit tests** (in `src/test/`) run on JVM; good for business logic tests (board evaluation, move validation)
- **Instrumented tests** (in `src/androidTest/`) run on device/emulator; needed for UI/Compose tests, file I/O, preferences
- Use `@get:Rule val instantExecutorRule = InstantTaskExecutorRule()` in instrumented tests to run coroutines synchronously during testing
- Example test structure: arrange state → act (call ViewModel method) → assert (check StateFlow updated correctly)

## Common Development Tasks

### Adding a New Screen

1. Create a composable in `ui/screens/`
2. Add `@Serializable data object ScreenName : Screen(R.string.screen_title)` to `NavGraph.kt`
3. Add route in `SetUpNavGraph()` composable
4. Update back button logic in `MainActivity.FlingAidTopAppBar()` if needed

### Modifying Game Logic

- Ball movement & collision logic lives in `FlickerEngine.kt` (rendering) and `FlickerBoard.kt` (position tracking)
- Board state persists to file via `FlickerBoard.saveToFile()`; load with `loadBallListFromFile()`

### Debugging

- Use `Log.i(Global.DEBUG_PREFIX, "message")` to log with the app's tag
- Filter Logcat by tag `"Flicker:"`
- Check `PreferenceStore` for saved settings if app state persists unexpectedly

### Audio Management

- Audio files are in `res/raw/`; loaded as `MediaPlayer.create(context, R.raw.filename)`
- App requests audio focus via `AudioManager.requestAudioFocus()` to handle interrupts gracefully
- Audio lifecycle tied to app lifecycle: paused on `ON_STOP`, resumed on `ON_START`
