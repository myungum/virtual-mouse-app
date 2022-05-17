package com.example.a_gun;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class OnGestureListener implements View.OnTouchListener {

    private final GestureDetector gestureDetector;
    protected boolean isScrolling = false;
    public OnGestureListener(Context context) {
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void onScrollUpDown(float distance) {

    }

    public void onSwipeLeft() {

    }

    public void onSwipeRight() {

    }

    public void onActionUp() {

    }

    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            onActionUp();
        }

        return gestureDetector.onTouchEvent(event);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_DISTANCE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        private static final int SCROLL_DISTANCE_THRESHOLD = 5;
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float distanceX = e2.getX() - e1.getX();
            float distanceY = e2.getY() - e1.getY();
            if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (distanceX > 0)
                    onSwipeRight();
                else
                    onSwipeLeft();
                return true;
            }
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (Math.abs(distanceY) > Math.abs(distanceX) && Math.abs(distanceY) > SCROLL_DISTANCE_THRESHOLD) {
                onScrollUpDown(distanceY);
                return true;
            }
            return false;
        }
    }
}