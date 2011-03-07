package com.games.tactics;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;


/**
 * A class for translating between physical and logical space and keeping track of the viewport in logical coordinates.
 * 
 * @author Hank
 */
class LogicalView
{
	LogicalView()
	{
		mLogicalSize = new Point(0, 0);
		mBoardSize = new Point(0, 0);
		mPhysicalSize = new Point(0, 0);
		
		mTileSize = new Point(256, 256);
		mTileDistance = 256; // since we are forcing a hex with length = height, we can dictate this (I think?)
		mTileAngledSide = (mTileSize.x / 2.0) / Math.cos(Math.PI / 6.0); // hypotenuse of corner angle of hex 
		mLogicalOverlap = new Point((int)(mTileAngledSide * Math.cos(Math.PI / 3.0)), (int)(mTileSize.y / 2.0));
		mTileHorizSide = mTileSize.x - (2.0 * mLogicalOverlap.x);
		mTileGradient = (double)mLogicalOverlap.y / (double)mLogicalOverlap.x; // gradient of the diagonal line
		
		mLogicalViewport = new Rect(500, 500, mTileSize.x * 10, mTileSize.y * 10); // default to ~10 tiles	

		mTileViewport = new Rect(0, 0, 0, 0);
	}
	
	/**
	 * Sets the dimensions of the game board.
	 * 
	 * @param inWidth
	 * @param inHeight
	 */
	public void setBoardSize(int inWidth, int inHeight)
	{
		mBoardSize = new Point(inWidth, inHeight);

		// each hex overlaps, except the last tile
		mLogicalSize.x = mBoardSize.x * (mTileSize.x - mLogicalOverlap.x) + mLogicalOverlap.x;
		// need to account for even columns shifted down 25% 
		mLogicalSize.y = mBoardSize.y * mTileSize.y + mLogicalOverlap.y;
				
		rectifyViewport(-1);
	}
	
	/**
	 * Sets the physical dimensions of the screen, and adjusts the viewport's aspect ratio if necessary.
	 * 
	 * @param inWidth
	 * @param inHeight
	 */
	public void setPhysicalSize(int inWidth, int inHeight)
	{
		mPhysicalSize = new Point(inWidth, inHeight);
		rectifyViewport((double)inWidth / (double)inHeight);
		mPhysicalOverlap = new Point((int)(((double)mLogicalOverlap.x / mLogicalViewport.width()) * mPhysicalSize.x), (int)(((double)mLogicalOverlap.y / mLogicalViewport.height()) * mPhysicalSize.y));
	}
	
	/**
	 * Pans the view by a number of physical pixels. One panning unit is a percentage of the viewport for smooth scrolling.
	 * @param inDelta
	 */
	public synchronized void pan(Point inDelta)
	{
		// convert the difference in physical pixels do logical - not the same as converting a point 
		inDelta.x = (int)(((double)inDelta.x / mPhysicalSize.x) * mLogicalViewport.width()); 
		inDelta.y = (int)(((double)inDelta.y / mPhysicalSize.y) * mLogicalViewport.height()); 
		mLogicalViewport.offset(inDelta.x, inDelta.y);
		rectifyViewport(-1);
	}
	
	/**
	 * Zooms in/out.
	 * @param inZoom
	 */
	public synchronized void zoom(double inScale)
	{
		double aspect = (double)mLogicalViewport.width() / (double)mLogicalViewport.height();
		mLogicalViewport.inset(mLogicalViewport.width() - (int)(mLogicalViewport.width() * inScale), mLogicalViewport.height() - (int)((mLogicalViewport.width() * inScale) / aspect));
		rectifyViewport(-1);
	}

	/**
	 * Gets a logical X coordinate given a physical one.
	 * 
	 * @param inPhysicalX
	 * @return
	 */
	public int physicalToLogicalX(int inPhysicalX)
	{
		return (int)(((double)inPhysicalX / mPhysicalSize.x) * (double)mLogicalViewport.width()) + mLogicalViewport.left;
	}

	/**
	 * Gets a logical Y coordinate given a physical one.
	 * 
	 * @param inPhysicalY
	 * @return
	 */
	public int physicalToLogicalY(int inPhysicalY)
	{
		return (int)(((double)inPhysicalY / mPhysicalSize.y) * (double)mLogicalViewport.height()) + mLogicalViewport.top;
	}
	
	/**
	 * Converts a physical point to a logical one.
	 * 
	 * @param inPoint
	 * @return
	 */
	public Point physicalToLogical(Point inPoint)
	{
		return new Point(physicalToLogicalX(inPoint.x), physicalToLogicalY(inPoint.y));
	}
	
	/**
	 * Gets a physical X coordinate given a logical one.
	 *  
	 * @param inLogicalX
	 * @return
	 */
	public int logicalToPhysicalX(int inLogicalX)
	{
		return (int)(((double)(inLogicalX - mLogicalViewport.left) / mLogicalViewport.width()) * mPhysicalSize.x);
	}

