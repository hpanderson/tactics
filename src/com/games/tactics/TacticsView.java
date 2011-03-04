package com.games.tactics;

import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.os.Handler;
import android.os.Message;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import java.util.Vector;
import java.util.Iterator;

class TacticsView extends SurfaceView implements SurfaceHolder.Callback
{
    public TacticsView(Context inContext, AttributeSet inAttrs)
   	{
        super(inContext, inAttrs);
		getHolder().addCallback(this); // register our interest in hearing about changes to our surface
        setFocusable(true); // make sure we get key events

        mThread = new TacticsThread(getHolder(), getContext(), new Handler()
		{
            @Override
            public void handleMessage(Message m) {
                /*mStatusText.setVisibility(m.getData().getInt("viz"));
                mStatusText.setText(m.getData().getString("text"));*/
            }
        });

        mLogicalView = new LogicalView();
		mThread.setLogicalView(mLogicalView);
    }

	public void newGame()
	{
		mThread.reset();
	}

    /**
     * Callback invoked when the surface dimensions change.
     */
    public void surfaceChanged(SurfaceHolder inHolder, int inFormat, int inWidth, int inHeight)
   	{
        mLogicalView.setPhysicalSize(inWidth, inHeight);
    }

    /**
     * Callback invoked when the Surface has been created and is ready to be used.
     */
    public void surfaceCreated(SurfaceHolder inHolder)
   	{
        // start the thread here so that we don't busy-wait in run() waiting for the surface to be created
        mThread.setRunning(true);
        mThread.start();
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder inHolder)
   	{
        // we have to tell thread to shut down & wait for it to finish, or else it might touch the Surface after we return and explode
        boolean retry = true;
        mThread.setRunning(false);
        while (retry) {
            try {
                mThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }
    
    public void panView(Point inDelta)
    {
    	mLogicalView.pan(inDelta);
    }

    public void zoom(double inScale)
    {
    	mLogicalView.zoom(inScale);
    }

	public TacticsThread getThread() { return mThread; }	
	public LogicalView getLogicalView() { return mLogicalView; }
	
	public void setGameBoard(GameBoard inBoard) 
	{
		mLogicalView.setBoardSize(inBoard.width(), inBoard.height());
		mThread.setGameBoard(inBoard);
	}

    private TacticsThread mThread;
    private LogicalView mLogicalView;

    class TacticsThread extends Thread
   	{
        public TacticsThread(SurfaceHolder inSurfaceHolder, Context inContext, Handler inHandler)
	   	{
			mSurfaceHolder = inSurfaceHolder;
			mContext = inContext;

			mRunning = false;

			reset();
		}
		
        private void reset()
		{
        	synchronized(mSurfaceHolder)
			{
				mTarget = new PointF(-1, -1);
				mMovingPlayer = false;
	
				mTime = 0;
				mEnemies = new Vector<Unit>();
			}
		}

        public void run()
	   	{
			while (mRunning)
			{
                Canvas c = null;
                try
			   	{
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder)
				   	{
						long now = System.currentTimeMillis();
                        if (now - mTime > 2000) {
						   	//updatePosition();
							mTime = now;
						}
						doDraw(c);
                    }
                } finally
			   	{
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
			}
		}

		public void doDraw(Canvas inCanvas)
		{
			// draw black background first
			inCanvas.drawColor(Color.BLACK);

			//long tileStart = System.currentTimeMillis();
			for (int x = 0; x < mBoard.width(); x++) {
				for (int y = 0; y < mBoard.height(); y++) {
					Point tilePoint = new Point(x, y);
					if (mLogicalView.tileInView(tilePoint))
						drawTile(tilePoint, inCanvas);
				}
			}
			/*long tileEnd = System.currentTimeMillis();
			Log.i(this.getClass().getName(), "Board draw time: " + Long.toString(tileEnd - tileStart));*/

			drawUnit(mPlayer, inCanvas);
			for (Iterator<Unit> iter = mEnemies.iterator(); iter.hasNext();)
				drawUnit(iter.next(), inCanvas);

			Paint APPaint = new Paint();
			APPaint.setColor(Color.RED);
			APPaint.setTextSize(20);
			inCanvas.drawText(Double.toString(mPlayer.getAPRemaining()), 20, 20, APPaint);

			if (mTarget.x >= 0)
		   	{				
				if (!mMovingPlayer)
				{					
					// draw target line
					//Rect enemyRect = boardToScreen(mEnemy.getLocation());
					Rect playerRect = mLogicalView.tileToPhysical(mPlayer.getLocation());
					Paint linePaint = new Paint();
					linePaint.setAntiAlias(true);

					/*if (enemyRect.contains((int)mTarget.x, (int)mTarget.y)) {
						linePaint.setColor(Color.RED);
					} else {*/
						linePaint.setColor(Color.WHITE);
					//}

					inCanvas.drawLine(playerRect.centerX(), playerRect.centerY(), mTarget.x, mTarget.y, linePaint);
				} else
			   	{
					double angle = mLogicalView.getUnitAngle(mPlayer, mTarget); 

					// draw ghost of player to show where move will result
					// don't need a full copy of the player, just the resource id and location
					Unit playerGhost = new Unit(mPlayer.getResourceId());
					playerGhost.moveTo(mPlayer.getLocation());
					playerGhost.move(angle, mBoard.getRect());
					int alpha = 100;
					drawUnit(playerGhost, inCanvas, alpha);
				}
			}
		}

		/**
		 * Draws a tile to the canvas.
		 * 
		 * @param inPoint
		 * @param inCanvas
		 */
		private void drawTile(Point inPoint, Canvas inCanvas)
		{
			/*Bitmap tileImage = BitmapFactory.decodeResource(getResources().getDrawable(mBoard.getResourceId(inPoint)));
			float verts[] = {
			inCanvas.drawBitmapMesh(tileImage, meshWidth, meshHeight, verts, vertOffset, null, 0, null);*/

			Rect tileRect = mLogicalView.tileToPhysical(inPoint);
			
			Drawable tileImage = mContext.getResources().getDrawable(mBoard.getResourceId(inPoint)); 
			tileImage.setBounds(tileRect);
			tileImage.draw(inCanvas);

			// draw light gray grid
			Paint tilePaint = new Paint();
            tilePaint.setAntiAlias(true);
            tilePaint.setColor(Color.GRAY);
			tilePaint.setStyle(Paint.Style.STROKE);

			RectF tileRectF = new RectF(tileRect);
			Point overlap = mLogicalView.getPhysicalTileOverlap();
			
			// starting with the leftmost point and going clockwise, the points are
			// P1(0, B) - P2 (A, 0)
			inCanvas.drawLine(tileRectF.left, tileRectF.top + overlap.y,  tileRectF.left + overlap.x, tileRectF.top, tilePaint);
			// P2(A, 0) - P3 (A+C, 0)
			inCanvas.drawLine(tileRectF.left + overlap.x, tileRectF.top, tileRectF.right - overlap.x, tileRectF.top, tilePaint);
			// P3(A+C, 0) - P4(2*A + C, B)
			inCanvas.drawLine(tileRectF.right - overlap.x, tileRectF.top, tileRectF.right, tileRectF.top + overlap.y, tilePaint);
			// P4(2*A + C, B) - P5(A+C, 2*B)
			inCanvas.drawLine(tileRectF.right, tileRectF.top + overlap.y, tileRectF.right - overlap.x, tileRectF.bottom, tilePaint);
			// P5(A+C, 2*B) - P6(A, 2*B)
			inCanvas.drawLine(tileRectF.right - overlap.x, tileRectF.bottom, tileRectF.left + overlap.x, tileRectF.bottom, tilePaint);
			// P6(A, 2*B) - P1(0, B)
			inCanvas.drawLine(tileRectF.left + overlap.x, tileRectF.bottom, tileRectF.left, tileRectF.top + overlap.y, tilePaint);
		}

		private void drawUnit(Unit inUnit, Canvas inCanvas) { drawUnit(inUnit, inCanvas, 255); }
		private void drawUnit(Unit inUnit, Canvas inCanvas, int inAlpha)
		{
			Drawable unitImage = mContext.getResources().getDrawable(inUnit.getResourceId()); 
			Rect unitRect = new Rect(mLogicalView.tileToPhysical(inUnit.getLocation()));
			unitImage.setAlpha(inAlpha);
			unitImage.setBounds(unitRect);
			unitImage.draw(inCanvas);
		}

		public void setTarget(double inX, double inY)
		{
			synchronized(mSurfaceHolder)
			{
				mTarget = new PointF((float)inX, (float)inY);
			}
		}
		
		public void setMovingPlayer(boolean inMoving) { mMovingPlayer = inMoving; }
		public boolean isMovingPlayer() { return mMovingPlayer; }

		public void setPlayer(Unit inPlayer) { mPlayer = inPlayer; }
		public void addEnemy(Unit inEnemy) { mEnemies.add(inEnemy); }

		private void setGameBoard(GameBoard inBoard) 
		{
			mBoard = inBoard;
		}
		
		private void setLogicalView(LogicalView inLogicalView)
		{
			mLogicalView = inLogicalView;
		}

        /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         * 
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b)
	   	{
            mRunning = b;
        }

        private SurfaceHolder mSurfaceHolder;
        private Context mContext;

		private boolean mRunning;

		private PointF mTarget;
		private boolean mMovingPlayer;

		private long mTime;

		private GameBoard mBoard;
		private Unit mPlayer;
		private Vector<Unit> mEnemies;
		
	    private LogicalView mLogicalView;
	}
}

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
		mTileOverlap = new Point((int)(mTileAngledSide * Math.cos(Math.PI / 3.0)), (int)(mTileSize.y / 2.0));
		mTileHorizSide = mTileSize.x - (2.0 * mTileOverlap.x);
		mTileGradient = (double)mTileOverlap.y / (double)mTileOverlap.x; // gradient of the diagonal line
		
