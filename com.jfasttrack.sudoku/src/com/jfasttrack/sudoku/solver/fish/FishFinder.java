/*
  FishFinder.java

  Copyright (C) 2008-2009 by Pete Boton, www.jfasttrack.com

  This file is part of Dancing Links Sudoku.

  Dancing Links Sudoku is free for non-commercial use. Contact the author for commercial use.

  You can redistribute and/or modify this software only under the terms of the GNU General Public
  License as published by the Free Software Foundation. Version 2 of the License or (at your option)
  any later version may be used.

  This program is distributed in the hope that it will be useful and enjoyable, but WITH NO
  WARRANTY; not even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  See the GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along with this program; if not,
  write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307.
*/

package com.jfasttrack.sudoku.solver.fish;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.jfasttrack.sudoku.puzzle.AbstractPuzzleModel;
import com.jfasttrack.sudoku.puzzle.Cell;
import com.jfasttrack.sudoku.puzzle.House;
import com.jfasttrack.sudoku.puzzle.PuzzleDelegate;
import com.jfasttrack.sudoku.solver.ISolver;
import com.jfasttrack.sudoku.step.AbstractStep;
import com.jfasttrack.sudoku.step.CandidateRemovalStep;
import com.jfasttrack.sudoku.ui.MessageBundle;


/**
 * This solver looks for fish in a sudoku. It has been tested for X-wings, swordfish, jellyfish,
 * and squirmbags. It has not been tested on larger fish, though it is expected to find them as
 * well.
 *
 * @author   Pete Boton
 * @version  2009/05
 */
public class FishFinder implements ISolver {

    /** The size of the fish to be found. */
    private final int fishSize;

    /** The internal name of this solver. */
    private final String internalName;

    /** The value on which a fish is being sought. */
    private int value;

    /**
     * A collection of <code>Cell</code>s that have a candidate being considered. Each element
     * of this <code>List</code> contains the selected <code>Cell</code>s for a single
     * <code>House</code>.
     */
    private final List testCells = new ArrayList();

    /** The collection of <code>Cell</code>s used to explain the step generated by this solver. */
    private final List supportingCells = new ArrayList();

    /**
     * Constructs a <code>FishFinder</code>.
     *
     * @param fishSize  The size of the fish to be found.
     */
    public FishFinder(final int fishSize) {
        this.fishSize = fishSize;

        switch (fishSize) {
        case 2 :
            internalName = "X-wing";
            break;
        case 3 :
            internalName = "swordfish";
            break;
        case 4 :
            internalName = "jellyfish";
            break;
        default :
            internalName = "squirmbag";
            break;
        }
    }

    /**
     * Looks for fish in a sudoku.
     *
     * @param puzzle  The puzzle to be solved.
     * @return        A <code>Step</code> describing a fish. <code>null</code> if the puzzle
     *                does not contain a fish.
     */
    public AbstractStep getNextStep(final AbstractPuzzleModel puzzle) {
        AbstractStep step = findHorizontalFish(puzzle);
        if (step == null) {
            step = findVerticalFish(puzzle);
        }

        return step;
    }

    /**
     * Looks for a fish based on rows in which a value appears as a candidate the specified
     * number of times.
     *
     * @param puzzle  The puzzle to be solved.
     * @return        A <code>Step</code> describing a horizontal fish. <code>null</code> if the
     *                puzzle does not contain a horizontal fish.
     */
    private AbstractStep findHorizontalFish(final AbstractPuzzleModel puzzle) {
        AbstractStep step = null;

        for (value = 1; value <= puzzle.getGridSize(); value++) {
            testCells.clear();

            // Build a list of rows that have the candidate value the requisite number of times.
            Iterator rows = puzzle.getAllRows();
            while (rows.hasNext()) {
                House row = (House) rows.next();
                List cellsInRow = new ArrayList();
                Iterator cells = row.getAllCells();
                while (cells.hasNext()) {
                    Cell cell = (Cell) cells.next();
                    if (cell.hasCandidate(value)) {
                        cellsInRow.add(cell);
                    }
                }
                if (cellsInRow.size() >= 2 && cellsInRow.size() <= fishSize) {
                    testCells.add(cellsInRow);
                }
            }

            if (testCells.size() >= fishSize) {
                step = checkForHorizontalFish(
                        puzzle,
                        0,
                        0,
                        new int[puzzle.getGridSize()],
                        new int[puzzle.getGridSize()]);
            }
            if (step != null) {
                break;
            }
        }    // for value

        return step;
    }

