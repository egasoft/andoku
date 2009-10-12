/*
 * Andoku - a sudoku puzzle game for Android.
 * Copyright (C) 2009  Markus Wiederkehr
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.googlecode.andoku.source;

import com.googlecode.andoku.model.Puzzle;
import com.googlecode.andoku.transfer.StandardAreas;

public class PuzzleHolder {
	static final char NUMBER_SEPARATOR = ':';

	private final PuzzleSource source;
	private final int number;

	private final Puzzle puzzle;
	private final int[][] solution;

	private transient PuzzleType puzzleType;

	public PuzzleHolder(PuzzleSource source, int number, Puzzle puzzle, int[][] solution)
			throws PuzzleIOException {
		this.source = source;
		this.number = number;
		this.puzzle = puzzle;
		this.solution = solution;
	}

	public PuzzleSource getSource() {
		return source;
	}

	public int getNumber() {
		return number;
	}

	public String getPuzzleId() {
		return source.getSourceId() + NUMBER_SEPARATOR + number;
	}

	public Puzzle getPuzzle() {
		return puzzle;
	}

	public int[][] getSolution() {
		return solution;
	}

	public PuzzleType getPuzzleType() {
		if (puzzleType == null)
			puzzleType = determinePuzzleType();

		return puzzleType;
	}

	public int getPuzzleDifficulty() {
		return source.getDifficulty(number);
	}

	private PuzzleType determinePuzzleType() {
		boolean squiggly = isSquiggly();
		boolean x = puzzle.getExtraRegions().length == 2;
		boolean hyper = puzzle.getExtraRegions().length == 4;

		if (squiggly) {
			if (x)
				return PuzzleType.SQUIGGLY_X;
			else if (hyper)
				return PuzzleType.SQUIGGLY_H;
			else
				return PuzzleType.SQUIGGLY;
		}
		else {
			if (x)
				return PuzzleType.STANDARD_X;
			else if (hyper)
				return PuzzleType.STANDARD_HYPER;
			else
				return PuzzleType.STANDARD;
		}
	}

	private boolean isSquiggly() {
		final int size = puzzle.getSize();
		int[][] stdAreas = StandardAreas.getAreas(size);

		for (int row = 0; row < size; row++)
			for (int col = 0; col < size; col++)
				if (puzzle.getAreaCode(row, col) != stdAreas[row][col])
					return true;

		return false;
	}
}