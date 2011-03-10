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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.Vector;
import java.util.Iterator;
import java.util.HashMap;

import com.games.tactics.GameBoard.TerrainType;

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

			// draw light gray grid
			mTileEdgePaint = new Paint();
			mTileEdgePaint.setAntiAlias(true);
			mTileEdgePaint.setColor(Color.GRAY);
			mTileEdgePaint.setStyle(Paint.Style.STROKE);
			
			mDrawableCache = new HashMap<Integer, Drawable>();
			mBitmapCache = new HashMap<Integer, Bitmap>();

			reset();
		}
		
        private void reset()
		{
        	synchronized(mSurfaceHolder)
			{
				mTarget = new PointF(-1, -1);
				mMovingPlayer = false;
				mShowingMenu = false;
	
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
			synchronized(mLogicalView) // make sure no one moves the view while we're drawing
			{
				//long drawStart = System.currentTimeMillis();
				// draw black background first
				inCanvas.drawColor(Color.BLACK);
	
				for (int x = mLogicalView.mTileViewport.left; x <= mLogicalView.mTileViewport.right; x++)
					for (int y = mLogicalView.mTileViewport.top; y <= mLogicalView.mTileViewport.bottom; y++)
						drawTile(x, y, inCanvas);
	
				drawUnit(mPlayer, inCanvas);
				for (Iterator<Unit> iter = mEnemies.iterator(); iter.hasNext();)
					drawUnit(iter.next(), inCanvas);
	
				Paint APPaint = new Paint();
				APPaint.setColor(Color.RED);
				APPaint.setTextSize(20);
				inCanvas.drawText(Double.toString(mPlayer.getAPRemaining()), 20, 20, APPaint);
	
				if (mTarget.x >= 0)
			   	{				
					if (mMovingPlayer)
					{		
						double angle = mLogicalView.getUnitAngle(mPlayer, mTarget); 
	
						// draw ghost of player to show where move will result
						// don't need a full copy of the player, just the resource id and location
						Unit playerGhost = new Unit(mPlayer.getResourceId());
						playerGhost.moveTo(mPlayer.getLocation());
						playerGhost.move(angle, mBoard.getRect());
						int alpha = 100;
						drawUnit(playerGhost, inCanvas, alpha);
					} else if (mShowingMenu)
					{
						RectF menuRect = new RectF(mTarget.x, mTarget.y - 200, mTarget.x + 200, mTarget.y);
						
						// draw background
						Paint menuPaint = new Paint();
						menuPaint.setAntiAlias(true);
						menuPaint.setColor(Color.MAGENTA);
						menuPaint.setAlpha(50);
						menuPaint.setStyle(Paint.Style.FILL);
						inCanvas.drawRect(menuRect, menuPaint);

						// draw border
						menuPaint.setColor(Color.RED);
						menuPaint.setAlpha(255);
						menuPaint.setStyle(Paint.Style.STROKE);
						inCanvas.drawRect(menuRect, menuPaint);
					
					} else
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
					}
				}
				
				//long drawEnd = System.currentTimeMillis();
				//Log.i(this.getClass().getName(), "Draw time: " + Long.toString(drawEnd - drawStart));
			}
		}

		/**
		 * Draws a tile to the canvas.
		 * 
		 * @param inPoint
		 * @param inCanvas
		 */
		private void drawTile(int inX, int inY, Canvas inCanvas)
		{
			/*Bitmap tileImage = BitmapFactory.decodeResource(getResources().getDrawable(mBoard.getResourceId(inPoint)));
			
			inCanvas.drawBitmap(tileImage, meshWidth, meshHeight, verts, vertOffset, null, 0, null);*/

			Point tilePoint = new Point(inX, inY);
			Rect tileRect = mLogicalView.tileToPhysical(tilePoint);
			
			/*Drawable tileImage;
			if (mDrawableCache.containsKey(mBoard.getResourceId(tilePoint))) {
				tileImage = mDrawableCache.get(mBoard.getResourceId(tilePoint));
			} else {
				tileImage = mContext.getResources().getDrawable(mBoard.getResourceId(tilePoint));
				mDrawableCache.put(mBoard.getResourceId(tilePoint), tileImage);
			}
			
			tileImage.setBounds(tileRect);
			tileImage.draw(inCanvas);*/
			
			Bitmap tileBitmap;
			if (mBitmapCache.containsKey(mBoard.getResourceId(tilePoint))) {
				tileBitmap = mBitmapCache.get(mBoard.getResourceId(tilePoint));
			} else {
				tileBitmap = BitmapFactory.decodeResource(mContext.getResources(), mBoard.getResourceId(tilePoint));
				mBitmapCache.put(mBoard.getResourceId(tilePoint), tileBitmap);
			}
			
			inCanvas.drawBitmap(tileBitmap, null, tileRect, null);
			
			// starting with the leftmost point and going clockwise, the points are
			// P1(0, B) - P2 (A, 0)
			inCanvas.drawLine(tileRect.left, tileRect.top + mLogicalView.mPhysicalOverlap.y,  tileRect.left + mLogicalView.mPhysicalOverlap.x, tileRect.top, mTileEdgePaint);
			// P2(A, 0) - P3 (A+C, 0)
			inCanvas.drawLine(tileRect.left + mLogicalView.mPhysicalOverlap.x, tileRect.top, tileRect.right - mLogicalView.mPhysicalOverlap.x, tileRect.top, mTileEdgePaint);
			// P3(A+C, 0) - P4(2*A + C, B)
			inCanvas.drawLine(tileRect.right - mLogicalView.mPhysicalOverlap.x, tileRect.top, tileRect.right, tileRect.top + mLogicalView.mPhysicalOverlap.y, mTileEdgePaint);
			// P4(2*A + C, B) - P5(A+C, 2*B)
			inCanvas.drawLine(tileRect.right, tileRect.top + mLogicalView.mPhysicalOverlap.y, tileRect.right - mLogicalView.mPhysicalOverlap.x, tileRect.bottom, mTileEdgePaint);
			// P5(A+C, 2*B) - P6(A, 2*B)
			inCanvas.drawLine(tileRect.right - mLogicalView.mPhysicalOverlap.x, tileRect.bottom, tileRect.left + mLogicalView.mPhysicalOverlap.x, tileRect.bottom, mTileEdgePaint);
			// P6(A, 2*B) - P1(0, B)
			inCanvas.drawLine(tileRect.left + mLogicalView.mPhysicalOverlap.x, tileRect.bottom, tileRect.left, tileRect.top + mLogicalView.mPhysicalOverlap.y, mTileEdgePaint);
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
			synchronized(mLogicalView)
			{
				mTarget = new PointF((float)inX, (float)inY);
			}
		}
		
		public void setMovingPlayer(boolean inMoving) { mMovingPlayer = inMoving; }
		public boolean isMovingPlayer() { return mMovingPlayer; }

		public void setShowingMenu(boolean inShowingMenu) { mShowingMenu = inShowingMenu; }
		public boolean isShowingMenu() { return mShowingMenu; }

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
		private boolean mShowingMenu;

		private long mTime;

		private GameBoard mBoard;
		private Unit mPlayer;
		private Vector<Unit> mEnemies;
		
		private Paint mTileEdgePaint;
		
	    private LogicalView mLogicalView;
	    
		private HashMap<Integer, Drawable> mDrawableCache;
		private HashMap<Integer, Bitmap> mBitmapCache;
	}
}
