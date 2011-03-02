package com.games.tactics;

import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.content.Context;
import android.util.AttributeSet;
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
    }

	public void newGame()
	{
		mThread.reset();
	}

    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder inHolder, int inFormat, int inWidth, int inHeight)
   	{
        mThread.setSurfaceSize(inWidth, inHeight);
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
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

	public TacticsThread getThread()
	{
		return mThread;
	}

    private TacticsThread mThread;

    class TacticsThread extends Thread
   	{
        public TacticsThread(SurfaceHolder inSurfaceHolder, Context inContext, Handler inHandler)
	   	{
			mSurfaceHolder = inSurfaceHolder;
			mContext = inContext;

			mRunning = false;

			mCanvasHeight = 0;
			mCanvasWidth = 0;

			mTileSize = new Point(1, 1);

			reset();
		}
		
		public void reset()
		{
			mTarget = new PointF(-1, -1);
			mMovingPlayer = false;

			mTime = 0;
			mEnemies = new Vector<Unit>();
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
						   	updatePosition();
							mTime = now;
						}
						doDraw(c);
                    }
                } finally
			   	{
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null)
                        mSurfaceHolder.unlockCanvasAndPost(c);
                }
			}
		}

		public void doDraw(Canvas inCanvas)
		{
			// draw black background first
			inCanvas.drawColor(Color.BLACK);

			for (int x = 0; x < mBoard.width(); x++) {
				for (int y = 0; y < mBoard.height(); y++) {
					drawTile(new Point(x, y), inCanvas);
				}
			}

			drawUnit(mPlayer, inCanvas);
			for (Iterator<Unit> iter = mEnemies.iterator(); iter.hasNext();)
				drawUnit((Unit)iter.next(), inCanvas);

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
					Rect playerRect = boardToScreen(mPlayer.getLocation());
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
					double angle = getUnitAngle(mPlayer, mTarget); 

					/*Paint anglePaint = new Paint();
					anglePaint.setColor(Color.RED);
					anglePaint.setTextSize(20);
					inCanvas.drawText(Double.toString(angle), 20, 20, anglePaint);*/

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

			Rect tileRect = boardToScreen(inPoint);

			Drawable tileImage = mContext.getResources().getDrawable(mBoard.getResourceId(inPoint)); 
			tileImage.setBounds(tileRect);
			tileImage.draw(inCanvas);

			// draw light gray grid
			Paint tilePaint = new Paint();
            tilePaint.setAntiAlias(true);
            tilePaint.setColor(Color.GRAY);
			tilePaint.setStyle(Paint.Style.STROKE);

			RectF tileRectF = new RectF(tileRect);

			// the lines A B and C form a triangle at the corner of the hex, from which all points in the hex are calculated
			float B = tileRectF.height() / (float)2.0;
			float C = tileRectF.width() / (float)2.0;
			float A = (float)0.5 * C;

			// starting with the leftmost point and going clockwise, the points are
			// P1(0, B) - P2 (A, 0)
			inCanvas.drawLine(tileRectF.left, tileRectF.top + B,  tileRectF.left + A, tileRectF.top, tilePaint);
			// P2(A, 0) - P3 (A+C, 0)
			inCanvas.drawLine(tileRectF.left + A, tileRectF.top, tileRectF.left + A + C, tileRectF.top, tilePaint);
			// P3(A+C, 0) - P4(2*C, B)
			inCanvas.drawLine(tileRectF.left + A + C, tileRectF.top, tileRectF.left + (2*C), tileRectF.top + B, tilePaint);
			// P4(2*C, B) - P5(A+C, 2*B)
			inCanvas.drawLine(tileRectF.left + (2*C), tileRectF.top + B, tileRectF.left + A + C, tileRectF.top + (2*B), tilePaint);
			// P5(A+C, 2*B) - P6(A, 2*B)
			inCanvas.drawLine(tileRectF.left + A + C, tileRectF.top + (2*B), tileRectF.left + A, tileRectF.top + (2*B), tilePaint);
			// P6(A, 2*B) - P1(0, B)
			inCanvas.drawLine(tileRectF.left + A, tileRectF.top + (2*B), tileRectF.left, tileRectF.top + B, tilePaint);
		}

		private void drawUnit(Unit inUnit, Canvas inCanvas) { drawUnit(inUnit, inCanvas, 255); }
		private void drawUnit(Unit inUnit, Canvas inCanvas, int inAlpha)
		{
			Drawable unitImage = mContext.getResources().getDrawable(inUnit.getResourceId()); 
			Rect unitRect = new Rect(boardToScreen(inUnit.getLocation()));
			unitImage.setAlpha(inAlpha);
			unitImage.setBounds(unitRect);
			unitImage.draw(inCanvas);
		}

        public void setSurfaceSize(int inWidth, int inHeight)
	   	{
			synchronized(mSurfaceHolder)
			{
				mCanvasHeight = inHeight;
				mCanvasWidth = inWidth;
				updateTileSize();
			}
		}

        /**
         * Gets the angle from a unit to a point on the screen, in radians.
         * 
         * @param inUnit
         * @param inPoint
         * @return
         */
		public double getUnitAngle(Unit inUnit, PointF inPoint)
		{
			Rect unitRect = mThread.boardToScreen(inUnit.getLocation());
			double dx = inPoint.x - unitRect.centerX();
			double dy = inPoint.y - unitRect.centerY();
			double angle = Math.atan2(dy, dx);
			angle *= 180 / Math.PI;
			return angle;
		}

		public void setTarget(double inX, double inY)
		{
			mTarget = new PointF((float)inX, (float)inY);
		}
		
		public void setMovingPlayer(boolean inMoving) { mMovingPlayer = inMoving; }
		public boolean getMovingPlayer() { return mMovingPlayer; }

		public void setPlayer(Unit inPlayer) { mPlayer = inPlayer; }
		public void addEnemy(Unit inEnemy) { mEnemies.add(inEnemy); }

		public void setGameBoard(GameBoard inBoard) 
		{
			mBoard = inBoard;
			updateTileSize();
		}

		/**
		 * Calculates the tile size from the physical dimensions.
		 */
		private void updateTileSize()
		{
			if (mBoard.width() > 0 && mBoard.height() > 0)
		   	{
				mTileSize = new Point();
			   	// each hex overlaps by 25%, except the last tile so x = .75 * width * tileCount + .25 * width
				mTileSize.x = (int)((double)mCanvasWidth / (0.75 * (double)mBoard.width() + 0.25));
				// need to account for even columns shifted down 25% 
				mTileSize.y = (int)((double)mCanvasHeight / ((double)mBoard.height() + 0.25));
			}
		}

		private void updatePosition()
		{
			//mEnemy.move(getDelta(), getDelta(), mBoard.getRect());
		}

		/*private int getDelta()
		{
			double randomProb = Math.random();
			if (randomProb > .667)
				return 1;
			if (randomProb < .333)
				return -1;
			return 0;
		}*/

		/**
		 * Converts a game tile coordinate to a physical rectangle.
		 */
		private Rect boardToScreen(Point inPoint)
		{
			Rect outRect = new Rect();

			outRect.left = inPoint.x * (int)(mTileSize.x * 0.75); // 25% overlap
			outRect.top = inPoint.y * mTileSize.y;
			if (inPoint.x % 2 != 0) // odd col? need to shift down
				outRect.top += (mTileSize.y / 2.0);

			outRect.right = outRect.left + mTileSize.x;
			outRect.bottom = outRect.top + mTileSize.y;
			return outRect;
		}

		/**
		 * Converts a physical point to a game tile coordinate.
		 * 
		 * @param inPoint
		 * @return
		 */
		public Point screenToBoard(Point inPoint)
		{
			Point outPoint = new Point();

			if (mTileSize.x == 0 || mTileSize.y == 0)
				return outPoint;

			outPoint.x = inPoint.x / mTileSize.x;
			outPoint.y = inPoint.y / mTileSize.y;
			return outPoint;
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

		private int mCanvasHeight;
		private int mCanvasWidth;
		private Point mTileSize;

		private boolean mRunning;

		private PointF mTarget;
		private boolean mMovingPlayer;

		private long mTime;

		private GameBoard mBoard;
		private Unit mPlayer;
		private Vector<Unit> mEnemies;
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
		mLogicalDim = new Point(10000, 10000);
		mBoardDim = new Point(0, 0);
		mPhysicalDim = new Point(0, 0);
		
		mViewport = new Rect(0, 0, 1000, 1000);
	}
	
	/**
	 * Sets the dimensions of the game board.
	 * 
	 * @param inWidth
	 * @param inHeight
	 */
	public void setBoardDim(int inWidth, int inHeight)
	{
		mBoardDim = new Point(inWidth, inHeight);
	}
	
	/**
	 * Sets the physical dimensions of the screen, and adjusts the viewport's aspect ratio if necessary.
	 * 
	 * @param inWidth
	 * @param inHeight
	 */
	public void setPhysicalDim(int inWidth, int inHeight)
	{
		mPhysicalDim = new Point(inWidth, inHeight);
		//double aspect = (double)inWidth / (double)inHeight;
		
		mViewport = new Rect(0, 0, mPhysicalDim.x, mPhysicalDim.y);
	}
	
	/**
	 * Gets a logical X coordinate given a physical one.
	 * 
	 * @param inPhysicalX
	 * @return
	 */
	public int physicalToLogicalX(int inPhysicalX)
	{
		return ((inPhysicalX / mPhysicalDim.x) * mViewport.width()) + mViewport.left;
	}

	/**
	 * Gets a logical Y coordinate given a physical one.
	 * 
	 * @param inPhysicalY
	 * @return
	 */
	public int physicalToLogicalY(int inPhysicalY)
	{
		return ((inPhysicalY / mPhysicalDim.y) * mViewport.height()) + mViewport.top;
	}
	
	/**
	 * Gets a physical X coordinate given a logical one.
	 *  
	 * @param inLogicalX
	 * @return
	 */
	public int logicalToPhysicalX(int inLogicalX)
	{
		return (inLogicalX / mViewport.width()) * mPhysicalDim.x;
	}

	/**
	 * Gets a physical Y coordinate given a logical one.
	 * 
	 * @param inLogicalY
	 * @return
	 */
	public int logicalToPhysicalY(int inLogicalY)
	{
		return (inLogicalY / mViewport.height()) * mPhysicalDim.y;
	}
	
	/**
	 * Converts a game tile coordinate to a physical rectangle.
	 */
	/*public Rect boardToScreen(Point inPoint)
	{
		Rect outRect = new Rect();

		outRect.left = inPoint.x * (int)(mTileSize.x * 0.75); // 25% overlap
		outRect.top = inPoint.y * mTileSize.y;
		if (inPoint.x % 2 != 0) // odd col? need to shift down
			outRect.top += (mTileSize.y / 2.0);

		outRect.right = outRect.left + mTileSize.x;
		outRect.bottom = outRect.top + mTileSize.y;
		return outRect;
	}*/

	Point mTileSize;
	Point mLogicalDim;
	Point mPhysicalDim;
	Point mBoardDim;
	Rect mViewport;
}
