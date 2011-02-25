package com.games.tactics;

import android.graphics.Rect;
import android.graphics.Point;
import java.util.EnumMap;

class GameBoard
{
	public GameBoard(int inWidth, int inHeight)
	{
		mDimensions = new Rect(0, 0, inWidth, inHeight);
		mTerrainType = new TerrainType[inWidth][inHeight];

		for (int x = 0; x < inWidth; x++)
			for (int y = 0; y < inHeight; y++)
				mTerrainType[x][y] = TerrainType.OUTSIDE;

		mTerrainResources = new EnumMap<TerrainType, Integer>(TerrainType.class);
	}

	public int width() { return mDimensions.width(); }
	public int height() { return mDimensions.height(); }
	public Rect getRect() { return mDimensions; }
	public TerrainType getTerrainType(Point inPoint) { return mTerrainType[inPoint.x][inPoint.y]; }

	public int getResourceId(Point inPoint)
	{
		if (!mTerrainResources.containsKey(getTerrainType(inPoint)))
			return 0;

		return ((Integer)mTerrainResources.get(getTerrainType(inPoint))).intValue();
	}

	public void mapTerrain(TerrainType inTerrain, int inResourceId)
	{
		mTerrainResources.put(inTerrain, new Integer(inResourceId));
	}

	public enum TerrainType
	{
		OUTSIDE,
		INSIDE,
		WALL,
		WATER
	}

	private EnumMap mTerrainResources;
	private Rect mDimensions;
	private TerrainType[][] mTerrainType;
}
