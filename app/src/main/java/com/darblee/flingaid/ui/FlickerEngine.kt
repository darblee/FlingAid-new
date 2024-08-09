package com.darblee.flingaid.ui

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.darblee.flingaid.Direction
import com.darblee.flingaid.Global
import com.darblee.flingaid.utilities.Pos
import kotlin.random.Random

/**
 * Track the thinking process to find the winning move.
 *
 * _Developer's note:_ `internal` means it will only be visible within that module. A module
 * is a set of Kotlin files that are compiled together e.g. a library or application. It provides real
 * encapsulation for the implementation details. In this case, it is shared wit the SolverEngine class.
 */
internal var gThinkingProgress = 0

/**
 * Store the winning direction for each corresponding task. Only 1 task will have the winning move
 * but we do not know which ones.
 * - Winning Direction from thread #1
 * - Winning Direction from thread #2
 */
internal var gTask1WinningDirection = Direction.NO_WINNING_DIRECTION
internal var gTask2WinningDirection = Direction.NO_WINNING_DIRECTION

/**
 * Engine that look for winnable move based on game layout information
 */
internal class FlickerEngine {
    private var flickerGrid = Array(Global.MAX_ROW_SIZE) { BooleanArray(Global.MAX_COL_SIZE) }

    /**
     * Initialize SolverEngine class
     */
    init {
        clearGameBoard()
    }

    /**
     * Clear the entire game board.
     */
    private fun clearGameBoard() {
        repeat(Global.MAX_ROW_SIZE) { row ->
            repeat(Global.MAX_COL_SIZE) { col ->
                flickerGrid[row][col] = false
            }
        }
    }

    /**
     * Populate the game board with balls
     *
     * @param ballPositionList List of all the ball positions
     */
    fun populateGrid(ballPositionList: SnapshotStateList<Pos>) {
        ballPositionList.forEach { pos ->
            flickerGrid[pos.row][pos.col] = true
        }
    }

    /**
     * Set-up another solver engine instance. The new instance will have exact
     * copy of the current game board.
     */
    private fun duplicate(): FlickerEngine {
        val tempBoard = FlickerEngine()

        // Clone the board
        repeat(Global.MAX_ROW_SIZE) { curRow ->
            repeat(Global.MAX_COL_SIZE) { curCol ->
                tempBoard.flickerGrid[curRow][curCol] = flickerGrid[curRow][curCol]
            }
        }

        return (tempBoard)
    }

    /**
     * Find the winning move
     *
     * @param totalBallCnt Used together with [curSearchLevel] parameter to determine whether it has found a solution or not.
     * @param curSearchLevel Current search level. Used together with [totalBallCnt] parameter determined whether it has found a solution or not.
     * It is also used to calculate thinking progress
     * @param thinkingDirectionOffset
     *
     * @return If winning move is found, it will return
     * - winning direction
     * - row position of winning ball
     * - column position of the winning ball
     *
     * If no winning move is found, it will return direction = NO_WINNING_DIRECTION
     */
    fun foundWinningMove(
        totalBallCnt: Int,
        curSearchLevel: Int,
        thinkingDirectionOffset: Int
    ): Triple<Direction, Int, Int> {
        var direction = Direction.NO_WINNING_DIRECTION
        var winningRow = -1
        var winningCol = -1

        val startRow: Int
        val exceededRow: Int
        val startColumn: Int
        val exceededCol: Int

        if (thinkingDirectionOffset == 1) {
            startRow = 0
            exceededRow = Global.MAX_ROW_SIZE

            startColumn = 0
            exceededCol = Global.MAX_COL_SIZE
        } else {
            startRow = Global.MAX_ROW_SIZE - 1
            exceededRow = -1

            startColumn = Global.MAX_COL_SIZE - 1
            exceededCol = -1
        }
        var curRow = startRow
        var currentCol: Int

        run repeatBlock@{
            while (curRow != exceededRow) {

                if (Thread.interrupted() || (gTask1WinningDirection != Direction.INCOMPLETE) || (gTask2WinningDirection != Direction.INCOMPLETE)) {
                    Log.d(
                        "${Global.DEBUG_PREFIX}:",
                        "Short circuit on row processing. task1: ${gTask1WinningDirection} task2: ${gTask2WinningDirection}"
                    )
                    direction = Direction.INCOMPLETE  // We should quit the current thread
                    return@repeatBlock

                }

                currentCol = startColumn

                var curCol: Int
                while (currentCol != exceededCol) {

                    curCol = currentCol

                    if (flickerGrid[curRow][curCol]) {
                        if (curSearchLevel == 1) Log.i(
                            "${Global.DEBUG_PREFIX}: Top level",
                            "Processing row=$curRow, col = $curCol"
                        )

                        if (winnableByMovingUp(totalBallCnt, curSearchLevel, curRow, curCol)) {
                            direction = Direction.UP
                            winningRow = curRow
                            winningCol = curCol
                            Log.i(
                                "${Global.DEBUG_PREFIX}:",
                                "Level #$curSearchLevel, Found winning move at direction $direction when processing at row: $curRow col: $curCol"
                            )
                            return@repeatBlock
                        }

                        if (curSearchLevel == 2) {
                            gThinkingProgress++
                        }
                        if (Thread.interrupted() || (gTask1WinningDirection != Direction.INCOMPLETE) || (gTask2WinningDirection != Direction.INCOMPLETE)) {
                            Log.d(
                                "${Global.DEBUG_PREFIX}:",
                                "Short circuit on col processing after up. task1: ${gTask1WinningDirection} task2: ${gTask2WinningDirection}"
                            )
                            direction = Direction.INCOMPLETE  // We should quit the current thread
                            return@repeatBlock
                        }

                        if (winnableByMovingDown(totalBallCnt, curSearchLevel, curRow, curCol)) {
                            direction = Direction.DOWN
                            winningRow = curRow
                            winningCol = curCol
                            return@repeatBlock
                        }

                        if (curSearchLevel == 2) {
                            gThinkingProgress++
                        }
                        if (Thread.interrupted() || (gTask1WinningDirection != Direction.INCOMPLETE) || (gTask2WinningDirection != Direction.INCOMPLETE)) {
                            Log.d(
                                "${Global.DEBUG_PREFIX}:",
                                "Short circuit on col processing after down. task1: ${gTask1WinningDirection} task2: ${gTask2WinningDirection}"
                            )
                            direction = Direction.INCOMPLETE  // We should quit the current thread
                            return@repeatBlock
                        }

                        if (winnableByMovingRight(totalBallCnt, curSearchLevel, curRow, curCol)) {
                            direction = Direction.RIGHT
                            winningRow = curRow
                            winningCol = curCol
                            return@repeatBlock
                        }

                        if (curSearchLevel == 2) {
                            gThinkingProgress++
                        }
                        if (Thread.interrupted() || (gTask1WinningDirection != Direction.INCOMPLETE) || (gTask2WinningDirection != Direction.INCOMPLETE)) {
                            Log.d(
                                "${Global.DEBUG_PREFIX}:",
                                "Short circuit on col processing after right. task1: ${gTask1WinningDirection} task2: ${gTask2WinningDirection}"
                            )
                            direction = Direction.INCOMPLETE  // We should quit the current thread
                            return@repeatBlock
                        }

                        if (winnableByMovingLeft(totalBallCnt, curSearchLevel, curRow, curCol)) {
                            direction = Direction.LEFT
                            winningRow = curRow
                            winningCol = curCol
                            return@repeatBlock
                        }

                        if (curSearchLevel == 2) {
                            gThinkingProgress++
                        }
                        if (Thread.interrupted() || (gTask1WinningDirection != Direction.INCOMPLETE) || (gTask2WinningDirection != Direction.INCOMPLETE)) {
                            Log.d(
                                "${Global.DEBUG_PREFIX}:",
                                "Short circuit on col processing after left. task1: ${gTask1WinningDirection} task2: ${gTask2WinningDirection}"
                            )
                            direction = Direction.INCOMPLETE  // We should quit the current thread
                            return@repeatBlock
                        }
                    }

                    currentCol += thinkingDirectionOffset
                } // Completed processing current Col

                curRow += thinkingDirectionOffset
            }  // Completed processing current row
        }

        if (direction != Direction.NO_WINNING_DIRECTION)
            Log.d(
                "${Global.DEBUG_PREFIX}:",
                "Returning winning (or incomplete) result: Level #$curSearchLevel, Direction is $direction"
            )

        return Triple(direction, winningRow, winningCol)
    }

