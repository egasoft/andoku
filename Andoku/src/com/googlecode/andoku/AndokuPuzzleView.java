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

package com.googlecode.andoku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.googlecode.andoku.model.AndokuPuzzle;
import com.googlecode.andoku.model.Position;
import com.googlecode.andoku.model.RegionError;
import com.googlecode.andoku.model.ValueSet;
import com.googlecode.andoku.symbols.PuzzleSymbols;

public class AndokuPuzzleView extends View {
	private static final String TAG = AndokuPuzzleView.class.getName();

	private static final int PREF_SIZE = 300;

	private AndokuPuzzle puzzle;
	private PuzzleSymbols symbols;
	private int size;
	private boolean paused;
	private boolean preview;
	private boolean drawAreaColors;

	private final Paint gridPaint;
	private final Paint regionBorderPaint;
	private final Paint extraRegionPaint;
	private final Paint valuePaint;
	private final Paint cluePaint;
	private final Paint errorPaint;
	private final Paint markedCellPaint;
	private final Paint markedCellCluePaint;
	private float textOffset;

	private int[] areaColors = { 0x0cff0000, 0x0c00ff00, 0x0c0000ff, 0x0cffff00, 0x0cff00ff,
			0x0c00ffff, 0x0c800000, 0x0c008000, 0x0c000080 };

	private final MultiValuesPainter multiValuesPainter;

	private Drawable congratsDrawable;
	private Drawable pausedDrawable;

	private float cellWidth;
	private float cellHeight;
	private float offsetX;
	private float offsetY;

	private int previewClueCounter;

	private Position markedCell;

	public AndokuPuzzleView(Context context, AttributeSet attrs) {
		super(context, attrs);

		setFocusable(true); // make sure we get key events

		gridPaint = new Paint();
		gridPaint.setAntiAlias(false);
		gridPaint.setARGB(64, 0, 0, 0);
		gridPaint.setStrokeCap(Cap.BUTT);
		// gridPaint.setShadowLayer(1, 1, 1, 0xff000000);

		regionBorderPaint = new Paint();
		regionBorderPaint.setAntiAlias(false);
		regionBorderPaint.setARGB(255, 0, 0, 0);
		regionBorderPaint.setStrokeCap(Cap.ROUND);

		extraRegionPaint = new Paint();
		extraRegionPaint.setAntiAlias(false);
		extraRegionPaint.setARGB(64, 0, 45, 255);

		valuePaint = new Paint();
		valuePaint.setAntiAlias(true);
		valuePaint.setARGB(255, 0, 96, 0);
		valuePaint.setTextAlign(Align.CENTER);

		cluePaint = new Paint();
		cluePaint.setAntiAlias(true);
		cluePaint.setARGB(255, 0, 0, 0);
		cluePaint.setTextAlign(Align.CENTER);
		cluePaint.setTypeface(Typeface.create(valuePaint.getTypeface(), Typeface.BOLD));

		errorPaint = new Paint();
		errorPaint.setAntiAlias(true);
		errorPaint.setARGB(255, 255, 0, 0);
		errorPaint.setStyle(Style.STROKE);
		errorPaint.setStrokeCap(Cap.BUTT);

		markedCellPaint = new Paint();
		markedCellPaint.setAntiAlias(false);
		markedCellPaint.setARGB(112, 0, 255, 0);

		markedCellCluePaint = new Paint();
		markedCellCluePaint.setAntiAlias(false);
		markedCellCluePaint.setARGB(112, 255, 0, 0);

		multiValuesPainter = new MultiValuesPainter(Typeface.create(valuePaint.getTypeface(),
				Typeface.BOLD));

		congratsDrawable = getResources().getDrawable(R.drawable.congrats);
		congratsDrawable.setAlpha(144);

		pausedDrawable = getResources().getDrawable(R.drawable.paused);
		pausedDrawable.setAlpha(144);
	}

	public void setLineWidths(float gridWidth, float regionBorderWidth) {
		gridPaint.setStrokeWidth(gridWidth);
		regionBorderPaint.setStrokeWidth(regionBorderWidth);
		errorPaint.setStrokeWidth(regionBorderWidth);
	}

	public void setPuzzle(AndokuPuzzle puzzle, PuzzleSymbols symbols) {
		this.puzzle = puzzle;
		this.symbols = symbols;
		size = puzzle == null ? 0 : puzzle.getSize();
		multiValuesPainter.setPuzzle(size, symbols);

		requestLayout();
		invalidate();
	}

	public AndokuPuzzle getPuzzle() {
		return puzzle;
	}