	/**
	 * Gets a physical Y coordinate given a logical one.
	 * 
	 * @param inLogicalY
	 * @return
	 */
	public int logicalToPhysicalY(int inLogicalY)
	{
		return (int)(((double)(inLogicalY - mLogicalViewport.top) / mLogicalViewport.height()) * mPhysicalSize.y);
	}
	
	/**
	 * Gets a physical Rect given a logical one.
	 * 
	 * @param inRect
	 * @return
	 */
	public Rect logicalToPhysical(Rect inRect)
	{
		return new Rect(logicalToPhysicalX(inRect.left), logicalToPhysicalY(inRect.top), logicalToPhysicalX(inRect.right), logicalToPhysicalY(inRect.bottom));
	}
	
	/**
	 * Gets a physical Point given a logical one.
	 * 
	 * @param inPoint
	 * @return
	 */
	public Point logicalToPhysical(Point inPoint)
	{
		return new Point(logicalToPhysicalX(inPoint.x), logicalToPhysicalY(inPoint.y));
	}

	/**
	 * Converts a game tile coordinate to a logical rectangle.
	 */
	public Rect tileToLogical(Point inPoint)
	{
		Rect outRect = new Rect();

		outRect.left = inPoint.x * (mTileSize.x - mLogicalOverlap.x);
		outRect.top = inPoint.y * mTileSize.y;
		if (inPoint.x % 2 != 0) // odd col? need to shift down
			outRect.top += mLogicalOverlap.y;

		outRect.right = outRect.left + mTileSize.x;
		outRect.bottom = outRect.top + mTileSize.y;
		return outRect;
	}

	/**
	 * Converts a game tile coordinate to a physical rectangle.
	 */
	public Rect tileToPhysical(Point inPoint)
	{
		return logicalToPhysical(tileToLogical(inPoint));		
	}

	/**
	 * Converts a physical point to a game tile coordinate.
	 * 
	 * @param inPoint
	 * @return
	 */
	public Point physicalToTile(Point inPoint)
	{
		return logicalToTile(physicalToLogical(inPoint));
	}
	
	/**
	 * Converts a logical point to a game tile coordinate.
	 * 
	 * @param inPoint
	 * @return
	 */
	public Point logicalToTile(Point inPoint)
	{
		Point outPoint = new Point();

		if (mTileSize.x == 0 || mTileSize.y == 0)
			return outPoint;
		
		// using a method from the gamedev.net article "coordinates in hexagon based tile maps"
		// break board into even square sections (the hexes will be cut up weird) and determine the section we are in
		outPoint.x = (int)((double)inPoint.x / (mTileSize.x - mLogicalOverlap.x));
		outPoint.y = (int)((double)inPoint.y / mTileSize.y);
		// what pixel in the section?
		Point sectionPixel = new Point(inPoint.x % (mTileSize.x - mLogicalOverlap.x), inPoint.y % mTileSize.y);
		
		// determine section type
		if (outPoint.x % 2 == 0)
		{
			// type A - includes 3/4 of a hex on the right side, plus a triangle from two other hexes on the left side
			if (sectionPixel.y > (mTileGradient * sectionPixel.x + mLogicalOverlap.y))
				outPoint.offset(-1, 1); // in the bottom triangle
			else if (sectionPixel.y < (-mTileGradient * sectionPixel.x + mLogicalOverlap.y)) 
				outPoint.offset(-1, 0); // in the top triangle
			// else we are in the middle 3/4 of a hex - don't need to change outPoint
		} else
		{
			// type B - includes the top and bottom halves of two hexes, plus the side of another
			// first determine which half we are in
			if (sectionPixel.y <= (mTileSize.x / 2.0)) {
				// in top half - are we in the small hex on the left?
				if (sectionPixel.y > mTileGradient * sectionPixel.x)
					outPoint.offset(-1, 0);
				else
					outPoint.offset(0, -1);
			} else {
				// in bottom half - are we in the small hex on the left?
				if (sectionPixel.y < -mTileGradient * sectionPixel.x + 2 * mLogicalOverlap.y)
					outPoint.offset(-1, 0);
			}
		}
		return outPoint;
	}
	
    /**
     * Gets the angle from a unit to a physical point on the screen, in radians.
     * 
     * @param inUnit
     * @param inPoint
     * @return
     */
	public double getUnitAngle(Unit inUnit, PointF inPoint)
	{
		Rect unitRect = tileToPhysical(inUnit.getLocation());
		double dx = inPoint.x - unitRect.centerX();
		double dy = inPoint.y - unitRect.centerY();
		double angle = Math.atan2(dy, dx);
		angle *= 180 / Math.PI;
		return angle;
	}
		
