package oscmansan.mocklocation;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private double latitude;
    private double longitude;

    Switch sw;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sw = (Switch) findViewById(R.id.sw);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if (isMyServiceRunning(InjectLocationService.class))
            restoreState();

        sw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw.isChecked()) {
                    startMockingLocation();
                } else {
                    stopMockingLocation();
                }
            }
        });

        findViewById(android.R.id.content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.edit_text).clearFocus();
            }
        });

        final EditText editText = (EditText) findViewById(R.id.edit_text);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    if (editText.getText().toString().equals(""))
                        editText.setText(null);
                }
            }
        });

        findViewById(R.id.hidden).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ((EditText) findViewById(R.id.edit_text)).setText("Plaça de Catalunya, Barcelona");
                return true;
            }
        });
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void restoreState() {
        ((EditText) findViewById(R.id.edit_text)).setText(sharedPref.getString("address", ""));
        latitude = Double.longBitsToDouble(sharedPref.getLong("latitude", 0));
        longitude = Double.longBitsToDouble(sharedPref.getLong("longitude", 0));
        TextView status = (TextView) findViewById(R.id.status);
        status.setText("Location set to " + latitude + ", " + longitude);
        status.setVisibility(View.VISIBLE);
        sw.setChecked(true);
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
        EditText editText = (EditText) findViewById(R.id.edit_text);
        editText.clearFocus();

        String address = editText.getText().toString();
        new GeocoderTask().execute(address);
    }

    private void stopMockingLocation() {
        ((EditText) findViewById(R.id.edit_text)).setText("");
        findViewById(R.id.status).setVisibility(View.INVISIBLE);
        Intent intent = new Intent(this, InjectLocationService.class);
        stopService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("address", ((EditText) findViewById(R.id.edit_text)).getText().toString());
        editor.putLong("latitude", Double.doubleToRawLongBits(latitude));
        editor.putLong("longitude", Double.doubleToRawLongBits(longitude));
        editor.apply();
    }

    private class GeocoderTask extends AsyncTask<String,Void,List<Address>> {

        @Override
        protected List<Address> doInBackground(String... params) {
            try {
                Geocoder geocoder = new Geocoder(MainActivity.this);
                return geocoder.getFromLocationName(params[0], 1);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {
            if (addresses.size() > 0) {
                latitude = addresses.get(0).getLatitude();
                longitude = addresses.get(0).getLongitude();

                TextView status = (TextView) findViewById(R.id.status);
                status.setText("Location set to " + latitude + ", " + longitude);
                status.setVisibility(View.VISIBLE);

                Snackbar
                        .make(findViewById(R.id.snackbar),
                                "Do you want to open Maps?",
                                Snackbar.LENGTH_LONG
                        )
                        .setAction("Go ahead", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse("geo:" + latitude + "," + longitude + "?z=15"));
                                startActivity(intent);
                            }
                        })
                        .show();

                Intent intent = new Intent(MainActivity.this, InjectLocationService.class);
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                startService(intent);
            }
            else {
                sw.setChecked(false);

                Snackbar snackbar = Snackbar.make(
                        findViewById(R.id.snackbar),
                        "Location not found",
                        Snackbar.LENGTH_SHORT
                );
                View snackbarView = snackbar.getView();
                snackbarView.setBackgroundResource(android.support.design.R.color.error_color);
                ((TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text))
                        .setGravity(Gravity.CENTER_HORIZONTAL);
                snackbar.show();
            }
        }
    }
}