package com.maps.pavan.semi_final_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import android.app.Application;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Reference: https://github.com/googlesamples/android-play-location/tree/master/LocationUpdates
 */

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.console)
    TextView textView;

    @BindView(R.id.start)
    Button btnStartUpdates;

    @BindView(R.id.stop)
    Button btnStopUpdates;

    // location last updated time
    private String mLastUpdateTime;

    // location updates interval - 10sec
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 2000;

    // fastest updates interval - 5 sec
    // location updates will be received if another app is requesting the locations
    // than your app can handle
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    private static final int REQUEST_CHECK_SETTINGS = 100;


    // bunch of location related apis
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;

    public String res = "0";

    private WifiManager wifiManager;
    private List<ScanResult> results;
    public float time_stamp;



    // boolean flag to toggle the ui
    private Boolean mRequestingLocationUpdates;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
//        getActionBar().hide();

        TextView textView = findViewById(R.id.console);
        textView.setMovementMethod(new ScrollingMovementMethod());

        // initialize the necessary libraries
        init();

        // restore the values from saved instance state
        restoreValuesFromBundle(savedInstanceState);

    }





    private void init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // location is received
                mCurrentLocation = locationResult.getLastLocation();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

                updateLocationUI();
            }
        };

        mRequestingLocationUpdates = false;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * Restoring values from saved instance state
     */
    private void restoreValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("is_requesting_updates")) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean("is_requesting_updates");
            }

            if (savedInstanceState.containsKey("last_known_location")) {
                mCurrentLocation = savedInstanceState.getParcelable("last_known_location");
            }

            if (savedInstanceState.containsKey("last_updated_on")) {
                mLastUpdateTime = savedInstanceState.getString("last_updated_on");
            }
        }

        updateLocationUI();
    }



    /**
     * Update the UI displaying the location data
     * and toggling the buttons
     */
    private void updateLocationUI() {
        if (mCurrentLocation != null) {


            res = mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude();
            textView.setTextColor(Color.WHITE);

//            textView.append(
//                    "\n Lat: " + mCurrentLocation.getLatitude() + ", " +
//                            "Lng: " + mCurrentLocation.getLongitude()
//
//
//            );

            // giving a blink animation on TextView
            textView.setAlpha(0);
            textView.animate().alpha(1).setDuration(300);

            // location last updated time
//            textView.append("Last updated on: " + mLastUpdateTime);
        }

        toggleButtons();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("is_requesting_updates", mRequestingLocationUpdates);
        outState.putParcelable("last_known_location", mCurrentLocation);
        outState.putString("last_updated_on", mLastUpdateTime);

    }

    private void toggleButtons() {
        if (mRequestingLocationUpdates) {
            btnStartUpdates.setEnabled(false);
            btnStopUpdates.setEnabled(true);
        } else {
            btnStartUpdates.setEnabled(true);
            btnStopUpdates.setEnabled(false);
        }
    }

    /**
     * Starting location updates
     * Check whether location settings are satisfied and then
     * location updates will be requested
     */
    private void startLocationUpdates() {
        mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        Toast.makeText(getApplicationContext(), "Started location updates!", Toast.LENGTH_SHORT).show();

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateLocationUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);

                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }

                        updateLocationUI();
                    }
                });
    }

    @OnClick(R.id.start)
    public void startLocationButtonClick() {
        // Requesting ACCESS_FINE_LOCATION using Dexter library
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        mRequestingLocationUpdates = true;
                        startLocationUpdates();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        if (response.isPermanentlyDenied()) {
                            // open device settings when the permission is
                            // denied permanently
                            openSettings();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    @OnClick(R.id.stop)
    public void stopLocationButtonClick() {
        mRequestingLocationUpdates = false;
        stopLocationUpdates();
    }

    public void stopLocationUpdates() {
        // Removing location updates
        mFusedLocationClient
                .removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(getApplicationContext(), "Location updates stopped!", Toast.LENGTH_SHORT).show();
                        toggleButtons();
                    }
                });
    }

    @OnClick(R.id.getlaloc)
    public void showLastKnownLocation() {
        if (mCurrentLocation != null) {
//            Toast.makeText(getApplicationContext(), "Lat: " + mCurrentLocation.getLatitude()
//                    + ", Lng: " + mCurrentLocation.getLongitude(), Toast.LENGTH_LONG).show();
            TextView textView = findViewById(R.id.console);
            textView.append("\n  current_location : "+"Lat: " + mCurrentLocation.getLatitude()+ ", Lng: " + mCurrentLocation.getLongitude());

        } else {
            Toast.makeText(getApplicationContext(), "Last known location is not available!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.e(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.e(TAG, "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        break;
                }
                break;
        }
    }

    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package",
                BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /*
        The below function onResume() is used to state what event to perform when the location updates are started
     */

    @Override
    public void onResume() {
        super.onResume();

        // Resuming location updates depending on button state and
        // allowed permissions
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        }

        updateLocationUI();
    }

    /*
    The below code is used to ckeck permissions for location updates
     */

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    /*
    The below function onpause is used to stop the location updates
     */

    @Override
    protected void onPause() {
        super.onPause();

        if (mRequestingLocationUpdates) {
            // pausing location updates
            stopLocationUpdates();
        }
    }


    /*
    The below code on_clear() is used to clear the display screen
     */

    public void on_Clear(View view) {

        TextView textView = findViewById(R.id.console);
        textView.setTextColor(Color.WHITE);
        textView.setText("");

    }


