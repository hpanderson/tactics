package com.games.tactics;

import java.util.Vector;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

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
		mInventory = new Vector<Item>();
		
		mResourceId = inResourceId;
		//mIsPlayerControlled = false;
	}
	
	public boolean giveItem(Item inItem)
	{
		if (mInventory.size() >= mInventorySpace)
			return false;
		return mInventory.add(inItem);
	}
	
	public void equipWeapon(Weapon inWeapon)
	{
		mEquippedWeapon = inWeapon;
	}

	public Point getLocation() { return mLocation; }

	public void move(double inAngle, Rect inBounds)
	{
		Point delta = new Point(0, 0);
		/* this was for square tiles
		if (inAngle >= -67.5 && inAngle <= 67.5)
			delta.x = inMagnitude; // move right
		if (inAngle >= 22.5 && inAngle <= 157.5)
			delta.y = inMagnitude; // move down
		if (inAngle >= -157.5 && inAngle <= -22.5)
			delta.y = -inMagnitude; // move up
		if (inAngle >= 112.5 || inAngle <= -112.5)
			delta.x = -inMagnitude; // move left*/
		
		// starting with 0 pointing to the right and moving clockwise
		if (inAngle >= 0 && inAngle < 60) {
			delta.x = 1;
			if (mLocation.x % 2 == 1)
				delta.y = 1;
		} else if (inAngle >= 60 && inAngle < 120) {
			delta.x = 0;
			delta.y = 1;
		} else if (inAngle >= 120 && inAngle < 180) {
			delta.x = -1;
			if (mLocation.x % 2 == 1)
				delta.y = 1;
		} else if (inAngle >= -180 && inAngle < -120) {
			delta.x = -1;
			if (mLocation.x % 2 == 0)
				delta.y = -1;
		} else if (inAngle >= -120 && inAngle < -60) {
			delta.x = 0;
			delta.y = -1;
		} else if (inAngle >= -60 && inAngle < 0) {
			delta.x = 1;
			if (mLocation.x % 2 == 0)
				delta.y = -1;
		}

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

	public void attack(Point inTarget)
	{
		mEquippedWeapon.attack(1);
	}
	
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
	public void resetAP() { mAPRemaining = mAPTotal; }

	private Point mLocation;

	private int mAPTotal;
	private int mAPRemaining;

	private int mInventorySpace;
	private int mResourceId;
	private Vector<Item> mInventory;
	
	private Weapon mEquippedWeapon;

	//private boolean mIsPlayerControlled;
}

