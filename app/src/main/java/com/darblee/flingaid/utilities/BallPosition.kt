package com.darblee.flingaid.utilities

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.darblee.flingaid.Global
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Position on the game board
 * - row
 * - column
 */
@Serializable
data class Pos(
    val row: Int,
    val col: Int,
)

/**
 * Ball Position Management
 *
 * Common routine to manage the ball positions
 *
 * [ballList] List of all the ball positions. It is in mutableStateListOf<Pos> format. Intended
 * to be used in composable
 *
 * [getBallCount] Get the number of active balls in the grid
 * [setGameFile] Setup file pointer file
 * [removeGameFile] Delete the file
 * [saveBallListToFile]  Save the list of position in the file
 * [loadBallListFromFile] Load the list of ball positions from the file
 * [printPositions] Print all the ball positions. It is intended for debugging purposes.
 *
 */
class BallPosition {
    /**
     * List of all the balls and its position on the game board
     *
     * _Developer's Note_
     * Use of mutableStateListOf to preserve the composable state of the ball position. The state
     * are kept appropriately isolated and can be performed in a safe manner without race condition
     * when they are used in multiple threads (e.g. LaunchEffects).
     */
    var ballList = mutableStateListOf<Pos>()

    /**
     * Return the number of active ball
     *
     * @return Number of active balls
     */
    fun getBallCount(): Int {
        return ballList.count()
    }

    private var _gameFile: File? = null

    /**
     * Set the game file
     *
     * @param file game file
     */
    fun setGameFile(file: File) {
        _gameFile = file
    }

    /**
     * Delete the game file
     */
    fun removeGameFile() {
        _gameFile?.delete()
    }

    /**
     * Save the game board to a file
     */
    fun saveBallListToFile() {
        val format = Json { prettyPrint = true }
        val ballList = mutableListOf<Pos>()

        for (currentPos in this.ballList) {
            ballList += currentPos
        }
        val output = format.encodeToString(ballList)

        try {
            val writer = FileWriter(_gameFile)
            writer.write(output)
            writer.close()
        } catch (e: Exception) {
            Log.i(Global.DEBUG_PREFIX, "${e.message}")
        }
    }

    /**
     * Load the saved game board from file
     **/
    fun loadBallListFromFile() {
        try {
            val reader = FileReader(_gameFile)
            val data = reader.readText()
            reader.close()

            val list = Json.decodeFromString<List<Pos>>(data)

            ballList.clear()
            for (pos in list) {
                ballList.add(pos)
            }
        } catch (e: Exception) {
            Log.i(Global.DEBUG_PREFIX, "An error occurred while reading the file: ${e.message}")
        }
    }

    /**
     * Print the ball positions. Used for debugging purposes
     */
    fun printPositions() {

        Log.i(Global.DEBUG_PREFIX, "============== Ball Listing ================)")

        for ((index, value) in ballList.withIndex()) {
            Log.i(Global.DEBUG_PREFIX, "Ball $index: (${value.row}, ${value.col})")
        }

    }
}