    /**
     * Determine if moving up is a winning move or not
     *
     * @param totalBallCnt Used together with [curSearchLevel] parameter to determine whether it has found a solution or not.
     * @param curSearchLevel Current search level. Used together with [totalBallCnt] parameter determined whether it has found a solution or not.
     * It is also used to calculate thinking progress
     * @param srcRow Row position of the ball to move up from
     * @param col Column of the ball to move from
     *
     * @return
     * - True if this is a winning move
     * - false if this is NOT a winning move
     */
    private fun winnableByMovingUp(
        totalBallCnt: Int,
        curSearchLevel: Int,
        srcRow: Int,
        col: Int
    ): Boolean {
        val targetRow = findTargetRowOnMoveUp(srcRow, col)

        if (targetRow == -1) {
            return false
        }

        // Now we will make the move. So let's create a temporary board to handle the move
        val tempBoard = this.duplicate() // Clone the board

        // Make the actual move
        tempBoard.moveUp(srcRow, targetRow, col)

        if (totalBallCnt == (curSearchLevel + 1)) {
            Log.d(
                "GM: Level $curSearchLevel",
                "TotalBallCnt = $totalBallCnt : FOUND A WINNABLE MOVE BY MOVING UP"
            )
            return true
        }

        val (direction, _, _) = tempBoard.foundWinningMove(totalBallCnt, (curSearchLevel + 1), 1)

        if ((direction == Direction.NO_WINNING_DIRECTION) || (direction == Direction.INCOMPLETE)) {
            return (false)
        }

        return true
    }

    /**
     * Determine if moving down is a winning move or not
     *
     * @param totalBallCnt Used together with [curSearchLevel] parameter to determine whether it has found a solution or not.
     * @param curSearchLevel Current search level. Used together with [totalBallCnt] parameter determined whether it has found a solution or not.
     * It is also used to calculate thinking progress
     * @param srcRow Row position of the ball to move down from
     * @param col Column of the ball to move from
     *
     * @return
     * - True if this is a winning move
     * - false if this is NOT a winning move
     */
    private fun winnableByMovingDown(
        totalBallCnt: Int,
        curSearchLevel: Int,
        srcRow: Int,
        col: Int
    ): Boolean {
        val targetRow = findTargetRowOnMoveDown(srcRow, col)

        if (targetRow == -1) {
            return false
        }

        val tempBoard = this.duplicate() // Clone the board

        // Make the actual move
        tempBoard.moveDown(srcRow, targetRow, col)

        if (totalBallCnt == (curSearchLevel + 1)) {
            Log.i(
                "${Global.DEBUG_PREFIX}: Level $curSearchLevel",
                "TotalBallCnt = $totalBallCnt : FOUND A WINNABLE MOVE BY MOVING DOWN"
            )
            return true
        }

        val (direction, _, _) = tempBoard.foundWinningMove(totalBallCnt, (curSearchLevel + 1), 1)

        if ((direction == Direction.NO_WINNING_DIRECTION) || (direction == Direction.INCOMPLETE)) {
            return (false)
        }

        return true
    }

