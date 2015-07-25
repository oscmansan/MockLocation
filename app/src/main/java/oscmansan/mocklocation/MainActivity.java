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
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private double latitude;
    private double longitude;

    private Switch sw;
    private EditText editText;
    private SharedPreferences sharedPref;
    ArrayList<String> history;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sw = (Switch) findViewById(R.id.sw);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

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

        editText = (EditText) findViewById(R.id.edit_text);
        history = new ArrayList<>();
        for (int i = 0; i < sharedPref.getInt("size",0); ++i)
            history.add(sharedPref.getString(String.valueOf(i),""));
        initAdapter();

        ((AutoCompleteTextView) editText).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                View v = getWindow().getDecorView().findViewById(android.R.id.content);
                hideKeyboard(v);
            }
        });

        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                    if (editText.getText().toString().equals(""))
                        editText.setText(null);
                }
            }
        });

        editText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!adapter.isEmpty())
                    ((AutoCompleteTextView) editText).showDropDown();
                return false;
            }
        });

        findViewById(R.id.hidden).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                editText.setText("Plaça de Catalunya, Barcelona");
                return true;
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

        switch (item.getItemId()) {
            case R.id.action_clear_history:
                history = new ArrayList<>();
                initAdapter();
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isMyServiceRunning(InjectLocationService.class))
            restoreState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveState();
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

    private void hideKeyboard(View v) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void initAdapter() {
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, history);
        ((AutoCompleteTextView) editText).setAdapter(adapter);
    }

    private void startMockingLocation() {
        editText.clearFocus();
        editText.setEnabled(false);

        String address = editText.getText().toString();
        new GeocoderTask().execute(address);
    }

    private void stopMockingLocation() {
        editText.setText("");
        editText.setEnabled(true);
        findViewById(R.id.status).setVisibility(View.INVISIBLE);
        Intent intent = new Intent(this, InjectLocationService.class);
        stopService(intent);
    }

    private void restoreState() {
        editText.setText(sharedPref.getString("address", ""));
        latitude = Double.longBitsToDouble(sharedPref.getLong("latitude", 0));
        longitude = Double.longBitsToDouble(sharedPref.getLong("longitude", 0));
        TextView status = (TextView) findViewById(R.id.status);
        status.setText("Location set to " + latitude + ", " + longitude);
        status.setVisibility(View.VISIBLE);
        sw.setChecked(true);
        editText.setEnabled(false);
    }

    private void saveState() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("address", editText.getText().toString());
        editor.putLong("latitude", Double.doubleToRawLongBits(latitude));
        editor.putLong("longitude", Double.doubleToRawLongBits(longitude));
        editor.putInt("size", adapter.getCount());
        for (int i = 0; i < adapter.getCount(); ++i)
            editor.putString(String.valueOf(i), adapter.getItem(i));
        editor.apply();
    }

    private class GeocoderTask extends AsyncTask<String,Void,List<Address>> {

        private String address;

        @Override
        protected List<Address> doInBackground(String... params) {
            try {
                address = params[0];
                Geocoder geocoder = new Geocoder(MainActivity.this);
                return geocoder.getFromLocationName(address, 1);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {
            if (addresses != null && addresses.size() > 0) {
                if (!history.contains(address)) {
                    history.add(address);
                    initAdapter();
                }

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
                intent.putExtra("address", address);
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                startService(intent);
            }
            else {
                sw.setChecked(false);
                editText.setEnabled(true);

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