    /**
     * The puzzle contains at least the minimum number of rows with the requisite number of cells
     * with the specified candidate. Checks to see how many different columns contain those cells.
     *
     * @param puzzle       The puzzle to be solved.
     * @param depth        The current search depth.
     * @param startIndex   The index of the first row in <code>testCells</code> to be considered.
     * @param rowsUsed     A record of the rows in which a fish may be found.
     * @param columnsUsed  A record of the columns in which a fish may be found.
     * @return             A <code>Step</code> describing a horizontal fish. <code>null</code> if
     *                     the puzzle does not contain a horizontal fish.
     */
    private AbstractStep checkForHorizontalFish(
            final AbstractPuzzleModel puzzle,
            final int                 depth,
            final int                 startIndex,
            final int[]               rowsUsed,
            final int[]               columnsUsed) {
        AbstractStep step = null;

        for (int rowIndex = startIndex; rowIndex < testCells.size(); rowIndex++) {
            List cellsInRow = (List) testCells.get(rowIndex);
            for (int i = 0; i < cellsInRow.size(); i++) {
                Cell cell = (Cell) cellsInRow.get(i);
                rowsUsed[cell.getRow()]++;
                supportingCells.add(cell);
                columnsUsed[cell.getColumn()]++;
            }

            if (depth == fishSize - 1) {
                step = reportHorizontalFish(puzzle, rowsUsed, columnsUsed);
            } else {
                step = checkForHorizontalFish(
                        puzzle, depth + 1, rowIndex + 1, rowsUsed, columnsUsed);
            }
            if (step != null) {
                break;
            }

            for (int i = 0; i < cellsInRow.size(); i++) {
                Cell cell = (Cell) cellsInRow.get(i);
                rowsUsed[cell.getRow()]--;
                supportingCells.remove(cell);
                columnsUsed[cell.getColumn()]--;
            }
        }

        return step;
    }

    /**
     * A fish has been found. Check to see whether this fish removes any candidates from any other
     * cells. If it does, then create and return an appropriate step.
     *
     * @param puzzle       The puzzle to be solved.
     * @param rowsUsed     A record of the rows in which a fish may be found.
     * @param columnsUsed  A record of the columns in which a fish may be found.
     * @return             A <code>Step</code> describing a horizontal fish. <code>null</code> if
     *                     the puzzle does not contain a horizontal fish.
     */
    private AbstractStep reportHorizontalFish(
        final AbstractPuzzleModel puzzle,
        final int[]               rowsUsed,
        final int[]               columnsUsed) {

        AbstractStep step = null;

        int nColumnsUsed = 0;
        for (int c = 0; c < puzzle.getGridSize(); c++) {
            if (columnsUsed[c] > 0) {
                nColumnsUsed++;
            }
        }

        if (nColumnsUsed == fishSize) {

            // This could be a fish. See if there are any cells that can be changed by this solver.
            Set cellsToBeChanged = new HashSet();
            for (int r = 0; r < puzzle.getGridSize(); r++) {
                if (rowsUsed[r] > 0) {
                    continue;
                }
                for (int c = 0; c < puzzle.getGridSize(); c++) {
                    if (columnsUsed[c] == 0) {
                        continue;
                    }
                    Cell cell = puzzle.getCellAt(r, c);
                    if (cell.hasCandidate(value)) {
                        cellsToBeChanged.add(cell);
                    }
                }
            }

            if (!cellsToBeChanged.isEmpty()) {
                step = createStep(cellsToBeChanged);
            }
        }

        return step;
    }

    /**
     * Looks for a fish based on columns in which a value appears as a candidate the specified
     * number of times.
     *
     * @param puzzle  The puzzle to be solved.
     * @return        A <code>Step</code> describing a vertical fish. <code>null</code> if the
     *                puzzle does not contain a vertical fish.
     */
    private AbstractStep findVerticalFish(final AbstractPuzzleModel puzzle) {
        AbstractStep step = null;

        for (value = 1; value <= puzzle.getGridSize(); value++) {
            testCells.clear();

            // Build a list of columns that have the candidate value the requisite number of times.
            Iterator columns = puzzle.getAllColumns();
            while (columns.hasNext()) {
                House column = (House) columns.next();
                List cellsInColumn = new ArrayList();
                Iterator cells = column.getAllCells();
                while (cells.hasNext()) {
                    Cell cell = (Cell) cells.next();
                    if (cell.hasCandidate(value)) {
                        cellsInColumn.add(cell);
                    }
                }
                if (cellsInColumn.size() >= 2 && cellsInColumn.size() <= fishSize) {
                    testCells.add(cellsInColumn);
                }
            }

            if (testCells.size() >= fishSize) {
                step = checkForVerticalFish(
                        puzzle,
                        0,
                        0,
                        new int[puzzle.getGridSize()],
                        new int[puzzle.getGridSize()]);
            }
            if (step != null) {
                break;
            }
        }    // for value

        return step;
    }