    /**
     * Determine if moving right is a winning move or not
     *
     * @param totalBallCnt Used together with [curSearchLevel] parameter to determine whether it has found a solution or not.
     * @param curSearchLevel Current search level. Used together with [totalBallCnt] parameter determined whether it has found a solution or not.
     * It is also used to calculate thinking progress
     * @param row Row position of the ball to move from
     * @param srcCol Column of the ball to move right from
     *
     * @return
     * - True if this is a winning move
     * - false if this is NOT a winning move
     */
    private fun winnableByMovingRight(
        totalBallCnt: Int,
        curSearchLevel: Int,
        row: Int,
        srcCol: Int
    ): Boolean {

        val targetCol = findTargetColOnMoveRight(row, srcCol)

        if (targetCol == -1) {
            // Log.d("GM: $curSearchLevel", "$debugIndentation $methodName: Could not find winnable move. No Target Col")
            return false
        }

        // Now we will make the move. So let's create a temporary board to handle the move
        val tempBoard = this.duplicate() // Clone the board

        // Make the actual move
        tempBoard.moveRight(srcCol, targetCol, row)

        if (totalBallCnt == (curSearchLevel + 1)) {
            Log.d(
                "${Global.DEBUG_PREFIX}: Level $curSearchLevel",
                "TotalBallCnt = $totalBallCnt : FOUND A WINNABLE MOVE BY MOVING RIGHT"
            )
            return true
        }

        val (direction, _, _) = tempBoard.foundWinningMove(totalBallCnt, (curSearchLevel + 1), 1)

        if ((direction == Direction.NO_WINNING_DIRECTION) || (direction == Direction.INCOMPLETE)) {
            return (false)
        }

        return true
    }

    /**
     * Determine if moving left is a winning move or not
     *
     * @param totalBallCnt Used together with [curSearchLevel] parameter to determine whether it has found a solution or not.
     * @param curSearchLevel Current search level. Used together with [totalBallCnt] parameter determined whether it has found a solution or not.
     * It is also used to calculate thinking progress
     * @param row Row position of the ball to move from
     * @param srcCol Column of the ball to move left from
     *
     * @return
     * - True if this is a winning move
     * - false if this is NOT a winning move
     */
    private fun winnableByMovingLeft(
        totalBallCnt: Int,
        curSearchLevel: Int,
        row: Int,
        srcCol: Int
    ): Boolean {
        val targetCol = findTargetColOnMoveLeft(row, srcCol)

        if (targetCol == -1) {
            return false
        }

        val tempBoard = this.duplicate() // Clone the board

        // Make the actual move
        tempBoard.moveLeft(srcCol, targetCol, row)

        if (totalBallCnt == (curSearchLevel + 1)) {
            Log.d(
                "GM: Level $curSearchLevel",
                "TOTAL BALL is $totalBallCnt : FOUND A WINNABLE MOVE BY MOVING LEFT"
            )
            return true
        }

        val (direction, _, _) = tempBoard.foundWinningMove(totalBallCnt, (curSearchLevel + 1), 1)

        if ((direction == Direction.NO_WINNING_DIRECTION) || (direction == Direction.INCOMPLETE)) {
            return (false)
        }

        return true
    }

    /**
     * Find the target Row above it. If it can not find one, then there is no move possible and it will
     * return -1
     *
     * @param srcRow Source row
     * @param col Current column
     *
     * @return Return the number of row it can move to. If it can not find any room, it will return -1
     */
    fun findTargetRowOnMoveUp(srcRow: Int, col: Int): Int {
        // If you are near the top of the grid, then you do not have any room to move up
        if (srcRow <= 1) {
            return -1
        }

        // If there is already a Ball right on top, then there is no room to move. You need at least 1 free grid box to start the move
        if (flickerGrid[srcRow - 1][col]) {
            return -1
        }

        // Now we have room to move, so let's find the target row
        var targetRow = (srcRow - 2)

        while ((!flickerGrid[targetRow][col]) && (targetRow > 0)) {
            targetRow--
        }

        if ((targetRow == 0) && (!flickerGrid[0][col])) {
            return -1
        }

        targetRow += 1

        return (targetRow)
    }

    /**
     * Find the target Row below it. If it can not find one, then there is no move possible and it will
     * return -1
     *
     * @param srcRow Source row
     * @param col Current column
     *
     * @return Return the number of row it can move to. If it can not find any room, it will return -1
     */
    fun findTargetRowOnMoveDown(srcRow: Int, col: Int): Int {
        // If you are near the bottom of the grid, then you do not have any room to move down
        if (srcRow > (Global.MAX_ROW_SIZE - 3)) {
            return -1
        }

        // If there is already a Ball right on bottom, then there is no room to move. You need at least 1 free grid box to start the move
        if (flickerGrid[srcRow + 1][col]) {
            return -1
        }

        // Now we have room to move, so let's find the target row
        var targetRow = (srcRow + 2)

        while ((!flickerGrid[targetRow][col]) && (targetRow < (Global.MAX_ROW_SIZE - 1))) {
            targetRow++
        }

        if ((targetRow == (Global.MAX_ROW_SIZE - 1)) && (!flickerGrid[(Global.MAX_ROW_SIZE - 1)][col])) {
            return -1
        }

        targetRow -= 1

        return (targetRow)
    }

