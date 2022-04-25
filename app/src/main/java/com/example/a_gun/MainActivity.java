package com.example.a_gun;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.nio.ByteBuffer;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity {
    private Button mButtonLeft, mButtonRight, mButtonTab, mButtonHold;
    private JoystickView mJoyStick;
    private final double WEIGHT = 70.0f; // mouse sensitivity

    private UdpClient mClient;
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonLeft = findViewById(R.id.btn_left);
        mButtonRight = findViewById(R.id.btn_right);
        mJoyStick = findViewById(R.id.joystick);
        mButtonTab = findViewById(R.id.btn_Tab);
        mButtonHold = findViewById(R.id.btn_hold);

        mClient = new UdpClient();
        mClient.resumeServerFinder(); // start finding server in LAN
        mClient.setOnFindServerListener(serverIP -> runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), "server`s ip : " + serverIP, Toast.LENGTH_SHORT).show();
        }));

        mTracker = new Tracker((SensorManager) getSystemService(Context.SENSOR_SERVICE));
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
            Tracker.Flag currentFlag = (Tracker.Flag) v.getTag();
            switch (event.getAction()) {
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

        // tab button up/down event
        mButtonTab.setTag(Tracker.Flag.TAB);
        mButtonTab.setOnTouchListener(onTouchListener);

        // hold button up/down event
        mButtonHold.setTag(Tracker.Flag.HOLD);
        mButtonHold.setOnTouchListener(onTouchListener);

        // joystick(move) event
        mJoyStick.setOnMoveListener((angle, strength) -> {
            int aFlag = 0;
            int rFlag = Tracker.Flag.W.getValue() |
                    Tracker.Flag.A.getValue() |
                    Tracker.Flag.S.getValue() |
                    Tracker.Flag.D.getValue();
            if (strength < 20) {
                ;
            }
            // w
            else if (45 < angle && angle <= 45 + 90) {
                aFlag = Tracker.Flag.W.getValue();
            }
            // a
            else if (45 + 90 < angle && angle <= 45 + 180) {
                aFlag = Tracker.Flag.A.getValue();
            }
            // s
            else if (45 + 180 < angle && angle <= 45 + 270) {
                aFlag = Tracker.Flag.S.getValue();
            }
            // d
            else {
                aFlag = Tracker.Flag.D.getValue();
            }
            mTracker.setFlag(aFlag, rFlag ^ aFlag);

            Log.i("joystick", "angle : " +angle + ", strength : " + strength);
        });
    }

    @Override
    // set menu
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_manu, menu);
        return true;
    }
    // menu select event
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mTracker != null) {
            mTracker.stopSensor();
            switch (item.getItemId()) {
                case R.id.menu_fast:
                    mTracker.setInterval(SensorManager.SENSOR_DELAY_FASTEST);
                    break;
                case R.id.menu_normal:
                    mTracker.setInterval(SensorManager.SENSOR_DELAY_GAME);
                    break;
                case R.id.menu_slow:
                    mTracker.setInterval(SensorManager.SENSOR_DELAY_UI);
                    break;
                default:
                    Toast.makeText(getApplicationContext(), "error : undefined menu id", Toast.LENGTH_SHORT).show();
                    break;
            }
            mTracker.resumeSensor();
        }

        return super.onOptionsItemSelected(item);
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