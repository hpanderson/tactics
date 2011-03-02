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
		mAPTotal = 10;
		mAPRemaining = mAPTotal;
		mInventorySpace = 5;
		mInventory = new int[mInventorySpace];
		for (int i = 0; i < mInventorySpace; i++)
			mInventory[i] = 0;

		mResourceId = inResourceId;
		//mIsPlayerControlled = false;
	}

	public Point getLocation() { return mLocation; }

	public void move(double inAngle, Rect inBounds)
	{
		move(inAngle, 1, inBounds);
	}

	public void move(double inAngle, int inMagnitude, Rect inBounds)
	{
		Point delta = new Point(0, 0);
		if (inAngle >= -67.5 && inAngle <= 67.5)
			delta.x = inMagnitude; // move right
		if (inAngle >= 22.5 && inAngle <= 157.5)
			delta.y = inMagnitude; // move down
		if (inAngle >= -157.5 && inAngle <= -22.5)
			delta.y = -inMagnitude; // move up
		if (inAngle >= 112.5 || inAngle <= -112.5)
			delta.x = -inMagnitude; // move left
		move(delta, inBounds);
	}

	public void move(Point inDelta) { move(inDelta.x, inDelta.y); }
	public void move(Point inDelta, Rect inBounds) { move(inDelta.x, inDelta.y, inBounds); }
	public void move(int inDX, int inDY, Rect inBounds)
	{
		move(inDX, inDY);
		mLocation.x = Math.max(inBounds.left, mLocation.x);
		mLocation.x = Math.min(inBounds.right - 1, mLocation.x);
		mLocation.y = Math.max(inBounds.top, mLocation.y);
		mLocation.y = Math.min(inBounds.bottom - 1, mLocation.y);
	}

	public void move(int inDX, int inDY)
	{
	   	mLocation.offset(inDX, inDY);
		mAPRemaining -= Math.max(inDX, inDY);
	}

	public void moveTo(Point inPoint) { moveTo(inPoint.x, inPoint.y); }
	public void moveTo(int inX, int inY) { mLocation.x = inX; mLocation.y = inY; }

	public void setResourceId(int inId) { mResourceId = inId; }
	public int getResourceId() { return mResourceId; }

	public void setActionPoints(int inAP)
	{
		mAPTotal = inAP;
		mAPRemaining = mAPTotal;
	}

	public int getAPRemaining() { return mAPRemaining; }
	public boolean hasAP() { return mAPRemaining > 0; }
	public void useAP(int inUsed) { mAPRemaining = Math.min(0, mAPRemaining - inUsed); }

	private Point mLocation;

	private int mAPTotal;
	private int mAPRemaining;

	private int mInventorySpace;
	private int mResourceId;
	private int[] mInventory;

	//private boolean mIsPlayerControlled;
}

