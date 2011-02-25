package com.games.tactics;

import android.graphics.Point;
import android.graphics.Rect;

class Unit
{
	public Unit()
	{
		this(0);
	}

	public Unit(int inResourceId)
	{
		mLocation = new Point(0, 0);
		mActionPoints = 10;
		mInventorySpace = 5;
		mInventory = new int[mInventorySpace];
		for (int i = 0; i < mInventorySpace; i++)
			mInventory[i] = 0;

		mResourceId = inResourceId;
	}

	public Point getLocation() { return mLocation; }

	public void move(int inDX, int inDY) { mLocation.offset(inDX, inDY); }

	public void move(int inDX, int inDY, Rect inBounds)
	{
		mLocation.offset(inDX, inDY);
		mLocation.x = Math.max(inBounds.left, mLocation.x);
		mLocation.x = Math.min(inBounds.right - 1, mLocation.x);
		mLocation.y = Math.max(inBounds.top, mLocation.y);
		mLocation.y = Math.min(inBounds.bottom - 1, mLocation.y);
	}

	public void moveTo(int inX, int inY) { mLocation.x = inX; mLocation.y = inY; }

	public void setResourceId(int inId) { mResourceId = inId; }
	public int getResourceId() { return mResourceId; }

	private Point mLocation;
	private int mActionPoints;
	private int mInventorySpace;
	private int mResourceId;
	private int[] mInventory;
}