	public Position getCell(float px, float py, float fuzzy) {
		if (puzzle == null)
			return null;

		px -= offsetX;
		if (px < (-cellWidth * fuzzy) || px >= (cellWidth * (size + fuzzy)))
			return null;

		py -= offsetY;
		if (py < (-cellHeight * fuzzy) || py >= (cellHeight * (size + fuzzy)))
			return null;

		int cx = (int) Math.floor(px / cellWidth);
		if (cx < 0)
			cx = 0;
		if (cx >= size)
			cx = size - 1;

		int cy = (int) Math.floor(py / cellHeight);
		if (cy < 0)
			cy = 0;
		if (cy >= size)
			cy = size - 1;

		return new Position(cy, cx);
	}

	public PointF getCellCenterPoint(Position cell) {
		float x = cell.col * cellWidth + cellWidth / 2 + offsetX;
		float y = cell.row * cellHeight + cellHeight / 2 + offsetY;
		return new PointF(x, y);
	}

	public void markCell(Position cell) {
		if (eq(cell, markedCell))
			return;

		invalidateCell(cell);
		invalidateCell(markedCell);

		markedCell = cell;
	}

	public Position getMarkedCell() {
		return markedCell;
	}

	public void invalidateCell(Position cell) {
		if (cell == null || puzzle == null)
			return;

		if (Constants.LOG_V)
			Log.v(TAG, "invalidateCell(" + cell + ")");

		float x0 = offsetX + cell.col * cellWidth;
		float x1 = x0 + cellWidth;
		float y0 = offsetY + cell.row * cellHeight;
		float y1 = y0 + cellHeight;
		invalidate((int) Math.floor(x0), (int) Math.floor(y0), (int) Math.ceil(x1), (int) Math
				.ceil(y1));
	}

	public void setPaused(boolean paused) {
		if (this.paused == paused)
			return;

		this.paused = paused;
		invalidate();
	}

	public void setPreview(boolean preview) {
		if (this.preview == preview)
			return;

		this.preview = preview;
		invalidate();
	}

	public void setDrawAreaColors(boolean drawAreaColors) {
		if (this.drawAreaColors == drawAreaColors)
			return;

		this.drawAreaColors = drawAreaColors;
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (puzzle == null)
			return;

		long t0 = System.currentTimeMillis();
		onDraw0(canvas);
		long t1 = System.currentTimeMillis();

		if (Constants.LOG_V)
			Log.v(TAG, "Draw time: " + (t1 - t0));
	}

	private void onDraw0(Canvas canvas) {
		if (Constants.LOG_V)
			Log.v(TAG, "onDraw(" + canvas.getClipBounds() + ")");

		canvas.save();
		canvas.translate(offsetX, offsetY);

		Rect clipBounds = canvas.getClipBounds();

		if (drawAreaColors)
			drawAreaColors(canvas, clipBounds);

		drawExtraRegions(canvas, clipBounds);

		if (puzzle.isSolved())
			drawCongrats(canvas);
		else if (paused)
			drawPaused(canvas);
		else
			drawMarkedCell(canvas);

		if (!preview && puzzle.hasErrors())
			drawErrors(canvas, clipBounds);

		drawValues(canvas, clipBounds);

		drawGrid(canvas);

		drawRegionBorders(canvas, clipBounds);

		canvas.restore();
	}

	private void drawMarkedCell(Canvas canvas) {
		if (markedCell == null)
			return;

		canvas.save();

		float x = markedCell.col * cellWidth;
		float y = markedCell.row * cellHeight;
		canvas.translate(x, y);

		Paint paint = puzzle.isClue(markedCell.row, markedCell.col)
				? markedCellCluePaint
				: markedCellPaint;
		canvas.clipRect(0, 0, cellWidth, cellHeight);
		canvas.drawPaint(paint);

		canvas.restore();
	}

	private void drawPaused(Canvas canvas) {
		pausedDrawable.setBounds(0, 0, Math.round(size * cellWidth), Math.round(size * cellHeight));
		pausedDrawable.draw(canvas);
	}

	private void drawAreaColors(Canvas canvas, Rect clipBounds) {
		for (int row = 0; row < size; row++) {
			float y = row * cellHeight;
			if (y > clipBounds.bottom || y + cellHeight < clipBounds.top)
				continue;

			for (int col = 0; col < size; col++) {
				float x = col * cellWidth;
				if (x > clipBounds.right || x + cellWidth < clipBounds.left)
					continue;

				canvas.save();
				canvas.translate(x, y);

				drawAreaColors(canvas, row, col);

				canvas.restore();
			}
		}
	}

