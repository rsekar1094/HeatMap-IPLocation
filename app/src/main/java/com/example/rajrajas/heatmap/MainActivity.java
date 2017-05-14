package com.example.rajrajas.heatmap;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Dialog;

import android.graphics.drawable.ColorDrawable;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;


import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Location;

import static com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    /**
     * Note that this may be null if the Google Play services APK is not available.
     */
    private GoogleMap mMap;
    String url;
    String result;
    Db_controller controller;
    Dialog dialog;
    List<LatLng> latlng;
    private int i_temp = 0;
    private Timer timer;

    private boolean play_bool = true;
    private FloatingActionButton fab;
    SeekBar seekBar;
    private HeatmapTileProvider provider;
    private TileOverlay overlay;


    private static final int ALT_HEATMAP_RADIUS = 40;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        controller = new Db_controller(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setTitle("Map");


        seekBar = (SeekBar) findViewById(R.id.seekBar1);
        seekBar.setVisibility(View.GONE);


        fab = (FloatingActionButton) findViewById(R.id.fab);


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (play_bool) {
                    fab.setImageResource(android.R.drawable.ic_media_pause);
                    play_function();
                    play_bool = false;
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    fab.setImageResource(android.R.drawable.ic_media_play);
                    timer.cancel();
                    play_bool = true;
                }
            }
        });


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
                seek_function(progresValue);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seek_function(progress);
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();

    }

    private void play_function() {
        i_temp = 0;
        timer = new Timer();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        timer.schedule(new SayHello(), 0, 2000);

    }

    private class SayHello extends TimerTask {
        public void run() {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    latlng = controller.get_IPLocation(i_temp);
                    if (latlng.size() > 0) {
                        addHeatMap(latlng);
                        mMap.moveCamera(newLatLngZoom(new LatLng(latlng.get(0).latitude, latlng.get(0).longitude), 4));
                        i_temp = i_temp + 1;
                    } else {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        timer.cancel();
                        fab.setImageResource(android.R.drawable.ic_media_play);
                        play_bool = true;
                    }
                }
            });

        }
    }

    private void seek_function(int temp_progress) {
        mMap.moveCamera(newLatLngZoom(new LatLng(latlng.get(temp_progress).latitude, latlng.get(temp_progress).longitude), 4));
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
        }
    }

    /**
     * Add a simple heat map to the map
     */
    private void addHeatMap(List<LatLng> list) {
        // Get the data: latitude/longitude positions of police stations.
        try {

            provider = new HeatmapTileProvider.Builder().data(list).build();
            provider.setRadius(ALT_HEATMAP_RADIUS);
            if (overlay != null)
                overlay.remove();

            overlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));

            provider.setOpacity(0.7);
            overlay.clearTileCache();
            seekBar.setMax(latlng.size() - 1);

        } catch (Exception e) {
            result = e.getMessage();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (mMap != null) {
            return;
        }
        mMap = googleMap;
        setUpMapIfNeeded();
        if (controller.get_query_string("select count(*) from IPLocation_insert").equals("0")) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            new Async_class().execute();
        } else {
            latlng = controller.get_IPLocation(0);
            addHeatMap(latlng);
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    private class Async_class extends AsyncTask<String, String, String> {

        @Override

        protected void onPreExecute() {
            super.onPreExecute();
            dialog_Show();
// Shows Progress Bar Dialog and then call doInBackground method
        }

        @Override
        protected String doInBackground(String... f_url) {
            try {
                insert_values_DB();
            } catch (Exception e) {
                e.printStackTrace();
                result = e.getMessage();
            }
            return null;
        }

        protected void onProgressUpdate(String... progress) {

        }

        @Override
        protected void onPostExecute(String file_url) {
            dialog.dismiss();
            try {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                addHeatMap(latlng);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "exec" + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        }
    }

    void dialog_Show() {
        dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.dialog_progressbar);
        dialog.setCancelable(false);

        dialog.show();

    }

    private int calc_ip_number(String ip_address) {
        String[] ip_portion = ip_address.split("\\.");


        return (16777216 * (Integer.parseInt(ip_portion[0])) + 65536 * (Integer.parseInt(ip_portion[1])) + 256 * (Integer.parseInt(ip_portion[2])) + (Integer.parseInt(ip_portion[3])));

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public Location getIPLocation(String ipStr) throws Exception {

        InputStream inputStream = getResources().openRawResource(R.raw.geolite2city);
        DatabaseReader reader = new DatabaseReader.Builder(inputStream).build();
        CityResponse response = reader.city(InetAddress.getByName(ipStr));

        return response.getLocation();

    }

    private void insert_values_DB() throws Exception {

        InputStream inputStream = getResources().openRawResource(R.raw.radar_search);
        @SuppressWarnings("resource")
        String json = new Scanner(inputStream).useDelimiter("\\A").next();
        JSONArray array = new JSONArray(json);
        controller.insert_IPLocation_insert(array.length() + "");

        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            String ip_address = object.getString("ip_Address");
            Location loc = getIPLocation(ip_address);
            String[] res = {ip_address, loc.getLatitude() + "", loc.getLongitude() + "", object.getString("time_stamp")};
            controller.insert_IPLocation(res);

        }

        latlng = controller.get_IPLocation(0);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_play) {
            latlng = controller.get_IPLocation(0);
            addHeatMap(latlng);
            mMap.moveCamera(newLatLngZoom(new LatLng(latlng.get(0).latitude, latlng.get(0).longitude), 4));

            try {
                timer.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
            play_bool = true;
            fab.setVisibility(View.VISIBLE);
            seekBar.setVisibility(View.GONE);
            return true;
        }
        else if (id == R.id.action_seek)
        {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            latlng = controller.get_IPLocation_1();
            addHeatMap(latlng);
            mMap.moveCamera(newLatLngZoom(new LatLng(latlng.get(0).latitude, latlng.get(0).longitude), 4));
            try {
                timer.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
            fab.setVisibility(View.GONE);
            seekBar.setVisibility(View.VISIBLE);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}