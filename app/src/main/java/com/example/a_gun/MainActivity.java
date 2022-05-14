package com.example.a_gun;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
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
    private Button mButton;

    private UdpClient mClient;
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

        mButton = (Button)findViewById(R.id.button);
        mButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTracker.addFlag(Tracker.Flag.LEFT);
                    break;
                case MotionEvent.ACTION_UP:
                    mTracker.removeFlag(Tracker.Flag.LEFT);
                    break;
            }

            return false;
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