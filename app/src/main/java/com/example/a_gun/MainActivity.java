package com.example.a_gun;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    public final int FLAG_LEFT = 0x01;
    public final int FLAG_RIGHT = 0x02;
    public final int FLAG_W = 0x04;
    public final int FLAG_S = 0x08;
    public final int FLAG_TAB = 0x10;
    public final int FLAG_HOLD = 0x20;
    private int mFlag = 0x00;
    private String mIP = null;
    private final int mMainPort = 20415;
    private final int mReplyPort = 20416;
    private Object mSensorValueLock = new Object();
    private Button mButtonLeft, mButtonRight, mButtonW, mButtonS, mButtonTab, mButtonHold;
    private float mGyroX, mGyroY, mGyroZ;
    private float mGravityX, mGravityZ;
    private final double WEIGHT = 70.0f; // mouse sensitivity
    private SensorManager mSensorManager;
    private Sensor mGyroSensor;
    private Sensor mGravitySensor;
    private DatagramSocket mSocket = null;
    private DatagramSocket mReplySocket = null;
    private int mPacketCount = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonLeft = findViewById(R.id.btn_left);
        mButtonRight = findViewById(R.id.btn_right);
        mButtonW = findViewById(R.id.btn_W);
        mButtonS = findViewById(R.id.btn_S);
        mButtonTab = findViewById(R.id.btn_Tab);
        mButtonHold = findViewById(R.id.btn_hold);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        // button touch listener (make flag)
        View.OnTouchListener onTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int currentFlag = (int)v.getTag();
                switch (event.getAction() ) {
                    case MotionEvent.ACTION_DOWN:
                        mFlag |= currentFlag;
                        break;
                    case MotionEvent.ACTION_UP:
                        mFlag &= ~currentFlag;
                        break;
                }
                return true;
            }
        };
        // left button up/down event
        mButtonLeft.setTag(FLAG_LEFT);
        mButtonLeft.setOnTouchListener(onTouchListener);

        // right button up/down event
        mButtonRight.setTag(FLAG_RIGHT);
        mButtonRight.setOnTouchListener(onTouchListener);

        // w button up/down event
        mButtonW.setTag(FLAG_W);
        mButtonW.setOnTouchListener(onTouchListener);

        // s button up/down event
        mButtonS.setTag(FLAG_S);
        mButtonS.setOnTouchListener(onTouchListener);

        // f button up/down event
        mButtonTab.setTag(FLAG_TAB);
        mButtonTab.setOnTouchListener(onTouchListener);

        // hold button up/down event
        mButtonHold.setTag(FLAG_HOLD);
        mButtonHold.setOnTouchListener(onTouchListener);

        // get 'packets per second'
        (new Thread(() -> {
            int pre_cnt = 0;
            while (true) {

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.i("send cnt", (mPacketCount - pre_cnt) + " packets/sec");
                pre_cnt = mPacketCount;
            }

        })).start();

        // find server's ip in LAN
        (new Thread(() -> {
            while (true) {
                try {
                    if (mIP != null)
                        break;

                    if (mReplySocket == null)
                        mReplySocket = new DatagramSocket(mReplyPort);
                    DatagramPacket dp = new DatagramPacket(new byte[10], 10);

                    // send broadcast packet
                    sendAndForget(mReplySocket, new byte[1], "255.255.255.255", mReplyPort);
                    // receive reply packet (without own packet)
                    mReplySocket.receive(dp);
                    if (!isThisMyIpAddress(dp.getAddress()))
                        mIP = dp.getAddress().getHostName();
                    Thread.sleep(500);
                } catch (Exception ex) {
                    Log.e("get server's ip", ex.getMessage());
                    mReplySocket = null;
                }
            }

        })).start();
    }

    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(gyroListener, mGyroSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(gravityListener, mGravitySensor, SensorManager.SENSOR_DELAY_GAME);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(gyroListener);
        mSensorManager.unregisterListener(gravityListener);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // udp send
    private void sendAndForget(DatagramSocket socket, byte[] data, String ip, int port) {
        Thread th = (new Thread(() -> {
            try {
                DatagramPacket dp = new DatagramPacket(data, data.length, InetAddress.getByName(ip), port);
                socket.send(dp);
            } catch (Exception ex) {
                Log.e("sendAndForget", ex.getMessage());
            }
        }));
        th.start();
    }

    // If the address is localhost return true. source : https://stackoverflow.com/questions/2406341/how-to-check-if-an-ip-address-is-the-local-host-on-a-multi-homed-system
    public boolean isThisMyIpAddress(InetAddress addr) {
        // Check if the address is a valid special local or loop back
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
            return true;

        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException e) {
            return false;
        }
    }

    public SensorEventListener gyroListener = new SensorEventListener() {

        public void onAccuracyChanged(Sensor sensor, int acc) {
        }

        public void onSensorChanged(SensorEvent event) {
            // calculation formula source : https://hackmd.io/oaTqnj61RCasSOSzaFw3Yw?view, https://www.youtube.com/watch?v=cRP3xnpOsM0
            // but, this code use gravity instead of euler angle
            final double moveX;
            final double moveY;
            synchronized (mSensorValueLock) {
                mGyroX = event.values[0];
                mGyroY = event.values[1];
                mGyroZ = event.values[2];

                double scar = Math.sqrt(mGravityX * mGravityX + mGravityZ * mGravityZ); // gravity vector size on XZ-plane
                double s = mGravityX / scar; // sin
                double c = mGravityZ / scar; // cos

                moveX = -mGyroZ * c - mGyroX * s; // x+ is right
                moveY = mGyroZ * s - mGyroX * c; // y+ is bottom, because left-top of screen is (0, 0)
            }

            final int x = (int)(WEIGHT * moveX);
            final int y = (int)(WEIGHT * moveY);
            if (Math.abs(x) + Math.abs(y) >= 0) {
                mPacketCount++;
                ByteBuffer buf = ByteBuffer.allocate(2 * Integer.BYTES);
                buf.putInt(x);
                buf.putInt(y);

                byte pos[] = buf.array();
                byte data[] = new byte[pos.length + 1];
                System.arraycopy(pos, 0, data, 0, pos.length);
                data[pos.length] = (byte) mFlag; // bit flag
                if (mIP != null) {
                    if (mSocket == null) {
                        try {
                            mSocket = new DatagramSocket(mMainPort);
                        } catch (SocketException e) {
                            e.printStackTrace();
                        }
                    }
                    sendAndForget(mSocket, data, mIP, mMainPort);
                }
            }
        }
    };

    public SensorEventListener gravityListener = new SensorEventListener() {

        public void onAccuracyChanged(Sensor sensor, int acc) {
        }

        public void onSensorChanged(SensorEvent event) {
            // float y = event.values[1]; consider only XZ-plane
            synchronized (mSensorValueLock) {
                mGravityX = event.values[0];
                mGravityZ = event.values[2];
            }
        }
    };
}