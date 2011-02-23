package com.games.tactics;

class GameBoard
{
	public GameBoard(int inWidth, int inHeight)
	{
		mWidth = inWidth;
		mHeight = inHeight;
		mTerrainType = new TerrainType[inWidth][inHeight];


		for (int x = 0; x < mWidth; x++)
			for (int y = 0; y < mHeight; y++)
				mTerrainType[x][y] = TerrainType.OUTSIDE;
	}

	public enum TerrainType
	{
		OUTSIDE,
		INSIDE,
		WALL,
		WATER
	}

	private int mWidth;
	private int mHeight;
	private TerrainType[][] mTerrainType;
}
