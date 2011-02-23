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
			mInitialized = false;

			mTime = 0;

			mCanvasHeight = 0;
			mCanvasWidth = 0;

			mLineEnd = new Point(-1, -1);
			initializeRect();
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
            rectPaint.setColor(Color.GREEN);

			//Log.w(this.getClass().getName(), mTopLeft.toString());

            inCanvas.drawLine(mTopLeft.x, mTopLeft.y, mTopRight.x, mTopRight.y, rectPaint);
            inCanvas.drawLine(mTopRight.x, mTopRight.y, mBottomRight.x, mBottomRight.y, rectPaint);
            inCanvas.drawLine(mBottomRight.x, mBottomRight.y, mBottomLeft.x, mBottomLeft.y, rectPaint);
            inCanvas.drawLine(mBottomLeft.x, mBottomLeft.y, mTopLeft.x, mTopLeft.y, rectPaint);

			if (mLineEnd.x >= 0) {
				Paint linePaint = new Paint();
				linePaint.setAntiAlias(true);

				Log.w(this.getClass().getName(), mLineEnd.toString());
				Log.w(this.getClass().getName(), mTargetRect.toString());
				if (mTargetRect.contains(mLineEnd.x, mLineEnd.y)) {
					linePaint.setColor(Color.RED);
				} else {
					linePaint.setColor(Color.WHITE);
				}

				inCanvas.drawLine(mLineStart.x, mLineStart.y, mLineEnd.x, mLineEnd.y, linePaint);
			}
		}

        public void setSurfaceSize(int inWidth, int inHeight)
	   	{
			synchronized(mSurfaceHolder)
			{
				mCanvasHeight = inHeight;
				mCanvasWidth = inWidth;

				if (!mInitialized) {
					initializeRect();
					mInitialized = true;
				}
			}
		}

		public void initializeRect()
		{
			int midH = mCanvasHeight / 2;
			int midW = mCanvasWidth / 2;

			mTopLeft = new Point(midW - 10, midH - 10);
			mTopRight = new Point(midW + 10, midH - 10);
			mBottomLeft = new Point(midW - 10, midH + 10);
			mBottomRight = new Point(midW + 10, midH + 10);

			mLineStart = new Point(midW, midH);
		}

		public void drawTargetLine(int inX, int inY)
		{
			mLineEnd = new Point(inX, inY);
		}

		private void updatePosition()
		{
			mTopLeft.offset(getDelta(), getDelta());
			mTopLeft = rectifyPoint(mTopLeft);

			mTopRight.offset(getDelta(), getDelta());
			mTopRight = rectifyPoint(mTopRight);

			mBottomLeft.offset(getDelta(), getDelta());
			mBottomLeft = rectifyPoint(mBottomLeft);

			mBottomRight.offset(getDelta(), getDelta());
			mBottomRight = rectifyPoint(mBottomRight);

			// rought target rect
			mTargetRect = new Rect();
			mTargetRect.right = Math.max(Math.max(Math.max(mTopLeft.x, mTopRight.x), mBottomLeft.x), mBottomRight.x);
			mTargetRect.left = Math.min(Math.min(Math.min(mTopLeft.x, mTopRight.x), mBottomLeft.x), mBottomRight.x);
			mTargetRect.bottom = Math.max(Math.max(Math.max(mTopLeft.y, mTopRight.y), mBottomLeft.y), mBottomRight.y);
			mTargetRect.top = Math.min(Math.min(Math.min(mTopLeft.y, mTopRight.y), mBottomLeft.y), mBottomRight.y);
		}

		private Point rectifyPoint(Point ioPoint)
		{
			if (ioPoint.x < 0)
				ioPoint.x = 0;
			
			if (ioPoint.y < 0)
				ioPoint.y = 0;

			if (ioPoint.x > mCanvasWidth)
				ioPoint.x = mCanvasWidth;

			if (ioPoint.y > mCanvasHeight)
				ioPoint.y = mCanvasHeight;

			return ioPoint;
		}

		private int getDelta()
		{
			int outDelta = (int)(Math.random() * 6) - 3; 
			//Log.w(this.getClass().getName(), Integer.toString(outDelta));
			return outDelta;
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

		private boolean mRunning;
		private boolean mInitialized;

		private Point mTopLeft;
		private Point mTopRight;
		private Point mBottomLeft;
		private Point mBottomRight;

		private Rect mTargetRect;

		private Point mLineEnd;
		private Point mLineStart;

		private long mTime;
	}
}
