package com.darblee.flingaid.ui

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.darblee.flingaid.Direction
import com.darblee.flingaid.Global

class SolverEngine {
    private var flingGrid = Array(Global.MaxRowSize) { BooleanArray(Global.MaxColSize) }

    init {
        reset()
    }

    private fun reset()
    {
        repeat (Global.MaxRowSize) { row ->
            repeat(Global.MaxColSize) { col ->
                flingGrid[row][col] = false
            }
        }
    }

    fun populateGrid(ballPositionList: SnapshotStateList<SolverGridPos>)
    {
        ballPositionList.forEach {pos ->
            flingGrid[pos.row][pos.col] = true
        }
    }

    private fun duplicate() : SolverEngine
    {
        val tempBoard = SolverEngine()

        // Clone the board
        repeat (Global.MaxRowSize) { curRow ->
            repeat(Global.MaxColSize) { curCol ->
                tempBoard.flingGrid[curRow][curCol] = flingGrid[curRow][curCol]
            }
        }

        return (tempBoard)
    }

    fun foundWinningMove(totalBallCnt : Int, curSearchLevel : Int, thinkingDirectionOffset : Int): Triple<Direction, Int, Int>
    {

        var direction = Direction.NO_WINNING_DIRECTION
        var winningRow = -1
        var winningCol = -1

        val startRow: Int
        val exceededRow: Int
        val startColumn: Int
        val exceededCol:Int

        if (thinkingDirectionOffset == 1) {
            startRow = 0
            exceededRow = Global.MaxRowSize

            startColumn = 0
            exceededCol = Global.MaxColSize
        } else {
            startRow = Global.MaxRowSize - 1
            exceededRow = -1

            startColumn = Global.MaxColSize - 1
            exceededCol = -1
        }
        var curRow = startRow
        var currentCol: Int

        run repeatBlock@{
            while (curRow != exceededRow) {

                if (Thread.interrupted() || (Global.task1_WinningDirection != Direction.INCOMPLETE) || (Global.task2_WinningDirection != Direction.INCOMPLETE)) {
                    val threadInterrupt = Thread.interrupted()
                    Log.d("${Global.debugPrefix}:", "Short circuit on row processing. Thread interrupt: $threadInterrupt  task1: ${Global.task1_WinningDirection} task2: ${Global.task2_WinningDirection}")
                    direction = Direction.INCOMPLETE  // We should quit the current thread
                    return@repeatBlock

                }

                currentCol = startColumn

                var curCol : Int
                while (currentCol != exceededCol) {

                    curCol = currentCol

                    if (flingGrid[curRow][curCol]) {
                        if (curSearchLevel == 1) Log.i("${Global.debugPrefix}: Top level", "Processing row=$curRow, col = $curCol")

                        if (winnableByMovingUp(totalBallCnt, curSearchLevel, curRow, curCol)) {
                            direction = Direction.UP
                            winningRow = curRow
                            winningCol = curCol
                            Log.i("${Global.debugPrefix}:", "Level #$curSearchLevel, Found winning move at direction $direction when processing at row: $curRow col: $curCol")
                            return@repeatBlock
                        }

                        if (curSearchLevel == 2) {
                            Global.ThinkingProgress++
                        }
                        if (Thread.interrupted() || (Global.task1_WinningDirection != Direction.INCOMPLETE) || (Global.task2_WinningDirection != Direction.INCOMPLETE)) {
                            val threadInterrupt = Thread.interrupted()
                            Log.d("${Global.debugPrefix}:", "Short circuit on col processing after up. Thread interrupt: $threadInterrupt  task1: ${Global.task1_WinningDirection} task2: ${Global.task2_WinningDirection}")
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
                            Global.ThinkingProgress++
                        }
                        if (Thread.interrupted() || (Global.task1_WinningDirection != Direction.INCOMPLETE) || (Global.task2_WinningDirection != Direction.INCOMPLETE)) {
                            val threadInterrupt = Thread.interrupted()
                            Log.d("${Global.debugPrefix}:", "Short circuit on col processing after down. Thread interrupt: $threadInterrupt  task1: ${Global.task1_WinningDirection} task2: ${Global.task2_WinningDirection}")
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
                            Global.ThinkingProgress++
                        }
                        if (Thread.interrupted() || (Global.task1_WinningDirection != Direction.INCOMPLETE) || (Global.task2_WinningDirection != Direction.INCOMPLETE)) {
                            val threadInterrupt = Thread.interrupted()
                            Log.d("${Global.debugPrefix}:", "Short circuit on col processing after right. Thread interrupt: $threadInterrupt  task1: ${Global.task1_WinningDirection} task2: ${Global.task2_WinningDirection}")
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
                            Global.ThinkingProgress++
                        }
                        if (Thread.interrupted() || (Global.task1_WinningDirection != Direction.INCOMPLETE) || (Global.task2_WinningDirection != Direction.INCOMPLETE)) {
                            val threadInterrupt = Thread.interrupted()
                            Log.d("${Global.debugPrefix}:", "Short circuit on col processing after left. Thread interrupt: $threadInterrupt  task1: ${Global.task1_WinningDirection} task2: ${Global.task2_WinningDirection}")
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
            Log.d("${Global.debugPrefix}:", "Returning winning (or incomplete) result: Level #$curSearchLevel, Direction is $direction")

        return Triple(direction, winningRow, winningCol)
    }

    private fun winnableByMovingUp(totalBallCnt : Int, curSearchLevel: Int, srcRow: Int, col : Int) : Boolean
    {
        val targetRow = findTargetRowOnMoveUp(srcRow, col)

        if (targetRow == -1) {
            return false
        }

        // Now we will make the move. So let's create a temporary board to handle the move
        val tempBoard = this.duplicate() // Clone the board

        // Make the actual move
        tempBoard.moveUp(srcRow, targetRow, col)

        if (totalBallCnt == (curSearchLevel + 1)) {
            Log.d("GM: Level $curSearchLevel", "TotalBallCnt = $totalBallCnt : FOUND A WINNABLE MOVE BY MOVING UP")
            return true
        }

        val (direction, _, _) = tempBoard.foundWinningMove(totalBallCnt, (curSearchLevel + 1), 1)

        if ((direction == Direction.NO_WINNING_DIRECTION) || (direction == Direction.INCOMPLETE)) {
            // Log.d("GM: $curSearchLevel","$debugIndentation $methodName: Could not find winnable move")
            return (false)
        }

        return true
    }

    private fun winnableByMovingDown(totalBallCnt : Int, curSearchLevel: Int, srcRow: Int, col : Int) : Boolean
    {
        val targetRow = findTargetRowOnMoveDown(srcRow, col)

        if (targetRow == -1) {
            return false
        }

        val tempBoard = this.duplicate() // Clone the board

        // Make the actual move
        tempBoard.moveDown(srcRow, targetRow, col)

        if (totalBallCnt == (curSearchLevel + 1)) {
            Log.i("${Global.debugPrefix}: Level $curSearchLevel", "TotalBallCnt = $totalBallCnt : FOUND A WINNABLE MOVE BY MOVING DOWN")
            return true
        }

        val (direction, _, _) = tempBoard.foundWinningMove(totalBallCnt, (curSearchLevel + 1), 1)

        if ((direction == Direction.NO_WINNING_DIRECTION) || (direction == Direction.INCOMPLETE)) {
            return (false)
        }

        return true
    }

    private fun winnableByMovingRight(totalBallCnt: Int, curSearchLevel: Int, row: Int, srcCol : Int) : Boolean
    {

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
            Log.d("${Global.debugPrefix}: Level $curSearchLevel", "TotalBallCnt = $totalBallCnt : FOUND A WINNABLE MOVE BY MOVING RIGHT")
            return true
        }

        val (direction, _, _) = tempBoard.foundWinningMove(totalBallCnt, (curSearchLevel + 1), 1)

        if ((direction == Direction.NO_WINNING_DIRECTION) || (direction == Direction.INCOMPLETE)) {
            return (false)
        }

        return true
    }

    private fun winnableByMovingLeft(totalBallCnt : Int, curSearchLevel: Int, row: Int, srcCol : Int) : Boolean
    {
        val targetCol = findTargetColOnMoveLeft(row, srcCol)

        if (targetCol == -1) {
            return false
        }

        val tempBoard = this.duplicate() // Clone the board

        // Make the actual move
        tempBoard.moveLeft(srcCol, targetCol, row)

        if (totalBallCnt == (curSearchLevel + 1)) {
            Log.d("GM: Level $curSearchLevel", "TOTAL BALL is $totalBallCnt : FOUND A WINNABLE MOVE BY MOVING LEFT")
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
    fun findTargetRowOnMoveUp(srcRow: Int, col: Int) : Int {
        // If you are near the top of the grid, then you do not have any room to move up
        if (srcRow <= 1) {
            // Log.d("GM: Details", "     ==> No upper move as it is near the top already. REJECT")
            return -1
        }

        // If there is already a Ball right on top, then there is no room to move. You need at least 1 free grid box to start the move
        if (flingGrid[srcRow - 1][col]) {
            // Log.d("GM: Details", "     ==> No upper move as there is a ball right on top. It leave no room to move. REJECT")
            return -1
        }

        // Now we have room to move, so let's find the target row
        var targetRow = (srcRow  - 2)
        // Log.d("GM: Details", "SrcRow is $srcRow Target $targetRow")

        while ((!flingGrid[targetRow][col]) && (targetRow > 0)) {
            targetRow--
            // Log.d("GM: Details", "     ==> Exploring Target $targetRow")
        }

        if ((targetRow == 0) && (!flingGrid[0][col])) {
            // Log.d("GM: Details", "     ==> No upper ball to knock off the edge. REJECT")
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
    fun findTargetRowOnMoveDown(srcRow: Int, col: Int) : Int {
        // If you are near the bottom of the grid, then you do not have any room to move down
        if (srcRow > (Global.MaxRowSize - 3)) {
            return -1
        }

        // If there is already a Ball right on bottom, then there is no room to move. You need at least 1 free grid box to start the move
        if (flingGrid[srcRow + 1][col]) {
            // Log.d("GM: Details", "     ==> No lower move as there is a ball right on bottom. It leave no room to move down. REJECT")
            return -1
        }

        // Now we have room to move, so let's find the target row
        var targetRow = (srcRow + 2)
        // Log.d("GM: Details", "     ==> SrcRow is $srcRow Target $targetRow")

        while ((!flingGrid[targetRow][col]) && (targetRow < (Global.MaxRowSize - 1))) {
            targetRow++
            // Log.d("GM: Details", "     ==> Exploring Target $targetRow")
        }

        if ((targetRow == (Global.MaxRowSize - 1)) && (!flingGrid[(Global.MaxRowSize - 1)][col])) {
            // Log.d("GM: Details", "     ==> No lower ball to knock off the edge. REJECT")
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
    fun findTargetColOnMoveRight(row: Int, srcCol: Int) : Int
    {

        // If you are near the right of the grid, then you do not have any room to move right
        if (srcCol > (Global.MaxColSize - 3)) {
            // Log.d("GM: Details", "     ==> No right move as it is near the right edge already. REJECT")
            return -1
        }

        // If there is already a Ball right on right, then there is no room to move. You need at least 1 free grid to start the move
        if (flingGrid[row][(srcCol + 1)]) {
            // Log.d("GM: Details", "     ==> No right move as there is a ball on the right. It leave no room to move down. REJECT")
            return -1
        }

        // Now we have room to move, so let's find the target col
        var targetCol = (srcCol + 2)
        // Log.d("GM: Details", "     ==> SrcCol is $srcCol Target $targetCol")

        while ((!flingGrid[row][targetCol]) && (targetCol < (Global.MaxColSize - 1))) {
            targetCol++
            // Log.d("GM: Details", "     ==> Exploring Target $targetCol")
        }

        if ((targetCol == (Global.MaxColSize - 1)) && (!flingGrid[row][(Global.MaxColSize - 1)])) {
            // Log.d("GM: Details", "     ==> No ball at the right to knock off the edge. REJECT")
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
    fun findTargetColOnMoveLeft(row: Int, srcCol: Int) : Int
    {
        // If you are near the left of the grid, then you do not have any room to move left
        if (srcCol < 2) {
            // Log.d("GM: Details", "     ==> No left move as it is near the left edge already. REJECT")
            return -1
        }

        // If there is already a Ball right on left, then there is no room to move. You need at least 1 free grid box to start the move
        if (flingGrid[row][(srcCol - 1)]) {
            // Log.d("GM: Details", "     ==> No left move as there is a ball on the left. It leave no room to move left. REJECT")
            return -1
        }

        // Now we have room to move, so let's find the target row
        var targetCol = (srcCol - 2)
        // Log.d("GM: Details", "     ==> SrcCol is $srcCol Target $targetCol")

        while ((!flingGrid[row][targetCol]) && (targetCol > 0)) {
            targetCol--
            // Log.d("GM: Details", "     ==> Exploring Target $targetCol")
        }

        if ((targetCol == 0) && (!flingGrid[row][0])) {
            // Log.d("GM: Details", "     ==> No ball at the left to knock off the edge. REJECT")
            return -1
        }

        targetCol += 1

        return (targetCol)
    }

    fun moveUp(srcRow : Int, targetRow : Int, col : Int)
    {
        // Do the first move
        if (!flingGrid[srcRow][col]) error("Unexpected ball status. row=$srcRow, col=$col should be true")
        if (flingGrid[targetRow][col]) error("Unexpected ball status. row=$targetRow, col=$col should be false")

        flingGrid[srcRow][col] = false
        flingGrid[targetRow][col] = true

        // There must be a ball adjacent.
        var nextSrcRow = targetRow - 1
        if (!(flingGrid[nextSrcRow][col])) error("Unexpected grid value. This adjacent box must have a ball, i.e. true")

        // Handle the edge first. If the next row is already at the top, then just move it out of the grid and you're done
        if (nextSrcRow == 0) {
            flingGrid[0][col] = false // Fell of the edge. One less ball
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            return
        }

        // If the next row is at 2nd row (e.g. row = 1), then handle it special since we are near the edge
        if (nextSrcRow == 1) {
            if (flingGrid[0][col]) {
                // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
                flingGrid[0][col] = false // Fell of the edge. One less ball
            } else {
                // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
                flingGrid[1][col] = false // Fell of the edge. One less ball
            }
            return
        }

        // Done handling the edge. Now handle the inner part of the grid

        var indexRow = nextSrcRow - 1
        // Move nextSrcRow pointer to the next one that has space to move the ball
        while ((flingGrid[indexRow][col]) && (indexRow > 0)){
            // There is no space. Then move nextRow pointer until there is space
            indexRow--
        }

        // Check if we have continuous balls all the way to the edge
        if (indexRow == 0) {
            if (flingGrid[0][col]) {
                flingGrid[0][col] = false // Fell of the edge. One less ball
            } else {
                if (flingGrid[1][col]) {
                    flingGrid[1][col] = false
                }
            }
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            return
        }

        nextSrcRow = indexRow + 1

        /**
         * If there is space, then need to check if there is any more ball above it
         * If no ball above it, then remove this ball from grid and you are done.
         * Otherwise, we need re-iterate the process in the next chain
         */
        var foundBallBeforeEdge = false
        indexRow = nextSrcRow - 1

        while ((indexRow > 0) && (!foundBallBeforeEdge)) {
            if (flingGrid[indexRow][col])
                foundBallBeforeEdge = true
            else
                indexRow--
        }

        // Need to handle one more edge case
        if ((!foundBallBeforeEdge) && (indexRow == 0)) {
            if (flingGrid[0][col]) foundBallBeforeEdge = true
        }

        if ((indexRow == 0) && (!foundBallBeforeEdge)) {
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            flingGrid[nextSrcRow][col] = false  // Fell of the edge. One less ball
            return
        }

        // We have a chain. Call itself recursively
        val srcRowNew = nextSrcRow
        val targetRowNew = indexRow + 1
        moveUp(srcRowNew, targetRowNew, col)

        return
    }

    fun moveDown(srcRow : Int, targetRow : Int, col : Int)
    {
        // Do the first move
        if (!flingGrid[srcRow][col]) error("Unexpected ball status. row=$srcRow, col=$col should be true")
        if (flingGrid[targetRow][col])  error("Unexpected ball status. row=$targetRow, col=$col should be false")
        flingGrid[srcRow][col] = false
        flingGrid[targetRow][col] = true

        // There must be a ball adjacent.
        var nextSrcRow = targetRow + 1
        if (!(flingGrid[nextSrcRow][col])) error("Unexpected grid value. This adjacent box must have a ball, i.e. true")

        // Handle the edge first
        // If the next row is already at the bottom, then just move it out of the grid and you're done
        if (nextSrcRow == (Global.MaxRowSize - 1)) {
            flingGrid[(Global.MaxRowSize - 1)][col] = false // Fell of the edge. One less ball
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            return
        }

        // If the next row is at 2nd last row (e.g. row = (Max Row - 2))), then handle it special since we are near the edge
        if (nextSrcRow == (Global.MaxRowSize - 2)) {
            if (flingGrid[(Global.MaxRowSize - 1)][col]) {
                flingGrid[(Global.MaxRowSize -1)][col] = false // Fell of the edge. One less ball
                // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            } else {
                flingGrid[(Global.MaxRowSize - 2)][col] = false // Fell of the edge. One less ball
                // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            }
            return
        }

        // Done handling the edge. Now handle the inner part of the grid

        var indexRow = nextSrcRow + 1
        // Move nextSrcRow pointer to the next one that has space to move the ball
        while ((flingGrid[indexRow][col]) && (indexRow < (Global.MaxRowSize - 1))){
            // There is no space. Then move indexRow pointer until there is space
            indexRow++
        }

        // Check if we have continuous balls all the way to the edge
        if (indexRow == (Global.MaxRowSize - 1)) {
            if (flingGrid[Global.MaxRowSize - 1][col]) {
                flingGrid[Global.MaxRowSize - 1][col] = false // Fell of the edge. One less ball
            } else {
                if (flingGrid[Global.MaxRowSize - 2][col]) {
                    flingGrid[Global.MaxRowSize - 2][col] = false
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
        while ((indexRow < (Global.MaxRowSize - 1)) && (!foundBallBeforeEdge)) {
            if (flingGrid[indexRow][col])
                foundBallBeforeEdge = true
            else
                indexRow++
        }

        // Need to handle one more edge case
        if ((!foundBallBeforeEdge) && (indexRow == (Global.MaxRowSize-1))) {
            if (flingGrid[Global.MaxRowSize-1][col]) foundBallBeforeEdge = true
        }

        if ((indexRow == (Global.MaxRowSize - 1)) && (!foundBallBeforeEdge)) {
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            flingGrid[nextSrcRow][col] = false  // Fell of the edge. One less ball
            return
        }

        // We have a chain. Call itself recursively
        val srcRowNew = nextSrcRow
        val targetRowNew = indexRow - 1
        moveDown(srcRowNew, targetRowNew, col)

        return
    }

    fun moveRight(srcCol : Int, targetCol : Int, row : Int)
    {
        // Do the first move
        if (!flingGrid[row][srcCol]) error("Unexpected ball status. row=$row, col=$srcCol should be true")
        if (flingGrid[row][targetCol])  error("Unexpected ball status. row=$row, col=$targetCol should be false")
        flingGrid[row][srcCol] = false
        flingGrid[row][targetCol] = true

        // There must be a ball adjacent. to its right
        var nextSrcCol = targetCol + 1
        if (!(flingGrid[row][nextSrcCol])) error("Unexpected grid value. This adjacent box must have a ball, i.e. true")

        // Handle the edge first
        // If the next column is already at the right, then just move it out of the grid and you're done
        if (nextSrcCol == (Global.MaxColSize - 1)) {
            flingGrid[row][Global.MaxColSize - 1] = false // Fell of the edge. One less ball
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            return
        }

        // If the next row is at 2nd last column (e.g. col = (Max Col - 2))), then handle it special since we are near the edge
        if (nextSrcCol == (Global.MaxColSize - 2)) {
            if (flingGrid[row][(Global.MaxColSize - 1)]) {
                flingGrid[row][(Global.MaxColSize - 1)] = false // Fell of the edge. One less ball
                // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            } else {
                flingGrid[row][(Global.MaxColSize - 2)] = false // Fell of the edge. One less ball
                // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            }
            return
        }

        // Done handling the edge. Now handle the inner part of the grid

        var indexCol = nextSrcCol + 1
        // Move nextSrcCol pointer to the next one that has space to move the ball
        while ((flingGrid[row][indexCol]) && (indexCol < (Global.MaxColSize - 1))){
            // There is no space. Then move indexCol pointer until there is space
            indexCol++
        }

        // Check if we have continuous balls all the way to the edge
        if (indexCol == (Global.MaxColSize - 1)) {

            if (flingGrid[row][Global.MaxColSize - 1]) {
                flingGrid[row][Global.MaxColSize - 1] = false // Fell of the edge. One less ball
            } else {
                if (flingGrid[row][Global.MaxColSize - 2]) {
                    flingGrid[row][Global.MaxColSize - 2] = false
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
        while ((indexCol < (Global.MaxColSize - 1)) && (!foundBallBeforeEdge)) {
            if (flingGrid[row][indexCol])
                foundBallBeforeEdge = true
            else
                indexCol++
        }

        // Need to handle one more edge case
        if ((!foundBallBeforeEdge) && (indexCol == (Global.MaxColSize-1))) {
            if (flingGrid[row][Global.MaxColSize-1]) foundBallBeforeEdge = true
        }

        if ((indexCol == (Global.MaxColSize - 1)) && (!foundBallBeforeEdge)) {
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            flingGrid[row][nextSrcCol] = false  // Fell of the edge. One less ball
            return
        }

        // We have a chain. Call itself recursively
        val srcColNew = nextSrcCol
        val targetColNew = indexCol - 1
        moveRight(srcColNew, targetColNew, row)

        return
    }

    fun moveLeft(srcCol : Int, targetCol : Int, row : Int)
    {
        // Do the first move
        if (!flingGrid[row][srcCol]) error("Unexpected ball status. row=$row, col=$srcCol should be true")
        if (flingGrid[row][targetCol])  error("Unexpected ball status. row=$row, col=$targetCol should be false")
        flingGrid[row][srcCol] = false
        flingGrid[row][targetCol] = true

        // There must be a ball adjacent.
        var nextSrcCol = targetCol - 1
        if (!(flingGrid[row][nextSrcCol])) error("Unexpected grid value. This adjacent box must have a ball, i.e. true")

        // Handle the edge first
        // If the next column is already at the left, then just move it out of the grid and you're done
        if (nextSrcCol == 0) {
            flingGrid[row][0] = false // Fell of the edge. One less ball
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            return
        }

        // If the next col is at 2nd last column (e.g. col == 1))), then handle it special since we are near the edge
        if (nextSrcCol == 1) {
            if (flingGrid[row][0]) {
                flingGrid[row][0] = false // Fell of the edge. One less ball
                // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            } else {
                flingGrid[row][1] = false // Fell of the edge. One less ball
                // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            }
            return
        }

        // Done handling the edge. Now handle the inner part of the grid

        var indexCol = nextSrcCol - 1
        // Move nextSrcCol pointer to the next one that has space to move the ball
        while ((flingGrid[row][indexCol]) && (indexCol > 0)){
            // There is no space. Then move indexCol pointer until there is space
            indexCol--
        }

        // Check if we have continuous balls all the way to the edge
        if (indexCol == 0) {

            if (flingGrid[row][0]) {
                flingGrid[row][0] = false // Fell of the edge. One less ball
            } else {
                if (flingGrid[row][1]) {
                    flingGrid[row][1] = false
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
            if (flingGrid[row][indexCol])
                foundBallBeforeEdge = true
            else
                indexCol--
        }

        // Need to handle one more edge case
        if ((!foundBallBeforeEdge) && (indexCol == 0)) {
            if (flingGrid[row][0]) foundBallBeforeEdge = true
        }

        if ((indexCol == 0) && (!foundBallBeforeEdge)) {
            // Log.d("GM: Details", "     ==> BALL DROPPED OPF")
            flingGrid[row][nextSrcCol] = false  // Fell of the edge. One less ball
            return
        }

        // We have a chain. Call itself recursively
        val srcColNew = nextSrcCol
        val targetColNew = indexCol + 1
        moveLeft(srcColNew, targetColNew, row)

        return
    }

    fun updateBallList(): SnapshotStateList<SolverGridPos>
    {
        val ballList: SnapshotStateList<SolverGridPos> = SnapshotStateList<SolverGridPos>().apply {

            // Clone the board
            repeat (Global.MaxRowSize) { curRow ->
                repeat(Global.MaxColSize) { curCol ->
                    if (flingGrid[curRow][curCol]) {
                        val solverGridPos = SolverGridPos(curRow,curCol)
                        add(solverGridPos)
                    }
                }
            }
        }
        return ballList
    }
}