    /**
     * The puzzle contains at least the minimum number of columns with the requisite number of cells
     * with the specified candidate. Checks to see how many different row contain those cells.
     *
     * @param puzzle       The puzzle to be solved.
     * @param depth        The current search depth.
     * @param startIndex   The index of the first row in <code>testCells</code> to be considered.
     * @param columnsUsed  A record of the columns in which a fish may be found.
     * @param rowsUsed     A record of the rows in which a fish may be found.
     * @return             A <code>Step</code> describing a vertical fish. <code>null</code> if
     *                     the puzzle does not contain a vertictal fish.
     */
    private AbstractStep checkForVerticalFish(
            final AbstractPuzzleModel puzzle,
            final int                 depth,
            final int                 startIndex,
            final int[]               columnsUsed,
            final int[]               rowsUsed) {
        AbstractStep step = null;

        for (int columnIndex = startIndex; columnIndex < testCells.size(); columnIndex++) {
            List cellsInColumn = (List) testCells.get(columnIndex);
            for (int i = 0; i < cellsInColumn.size(); i++) {
                Cell cell = (Cell) cellsInColumn.get(i);
                columnsUsed[cell.getColumn()]++;
                supportingCells.add(cell);
                rowsUsed[cell.getRow()]++;
            }

            if (depth == fishSize - 1) {
                step = reportVerticalFish(puzzle, columnsUsed, rowsUsed);
            } else {
                step = checkForVerticalFish(
                        puzzle, depth + 1, columnIndex + 1, columnsUsed, rowsUsed);
            }
            if (step != null) {
                break;
            }

            for (int i = 0; i < cellsInColumn.size(); i++) {
                Cell cell = (Cell) cellsInColumn.get(i);
                columnsUsed[cell.getColumn()]--;
                supportingCells.remove(cell);
                rowsUsed[cell.getRow()]--;
            }
        }

        return step;
    }

    /**
     * A fish has been found. Check to see whether this fish removes any candidates from any other
     * cells. If it does, then create and return an appropriate step.
     *
     * @param puzzle       The puzzle to be solved.
     * @param rowsUsed     A record of the rows in which a fish may be found.
     * @param columnsUsed  A record of the columns in which a fish may be found.
     * @return             A <code>Step</code> describing a vertical fish. <code>null</code> if
     *                     the puzzle does not contain a vertical fish.
     */
    private AbstractStep reportVerticalFish(
        final AbstractPuzzleModel puzzle,
        final int[]               columnsUsed,
        final int[]               rowsUsed) {

        AbstractStep step = null;

        int nRowsUsed = 0;
        for (int r = 0; r < puzzle.getGridSize(); r++) {
            if (rowsUsed[r] > 0) {
                nRowsUsed++;
            }
        }

        if (nRowsUsed == fishSize) {

            // This could be a fish. See if there are any cells that can be changed by this solver.
            Set cellsToBeChanged = new HashSet();
            for (int c = 0; c < puzzle.getGridSize(); c++) {
                if (columnsUsed[c] > 0) {
                    continue;
                }
                for (int r = 0; r < puzzle.getGridSize(); r++) {
                    if (rowsUsed[r] == 0) {
                        continue;
                    }
                    Cell cell = puzzle.getCellAt(r, c);
                    if (cell.hasCandidate(value)) {
                        cellsToBeChanged.add(cell);
                    }
                }
            }

            if (!cellsToBeChanged.isEmpty()) {
                step = createStep(cellsToBeChanged);
            }
        }

        return step;
    }

    /**
     * Creates a <code>Step</code> describing a fish.
     *
     * @param cellsToBeChanged  The <code>Cell</code> from which the value can be removed.
     * @return                  A <code>Step</code> describing a fish.
     */
    private AbstractStep createStep(final Set cellsToBeChanged) {
        MessageBundle messageBundle = MessageBundle.getInstance();
        String smallHint = messageBundle.getString("solver.fish." + internalName + ".small.hint");
        String bigHint = messageBundle.getString(
                "solver.fish." + internalName + ".big.hint",
                new String[] {
                    String.valueOf(PuzzleDelegate.CHARACTERS.charAt(value)),
                }
        );
        AbstractStep step = new CandidateRemovalStep(smallHint, bigHint, cellsToBeChanged, value);
        Iterator i = supportingCells.iterator();
        while (i.hasNext()) {
            step.addExplainingCell((Cell) i.next());
        }

        return step;
    }

    /**
     * Gets the text for the menu item used to invoke this solver.
     *
     * @return  The text for the menu item used to invoke this solver.
     */
    public String getNameOfMenuItem() {
        return MessageBundle.getInstance().getString("solver.fish." + internalName + ".menu.item");
    }

    /**
     * Gets the message to be displayed when this solver cannot be applied.
     *
     * @return  The message to be displayed when this solver cannot be applied.
     */
    public String getSolverNotApplicableMessage() {
        return MessageBundle.getInstance().getString(
                "solver.fish." + internalName + ".not.applicable");
    }
}
