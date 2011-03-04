package com.games.tactics;

import java.util.Iterator;
import java.util.Vector;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.content.res.Resources;

import android.view.View;
import android.view.View.OnTouchListener;
import android.view.View.OnKeyListener;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

import android.graphics.Point;
import android.graphics.PointF;

import com.games.tactics.TacticsView.TacticsThread;

public class Tactics extends Activity implements OnTouchListener, OnKeyListener
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
        Log.i(this.getClass().getName(), "Created tactics view");
	
		mTacticsView.setOnTouchListener(this);
		mTacticsView.setOnKeyListener(this);
		newGame();
	}

	public boolean onTouch(View inView, MotionEvent inEvent)
	{
		Point landingPoint = mTacticsView.getLogicalView().physicalToTile(new Point((int)inEvent.getX(), (int)inEvent.getY()));
		Point playerPoint = mPlayer.getLocation();
		boolean onPlayer = landingPoint.equals(playerPoint);
		
		switch (inEvent.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				if (onPlayer && mPlayer.hasAP())
					mThread.setMovingPlayer(true);
				break;
				
			case MotionEvent.ACTION_UP:
				if (mThread.isMovingPlayer() && !onPlayer) {
					double angle = mTacticsView.getLogicalView().getUnitAngle(mPlayer, new PointF(inEvent.getX(), inEvent.getY()));
					mPlayer.move(angle, mBoard.getRect());
				}

				mThread.setTarget(-1, -1);
				mThread.setMovingPlayer(false);			
				break;
				
			case MotionEvent.ACTION_MOVE:				
				
				// problem - the thread doesn't block on the view. it could be in the middle of drawing when we zoom or pan, which causes cracks in the tiles
				if (!mThread.isMovingPlayer()) {
					if (inEvent.getHistorySize() > 0) // just get the previous point
						mTacticsView.panView(new Point((int)(inEvent.getHistoricalX(0) - inEvent.getX()), (int)(inEvent.getHistoricalY(0) - inEvent.getY())));
				} else if (onPlayer)
					mThread.setTarget(-1, -1); // don't display ghost if user is touching the player
				else
					mThread.setTarget(inEvent.getX(), inEvent.getY());
				break;

			default:
				break;
		}
		
		return true;
	}
	
	public boolean onKey(View inView, int inKeyCode, KeyEvent inEvent)
	{
		if (inEvent.getAction() != KeyEvent.ACTION_DOWN)
			return false;
		
		boolean handled = false;
		switch(inKeyCode)
		{
			case KeyEvent.KEYCODE_DPAD_UP:
				mTacticsView.zoom(0.9);
				handled = true;
				break;
				
			case KeyEvent.KEYCODE_DPAD_DOWN:
				mTacticsView.zoom(1.1);
				handled = true;
				break;
		}
		
		return handled;
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
				endTurn();
				return true;
			case R.id.new_game:
				newGame();
				return true;
			default:
				return super.onOptionsItemSelected(inItem);
		}
	}

	private void newGame()
	{
        mTacticsView.newGame();
		mThread = mTacticsView.getThread();

		Resources res = getResources();
		mBoard = new GameBoard(res.getInteger(R.integer.board_width), res.getInteger(R.integer.board_height));
		mBoard.mapTerrain(GameBoard.TerrainType.OUTSIDE, R.drawable.tile_soil_cracked);

		mPlayer = new Unit(R.drawable.unit_player);
		mPlayer.setActionPoints(5);
		mPlayer.moveTo(4, 4);
		
		mEnemies = new Vector<Unit>();
		for (int i = 0; i < res.getInteger(R.integer.enemy_count); i++) {
			Unit enemy = new Unit(R.drawable.unit_enemy);
			enemy.moveTo(mBoard.width() - (1 + i), mBoard.height() - 1); // move to opposite end of board
			mEnemies.add(enemy);
			mThread.addEnemy(enemy);
		}

        mTacticsView.setGameBoard(mBoard);
		mThread.setPlayer(mPlayer);
		mMovingPlayer = false;
	}
	
	private void endTurn()
	{
		mPlayer.resetAP();
		
		//moveNPC();
		
		for (Iterator<Unit> iter = mEnemies.iterator(); iter.hasNext();)
			iter.next().resetAP();
	}

	boolean mMovingPlayer;
	private GameBoard mBoard;
	private Unit mPlayer;
	private Vector<Unit> mEnemies;
	private TacticsView mTacticsView;
	private TacticsThread mThread;
}