		mViewport = new Rect(500, 500, mTileSize.x * 10, mTileSize.y * 10); // default to ~10 tiles	
	}
	
	/**
	 *  Gets the overlap of the tiles in physical units.
	 */
	public Point getPhysicalTileOverlap()
	{
		return new Point((int)(((double)mTileOverlap.x / mViewport.width()) * mPhysicalSize.x), (int)(((double)mTileOverlap.y / mViewport.height()) * mPhysicalSize.y));
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
		mLogicalSize.x = mBoardSize.x * (mTileSize.x - mTileOverlap.x) + mTileOverlap.x;
		// need to account for even columns shifted down 25% 
		mLogicalSize.y = mBoardSize.y * mTileSize.y + mTileOverlap.y;
				
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
	}
	
	/**
	 * Pans the view by a number of physical pixels. One panning unit is a percentage of the viewport for smooth scrolling.
	 * @param inDelta
	 */
	public void pan(Point inDelta)
	{
		// convert the difference in physical pixels do logical - not the same as converting a point 
		inDelta.x = (int)(((double)inDelta.x / mPhysicalSize.x) * mViewport.width()); 
		inDelta.y = (int)(((double)inDelta.y / mPhysicalSize.y) * mViewport.height()); 
		mViewport.offset(inDelta.x, inDelta.y);
		rectifyViewport(-1);		
	}
	
	/**
	 * Zooms in/out.
	 * @param inZoom
	 */
	public void zoom(double inScale)
	{
		double aspect = (double)mViewport.width() / (double)mViewport.height();
		mViewport.inset(mViewport.width() - (int)(mViewport.width() * inScale), mViewport.height() - (int)((mViewport.width() * inScale) / aspect));
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
		return (int)(((double)inPhysicalX / mPhysicalSize.x) * (double)mViewport.width()) + mViewport.left;
	}

	/**
	 * Gets a logical Y coordinate given a physical one.
	 * 
	 * @param inPhysicalY
	 * @return
	 */
	public int physicalToLogicalY(int inPhysicalY)
	{
		return (int)(((double)inPhysicalY / mPhysicalSize.y) * (double)mViewport.height()) + mViewport.top;
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
		return (int)(((double)(inLogicalX - mViewport.left) / mViewport.width()) * mPhysicalSize.x);
	}

	/**
	 * Gets a physical Y coordinate given a logical one.
	 * 
	 * @param inLogicalY
	 * @return
	 */
	public int logicalToPhysicalY(int inLogicalY)
	{
		return (int)(((double)(inLogicalY - mViewport.top) / mViewport.height()) * mPhysicalSize.y);
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

		outRect.left = inPoint.x * (mTileSize.x - mTileOverlap.x);
		outRect.top = inPoint.y * mTileSize.y;
		if (inPoint.x % 2 != 0) // odd col? need to shift down
			outRect.top += mTileOverlap.y;

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
		outPoint.x = (int)((double)inPoint.x / (mTileSize.x - mTileOverlap.x));
		outPoint.y = (int)((double)inPoint.y / mTileSize.y);
		// what pixel in the section?
		Point sectionPixel = new Point(inPoint.x % (mTileSize.x - mTileOverlap.x), inPoint.y % mTileSize.y);
		
		// determine section type
		if (outPoint.x % 2 == 0)
		{
			// type A - includes 3/4 of a hex on the right side, plus a triangle from two other hexes on the left side
			if (sectionPixel.y > (mTileGradient * sectionPixel.x + mTileOverlap.y))
				outPoint.offset(-1, 1); // in the bottom triangle
			else if (sectionPixel.y < (-mTileGradient * sectionPixel.x + mTileOverlap.y)) 
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
				if (sectionPixel.y < -mTileGradient * sectionPixel.x + 2 * mTileOverlap.y)
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
		if (mViewport.height() > 0 && mViewport.width() > 0)
			aspect = (double)mViewport.width() / (double)mViewport.height();
		
		if (inAspectRatio > 0) // -1 means don't change aspect ratio
		{			
			Log.i(this.getClass().getName(), "Prev Aspect: " + Double.toString(aspect));
			Log.i(this.getClass().getName(), "New Aspect: " + Double.toString(inAspectRatio));

			Log.i(this.getClass().getName(), "Prev Viewport: " + mViewport.toString());
			
			if (inAspectRatio > aspect) // getting fatter, adjust width
				mViewport.bottom = mViewport.top + (int)((double)mViewport.width() * (1 / inAspectRatio));
			else // getting thinner, adjust height
				mViewport.right = mViewport.left + (int)((double)mViewport.height() * inAspectRatio);
			
			aspect = inAspectRatio;
			Log.i(this.getClass().getName(), "Viewport: " + mViewport.toString());
		}
		
		if (mViewport.right > mLogicalSize.x)
			mViewport.offset(mLogicalSize.x - mViewport.right, 0); // viewport is off the right side of the screen, try shifting left		
		if (mViewport.left < 0)
			mViewport.offset(-mViewport.left, 0); // too far left, back to 0 - part of the view may hang off the right side now
				
		if (mViewport.bottom > mLogicalSize.y)
			mViewport.offset(0, mLogicalSize.y - mViewport.bottom); // off the bottom of the screen, try shifting up		
		if (mViewport.top < 0)
			mViewport.offset(0, -mViewport.top); // too far up, back to 0 - part of the view may hang off the bottom now
		
		if (mViewport.right > mLogicalSize.x && mViewport.bottom > mLogicalSize.y)
		{
			// shrink the view until one side is flush with the edge of the map
			int hDiff = mViewport.right - mLogicalSize.x;
			int vDiff = mViewport.bottom - mLogicalSize.y;
			
			if (hDiff >= vDiff)
			{
				//reduce the width
				mViewport.left = 0;
				mViewport.right = mLogicalSize.x;

				// and adjust the height to meet the aspect ratio
				mViewport.bottom = mViewport.top + (int)((double)mViewport.width() / aspect);
			} else
			{
				// reduce the height
				mViewport.top = 0;
				mViewport.bottom = mLogicalSize.y;
				
				// and adjust the width to meet the aspect ratio
				mViewport.right = mViewport.left + (int)((double)mViewport.height() * aspect);
			}
		}
	}
	
	/**
	 * Determines whether a tile is fully or partially in the viewport.
	 * @param inPoint
	 * @return
	 */
	public boolean tileInView(Point inPoint)
	{
		return Rect.intersects(mViewport, tileToLogical(inPoint));
	}
	
	Point mTileSize;
	Point mLogicalSize; ///< Calculated from the tile size and the board dimensions
	Point mPhysicalSize;
	Point mBoardSize;
	Rect mViewport;
	
	double mTileAngledSide; ///< Logical length of the angled line segments of a tile hex.
	double mTileGradient; ///< The gradient of the mTileAngledSide line.
	double mTileHorizSide; ///< Logical length of the horizontal line segments of a tile hex. 
	double mTileDistance; ///< Logical distance between the centers of two adjacent tiles.
	Point mTileOverlap; ///< Logical x and y distance an adjacent tile will "overlap" another tile, unless it is directly above or below.
}
