package com.games.tactics;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;

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
	}

	public boolean onTouch(View inView, MotionEvent inEvent)
	{
		if (inEvent.getAction() != MotionEvent.ACTION_UP) {
			mThread.drawTargetLine((int)inEvent.getX(), (int)inEvent.getY());
		} else {
			mThread.drawTargetLine(-1, -1);
		}

		if (inEvent.getPointerCount() > 1) {
			mEnemy.moveTo(mBoard.width() - 1, mBoard.height() - 1);
		}

		return true;
	}

	private GameBoard mBoard;
	private Unit mPlayer;
	private Unit mEnemy;
	private TacticsView mTacticsView;
	private TacticsThread mThread;
}