	private void drawAreaColors(Canvas canvas, int row, int col) {
		int colorNumber = puzzle.getAreaColor(row, col);
		int color = areaColors[colorNumber];
		canvas.clipRect(0, 0, cellWidth, cellHeight);
		canvas.drawColor(color);
	}

	private void drawExtraRegions(Canvas canvas, Rect clipBounds) {
		for (int row = 0; row < size; row++) {
			float y = row * cellHeight;
			if (y > clipBounds.bottom || y + cellHeight < clipBounds.top)
				continue;

			for (int col = 0; col < size; col++) {
				float x = col * cellWidth;
				if (x > clipBounds.right || x + cellWidth < clipBounds.left)
					continue;

				canvas.save();
				canvas.translate(x, y);

				drawExtraRegions(canvas, row, col);

				canvas.restore();
			}
		}
	}

	private void drawExtraRegions(Canvas canvas, int row, int col) {
		if (puzzle.isExtraRegion(row, col)) {
			canvas.drawRect(0, 0, cellWidth, cellHeight, extraRegionPaint);
		}
	}

	private void drawCongrats(Canvas canvas) {
		congratsDrawable.setBounds(0, 0, Math.round(size * cellWidth), Math.round(size * cellHeight));
		congratsDrawable.draw(canvas);
	}

	private void drawValues(Canvas canvas, Rect clipBounds) {
		previewClueCounter = 0;

		if (preview)
			cluePaint.setAlpha(128);
		else
			cluePaint.setAlpha(255);

		for (int row = 0; row < size; row++) {
			float y = row * cellHeight;
			if (y > clipBounds.bottom || y + cellHeight < clipBounds.top)
				continue;

			for (int col = 0; col < size; col++) {
				float x = col * cellWidth;
				if (x > clipBounds.right || x + cellWidth < clipBounds.left)
					continue;

				canvas.save();
				canvas.translate(x, y);

				drawValues(canvas, row, col);

				canvas.restore();
			}
		}
	}

	private void drawValues(Canvas canvas, int row, int col) {
		ValueSet values = puzzle.getValues(row, col);
		if (values.isEmpty())
			return;

		if (preview && !puzzle.isSolved()) {
			if (puzzle.isClue(row, col)) {
				boolean show = previewClueCounter++ % 4 != 0;
				String dv = show ? String.valueOf(symbols.getSymbol(values.nextValue(0))) : "?";
				canvas.drawText(dv, cellWidth / 2f, textOffset, cluePaint);
			}
		}
		else if (values.size() == 1
				&& (puzzle.isClue(row, col) || markedCell == null || markedCell.row != row
						|| markedCell.col != col || puzzle.isErrorPosition(row, col))) {
			String dv = String.valueOf(symbols.getSymbol(values.nextValue(0)));
			Paint paint = puzzle.isClue(row, col) ? cluePaint : valuePaint;
			canvas.drawText(dv, cellWidth / 2f, textOffset, paint);
		}
		else {
			multiValuesPainter.paintValues(canvas, values);
		}
	}

	private void drawGrid(Canvas canvas) {
		float gridWidth = size * cellWidth;
		float gridHeight = size * cellHeight;
		for (int i = 1; i < size; i++) {
			float x = i * cellWidth;
			float y = i * cellHeight;
			canvas.drawLine(0, y, gridWidth, y, gridPaint);
			canvas.drawLine(x, 0, x, gridHeight, gridPaint);
		}
	}

	private void drawRegionBorders(Canvas canvas, Rect clipBounds) {
		for (int row = 0; row < size; row++) {
			float y = row * cellHeight;
			if (y > clipBounds.bottom || y + cellHeight < clipBounds.top)
				continue;

			for (int col = 0; col < size; col++) {
				float x = col * cellWidth;
				if (x > clipBounds.right || x + cellWidth < clipBounds.left)
					continue;

				if (row > 0 && puzzle.getAreaCode(row, col) != puzzle.getAreaCode(row - 1, col)) {
					canvas.drawLine(x, y, x + cellWidth, y, regionBorderPaint);
				}
				if (col > 0 && puzzle.getAreaCode(row, col) != puzzle.getAreaCode(row, col - 1)) {
					canvas.drawLine(x, y, x, y + cellHeight, regionBorderPaint);
				}
			}
		}
	}