////////////////////////////////////////////////////////


    public double calculateDistance(double signalLevelInDb, double freqInMHz) {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(signalLevelInDb)) / 20.0;
        return Math.pow(10.0, exp);
    }


    /*
    The Below scanWifi() is used to register Receiver and obtain the data provided by the receiver.
     */

    public void scanWifi() {


        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Toast.makeText(this, "Scanning .....", Toast.LENGTH_LONG).show();
    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {



            results = wifiManager.getScanResults();
//            reso = wifiManager.getScanResults();
            TextView textView = findViewById(R.id.console);
            textView.setTextColor(Color.WHITE);
            for (ScanResult scanResult : results) {
                textView.append("\n "+scanResult.SSID + " ");
                textView.append(scanResult.BSSID+ "\n");
//                textView.append(String.valueOf(scanResult.timestamp) + "\n");
//                textView.append("Freq :"+String.valueOf(scanResult.frequency) + "\n");
                textView.append("  level : "+ String.valueOf(WifiManager.calculateSignalLevel(scanResult.level, 4)));
//                double exp = (27.55 - (20 * Math.log10(scanResult.frequency)) + Math.abs(scanResult.level)) / 20.0;
//                textView.append("\n distance : "+String.valueOf(Math.pow(10.0, exp)));
                textView.append("   Signal Strength : " + String.valueOf(scanResult.level) + "\n");
                textView.append(" Distance : "+String.valueOf(calculateDistance(scanResult.level,scanResult.frequency)));



            }
            unregisterReceiver(this);


        }
    };


    /*
    The Below function get wifi is used to check permissions and execute the scanWifi() function.
     */


    public void getWifi() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 0x12345);
        } else {
            scanWifi(); // the actual wifi scanning

        }
    }

    /*
    The Below Function sc_plz() uses WifiManager class to access wifi parameters such as wifi state and change wifi state
     */

    public void sc_plz(View view) {


        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WIFI is disabled", Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }


        if (checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 0x12345);

        } else {
            scanWifi();

        }

    }


////////////////////////////////////////////////

    /*
    The below function collect_data is used to upload the captured parameters such as :

                1.Location
                2.WifiDetails
      to the cloud database.
     */

    public void collect_data(View view) throws ParseException {


        Switch sw = findViewById(R.id.IsRouter);
        TextView inpview = (TextView) findViewById(R.id.itno);
        boolean state = sw.isChecked();
        ParseObject parseObject;

        {


            for(ScanResult scanResult : results)
            {

                parseObject = new ParseObject("LocationData");

                if(state){

                    parseObject.put("IsRouter",1);

                }
                else
                {
                    parseObject.put("IsRouter",0);
                }

                parseObject.put("Iteration_No",inpview.getText().toString());
                String[] loc_list = res.split(",");

                parseObject.put("Latitude",loc_list[0]);
                parseObject.put("Longitude",loc_list[1]);

                parseObject.put("SSID",String.valueOf(scanResult.SSID));

                parseObject.put("BSSID",String.valueOf(scanResult.BSSID));

                parseObject.put("Signal_Strength",String.valueOf(scanResult.level));


//                parseObject.put("Estimated Distance",calculateDistance(scanResult.frequency,scanResult.level));

                time_stamp = scanResult.timestamp;
                parseObject.put("timeStamp",String.valueOf(time_stamp));


                parseObject.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {

                        Log.d("MESSAGE OBJECT", " SAVED");

                        if (e != null){
                            Log.e("ERROR",e.toString());
                        }


                    }});


                parseObject.save();




            }





        }

        TextView textView = (TextView) findViewById(R.id.console);
        textView.setTextColor(Color.WHITE);
        Toast.makeText(this,"uploading....",Toast.LENGTH_SHORT).show();
        textView.append("\n .:::::::::> Uploaded :::::: \n");


    }

    /*the Below code is used to delete the last entry thet hasbeen collected by the user
      the function del_last_rec() uses lastrecorded timestamp to delete the required entry.
     */

    public void del_last_rec(View view){



        ParseQuery<ParseObject> query = ParseQuery.getQuery("LocationData");
        query.whereEqualTo("timeStamp", String.valueOf(time_stamp));
        query.findInBackground(new FindCallback<ParseObject>() {

            @Override
            public void done(List<ParseObject> objects, ParseException e) {


                if(e==null)
                {
                    if(objects.size()>0){
                        for(ParseObject object: objects){


                           object.deleteInBackground(new DeleteCallback() {
                               @Override
                               public void done(ParseException e) {
                                   if(e==null)
                                   {
                                       TextView textView = findViewById(R.id.console);
                                       textView.append("\n -----> Last Entry Deleted \n");
                                   }
                                   else
                                   {
                                       TextView textView = findViewById(R.id.console);
                                       textView.setTextColor(Color.RED);
                                       textView.append(e.toString());

                                   }
                               }
                           });
                        }
                    }
                    else {

                        TextView textView = findViewById(R.id.console);
                        textView.setTextColor(Color.RED);
                        textView.append("\n Didn't find any Match for last entry !");

                    }
                }
            }
        });

//        query.findInBackground(new FindCallback<ParseObject>() {
//            @Override
//            public void done(List<ParseObject> objects, ParseException e) {
//                for(ParseObject object : objects)
//                {
//                    object.deleteInBackground();
//                    TextView textView = (TextView) findViewById(R.id.console);
//                    textView.setText("Done");
//
//                }
//            }
//        });


    }





}







