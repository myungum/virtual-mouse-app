package com.example.a_gun;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    private Button mButtonLeft, mButtonRight, mButtonW, mButtonS, mButtonTab, mButtonHold;
    private final double WEIGHT = 70.0f; // mouse sensitivity

    private UdpClient mClient;
    private Tracker mTracker;

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

        mClient = new UdpClient();
        mClient.resumeServerFinder(); // start finding server in LAN
        mClient.setOnFindServerListener(serverIP -> runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), "server`s ip : " + serverIP, Toast.LENGTH_SHORT).show();
        }));

        mTracker = new Tracker((SensorManager)getSystemService(Context.SENSOR_SERVICE));
        // if mouse pointer location is changed, send packet.
        mTracker.setOnLocationChangeListener((x, y, flag) -> {
            ByteBuffer buf = ByteBuffer.allocate(2 * Integer.BYTES + 1);
            buf.putInt(x);
            buf.putInt(y);
            buf.put(flag);
            mClient.send(buf.array());
        });

        // button touch listener (make flag)
        View.OnTouchListener onTouchListener = (v, event) -> {
            Tracker.Flag currentFlag = (Tracker.Flag)v.getTag();
            switch (event.getAction() ) {
                case MotionEvent.ACTION_DOWN:
                    mTracker.addFlag(currentFlag);
                    break;
                case MotionEvent.ACTION_UP:
                    mTracker.removeFlag(currentFlag);
                    break;
            }
            return true;
        };
        // left button up/down event
        mButtonLeft.setTag(Tracker.Flag.LEFT);
        mButtonLeft.setOnTouchListener(onTouchListener);

        // right button up/down event
        mButtonRight.setTag(Tracker.Flag.RIGHT);
        mButtonRight.setOnTouchListener(onTouchListener);

        // w button up/down event
        mButtonW.setTag(Tracker.Flag.W);
        mButtonW.setOnTouchListener(onTouchListener);

        // s button up/down event
        mButtonS.setTag(Tracker.Flag.S);
        mButtonS.setOnTouchListener(onTouchListener);

        // tab button up/down event
        mButtonTab.setTag(Tracker.Flag.TAB);
        mButtonTab.setOnTouchListener(onTouchListener);

        // hold button up/down event
        mButtonHold.setTag(Tracker.Flag.HOLD);
        mButtonHold.setOnTouchListener(onTouchListener);
    }

    public void onResume() {
        super.onResume();
        mTracker.resumeSensor();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void onStop() {
        super.onStop();
        mTracker.stopSensor();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}