    /**
     * Find the target column to its right. If it can not find one, then there is no move possible and it will
     * return -1
     *
     * @param row Current row
     * @param srcCol Source column
     *
     * @return Return the number of column it can move to. If it can not find any room, it will return -1
     */
    fun findTargetColOnMoveRight(row: Int, srcCol: Int): Int {
        // If you are near the right of the grid, then you do not have any room to move right
        if (srcCol > (Global.MAX_COL_SIZE - 3)) {
            return -1
        }

        // If there is already a Ball right on right, then there is no room to move. You need at least 1 free grid to start the move
        if (flickerGrid[row][(srcCol + 1)]) {
            return -1
        }

        // Now we have room to move, so let's find the target col
        var targetCol = (srcCol + 2)

        while ((!flickerGrid[row][targetCol]) && (targetCol < (Global.MAX_COL_SIZE - 1))) {
            targetCol++
        }

        if ((targetCol == (Global.MAX_COL_SIZE - 1)) && (!flickerGrid[row][(Global.MAX_COL_SIZE - 1)])) {
            return -1
        }

        targetCol -= 1

        return (targetCol)
    }

    /**
     * Find the target column to its left. If it can not find one, then there is no move possible and it will
     * return -1
     *
     * @param row Current row
     * @param srcCol Source column
     *
     * @return Return the number of column it can move to. If it can not find any room, it will return -1
     */
    fun findTargetColOnMoveLeft(row: Int, srcCol: Int): Int {
        // If you are near the left of the grid, then you do not have any room to move left
        if (srcCol < 2) {
            return -1
        }

        // If there is already a Ball right on left, then there is no room to move. You need at least 1 free grid box to start the move
        if (flickerGrid[row][(srcCol - 1)]) {
            return -1
        }

        // Now we have room to move, so let's find the target row
        var targetCol = (srcCol - 2)

        while ((!flickerGrid[row][targetCol]) && (targetCol > 0)) {
            targetCol--
        }

        if ((targetCol == 0) && (!flickerGrid[row][0])) {
            return -1
        }

        targetCol += 1

        return (targetCol)
    }

    /**
     * Move the ball upward from the source row to the target row
     * Bump adjacent balls as needed
     *
     * @param srcRow Row position of the ball to move from
     * @param targetRow Target row to move to
     * @param col column involved
     *
     * @return Return the number of column it can move to. If it can not find any room, it will return -1
     */
    fun moveUp(srcRow: Int, targetRow: Int, col: Int) {
        // Do the first move
        if (!flickerGrid[srcRow][col]) error("Unexpected ball status. row=$srcRow, col=$col should be true")
        if (flickerGrid[targetRow][col]) error("Unexpected ball status. row=$targetRow, col=$col should be false")

        flickerGrid[srcRow][col] = false
        flickerGrid[targetRow][col] = true

        // There must be a ball adjacent.
        var nextSrcRow = targetRow - 1
        if (!(flickerGrid[nextSrcRow][col])) error("Unexpected grid value. This adjacent box must have a ball, i.e. true")

        // Handle the edge first. If the next row is already at the top, then just move it out of the grid and you're done
        if (nextSrcRow == 0) {
            flickerGrid[0][col] = false // Fell of the edge. One less ball
            return
        }

        // If the next row is at 2nd row (e.g. row = 1), then handle it special since we are near the edge
        if (nextSrcRow == 1) {
            if (flickerGrid[0][col]) {
                // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
                flickerGrid[0][col] = false // Fell of the edge. One less ball
            } else {
                // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
                flickerGrid[1][col] = false // Fell of the edge. One less ball
            }
            return
        }

        // Done handling the edge. Now handle the inner part of the grid

        var indexRow = nextSrcRow - 1
        // Move nextSrcRow pointer to the next one that has space to move the ball
        while ((flickerGrid[indexRow][col]) && (indexRow > 0)) {
            // There is no space. Then move nextRow pointer until there is space
            indexRow--
        }

        // Check if we have continuous balls all the way to the edge
        if (indexRow == 0) {
            if (flickerGrid[0][col]) {
                flickerGrid[0][col] = false // Fell of the edge. One less ball
            } else {
                if (flickerGrid[1][col]) {
                    flickerGrid[1][col] = false
                }
            }
            return
        }

        nextSrcRow = indexRow + 1

        // If there is space, then need to check if there is any more ball above it
        // If no ball above it, then remove this ball from grid and you are done.
        // Otherwise, we need re-iterate the process in the next chain
        var foundBallBeforeEdge = false
        indexRow = nextSrcRow - 1

        while ((indexRow > 0) && (!foundBallBeforeEdge)) {
            if (flickerGrid[indexRow][col])
                foundBallBeforeEdge = true
            else
                indexRow--
        }

        // Need to handle one more edge case
        if ((!foundBallBeforeEdge) && (indexRow == 0)) {
            if (flickerGrid[0][col]) foundBallBeforeEdge = true
        }

        if ((indexRow == 0) && (!foundBallBeforeEdge)) {
            flickerGrid[nextSrcRow][col] = false  // Fell of the edge. One less ball
            return
        }

        // We have a chain. Call itself recursively
        val srcRowNew = nextSrcRow
        val targetRowNew = indexRow + 1
        moveUp(srcRowNew, targetRowNew, col)

        return
    }

