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
import android.util.Log;

import java.util.Vector;
import java.util.Iterator;

class TacticsView extends SurfaceView implements SurfaceHolder.Callback
{
    public TacticsView(Context inContext, AttributeSet inAttrs)
   	{
        super(inContext, inAttrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        mThread = new TacticsThread(holder, inContext, new Handler()
		{
            @Override
            public void handleMessage(Message m) {
                /*mStatusText.setVisibility(m.getData().getInt("viz"));
                mStatusText.setText(m.getData().getString("text"));*/
            }
        });


        setFocusable(true); // make sure we get key events
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
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
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
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
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
			mHandler = inHandler; // not sure if this is necessary

			mRunning = false;

			mTime = 0;

			mCanvasHeight = 0;
			mCanvasWidth = 0;

			mTarget = new PointF(-1, -1);
			mTileSize = new Point(1, 1);

			mMovingPlayer = false;

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
			for (Iterator iter = mEnemies.iterator(); iter.hasNext();)
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

		private void drawTile(Point inPoint, Canvas inCanvas)
		{
			Drawable tileImage = mContext.getResources().getDrawable(mBoard.getResourceId(inPoint)); 
			Rect tileRect = boardToScreen(inPoint);
			tileImage.setBounds(tileRect);
			tileImage.draw(inCanvas);

			// draw light gray grid
			Paint rectPaint = new Paint();
            rectPaint.setAntiAlias(true);
            rectPaint.setColor(Color.GRAY);
			rectPaint.setStyle(Paint.Style.STROKE);

			inCanvas.drawRect(tileRect, rectPaint);
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

				if (mBoard.width() > 0 && mBoard.height() > 0)
					mTileSize = new Point(mCanvasWidth / mBoard.width(), mCanvasHeight / mBoard.height());
			}
		}

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

			if (mBoard.width() > 0 && mBoard.height() > 0)
				mTileSize = new Point(mCanvasWidth / mBoard.width(), mCanvasHeight / mBoard.height());
		}

		private void updatePosition()
		{
			//mEnemy.move(getDelta(), getDelta(), mBoard.getRect());
		}

		private int getDelta()
		{
			double randomProb = Math.random();
			if (randomProb > .667)
				return 1;
			if (randomProb < .333)
				return -1;
			return 0;
		}

		private Rect boardToScreen(Point inPoint)
		{
			Rect outRect = new Rect();

			outRect.left = inPoint.x * mTileSize.x;
			outRect.top = inPoint.y * mTileSize.y;
			outRect.right = outRect.left + mTileSize.x;
			outRect.bottom = outRect.top + mTileSize.y;
			return outRect;
		}

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
        private Handler mHandler;
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