	private void drawErrors(Canvas canvas, Rect clipBounds) {
		float radius = Math.min(cellWidth, cellHeight) * 0.4f;

		for (Position p : puzzle.getErrorPositions()) {
			float x = p.col * cellWidth;
			if (x > clipBounds.right || x + cellWidth < clipBounds.left)
				continue;
			float y = p.row * cellHeight;
			if (y > clipBounds.bottom || y + cellHeight < clipBounds.top)
				continue;

			float cx = x + cellWidth / 2;
			float cy = y + cellHeight / 2;
			canvas.drawCircle(cx, cy, radius, errorPaint);
		}

		// radius += errorPaint.getStrokeWidth() / 2;

		for (RegionError error : puzzle.getRegionErrors()) {
			float cx1 = error.p1.col * cellWidth + cellWidth / 2;
			float cy1 = error.p1.row * cellHeight + cellHeight / 2;

			float cx2 = error.p2.col * cellWidth + cellWidth / 2;
			float cy2 = error.p2.row * cellHeight + cellHeight / 2;

			if (cx1 == cx2) // vertical line
			{
				float vy = cy2 - cy1;
				vy *= (radius / Math.abs(vy));

				canvas.drawLine(cx1, cy1 + vy, cx2, cy2 - vy, errorPaint);
			}
			else if (cy1 == cy2) // horizontal line
			{
				float vx = cx2 - cx1;
				vx *= (radius / Math.abs(vx));

				canvas.drawLine(cx1 + vx, cy1, cx2 - vx, cy2, errorPaint);
			}
			else {
				float vx = cx2 - cx1;
				float vy = cy2 - cy1;
				float scale = (float) (radius / Math.sqrt(vx * vx + vy * vy));
				vx *= scale;
				vy *= scale;

				canvas.drawLine(cx1 + vx, cy1 + vy, cx2 - vx, cy2 - vy, errorPaint);
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (Constants.LOG_V)
			Log.v(TAG, "onMeasure(" + MeasureSpec.toString(widthMeasureSpec) + ", "
					+ MeasureSpec.toString(heightMeasureSpec) + ")");

		int wMode = MeasureSpec.getMode(widthMeasureSpec);
		int wSize = MeasureSpec.getSize(widthMeasureSpec);
		int hMode = MeasureSpec.getMode(heightMeasureSpec);
		int hSize = MeasureSpec.getSize(heightMeasureSpec);

		if (wMode == MeasureSpec.EXACTLY) {
			if (hMode == MeasureSpec.EXACTLY) {
				setSize(wSize, hSize);
			}
			else if (hMode == MeasureSpec.AT_MOST) {
				setSize(wSize, Math.min(wSize, hSize));
			}
			else {
				setSize(wSize, wSize);
			}
		}
		else if (wMode == MeasureSpec.AT_MOST) {
			if (hMode == MeasureSpec.EXACTLY) {
				// f*ck exact height for vertical LinearLayout to work as desired
				// setSize(Math.min(wSize, hSize), hSize);
				setSize(Math.min(wSize, hSize), Math.min(wSize, hSize));
			}
			else if (hMode == MeasureSpec.AT_MOST) {
				setSize(Math.min(wSize, hSize), Math.min(wSize, hSize));
			}
			else {
				setSize(wSize, wSize);
			}
		}
		else {
			if (hMode == MeasureSpec.EXACTLY) {
				setSize(hSize, hSize);
			}
			else if (hMode == MeasureSpec.AT_MOST) {
				setSize(hSize, hSize);
			}
			else {
				setSize(PREF_SIZE, PREF_SIZE);
			}
		}
	}

	private void setSize(int width, int height) {
		setMeasuredDimension(width, height);

		int gridWidth = width - getPaddingLeft() - getPaddingRight();
		int gridHeight = height - getPaddingTop() - getPaddingBottom();
		cellWidth = gridWidth / (float) size;
		cellHeight = gridHeight / (float) size;
		offsetX = getPaddingLeft();
		offsetY = getPaddingTop();

		float fontSize = cellHeight * 0.8f;
		cluePaint.setTextSize(fontSize);
		valuePaint.setTextSize(fontSize);
		calcTextOffset();

		float smallFontSize = cellHeight * 0.3f;
		multiValuesPainter.setCellSize(cellWidth, cellHeight);
		multiValuesPainter.setFontSize(smallFontSize);
	}

	private void calcTextOffset() {
		FontMetrics fontMetrics = valuePaint.getFontMetrics();
		float fontSize = -fontMetrics.ascent - fontMetrics.descent;
		textOffset = cellHeight - (cellHeight - fontSize) / 2 + 0.5f;
	}

	private boolean eq(Object o1, Object o2) {
		return o1 == null ? o2 == null : o1.equals(o2);
	}
}
