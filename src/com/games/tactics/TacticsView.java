package com.games.tactics;

import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.content.Context;
import android.util.AttributeSet;
import android.os.Handler;
import android.os.Message;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint;
import android.graphics.Color;
import android.util.Log;

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

			mLineEnd = new Point(-1, -1);
			mTileSize = new Point(1, 1);
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
                        if (now - mTime > 25) {
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
			inCanvas.drawColor(Color.BLACK);

			Paint rectPaint = new Paint();
            rectPaint.setAntiAlias(true);
            rectPaint.setColor(Color.WHITE);
			rectPaint.setStyle(Paint.Style.STROKE);

			//Log.w(this.getClass().getName(), mTopLeft.toString());

            /*inCanvas.drawLine(mTopLeft.x, mTopLeft.y, mTopRight.x, mTopRight.y, rectPaint);
            inCanvas.drawLine(mTopRight.x, mTopRight.y, mBottomRight.x, mBottomRight.y, rectPaint);
            inCanvas.drawLine(mBottomRight.x, mBottomRight.y, mBottomLeft.x, mBottomLeft.y, rectPaint);
            inCanvas.drawLine(mBottomLeft.x, mBottomLeft.y, mTopLeft.x, mTopLeft.y, rectPaint);*/

			for (int x = 0; x < mBoard.width(); x++) {
				for (int y = 0; y < mBoard.height(); y++) {
					Rect tileRect = boardToScreen(new Point(x, y));
					inCanvas.drawRect(tileRect, rectPaint);
				}
			}

			Paint playerPaint = new Paint();
            playerPaint.setAntiAlias(true);
			playerPaint.setColor(Color.BLUE);
			playerPaint.setStyle(Paint.Style.STROKE);

			RectF playerRect = new RectF(boardToScreen(mPlayer.getLocation()));
			inCanvas.drawOval(playerRect, playerPaint);

			Paint enemyPaint = new Paint();
            enemyPaint.setAntiAlias(true);
			enemyPaint.setColor(Color.GREEN);
			enemyPaint.setStyle(Paint.Style.STROKE);

			RectF enemyRect = new RectF(boardToScreen(mEnemy.getLocation()));
			//Log.w(this.getClass().getName(), mEnemy.getLocation().toString());
			inCanvas.drawOval(enemyRect, enemyPaint);

			if (mLineEnd.x >= 0) {
				Paint linePaint = new Paint();
				linePaint.setAntiAlias(true);

				if (enemyRect.contains(mLineEnd.x, mLineEnd.y)) {
					linePaint.setColor(Color.RED);
				} else {
					linePaint.setColor(Color.WHITE);
				}

				inCanvas.drawLine(playerRect.centerX(), playerRect.centerY(), mLineEnd.x, mLineEnd.y, linePaint);
			}
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

		public void drawTargetLine(int inX, int inY)
		{
			mLineEnd = new Point(inX, inY);
		}

		public void setPlayer(Unit inPlayer)
		{
			mPlayer = inPlayer;
		}

		public void setEnemy(Unit inEnemy)
		{
			mEnemy = inEnemy;
		}

		public void setGameBoard(GameBoard inBoard)
		{
			mBoard = inBoard;

			if (mBoard.width() > 0 && mBoard.height() > 0)
				mTileSize = new Point(mCanvasWidth / mBoard.width(), mCanvasHeight / mBoard.height());
		}

		private void updatePosition()
		{
			mEnemy.move(getDelta(), getDelta(), mBoard.getRect());
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

		private Point mLineEnd;

		private long mTime;

		private GameBoard mBoard;
		private Unit mPlayer;
		private Unit mEnemy;
	}
}
