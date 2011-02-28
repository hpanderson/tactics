package com.games.tactics;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.content.res.Resources;

import android.view.View;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;

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

		Resources res = getResources();
		mBoard = new GameBoard(res.getInteger(R.integer.board_width), res.getInteger(R.integer.board_height));
		mBoard.mapTerrain(GameBoard.TerrainType.OUTSIDE, R.drawable.tile_soil_cracked);

		mPlayer = new Unit(R.drawable.unit_player);
		mPlayer.setActionPoints(5);

		for (int i = 0; i < res.getInteger(R.integer.enemy_count); i++) {
			Unit enemy = new Unit(R.drawable.unit_enemy);
			enemy.moveTo(mBoard.width() - (1 + i), mBoard.height() - 1); // move to opposite end of board
			mThread.addEnemy(enemy);
		}
		mThread.setGameBoard(mBoard);
		mThread.setPlayer(mPlayer);
		mMovingPlayer = false;
	}

	public boolean onTouch(View inView, MotionEvent inEvent)
	{
		Point landingPoint = mThread.screenToBoard(new Point((int)inEvent.getX(), (int)inEvent.getY()));
		Point playerPoint = mPlayer.getLocation();
		boolean onPlayer = landingPoint.equals(playerPoint);
		switch (inEvent.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				if (mPlayer.hasAP())
					mThread.setMovingPlayer(onPlayer);
				break;
		
			case MotionEvent.ACTION_UP:

				if (mThread.getMovingPlayer() && !onPlayer) {
					double angle = mThread.getUnitAngle(mPlayer, new PointF(inEvent.getX(), inEvent.getY()));
					mPlayer.move(angle, mBoard.getRect());
				}

				mThread.setTarget(-1, -1);
				mThread.setMovingPlayer(false);
				break;

			default:
				break;
		}

		if (inEvent.getAction() != MotionEvent.ACTION_UP) {
			if (onPlayer)
				mThread.setTarget(-1, -1); // don't display ghost if user is touching the player
			else
				mThread.setTarget(inEvent.getX(), inEvent.getY());
		}

		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu inMenu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, inMenu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem inItem)
	{
		switch (inItem.getItemId())
		{
			case R.id.end_turn:
				return true;
			default:
				return super.onOptionsItemSelected(inItem);
		}
	}

	boolean mMovingPlayer;
	private GameBoard mBoard;
	private Unit mPlayer;
	private TacticsView mTacticsView;
	private TacticsThread mThread;
}
