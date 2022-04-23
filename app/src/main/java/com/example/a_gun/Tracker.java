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
        double moveX, moveY ; // y+ is bottom, because left-top of screen is (0, 0)

        synchronized (mSensorValueLock) {
            final double scar = Math.sqrt(mGravityX * mGravityX + mGravityZ * mGravityZ); // gravity vector size on XZ-plane
            final double s = mGravityX / scar; // sin
            final double c = mGravityZ / scar; // cos

            moveX = -mGyroZ * c - mGyroX * s; // x+ is right
            moveY = mGyroZ * s - mGyroX * c; // y+ is bottom, because left-top of screen is (0, 0)
        }

        final int x = (int)(WEIGHT * moveX);
        final int y = (int)(WEIGHT * moveY);

        if (onLocationChangeListener != null)
            onLocationChangeListener.onLocationChanged(x, y, (byte)mFlag);
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
    }

    // remove flag (mouse up, key up...)
    public void removeFlag(Flag flag) {
        synchronized (mSensorValueLock) {
            mFlag &= ~flag.getValue();
        }
    }
}