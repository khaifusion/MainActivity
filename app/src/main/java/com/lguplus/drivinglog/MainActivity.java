package com.lguplus.drivinglog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.SphericalUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;

    private View mLayout;  // Snackbar ???????????? ???????????? View??? ?????????(????????? Toast????????? Context??? ?????????)
    private LocationRequest locationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private GoogleMap mMap;
    private boolean cameraIdle; // ???????????? ????????? ???????????? ????????? ???????????? flag
    private Marker currentMarker = null;
    private static final String TAG = "===== Driving Log =====";

    private LatLng startLatLng = new LatLng(0, 0);
    private LatLng endLatLng = new LatLng(0, 0);
    List<Polyline> polylines = new ArrayList<>();

    LatLng currentPosition;
    Location mCurrentLocation;

    List<Location> locationList = null;
    double latitude;
    double longitude;

    Button startBtn;
    Button stopBtn;
    Button buttonStats;

    // ????????? ?????? ??????
    private Chronometer chronometer;
    private boolean isRunningTimer;
    private long pauseOffset;
    private int totTime = 0;
    private double totDistance = 0;

    // ???????????? ?????? ??????
    TextView tv_distance;

    // back??? ?????? ?????? ??????
    private final long finishtimeed = 2000;
    private long presstime = 0;

    // ?????? ?????? ?????? ??????
    private String SERVER_URL = "http://124.49.91.86:8002/cars/completeDrive/";
    private String carNum;
    private String id;
    private String statsStartTime;
    private String statsStopTime;
    private String statsStartLatLng;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ?????? sleep ????????? ???????????? ????????? ??????
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        // ????????????, id ???????????? ?????????
        Intent intent = getIntent();
        carNum = intent.getStringExtra("carNum");
        id = intent.getStringExtra("id");
        Toast.makeText(this, id + " ?????? ???????????? ?????????\n" + carNum + "?????????.", Toast.LENGTH_LONG).show();

        // ????????? ?????? ??????
        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat("?????? : %s");

        // ???????????? ?????? ??????
        tv_distance = findViewById(R.id.tv_distance);


        mLayout = findViewById(R.id.map);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        // ??????????????? ??????????????? ????????? ??????????????? ???????????? ??????
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            statsStartLatLng = location.getLatitude() + ", " + location.getLongitude();
                        }
                    }
                });


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);


        startBtn = findViewById(R.id.buttonStartLocationUpdates);
        stopBtn = findViewById(R.id.buttonStopLocationUpdates);
        buttonStats = findViewById(R.id.buttonStats);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
                } else {
                    startLocationService();
                    // ????????? ??????
                    startTimer();
                    // ?????? ???????????? ???????????? ?????? ???????????? ??????
                    statsStartTime = getDateTime();
                }
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationService();
                // ????????? ??????
                stopTimer();
                // ?????? ???????????? ???????????? ?????? ???????????? ??????
                statsStopTime = getDateTime();
                // ????????? ???????????? ??????
                makeRequest();
            }
        });

        buttonStats.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // ?????? ?????? ???????????? ?????? ??????
                Intent intent = new Intent(getApplicationContext(), StatsActivity.class);
                intent.putExtra("carNum", carNum);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isLocationServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (LocationService.class.getName().equals(service.service.getClassName())) {
                    if (service.foreground) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    private void startLocationService() {
        if (!isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this, "Location service started", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationService() {
        if (isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this, "Location service stopped", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "onMapReady() START");
        mMap = googleMap;

        //????????? ??????????????? ????????? ??????
        setDefaultLocation();

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                cameraIdle = true;
                Log.d("== onMapReady", "cameraIdle ?????? TRUE??? ??????");
            }
        });
        Log.d(TAG, "onMapReady() END");
    }

    public void setDefaultLocation() {
        Log.d(TAG, "setDefaultLocation() START");
        LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);

        String markerTitle = "???????????? ????????? ??? ??????";
        String markerSnippet = "?????? ???????????? GPS ?????? ????????? ???????????????";

        if (currentMarker != null) currentMarker.remove();

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        currentMarker = mMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 18);
        mMap.moveCamera(cameraUpdate);    // ?????? ?????? ????????????

        if (mMap != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mMap.setMyLocationEnabled(true);    // ?????? ???????????? ?????? ????????? ??????

        } else {
            Log.d("-------- ERROR --------", "?????? ?????? ??????.. setDefaultLocation() ?????? ");
        }
        Log.d(TAG, "setDefaultLocation() END");
    }

    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {
        Log.d(TAG, "setCurrentLocation() START");
        if (currentMarker != null) currentMarker.remove();

        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLatLng);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);

        currentMarker = mMap.addMarker(markerOptions);

        if(cameraIdle == true) {
            cameraIdle = false;
            Log.d("== setCurrentLocation()", "cameraIdle ?????? TRUE?????? ???????????????.");
        } else {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
            mMap.moveCamera(cameraUpdate);    // ?????? ??? ??????
            mMap.animateCamera(cameraUpdate);   // ???????????? ??? ??????
            Log.d("== setCurrentLocation()", "cameraIdle ?????? FALSE??? ??????!!!");
        }
        Log.d(TAG, "setCurrentLocation() END");
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(mMessageReceiver, new IntentFilter("latLngIntent"));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BroadcastReceiver START");
            // TODO Auto-generated method stub
            // ????????? -> ??????????????? LocationList??? ???????????? ?????????????????? ?????????
            locationList = intent.getParcelableArrayListExtra("BroadcastLocationList");
            mCurrentLocation = locationList.get(locationList.size() - 1);
            latitude = mCurrentLocation.getLatitude();
            longitude = mCurrentLocation.getLongitude();

            if(startLatLng.latitude==0 && startLatLng.longitude==0){
                cameraIdle = false; // ?????? Start????????? ????????? ?????? ?????? ??????????????? ???????????? ????????? ????????? ?????? false??? ??????
                startLatLng = new LatLng(latitude,longitude);
            }

            // LocationList??? ????????? Location?????? ???????????? ??? ?????? ???????????? ??????
            currentPosition = new LatLng(latitude, longitude);
            setCurrentLocation(mCurrentLocation,
                    getCurrentAddress(currentPosition),
                    "????????? : " + latitude + ", " + longitude);
            endLatLng = new LatLng(latitude, longitude);
            drawPath();

            // distance : ?????? ????????????, totDistance : ??? ????????????
            double distance = SphericalUtil.computeDistanceBetween(startLatLng, endLatLng);
            totDistance += distance;
            
            if(totDistance < 1000){
                tv_distance.setText("???????????? : " + Math.round(totDistance) + "m");
            } else {
                // 1,000m ????????? ?????? km????????? ???????????? ????????? ?????????????????? ?????????
                tv_distance.setText("???????????? : " + (double)Math.round(totDistance/1000*100)/100 + "km");
            }

            startLatLng = endLatLng;
            Log.d(TAG, "BroadcastReceiver END");
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }


    public String getCurrentAddress(LatLng latlng) {
        //???????????? : GPS??? ????????? ??????
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(latlng.latitude, latlng.longitude, 1);
        } catch (IOException ioException) {
            //???????????? ????????? ??????
            return "???????????? ????????? ????????????";
        } catch (IllegalArgumentException illegalArgumentException) {
            return "????????? GPS ??????";
        }

        if (addresses==null || addresses.size()==0) {
            return "?????? ?????????";
        } else {
            Address address = addresses.get(0);
            return address.getAddressLine(0);
        }
    }

    // PolyLine??? ???????????? ?????????
    private void drawPath(){
        Log.d(TAG, "drawPath START");
        PolylineOptions options = new PolylineOptions().add(startLatLng).add(endLatLng).width(15).color(Color.BLACK).geodesic(true);
        polylines.add(mMap.addPolyline(options));

        /*
        if(cameraIdle == true) {
            cameraIdle = false;
            Log.d("===== drawPath() =====", "cameraIdle ?????? TRUE?????? ???????????????.");
        } else {
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 18));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 18));
            Log.d("===== drawPath() =====", "cameraIdle ?????? FALSE??? ??????!!!");
        }
        */

        Log.d(TAG, "drawPath END");
    }

    public void startTimer(){
        chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
        chronometer.start();
        isRunningTimer = true;
    }

    public void stopTimer(){
        if(isRunningTimer){
            chronometer.stop();
            pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
            isRunningTimer = false;
            totTime = Math.round(pauseOffset/1000);
            Log.d("-------- DEGUB --------", String.valueOf(totTime + "???"));
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - presstime;

        if (0 <= intervalTime && finishtimeed >= intervalTime)
        {
            //Intent tmpIntent = new Intent(this, MainActivity.class);
            //tmpIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            //startActivity(tmpIntent);
            stopLocationService();
            finish();
        }
        else
        {
            presstime = tempTime;
            Toast.makeText(getApplicationContext(), "????????? ???????????? ?????? ???????????????", Toast.LENGTH_SHORT).show();
        }
    }

    // ????????? ????????? ???????????? ??????
    public void makeRequest(){
        String url = SERVER_URL;

        //RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject requestjsonObject = new JSONObject();
        try{
            requestjsonObject.put("id", id);
            requestjsonObject.put("carNum", carNum);
            requestjsonObject.put("startTime", statsStartTime);
            requestjsonObject.put("endTime", statsStopTime);
            requestjsonObject.put("startLatLng", statsStartLatLng);
            requestjsonObject.put("endLatLng", endLatLng.latitude + ", " + endLatLng.longitude);
            requestjsonObject.put("totDistance", totDistance);
            Log.d(TAG, "id : " + id);
            Log.d(TAG, "carNum : " + carNum);
            Log.d(TAG, "startTime : " + statsStartTime);
            Log.d(TAG, "endTime : " + statsStopTime);
            Log.d(TAG, "startLatLng : " + statsStartLatLng);
            Log.d(TAG, "endLatLng : " + endLatLng.latitude + ", " + endLatLng.longitude);
            Log.d(TAG, "totDistance : " + totDistance);
        }catch (JSONException e){
            e.printStackTrace();
        }


        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestjsonObject,
                //TODO ????????? ?????? ?????? ??????
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try{
                            Log.d(TAG, "?????? ?????? -> " + response.toString());

                        } catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                },
                //TODO ????????? ?????? ?????? ?????? ??????
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "?????? ?????? -> " + error.toString());
                    }
                });

        request.setShouldCache(false);
        NumberActivity.requestQueue.add(request);
    }

    public String getDateTime(){
        String formatedNow = null;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDateTime now = LocalDateTime.now();
            formatedNow = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        return formatedNow;
    }

    // ????????? ????????? ???????????? ??????
    /*
    public void makeRequest(){
        String url = SERVER_URL;

        //RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject jsonBodyObj = new JSONObject();
        try{
            jsonBodyObj.put("carNum", carNum);
            jsonBodyObj.put("startTime", "0000");
            jsonBodyObj.put("endTime", "9999");
            jsonBodyObj.put("startLatLng", "000.000");
            jsonBodyObj.put("endLatLng", "999.999");
            jsonBodyObj.put("totDistance", totDistance);
        }catch (JSONException e){
            e.printStackTrace();
        }
        final String requestBody = String.valueOf(jsonBodyObj.toString());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, null,
                //TODO ????????? ?????? ?????? ??????
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try{
                            Log.d(TAG, "?????? ?????? -> " + response.toString());

                        } catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                },
                //TODO ????????? ?????? ?????? ?????? ??????
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "?????? ?????? -> " + error.toString());
                    }
                })
                //TODO ????????? ?????? ?????? ??? Body ????????? ????????? ?????? ??????
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError{
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
            @Override
            public byte[] getBody() {
                try {
                    if (requestBody != null && requestBody.length()>0 && !requestBody.equals("")){
                        return requestBody.getBytes("utf-8");
                    }
                    else {
                        return null;
                    }
                } catch (UnsupportedEncodingException uee) {
                    return null;
                }
            }
        };

        request.setShouldCache(false);
        NumberActivity.requestQueue.add(request);
    }
    */

}