	/**
	 * Adjusts viewport to be within the bounds of the logical dimensions and at the given aspect ratio. 
	 */
	private void rectifyViewport(double inAspectRatio)
	{
		double aspect = 1;
		if (mLogicalViewport.height() > 0 && mLogicalViewport.width() > 0)
			aspect = (double)mLogicalViewport.width() / (double)mLogicalViewport.height();
		
		if (inAspectRatio > 0) // -1 means don't change aspect ratio
		{			
			Log.i(this.getClass().getName(), "Prev Aspect: " + Double.toString(aspect));
			Log.i(this.getClass().getName(), "New Aspect: " + Double.toString(inAspectRatio));

			Log.i(this.getClass().getName(), "Prev Viewport: " + mLogicalViewport.toString());
			
			if (inAspectRatio > aspect) // getting fatter, adjust width
				mLogicalViewport.bottom = mLogicalViewport.top + (int)((double)mLogicalViewport.width() * (1 / inAspectRatio));
			else // getting thinner, adjust height
				mLogicalViewport.right = mLogicalViewport.left + (int)((double)mLogicalViewport.height() * inAspectRatio);
			
			aspect = inAspectRatio;
			Log.i(this.getClass().getName(), "Viewport: " + mLogicalViewport.toString());
		}
		
		if (mLogicalViewport.right > mLogicalSize.x)
			mLogicalViewport.offset(mLogicalSize.x - mLogicalViewport.right, 0); // viewport is off the right side of the screen, try shifting left		
		if (mLogicalViewport.left < 0)
			mLogicalViewport.offset(-mLogicalViewport.left, 0); // too far left, back to 0 - part of the view may hang off the right side now
				
		if (mLogicalViewport.bottom > mLogicalSize.y)
			mLogicalViewport.offset(0, mLogicalSize.y - mLogicalViewport.bottom); // off the bottom of the screen, try shifting up		
		if (mLogicalViewport.top < 0)
			mLogicalViewport.offset(0, -mLogicalViewport.top); // too far up, back to 0 - part of the view may hang off the bottom now
		
		if (mLogicalViewport.right > mLogicalSize.x && mLogicalViewport.bottom > mLogicalSize.y)
		{
			// shrink the view until one side is flush with the edge of the map
			int hDiff = mLogicalViewport.right - mLogicalSize.x;
			int vDiff = mLogicalViewport.bottom - mLogicalSize.y;
			
			if (hDiff >= vDiff)
			{
				//reduce the width
				mLogicalViewport.left = 0;
				mLogicalViewport.right = mLogicalSize.x;

				// and adjust the height to meet the aspect ratio
				mLogicalViewport.bottom = mLogicalViewport.top + (int)((double)mLogicalViewport.width() / aspect);
			} else
			{
				// reduce the height
				mLogicalViewport.top = 0;
				mLogicalViewport.bottom = mLogicalSize.y;
				
				// and adjust the width to meet the aspect ratio
				mLogicalViewport.right = mLogicalViewport.left + (int)((double)mLogicalViewport.height() * aspect);
			}
		}

		// each hex overlaps, except the last tile
		mLogicalSize.x = mBoardSize.x * (mTileSize.x - mLogicalOverlap.x) + mLogicalOverlap.x;
		// need to account for even columns shifted down 25% 
		mLogicalSize.y = mBoardSize.y * mTileSize.y + mLogicalOverlap.y;
		mTileViewport = new Rect((int)((double)mLogicalViewport.left / (mTileSize.x - mLogicalOverlap.x)) - 1, (int)((double)mLogicalViewport.top / mTileSize.y) - 1,
						(int)((double)mLogicalViewport.right / (mTileSize.x - mLogicalOverlap.x)), (int)((double)mLogicalViewport.bottom / mTileSize.y));
		mTileViewport.left = Math.max(mTileViewport.left, 0);
		mTileViewport.top = Math.max(mTileViewport.top, 0);
		mTileViewport.right = Math.min(mTileViewport.right, mBoardSize.x - 1);
		mTileViewport.bottom = Math.min(mTileViewport.bottom, mBoardSize.y - 1);
	}
	
	/**
	 * Determines whether a tile is fully or partially in the viewport.
	 * @param inPoint
	 * @return
	 */
	public boolean tileInView(Point inPoint)
	{
		return Rect.intersects(mLogicalViewport, tileToLogical(inPoint));
	}
	
	private Point mTileSize;
	private Point mLogicalSize; ///< Calculated from the tile size and the board dimensions
	private Point mPhysicalSize;
	private Point mBoardSize;
	
	private Rect mLogicalViewport; ///< Viewport in logical units
	public Rect mTileViewport; ///< Viewport in tile units
	
	private double mTileAngledSide; ///< Logical length of the angled line segments of a tile hex.
	private double mTileGradient; ///< The gradient of the mTileAngledSide line.
	private double mTileHorizSide; ///< Logical length of the horizontal line segments of a tile hex. 
	private double mTileDistance; ///< Logical distance between the centers of two adjacent tiles.
	
	private Point mLogicalOverlap; ///< Logical x and y distance an adjacent tile will "overlap" another tile, unless it is directly above or below.
	public Point mPhysicalOverlap; ///< Physical x and y distance an adjacent tile will "overlap" another tile, unless it is directly above or below.
}
