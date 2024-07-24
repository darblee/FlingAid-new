package com.darblee.flingaid.utilities

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.darblee.flingaid.Direction
import com.darblee.flingaid.Global
import com.darblee.flingaid.ui.MovingRec
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

    /**
     * Build a list of move records on this single move. If only one ball is involved, then this
     * will be a chain of 1 ball record movement.
     *
     * @param initialRow Row of position to move from
     * @param initialCol Column of position to move from
     * @param direction Direction to move to
     *
     * @return List of movement records, If this only has 1 move, then this will be a chain of
     * one movement record. It will return an empty list if there is no movement needed.
     */
    fun buildMovingChain(
        initialRow: Int,
        initialCol: Int,
        direction: Direction
    ): List<MovingRec> {
        val movingList = mutableListOf<MovingRec>()

        var chainRecInfo =
            findFreeSpaceCount(initialRow, initialCol, direction, firstMove = true)
        var movingDistance = chainRecInfo.first
        var nextSourcePos = chainRecInfo.second

        // For the first ball in chain, we must have at least one free space to move
        if (movingDistance == 0) return movingList

        Log.i(Global.DEBUG_PREFIX, "Moving from ${initialRow}, ${initialCol} for $movingDistance blocks")

        movingList.add(MovingRec(Pos(initialRow, initialCol), movingDistance))

        // For subsequent ball in the chain, it is fine to have zero distance
        var fallenOffEdge = false
        while (!fallenOffEdge) {
            if (nextSourcePos == null) {
                fallenOffEdge = true
            } else {
                val curSourcePos = nextSourcePos
                chainRecInfo =
                    findFreeSpaceCount(
                        curSourcePos.row,
                        curSourcePos.col,
                        direction,
                        firstMove = false
                    )
                movingDistance = chainRecInfo.first
                nextSourcePos = chainRecInfo.second

                Log.i(Global.DEBUG_PREFIX, "Chain : Moving from ${curSourcePos.row}, ${curSourcePos.col} for $movingDistance blocks")

                movingList.add(MovingRec(curSourcePos, movingDistance))
            }
        }

        return (movingList)
    }


    /**
     *  Find the number of free space in front of the ball doing on a specific direction. If the
     *  ball is on the edge, then automatically provide 2 free spaces. The caller [buildMovingChain]
     *  will send the ball off the grid. We need to see it fall off the edge of the phone screen,
     *  which is why two space is provided instead of one space.
     *
     *  @param row Row of existing ball
     *  @param col Column of existing ball
     *  @param direction Direction to look for free space
     *  @param firstMove If this is the first move in the moving chain, we need to do special check
     *  when the ball is on the edge of the grid. Ball on edge can not move off edge of the board.
     *  If this is not the first ball on the chain, then it can fall off the edge
     *
     *  @return Pair<Int, SolverGridPos?> where the first element is the number of free space and
     *  second element is the position of the next ball. If there is no possible move, it will
     *  return <0, null>
     *
     *  @see buildMovingChain
     */
    private fun findFreeSpaceCount(
        row: Int,
        col: Int,
        direction: Direction,
        firstMove: Boolean
    ): Pair<Int, Pos?> {
        var xOffset = 0
        var yOffset = 0
        var sourceRow = row
        var sourceCol = col

        when (direction) {
            Direction.RIGHT -> yOffset = 1
            Direction.LEFT -> yOffset = -1
            Direction.UP -> xOffset = -1
            Direction.DOWN -> xOffset = 1
            else -> assert(true) { "Got unexpected direction value" }
        }

        if ((direction == Direction.LEFT) && (col == 0)) {
            if (firstMove) return (Pair(0, null))
            return (Pair(2, null))
        }

        if ((direction == Direction.RIGHT) && (col == Global.MAX_COL_SIZE)) {
            if (firstMove) return (Pair(0, null))
            return (Pair(2, null))
        }

        if ((direction == Direction.UP) && (row == 0)) {
            if (firstMove) return (Pair(0, null))
            return (Pair(2, null))
        }

        if ((direction == Direction.DOWN) && (row == Global.MAX_ROW_SIZE)) {
            if (firstMove) return (Pair(0, null))
            return (Pair(2, null))
        }

        var newRow: Int
        var newCol: Int

        var distance = 0
        var hitWall = false

        var nextSourcePos: Pos? = null

        while (!hitWall) {
            newRow = sourceRow + xOffset
            newCol = sourceCol + yOffset

            if ((newRow == -1) || (newRow == Global.MAX_ROW_SIZE) ||
                (newCol == -1) || (newCol == Global.MAX_COL_SIZE)
            ) {
                distance++ // Add two to make it fall off the edge
                distance++
                hitWall = true
            } else {
                if (ballList.contains(Pos(newRow, newCol))) {
                    nextSourcePos = Pos(newRow, newCol)
                    hitWall = true
                } else {
                    distance++
                    sourceRow = newRow
                    sourceCol = newCol
                }
            }
        }

        return (Pair(distance, nextSourcePos))
    }
}