package com.games.tactics;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.graphics.Point;

import com.games.tactics.TacticsView.TacticsThread;

public class Tactics extends Activity implements OnTouchListener
{
    /**
     * Invoked when the Activity is created.
     * 
     * @param savedInstanceState a Bundle containing state saved from a previous
     *        execution, or null if this is a new execution
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // tell system to use the layout defined in our XML file
        setContentView(R.layout.main);

		mTacticsView = (TacticsView) findViewById(R.id.tactics);
        Log.w(this.getClass().getName(), "Created tactics view");
	
		mTacticsView.setOnTouchListener(this);
		mThread = mTacticsView.getThread();

		mBoard = new GameBoard(6, 10);
		mPlayer = new Unit();
		mEnemy = new Unit();
		mEnemy.moveTo(mBoard.width() - 1, mBoard.height() - 1); // move to opposite end of board
		mThread.setGameBoard(mBoard);
		mThread.setPlayer(mPlayer);
		mThread.setEnemy(mEnemy);
		mMovingPlayer = false;
	}

	public boolean onTouch(View inView, MotionEvent inEvent)
	{
		Point landingPoint = mThread.screenToBoard(new Point((int)inEvent.getX(), (int)inEvent.getY()));
		Point playerPoint = mPlayer.getLocation();

		switch (inEvent.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				mMovingPlayer = landingPoint.equals(playerPoint);
				break;
		
			case MotionEvent.ACTION_UP:

				if (mMovingPlayer)
			   	{
					int dx = 0;
					int dy = 0;
					if (landingPoint.x == playerPoint.x) {
					   if (landingPoint.y > playerPoint.y)
							dy = 1;
					   else if (landingPoint.y < playerPoint.y)
							dy = -1;
					} else if (landingPoint.y == playerPoint.y) {
					   if (landingPoint.x > playerPoint.x)
							dx = 1;
					   else if (landingPoint.x < playerPoint.x)
							dx = -1;
					}

					mPlayer.move(dx, dy, mBoard.getRect());
				} else
					mThread.drawTargetLine(-1, -1);
				mMovingPlayer = false;
				break;

			default:
				if (!mMovingPlayer)
					mThread.drawTargetLine((int)inEvent.getX(), (int)inEvent.getY());
		}

		if (inEvent.getPointerCount() > 1)
			mEnemy.moveTo(mBoard.width() - 1, mBoard.height() - 1);

		return true;
	}

	boolean mMovingPlayer;
	private GameBoard mBoard;
	private Unit mPlayer;
	private Unit mEnemy;
	private TacticsView mTacticsView;
	private TacticsThread mThread;
}
