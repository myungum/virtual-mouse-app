package com.example.a_gun;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class Tracker {
    interface OnMouseStatusChangeListener {
        void onMouseStatusChanged(int x, int y, int scroll, byte flag);
    }

    public enum Flag {
        LEFT(0x01),
        RIGHT(0x02),
        BACK(0x04),
        FORWARD(0x08);

        private final int value;
        Flag(int value) { this.value = value; }
        public int getValue() { return value; }
    }
    private int mFlag = 0x00;

    private Object mSensorValueLock = new Object();
    private Object mLocationLock = new Object();
    private Object mFlagLock = new Object();
    private Object mScrollLock = new Object();
    private int mScrollSpeed;

    private float mGyroX, mGyroY, mGyroZ;
    private float mGravityX, mGravityZ;
    private double mMouseSensitivity = 70.0f; // mouse sensitivity
    private double mScrollSensitivity = 1.4f; // scroll sensitivity
    private double mScrollInertia = 50;

    private int mPreMoveX, mPreMoveY;
    private int mPreFlag;
    private int mPreScrollSpeed;
    private long mLastUpdateTime;

    private SensorManager mSensorManager;
    private Sensor mGyroSensor;
    private Sensor mGravitySensor;
    private OnMouseStatusChangeListener onMouseStatusChangeListener;
    private int mInterval = SensorManager.SENSOR_DELAY_GAME; // default frequency

    public Tracker(SensorManager sensorManager) {
        mSensorManager = sensorManager;
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        resumeSensor();
    }

    public void setMouseSensitivity(double mouseSensitivity) {
        mMouseSensitivity = mouseSensitivity;
    }

    public void setScrollSensitivity(double scrollSensitivity) {
        mScrollSensitivity = scrollSensitivity;
    }

    // make mouse status data
    private void locationChange() {
        int moveX, moveY;
        boolean changed = false;

        synchronized (mSensorValueLock) {
            final double scar = Math.sqrt(mGravityX * mGravityX + mGravityZ * mGravityZ); // gravity vector size on XZ-plane
            final double s = mGravityX / scar; // sin
            final double c = mGravityZ / scar; // cos

            moveX = (int)(mMouseSensitivity * (-mGyroZ * c - mGyroX * s)); // x+ is right
            moveY = (int)(mMouseSensitivity * ( mGyroZ * s - mGyroX * c)); // y+ is bottom, because left-top of screen is (0, 0)

        }

        synchronized (mLocationLock) {
            // make event when the location has changed
            if (moveX != mPreMoveX || moveY != mPreMoveY) {
                mPreMoveX = moveX;
                mPreMoveY = moveY;
                changed = true;
            }
        }

        int scroll = 0;
        synchronized (mScrollLock) {
            long currentTime = System.currentTimeMillis();

            // make event when the mouse has scrolled
            Log.i("Tracker", "scroll : " + mScrollSpeed);

            if (mScrollSpeed != 0) {
                // set distance
                if (mPreScrollSpeed != 0) {
                    // distance = velocity * time
                    scroll = (int)(((currentTime - mLastUpdateTime) * mScrollSpeed) / 20);


                }
                else {
                    scroll = mScrollSpeed;
                }
                mPreScrollSpeed = mScrollSpeed;

                // decelerate
                if (scroll > 0) {
                    mScrollSpeed -= Math.ceil((double) scroll / mScrollInertia);
                }
                else {
                    mScrollSpeed += Math.ceil((double) -scroll / mScrollInertia);
                }
                changed = true;
            }
            else{
                mPreScrollSpeed = 0;
            }
            mLastUpdateTime = currentTime;
        }

        int flag = 0;
        synchronized (mFlagLock) {
            flag = mFlag;
            // make event when the flag has changed
            if (flag != mPreFlag) {
                mPreFlag = flag;
                changed = true;
            }
        }

        // make event
        if (changed && onMouseStatusChangeListener != null) {
            onMouseStatusChangeListener.onMouseStatusChanged(moveX, moveY, scroll, (byte) flag);
        }

        if (scroll > 0) {
            locationChange();
        }
    }

    public void setOnMouseStatusChangeListener(OnMouseStatusChangeListener onMouseStatusChangeListener) {
        this.onMouseStatusChangeListener = onMouseStatusChangeListener;
    }

    public void setInterval(int interval) {
        mInterval = interval;
    }

    // gyro sensor
    public SensorEventListener gyroListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) {}
        public void onSensorChanged(SensorEvent event) {
            // calculation formula source : https://hackmd.io/oaTqnj61RCasSOSzaFw3Yw?view, https://www.youtube.com/watch?v=cRP3xnpOsM0
            // but, this code use gravity instead of euler angle

            synchronized (mSensorValueLock) {
                mGyroX = event.values[0];
                mGyroY = event.values[1];
                mGyroZ = event.values[2];
            }
            locationChange();
        }
    };
    // gravity sensor
    public SensorEventListener gravityListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) {}
        public void onSensorChanged(SensorEvent event) {
            // float y = event.values[1]; consider only XZ-plane
            synchronized (mSensorValueLock) {
                mGravityX = event.values[0];
                mGravityZ = event.values[2];
            }
            locationChange();
        }
    };

    public void stopSensor() {
        mSensorManager.unregisterListener(gyroListener);
        mSensorManager.unregisterListener(gravityListener);
    }

    public void resumeSensor() {
        mSensorManager.registerListener(gyroListener, mGyroSensor, SensorManager.SENSOR_DELAY_FASTEST == mInterval ? SensorManager.SENSOR_DELAY_GAME : mInterval); // max frequency of gyro sensor is SENSOR_DELAY_GAME
        mSensorManager.registerListener(gravityListener, mGravitySensor, mInterval);
    }

    // set scroll speed
    public void setScrollSpeed(float scroll) {
        synchronized (mScrollLock) {
            mScrollSpeed = (int) (mScrollSensitivity * scroll);
        }
        locationChange();
    }

    // add flag (mouse down, key down...)
    public void addFlag(Flag flag) {
        boolean changed = false;
        synchronized (mFlagLock) {
            if ((mFlag & flag.getValue()) == 0) {
                mFlag |= flag.getValue();
                changed = true;
            }
        }
        if (changed) {
            locationChange();
        }
    }

    // remove flag (mouse up, key up...)
    public void removeFlag(Flag flag) {
        boolean changed = false;
        synchronized (mFlagLock) {
            if ((mFlag & flag.getValue()) > 0) {
                mFlag &= ~flag.getValue();
                changed = true;
            }
        }
        if (changed) {
            locationChange();
        }
    }

    // add and remove flag
    public void setFlag(int aFlag, int rFlag) {
        boolean changed = false;
        synchronized (mFlagLock) {
            int newFlag = (mFlag | aFlag) & ~rFlag;
            if (newFlag != mFlag) {
                mFlag = newFlag;
                changed = true;
            }
        }
        if (changed) {
            locationChange();
        }
    }
}