    fun moveDown(srcRow: Int, targetRow: Int, col: Int) {
        // Do the first move
        if (!flickerGrid[srcRow][col]) error("Unexpected ball status. row=$srcRow, col=$col should be true")
        if (flickerGrid[targetRow][col]) error("Unexpected ball status. row=$targetRow, col=$col should be false")
        flickerGrid[srcRow][col] = false
        flickerGrid[targetRow][col] = true

        // There must be a ball adjacent.
        var nextSrcRow = targetRow + 1
        if (!(flickerGrid[nextSrcRow][col])) error("Unexpected grid value. This adjacent box must have a ball, i.e. true")

        // Handle the edge first
        // If the next row is already at the bottom, then just move it out of the grid and you're done
        if (nextSrcRow == (Global.MAX_ROW_SIZE - 1)) {
            flickerGrid[(Global.MAX_ROW_SIZE - 1)][col] = false // Fell of the edge. One less ball
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            return
        }

        // If the next row is at 2nd last row (e.g. row = (Max Row - 2))), then handle it special since we are near the edge
        if (nextSrcRow == (Global.MAX_ROW_SIZE - 2)) {
            if (flickerGrid[(Global.MAX_ROW_SIZE - 1)][col]) {
                flickerGrid[(Global.MAX_ROW_SIZE - 1)][col] =
                    false // Fell of the edge. One less ball
            } else {
                flickerGrid[(Global.MAX_ROW_SIZE - 2)][col] =
                    false // Fell of the edge. One less ball
            }
            return
        }

        // Done handling the edge. Now handle the inner part of the grid

        var indexRow = nextSrcRow + 1
        // Move nextSrcRow pointer to the next one that has space to move the ball
        while ((flickerGrid[indexRow][col]) && (indexRow < (Global.MAX_ROW_SIZE - 1))) {
            // There is no space. Then move indexRow pointer until there is space
            indexRow++
        }

        // Check if we have continuous balls all the way to the edge
        if (indexRow == (Global.MAX_ROW_SIZE - 1)) {
            if (flickerGrid[Global.MAX_ROW_SIZE - 1][col]) {
                flickerGrid[Global.MAX_ROW_SIZE - 1][col] = false // Fell of the edge. One less ball
            } else {
                if (flickerGrid[Global.MAX_ROW_SIZE - 2][col]) {
                    flickerGrid[Global.MAX_ROW_SIZE - 2][col] = false
                }
            }
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            return
        }

        nextSrcRow = indexRow - 1

        // If there is space, then need to check if there is any more ball below it
        // If no ball below it, then remove this ball from grid and you are done.
        // Otherwise, we need re-iterate the process in the next chain
        var foundBallBeforeEdge = false
        indexRow = nextSrcRow + 1
        while ((indexRow < (Global.MAX_ROW_SIZE - 1)) && (!foundBallBeforeEdge)) {
            if (flickerGrid[indexRow][col])
                foundBallBeforeEdge = true
            else
                indexRow++
        }

        // Need to handle one more edge case
        if ((!foundBallBeforeEdge) && (indexRow == (Global.MAX_ROW_SIZE - 1))) {
            if (flickerGrid[Global.MAX_ROW_SIZE - 1][col]) foundBallBeforeEdge = true
        }

        if ((indexRow == (Global.MAX_ROW_SIZE - 1)) && (!foundBallBeforeEdge)) {
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            flickerGrid[nextSrcRow][col] = false  // Fell of the edge. One less ball
            return
        }

        // We have a chain. Call itself recursively
        val srcRowNew = nextSrcRow
        val targetRowNew = indexRow - 1
        moveDown(srcRowNew, targetRowNew, col)

        return
    }

    fun moveRight(srcCol: Int, targetCol: Int, row: Int) {
        // Do the first move
        if (!flickerGrid[row][srcCol]) error("Unexpected ball status. row=$row, col=$srcCol should be true")
        if (flickerGrid[row][targetCol]) error("Unexpected ball status. row=$row, col=$targetCol should be false")
        flickerGrid[row][srcCol] = false
        flickerGrid[row][targetCol] = true

        // There must be a ball adjacent. to its right
        var nextSrcCol = targetCol + 1
        if (!(flickerGrid[row][nextSrcCol])) error("Unexpected grid value. This adjacent box must have a ball, i.e. true")

        // Handle the edge first
        // If the next column is already at the right, then just move it out of the grid and you're done
        if (nextSrcCol == (Global.MAX_COL_SIZE - 1)) {
            flickerGrid[row][Global.MAX_COL_SIZE - 1] = false // Fell of the edge. One less ball
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            return
        }

        // If the next row is at 2nd last column (e.g. col = (Max Col - 2))), then handle it special since we are near the edge
        if (nextSrcCol == (Global.MAX_COL_SIZE - 2)) {
            if (flickerGrid[row][(Global.MAX_COL_SIZE - 1)]) {
                flickerGrid[row][(Global.MAX_COL_SIZE - 1)] =
                    false // Fell of the edge. One less ball
            } else {
                flickerGrid[row][(Global.MAX_COL_SIZE - 2)] =
                    false // Fell of the edge. One less ball
            }
            return
        }

        // Done handling the edge. Now handle the inner part of the grid

        var indexCol = nextSrcCol + 1
        // Move nextSrcCol pointer to the next one that has space to move the ball
        while ((flickerGrid[row][indexCol]) && (indexCol < (Global.MAX_COL_SIZE - 1))) {
            // There is no space. Then move indexCol pointer until there is space
            indexCol++
        }

        // Check if we have continuous balls all the way to the edge
        if (indexCol == (Global.MAX_COL_SIZE - 1)) {

            if (flickerGrid[row][Global.MAX_COL_SIZE - 1]) {
                flickerGrid[row][Global.MAX_COL_SIZE - 1] = false // Fell of the edge. One less ball
            } else {
                if (flickerGrid[row][Global.MAX_COL_SIZE - 2]) {
                    flickerGrid[row][Global.MAX_COL_SIZE - 2] = false
                }
            }

            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            return
        }

        nextSrcCol = indexCol - 1

        // If there is space, then need to check if there is any more ball to its right
        // If no ball on the right, then remove this ball from grid and you are done.
        // Otherwise, we need re-iterate the process in the next chain
        var foundBallBeforeEdge = false
        indexCol = nextSrcCol + 1

        // while ((indexCol < MaxColSize) && (!foundBallBeforeEdge)) {
        while ((indexCol < (Global.MAX_COL_SIZE - 1)) && (!foundBallBeforeEdge)) {
            if (flickerGrid[row][indexCol])
                foundBallBeforeEdge = true
            else
                indexCol++
        }

        // Need to handle one more edge case
        if ((!foundBallBeforeEdge) && (indexCol == (Global.MAX_COL_SIZE - 1))) {
            if (flickerGrid[row][Global.MAX_COL_SIZE - 1]) foundBallBeforeEdge = true
        }

        if ((indexCol == (Global.MAX_COL_SIZE - 1)) && (!foundBallBeforeEdge)) {
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            flickerGrid[row][nextSrcCol] = false  // Fell of the edge. One less ball
            return
        }

        // We have a chain. Call itself recursively
        val srcColNew = nextSrcCol
        val targetColNew = indexCol - 1
        moveRight(srcColNew, targetColNew, row)

        return
    }

