package com.games.tactics;
import android.graphics.Point;

class Unit
{
	public Unit()
	{
		mLocation = new Point(0, 0);
		mActionPoints = 10;
		mInventorySpace = 5;
		mInventory = new int[mInventorySpace];
		for (int i = 0; i < mInventorySpace; i++)
			mInventory[i] = 0;
	}

	private Point mLocation;
	private int mActionPoints;
	private int mInventorySpace;
	private int[] mInventory;
}

