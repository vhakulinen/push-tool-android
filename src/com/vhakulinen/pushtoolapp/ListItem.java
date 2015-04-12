package com.vhakulinen.pushtoolapp;

import java.lang.Runnable;

import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.MotionEvent;
import android.graphics.Color;

class ListItem extends LinearLayout {
    // So that we can open the dialog
    private Activity dialogActivity;
    private PushData data;

    private final Handler handler = new Handler(); 
    private Runnable mLongPressed = new Runnable() { 
        public void run() { 
            setBackgroundColor(0xFFFFFFFF);

            AlertDialog.Builder builder = new AlertDialog.Builder(dialogActivity);
            builder.setTitle("What to do?");
            builder.setPositiveButton("Open Url", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface arg0, int arg1) {
                String url = data.getUrl();
                if (!url.startsWith("http://") && !url.startsWith("https://"))
                    url = "http://" + url;
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                dialogActivity.startActivity(browserIntent);
              }
            });
            builder.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface arg0, int arg1) {
              }
            });
            builder.setNeutralButton("Close", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface arg0, int arg1) {
              }
            });
            builder.show();
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

    public void init(Activity a, PushData data) {
        this.dialogActivity = a;
        this.data = data;

        ((TextView) findViewById(R.id.title)).setText(data.getTitle());
        ((TextView) findViewById(R.id.body)).setText(data.getBody());
        ((TextView) findViewById(R.id.date)).setText(data.getTime());
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
