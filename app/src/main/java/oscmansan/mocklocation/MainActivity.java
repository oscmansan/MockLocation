package oscmansan.mocklocation;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final double latitude = 41.386667;
    private static final double longitude = 2.17;
    private static final int TIMER_PERIOD = 5000;


    private String mocLocationProvider;
    private LocationManager locationManager;
    private Switch sw;
    private Timer timer;
    private TimerTask timerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mocLocationProvider = LocationManager.GPS_PROVIDER;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.addTestProvider(mocLocationProvider, false, false, false, false,
                true, true, true, 0, 5);
        locationManager.setTestProviderEnabled(mocLocationProvider, true);
        locationManager.requestLocationUpdates(mocLocationProvider, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        });

        sw = (Switch) findViewById(R.id.sw);
        sw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw.isChecked()) {
                    startMockingLocation();
                }
                else {
                    stopMockingLocation();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startMockingLocation() {
        timer = new Timer();
        initTimerTask();
        timer.schedule(timerTask, 0, TIMER_PERIOD);
    }

    private void initTimerTask() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOG_TAG, "mock " + System.currentTimeMillis() / 1000 % 100);
                        Location mockLocation = new Location(mocLocationProvider);
                        mockLocation.setLatitude(latitude);
                        mockLocation.setLongitude(longitude);
                        mockLocation.setAccuracy(5);
                        mockLocation.setTime(System.currentTimeMillis());
                        mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                        locationManager.setTestProviderLocation(mocLocationProvider, mockLocation);
                    }
                });
            }
        };
    }

    private void stopMockingLocation() {
        timerTask.cancel();
    }
}
