package oscmansan.mocklocation;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class InjectLocationService extends Service {

    private static final String LOG_TAG = InjectLocationService.class.getSimpleName();

    private static final int TIMER_PERIOD = 3000;

    private String mocLocationProvider;
    private LocationManager locationManager;
    private TimerTask timerTask;
    private String address;
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
            address = intent.getStringExtra("address");
            latitude = intent.getDoubleExtra("latitude", 0);
            longitude = intent.getDoubleExtra("longitude", 0);
        }
        else {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            address = sharedPref.getString("address","");
            latitude = Double.longBitsToDouble(sharedPref.getLong("latitude", 0));
            longitude = Double.longBitsToDouble(sharedPref.getLong("longitude", 0));
        }

        startForeground(1, buildNotification());

        Timer timer = new Timer();
        initTimerTask();
        timer.schedule(timerTask, 0, TIMER_PERIOD);

        return START_STICKY;
    }

    private Notification buildNotification() {
        String contentText = "Location set to " + address + " (" + latitude + ", " + longitude + ")";
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("Mock Location")
                        .setContentText(contentText)
                        .setSmallIcon(R.drawable.ic_my_location)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                        .setColor(getResources().getColor(R.color.accent_material_light))
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText));

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        return builder.build();
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

        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
