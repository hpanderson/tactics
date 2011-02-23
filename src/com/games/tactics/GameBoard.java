package com.games.tactics;

import android.graphics.Rect;

class GameBoard
{
	public GameBoard(int inWidth, int inHeight)
	{
		mDimensions = new Rect(0, 0, inWidth, inHeight);
		mTerrainType = new TerrainType[inWidth][inHeight];

		for (int x = 0; x < inWidth; x++)
			for (int y = 0; y < inHeight; y++)
				mTerrainType[x][y] = TerrainType.OUTSIDE;
	}

	public int width() { return mDimensions.width(); }
	public int height() { return mDimensions.height(); }
	public Rect getRect() { return mDimensions; }

	public enum TerrainType
	{
		OUTSIDE,
		INSIDE,
		WALL,
		WATER
	}

	private Rect mDimensions;
	private TerrainType[][] mTerrainType;
}