    fun moveLeft(srcCol: Int, targetCol: Int, row: Int) {
        // Do the first move
        if (!flickerGrid[row][srcCol]) error("Unexpected ball status. row=$row, col=$srcCol should be true")
        if (flickerGrid[row][targetCol]) error("Unexpected ball status. row=$row, col=$targetCol should be false")
        flickerGrid[row][srcCol] = false
        flickerGrid[row][targetCol] = true

        // There must be a ball adjacent.
        var nextSrcCol = targetCol - 1
        if (!(flickerGrid[row][nextSrcCol])) error("Unexpected grid value. This adjacent box must have a ball, i.e. true")

        // Handle the edge first
        // If the next column is already at the left, then just move it out of the grid and you're done
        if (nextSrcCol == 0) {
            flickerGrid[row][0] = false // Fell of the edge. One less ball
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            return
        }

        // If the next col is at 2nd last column (e.g. col == 1))), then handle it special since we are near the edge
        if (nextSrcCol == 1) {
            if (flickerGrid[row][0]) {
                flickerGrid[row][0] = false // Fell of the edge. One less ball
                // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            } else {
                flickerGrid[row][1] = false // Fell of the edge. One less ball
                // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            }
            return
        }

        // Done handling the edge. Now handle the inner part of the grid

        var indexCol = nextSrcCol - 1
        // Move nextSrcCol pointer to the next one that has space to move the ball
        while ((flickerGrid[row][indexCol]) && (indexCol > 0)) {
            // There is no space. Then move indexCol pointer until there is space
            indexCol--
        }

        // Check if we have continuous balls all the way to the edge
        if (indexCol == 0) {

            if (flickerGrid[row][0]) {
                flickerGrid[row][0] = false // Fell of the edge. One less ball
            } else {
                if (flickerGrid[row][1]) {
                    flickerGrid[row][1] = false
                }
            }

            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            return
        }

        nextSrcCol = indexCol + 1

        // If there is space, then need to check if there is any more ball to its right
        // If no ball on the right, then remove this ball from grid and you are done.
        // Otherwise, we need re-iterate the process in the next chain
        var foundBallBeforeEdge = false
        indexCol = nextSrcCol - 1
        while ((indexCol > 0) && (!foundBallBeforeEdge)) {
            if (flickerGrid[row][indexCol])
                foundBallBeforeEdge = true
            else
                indexCol--
        }

        // Need to handle one more edge case
        if ((!foundBallBeforeEdge) && (indexCol == 0)) {
            if (flickerGrid[row][0]) foundBallBeforeEdge = true
        }

        if ((indexCol == 0) && (!foundBallBeforeEdge)) {
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            flickerGrid[row][nextSrcCol] = false  // Fell of the edge. One less ball
            return
        }

        // We have a chain. Call itself recursively
        val srcColNew = nextSrcCol
        val targetColNew = indexCol + 1
        moveLeft(srcColNew, targetColNew, row)

        return
    }

    fun updateBallList(): SnapshotStateList<Pos> {
        val ballList: SnapshotStateList<Pos> = SnapshotStateList<Pos>().apply {

            // Clone the board
            repeat(Global.MAX_ROW_SIZE) { curRow ->
                repeat(Global.MAX_COL_SIZE) { curCol ->
                    if (flickerGrid[curRow][curCol]) {
                        val solverGridPos = Pos(curRow, curCol)
                        add(solverGridPos)
                    }
                }
            }
        }
        return ballList
    }

     data class ValidMove(
         var addPosList : MutableList<Pos> = mutableListOf(),
         var delPosList : MutableList<Pos> = mutableListOf()
    )

    /**
     * Move back one move
     */
    fun moveBack() {

        // Build a list of valid moves
        // Each ball on grid could potentially have up to 4 valid moves (i.e. one in each direction)

        val validMoveList =  mutableListOf<ValidMove>()

        repeat(Global.MAX_ROW_SIZE) { curRow ->
            repeat(Global.MAX_COL_SIZE) { curCol ->
                if (flickerGrid[curRow][curCol]) {

                    Log.i(Global.DEBUG_PREFIX, "Prev Move [$curRow, $curCol]")

                    var move = backMoveUp(curRow, curCol)
                    if (move != null) { validMoveList.add(move) }

                    move = backMoveDown(curRow, curCol)
                    if (move != null) { validMoveList.add(move) }

                    move = backMoveLeft(curRow, curCol)
                    if (move != null) { validMoveList.add((move)) }

                    move = backMoveRight(curRow, curCol)
                    if (move != null) { validMoveList.add((move)) }

                }  // if flickerGrid[curRow][curCol] has a ball
            }
        }

        val validMoveCount = validMoveList.size
        if (validMoveCount == 0) {
            Log.i(Global.DEBUG_PREFIX, "Unable to find any valid move")
            return
        }

        // Randomly pick one of the valid moves
        val i = Random.nextInt(validMoveCount)

        Log.i(Global.DEBUG_PREFIX, "==================== LIST OF VALID MOVES ==================")
        validMoveList.forEach {
            Log.i(Global.DEBUG_PREFIX, "add pos: ${it.addPosList}    del pos: ${it.delPosList}")
        }
        val chosenValidMove = validMoveList[i]

        Log.i(Global.DEBUG_PREFIX, "Chosen: add: ${chosenValidMove.addPosList}    del: ${chosenValidMove.delPosList}")

        // Do the actual move
        chosenValidMove.addPosList.forEach { curPos ->
            flickerGrid[curPos.row][curPos.col] = true
        }
        chosenValidMove.delPosList.forEach { curPos->
            flickerGrid[curPos.row][curPos.col] = false
        }

        return
    }

    /**
     * Move back one move. Handling the UP direction only.
     * - Find the departing ball source position
     * - Find the action ball source position
     * Put in these values into the move record
     *
     * @param actionBallTargetRow Position of the target action ball (row)
     * @param actionBallTargetCol Position of the target action ball (column)
     *
     * @return [ValidMove]
     *  If it is null, then it means there is no valid. Otherwise, it contain valid move
     */
    private fun backMoveUp(
        actionBallTargetRow: Int, actionBallTargetCol: Int
    ): ValidMove? {
        val move = ValidMove()

        // Find the departing ball source position

        // Target action ball is on top edge. No room to have a departing ball above it.
        // Hence, this is not a valid move
        if (actionBallTargetRow == 0) return  (null)

        // Make sure there is a clear path for departing ball to fall off the grid
        var curTargetRowDeparting = actionBallTargetRow - 1
        var validMove = true
        while (curTargetRowDeparting > 0) {
            if (flickerGrid[curTargetRowDeparting][actionBallTargetCol]) {
                validMove = false
                break
            }
            curTargetRowDeparting--
        }

        // This is invalid move if target ball could not fall off the grid since as there is
        // other ball in the path
        if (!validMove) return (null)

        // The departing ball is good
        val departBallUpPos = Pos((actionBallTargetRow - 1), actionBallTargetCol)

        /************************************************************/

        // Find the action source ball

        // Check for sufficient space for action ball to hit the target ball
        // in the up direction

        // Destination action ball is at bottom edge. No room for action ball to move up to it
        if (actionBallTargetRow == (Global.MAX_ROW_SIZE - 1)) return (null)

        // Find the number of spaces for action ball to move upward
        var curActionBallRow = actionBallTargetRow + 1
        var numSpaceForActionBall = 0
        while (curActionBallRow <= (Global.MAX_ROW_SIZE - 1)) {
            if (flickerGrid[curActionBallRow][actionBallTargetCol]) {
                // There is a ball in this location. So we reached the end of path to put in the source ball
                break
            }
            numSpaceForActionBall++
            curActionBallRow++
        }

        // There is no room for action ball to move, hence this is not valid
        if (numSpaceForActionBall == 0) return (null)

        // We have a valid move. Now we need to randomly choose one of the source position
        val actionBallSrcUpPos =
            if (numSpaceForActionBall == 1) {
                Pos((actionBallTargetRow + 1), actionBallTargetCol)
            } else {
                Pos((actionBallTargetRow + (Random.nextInt(numSpaceForActionBall) + 1)), actionBallTargetCol)
            }

        move.addPosList.add(actionBallSrcUpPos)
        move.addPosList.add(departBallUpPos)
        move.delPosList.add(Pos(actionBallTargetRow, actionBallTargetCol))

        return (move)
    }

    /**
     * Move back one move. Handling the DOWN direction only.
     * - Find the departing ball source position
     * - Find the action ball source position
     * Put in these values into the move record
     *
     * @param actionBallTargetRow Position of the target action ball (row)
     * @param actionBallTargetCol Position of the target action ball (column)
     *
     * @return [ValidMove]
     * If it is null, then it means there is no valid. Otherwise, it contain valid move
     */
    private fun backMoveDown(
        actionBallTargetRow: Int, actionBallTargetCol: Int
    ): ValidMove? {
        val move = ValidMove()

        // Find the departing ball source position

        // Target action ball is on bottom edge. No room to have a departing ball below it.
        // Hence, this is not a valid move
        if (actionBallTargetRow == (Global.MAX_ROW_SIZE - 1)) return (null)

        // Make sure there is a clear path for departing ball to fall off the grid
        var curTargetRowDeparting = actionBallTargetRow + 1
        var validMove = true
        while (curTargetRowDeparting < Global.MAX_ROW_SIZE) {
            if (flickerGrid[curTargetRowDeparting][actionBallTargetCol]) {
                validMove = false
                break
            }
            curTargetRowDeparting++
        }

        // This is invalid move if target ball could not fall off the grid since as there is
        // other ball in the path
        if (!validMove)  return (null)

        // The departing ball is good
        val departBallDownPos = Pos((actionBallTargetRow + 1), actionBallTargetCol)

        /************************************************************/

        // Find the action source ball

        // Check for sufficient space for action ball to hit the target ball
        // in the down direction

        // Destination action ball is at top edge. No room for action ball to move down to it
        if (actionBallTargetRow == 0)  return (null)

        // Find the number of spaces for action ball to move downward
        var curActionBallRow = actionBallTargetRow - 1
        var numSpaceForActionBall = 0
        while (curActionBallRow >= 0) {
            if (flickerGrid[curActionBallRow][actionBallTargetCol]) {
                // There is a ball in this location. So we reached the end of path to put in the source ball
                break
            }
            numSpaceForActionBall++
            curActionBallRow--
        }

        // If is no room for action ball to move, then this is not a valid move
        if (numSpaceForActionBall == 0)  return (null)

        // We have a valid move. Now we need to randomly choose one of the source position
        val actionBallSrcDownPos =
            if (numSpaceForActionBall == 1) {
                 Pos((actionBallTargetRow - 1), actionBallTargetCol)
            } else {
                 Pos((actionBallTargetRow - (Random.nextInt(numSpaceForActionBall) + 1)), actionBallTargetCol)
            }

        move.addPosList.add(actionBallSrcDownPos)
        move.addPosList.add(departBallDownPos)
        move.delPosList.add(Pos(actionBallTargetRow, actionBallTargetCol))

        return (move)
    }

    /**
     * Move back one move. Handling the LEFT direction only.
     * - Find the departing ball source position
     * - Find the action ball source position
     * Put in these values into the move record
     *
     * @param actionBallTargetRow Position of the target action ball (row)
     * @param actionBallTargetCol Position of the target action ball (column)
     *
     * @return [ValidMove]
     *  If it is null, then it means there is no valid. Otherwise, it contain valid move
     */
    private fun backMoveLeft(
        actionBallTargetRow: Int, actionBallTargetCol: Int
    ): ValidMove?
    {
        val move = ValidMove()

        // Find the departing ball source position

        // Target action ball is on left edge. No room to have a departing ball on the left of it.
        // Hence, this is not a valid move
        if (actionBallTargetCol == 0) { return (null) }

        // Make sure there is a clear path for departing ball to fall off the grid
        var curTargetColDeparting = actionBallTargetCol - 1
        var validMove = true
        while (curTargetColDeparting > 0) {
            if (flickerGrid[actionBallTargetRow][curTargetColDeparting]) {
                validMove = false
                break
            }
            curTargetColDeparting--
        }

        // This is invalid move if target ball could not fall off the grid as there is
        // other ball in the path
        if (!validMove) { return (null) }

        // The departing ball is good
        val departBallLeftPos = Pos((actionBallTargetRow), actionBallTargetCol - 1)

        /************************************************************/

        // Find the action source ball

        // Check for sufficient space for action ball to hit the target ball
        // in the left direction

        // Destination action ball is at right edge. No room to have action ball move from the left
        if (actionBallTargetCol == (Global.MAX_COL_SIZE - 1)) { return (null) }

        // Find the number of spaces for action ball to move from the left
        var curActionBallCol = actionBallTargetCol + 1
        var numSpaceForActionBall = 0
        while (curActionBallCol <= (Global.MAX_COL_SIZE - 1)) {
            if (flickerGrid[actionBallTargetRow][curActionBallCol]) {
                // There is a ball in this location. So we reached the end of path to put in the source ball
                break
            }
            numSpaceForActionBall++
            curActionBallCol++
        }

        // There is no room for action ball to move, hence this is not valid
        if (numSpaceForActionBall == 0) { return (null) }

        // We have a valid move. Now we need to randomly choose one of the source position
        val actionBallSrcLeftPos =
            if (numSpaceForActionBall == 1) {
                Pos(actionBallTargetRow, (actionBallTargetCol + 1))
            } else {
                Pos(actionBallTargetRow, (actionBallTargetCol + Random.nextInt(numSpaceForActionBall) + 1))
            }

        move.addPosList.add(departBallLeftPos)
        move.addPosList.add(actionBallSrcLeftPos)
        move.delPosList.add(Pos(actionBallTargetRow, actionBallTargetCol))

        return (move)
    }

    /**
     * Move back one move. Handling the RIGHT direction only.
     * - Find the departing ball source position
     * - Find the action ball source position
     * Put in these values into the move record
     *
     * @param actionBallTargetRow Position of the target action ball (row)
     * @param actionBallTargetCol Position of the target action ball (column)
     *
     * @return [ValidMove]
     *  If it is null, then it means there is no valid. Otherwise, it contain valid move
     */
    private fun backMoveRight(
        actionBallTargetRow: Int, actionBallTargetCol: Int
    ): ValidMove? {
        val move = ValidMove()

        // Find the departing ball source position

        // Target action ball is on right edge. No room to have a departing ball on the right of it.
        // Hence, this is not a valid move
        if (actionBallTargetCol == (Global.MAX_COL_SIZE - 1)) { return (null) }

        // Make sure there is a clear path for departing ball to fall off the grid
        var curTargetColDeparting = actionBallTargetCol + 1
        var validMove = true
        while (curTargetColDeparting <= (Global.MAX_COL_SIZE - 1 )) {
            if (flickerGrid[actionBallTargetRow][curTargetColDeparting]) {
                validMove = false
                break
            }
            curTargetColDeparting++
        }

        // This is invalid move if target ball could not fall off the grid since as there is
        // other ball in the path
        if (!validMove) { return (null) }

        // The departing ball is good
        val departBallRightPos = Pos((actionBallTargetRow), actionBallTargetCol + 1)

        /************************************************************/

        // Find the action source ball

        // Check for sufficient space for action ball to hit the target ball
        // in the right direction

        // Destination action ball is at left edge. No room to have action ball move from the right
        if (actionBallTargetCol == 0) { return (null) }

        // Find the number of spaces for action ball to move from the right
        var curActionBallCol = actionBallTargetCol - 1
        var numSpaceForActionBall = 0
        while (curActionBallCol >= 0) {
            if (flickerGrid[actionBallTargetRow][curActionBallCol]) {
                // There is a ball in this location. So we reached the end of path to put in the source ball
                break
            }
            numSpaceForActionBall++
            curActionBallCol--
        }

        // There is no room for action ball to move, hence this is not valid
        if (numSpaceForActionBall == 0) { return (null) }

        // We have a valid move. Now we need to randomly choose one of the source position
        val actionBallSrcRightPos =
            if (numSpaceForActionBall == 1) {
                Pos(actionBallTargetRow, (actionBallTargetCol - 1))
            } else {
                Pos(actionBallTargetRow, (actionBallTargetCol - Random.nextInt(numSpaceForActionBall)-1))
            }

        move.addPosList.add(departBallRightPos)
        move.addPosList.add(actionBallSrcRightPos)
        move.delPosList.add(Pos(actionBallTargetRow, actionBallTargetCol))

        return (move)
    }
}

