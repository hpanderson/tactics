package com.games.tactics;

import java.util.Iterator;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;

public class PopupMenu extends ContextMenu
{
	public PopupMenu()
	{
		mTextSize = 40;		
		mMaxTextWidth = 0;
		
		mTextPaint = new Paint();
		mTextPaint.setColor(Color.WHITE);
		mTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
		mTextPaint.setTextSize(mTextSize);

		mBackgroundPaint = new Paint();
		mBackgroundPaint.setAntiAlias(true);
		mBackgroundPaint.setColor(Color.MAGENTA);
		mBackgroundPaint.setAlpha(50);
		mBackgroundPaint.setStyle(Paint.Style.FILL);

		mBorderPaint = new Paint();
		mBorderPaint.setColor(Color.RED);
		mBorderPaint.setAlpha(255);
		mBorderPaint.setStyle(Paint.Style.STROKE);
	}
	
	public void AddCommand(ContextCommand inCommandId)
	{
		super.AddCommand(inCommandId);
		
		float textWidth = mTextPaint.measureText(inCommandId.toString()); 
		if (textWidth > mMaxTextWidth)
			mMaxTextWidth = textWidth;
		
		if (mMenuRect != null)
			mMenuRect = new RectF(mMenuRect.right - mMaxTextWidth, mMenuRect.top, mMenuRect.right, mMenuRect.bottom);
		else
			mMenuRect = new RectF(0, 0, mMaxTextWidth, mTextSize * size());
	}
	
	public void setTarget(PointF inTarget)
	{
		mMenuRect = new RectF(inTarget.x - mMaxTextWidth, inTarget.y - (mTextSize * size()), inTarget.x, inTarget.y);
	}
	
	public void draw(Canvas inCanvas)
	{		
		int menuIndex = 0;
		for (Iterator<ContextCommand> iter = iterator(); iter.hasNext();) {
			ContextCommand currentCommand = iter.next();
			inCanvas.drawText(currentCommand.toString(), mMenuRect.left, mMenuRect.bottom - (mTextSize * menuIndex++), mTextPaint);
		}
		
		inCanvas.drawRect(mMenuRect, mBackgroundPaint);
		inCanvas.drawRect(mMenuRect, mBorderPaint);			
	}
	
	Paint mTextPaint;
	Paint mBackgroundPaint;
	Paint mBorderPaint;
	
	int mTextSize;
	float mMaxTextWidth;
	RectF mMenuRect;
}
