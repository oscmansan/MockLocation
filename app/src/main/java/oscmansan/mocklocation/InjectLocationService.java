package oscmansan.mocklocation;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class InjectLocationService extends Service {

    private static final String LOG_TAG = InjectLocationService.class.getSimpleName();

    private static final int TIMER_PERIOD = 3000;

    private String mocLocationProvider;
    private LocationManager locationManager;
    private TimerTask timerTask;
    private double latitude;
    private double longitude;

    @Override
    public void onCreate() {
        super.onCreate();
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            latitude = intent.getDoubleExtra("latitude", 0);
            longitude = intent.getDoubleExtra("longitude", 0);
        }
        else {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            latitude = Double.longBitsToDouble(sharedPref.getLong("latitude", 0));
            longitude = Double.longBitsToDouble(sharedPref.getLong("longitude", 0));
        }

        Timer timer = new Timer();
        initTimerTask();
        timer.schedule(timerTask, 0, TIMER_PERIOD);

        return START_STICKY;
    }

    private void initTimerTask() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "mock " + System.currentTimeMillis());
                Location mockLocation = new Location(mocLocationProvider);
                mockLocation.setLatitude(latitude);
                mockLocation.setLongitude(longitude);
                mockLocation.setAccuracy(5);
                mockLocation.setTime(System.currentTimeMillis());
                mockLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                locationManager.setTestProviderLocation(mocLocationProvider, mockLocation);
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timerTask.cancel();
        locationManager.removeTestProvider(mocLocationProvider);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
