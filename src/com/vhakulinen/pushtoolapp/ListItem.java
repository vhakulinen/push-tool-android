package com.vhakulinen.pushtoolapp;

import java.lang.Runnable;

import android.os.Handler;
import android.util.AttributeSet;
import android.content.Context;
import android.widget.LinearLayout;
import android.view.MotionEvent;
import android.graphics.Color;

class ListItem extends LinearLayout {
    private final Handler handler = new Handler(); 
    private Runnable mLongPressed = new Runnable() { 
        public void run() { 
            setBackgroundColor(0xFFFFFFFF);
        }   
    };

    public ListItem(Context context) {
        super(context);
    }

    public ListItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Write your code to perform an action on down
                this.setBackgroundColor(Color.LTGRAY);
                handler.postDelayed(mLongPressed, 500);
                break;
            case MotionEvent.ACTION_MOVE:
                // Write your code to perform an action on contineus touch move
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // Write your code to perform an action on touch up
                handler.removeCallbacks(mLongPressed);
                this.setBackgroundColor(Color.TRANSPARENT);
                break;
        }
        return true;
    }
}
