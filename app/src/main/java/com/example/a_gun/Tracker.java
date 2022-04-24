package com.example.a_gun;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Tracker {
    interface OnLocationChangeListener {
        void onLocationChanged(int x, int y, byte flag);
    }

    public enum Flag {
        LEFT(0x01),
        RIGHT(0x02),
        W(0x04),
        S(0x08),
        TAB(0x10),
        HOLD(0x20);

        private final int value;
        Flag(int value) { this.value = value; }
        public int getValue() { return value; }
    }
    private int mFlag = 0x00;

    private Object mSensorValueLock = new Object();
    private Object mLocationLock = new Object();
    private int mPreMoveX, mPreMoveY;
    private int mPreFlag;
    private float mGyroX, mGyroY, mGyroZ;
    private float mGravityX, mGravityZ;
    private final double WEIGHT = 70.0f; // mouse sensitivity
    private SensorManager mSensorManager;
    private Sensor mGyroSensor;
    private Sensor mGravitySensor;
    private OnLocationChangeListener onLocationChangeListener;
    private int mInterval = SensorManager.SENSOR_DELAY_GAME; // default frequency

    public Tracker(SensorManager sensorManager) {
        mSensorManager = sensorManager;
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        resumeSensor();
    }

    // make mouse status data
    private void locationChange() {
        int moveX, moveY;
        int flag;
        boolean changed = false;

        synchronized (mSensorValueLock) {
            final double scar = Math.sqrt(mGravityX * mGravityX + mGravityZ * mGravityZ); // gravity vector size on XZ-plane
            final double s = mGravityX / scar; // sin
            final double c = mGravityZ / scar; // cos

            moveX = (int)(WEIGHT * (-mGyroZ * c - mGyroX * s)); // x+ is right
            moveY = (int)(WEIGHT * ( mGyroZ * s - mGyroX * c)); // y+ is bottom, because left-top of screen is (0, 0)
            flag = mFlag; // get value while avoiding race condition
        }

        synchronized (mLocationLock) {
            // don't make event when the location has not changed
            if (moveX != mPreMoveX || moveY != mPreMoveY || flag != mPreFlag) {
                mPreMoveX = moveX;
                mPreMoveY = moveY;
                mPreFlag = flag;
                changed = true;
            }
        }

        // make event
        if (changed && onLocationChangeListener != null)
            onLocationChangeListener.onLocationChanged(moveX, moveY, (byte)mFlag);
    }

    public void setOnLocationChangeListener(OnLocationChangeListener onLocationChangeListener) {
        this.onLocationChangeListener = onLocationChangeListener;
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

    // add flag (mouse down, key down...)
    public void addFlag(Flag flag) {
        synchronized (mSensorValueLock) {
            mFlag |= flag.getValue();
        }
        locationChange();
    }

    // remove flag (mouse up, key up...)
    public void removeFlag(Flag flag) {
        synchronized (mSensorValueLock) {
            mFlag &= ~flag.getValue();
        }
        locationChange();
    }
}