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

import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

import android.graphics.Point;
import android.graphics.PointF;

import com.games.tactics.TacticsView.TacticsThread;

import android.os.Debug;

public class Tactics extends Activity implements OnTouchListener, OnKeyListener, OnScaleGestureListener
{
	/**
	 * Invoked when the Activity is created.
	 * 
	 * @param savedInstanceState a Bundle containing state saved from a previous
	 *        execution, or null if this is a new execution
	 */
	protected void onCreate(Bundle savedInstanceState)
	{
		//Debug.startMethodTracing("tactics");
		super.onCreate(savedInstanceState);
		// tell system to use the layout defined in our XML file
		setContentView(R.layout.main);

		mScaleDetector = new ScaleGestureDetector(getBaseContext() , this);
		mTacticsView = (TacticsView) findViewById(R.id.tactics);
		Log.i(this.getClass().getName(), "Created tactics view");

		mTacticsView.setOnTouchListener(this);
		mTacticsView.setOnKeyListener(this);
		newGame();
	}

	protected void onDestroy()
	{
		super.onDestroy();
		//Debug.stopMethodTracing();
	}

	public boolean onTouch(View inView, MotionEvent inEvent)
	{
		if (inEvent.getPointerCount() > 1) {
			Log.i(this.getClass().getName(), "passing touch event to scale detector");
			return mScaleDetector.onTouchEvent(inEvent);
		}
		
		//long touchStart = System.currentTimeMillis();
		Point landingPoint = mTacticsView.getLogicalView().physicalToTile(new Point((int)inEvent.getX(), (int)inEvent.getY()));
		Point playerPoint = mPlayer.getLocation();
		boolean onPlayer = landingPoint.equals(playerPoint);

		switch (inEvent.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				if (onPlayer && mPlayer.hasAP())
					mThread.setMovingPlayer(true);
				else
					mThread.setShowingMenu(false);
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
	
				if (!mThread.isMovingPlayer())
				{
					Point holdPoint = mTacticsView.getLogicalView().physicalToTile(new Point((int)inEvent.getHistoricalX(0), (int)inEvent.getHistoricalY(0)));
					long holdTime = inEvent.getEventTime() - inEvent.getDownTime();
					if (holdTime > 2000 && landingPoint.equals(holdPoint)) {
						// user is holding on one spot, display popup menu
						mThread.setTarget(inEvent.getX(), inEvent.getY());
						mThread.setShowingMenu(true);
					} else {
						if (inEvent.getHistorySize() > 0) // just get the previous point
							mTacticsView.panView(new Point((int)(inEvent.getHistoricalX(0) - inEvent.getX()), (int)(inEvent.getHistoricalY(0) - inEvent.getY())));
					}
				} else {					
					if (onPlayer)
						mThread.setTarget(-1, -1); // don't display ghost if user is touching the player
					else
						mThread.setTarget(inEvent.getX(), inEvent.getY());
				}
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

	public boolean onScale(ScaleGestureDetector inDetector)
	{
		//Log.i(this.getClass().getName(), "Current Span: " + Double.toString(inDetector.getCurrentSpan()) + " Previous Span: " + Double.toString(inDetector.getPreviousSpan()));
		mTacticsView.zoom(inDetector.getCurrentSpan() / inDetector.getPreviousSpan());
		return true;
	}

	public boolean onScaleBegin(ScaleGestureDetector inDetector) { /*Log.i(this.getClass().getName(), "Scale begin");*/ return true; }

	public void onScaleEnd(ScaleGestureDetector inDetector) { /*Log.i(this.getClass().getName(), "Scale end");*/ }

	public boolean onCreateOptionsMenu(Menu inMenu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, inMenu);
		return true;
	}

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
		mBoard.mapTerrain(GameBoard.TerrainType.WATER, R.drawable.tile_water_plain);

		mPlayer = new Unit(R.drawable.unit_player);
		mPlayer.setActionPoints(5);
		mPlayer.moveTo(4, 4);
		mPlayer.equipWeapon(new Pistol());

		mEnemies = new Vector<Unit>();
		for (int i = 0; i < res.getInteger(R.integer.enemy_count); i++) {
			Unit enemy = new Unit(R.drawable.unit_enemy);
			Point randomPoint;
			do {
				randomPoint = new Point((int)(Math.random() * mBoard.width()), (int)(Math.random() * mBoard.height()));
			} while (!mBoard.isPassableTerrain(randomPoint));
			enemy.moveTo(randomPoint); // move to random location
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

		for (Iterator<Unit> iter = mEnemies.iterator(); iter.hasNext();)
			NPCTurn(iter.next());				
	}

	private void NPCTurn(Unit inUnit)
	{
		inUnit.resetAP();

		Vector<Point> adjacentTiles = mBoard.getAdjacentTiles(inUnit);

		double randomResult = Math.random();
		randomResult *= adjacentTiles.size();

		inUnit.moveTo(adjacentTiles.elementAt((int)randomResult));
	}

	boolean mMovingPlayer;
	private GameBoard mBoard;
	private Unit mPlayer;
	private Vector<Unit> mEnemies;
	private TacticsView mTacticsView;
	private TacticsThread mThread;
	private ScaleGestureDetector mScaleDetector;
}
