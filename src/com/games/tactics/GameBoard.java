package com.games.tactics;

import android.graphics.Rect;
import android.graphics.Point;

import java.util.EnumMap;
import java.util.Vector;

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
	
	public Vector<Point> getAdjacentTiles(Unit inUnit)
	{
		Vector<Point> outPoints = new Vector<Point>();
		Point unitLocation = inUnit.getLocation();
		
		// there is only 1 tile that can decrease the row number (0, -1)
		if (unitLocation.y > 0)
			outPoints.add(new Point(unitLocation.x, unitLocation.y - 1));
		
		// check the remaining 5 tiles
		for (int x = -1; x <= 1; x++) {
			for (int y = 0; y <= 1; y++) {
				if (x == 0 && y == 0)
					continue; // don't use my own point!
				
				Point p = new Point(unitLocation.x + x, unitLocation.y + y);
				if (!mDimensions.contains(p.x, p.y))
					continue; // off edge of board
				
				if (mTerrainType[p.x][p.y] == TerrainType.WALL || mTerrainType[p.x][p.y] == TerrainType.WATER)
					continue; // can't walk on this terrain type
				
				// need to check if there is another unit here?

				outPoints.add(p);
			}
		}
		
		return outPoints;
	}

	public enum TerrainType
	{
		OUTSIDE,
		INSIDE,
		WALL,
		WATER
	}

	private EnumMap<TerrainType, Integer> mTerrainResources;
	private Rect mDimensions;
	private TerrainType[][] mTerrainType;
}
