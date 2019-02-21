package com.example.googleapilocation;


import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.onesignal.OneSignal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener,GoogleMap.OnMarkerDragListener {
    private static final String TAG = "MAp";
    GoogleMap mMap;
    boolean mLocationPermissionGranted;
    FusedLocationProviderClient mFusedLocationProviderClient;
    Location mLastKnownLocation;
    String unixDate;



    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION=1;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // OneSignal Initialization
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();

        mFusedLocationProviderClient = new FusedLocationProviderClient(this);
       Button btn_dark=findViewById(R.id.btn_dark);

       btn_dark.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v){
             getDateDialog();

               double lat = mMap.getCameraPosition().target.latitude;
               double lng = mMap.getCameraPosition().target.longitude;


             if(unixDate!=null)
             getDarkSky( lat,lng,unixDate);
             else
                 Toast.makeText(getApplicationContext(),"Dont give me null DATE",Toast.LENGTH_SHORT).show();

           }
       });

    }




    private void getDateDialog(){


        DatePickerDialog datePick=new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            Calendar calendar=Calendar.getInstance();
            calendar.set(year,month,dayOfMonth);
                long epoch=calendar.getTimeInMillis()/ 1000;
                unixDate =Long.toString(epoch);


            }
        },
                2019,1,15 );
        datePick.show();



    }



    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted= true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap=googleMap;
        mMap.setOnMapClickListener(this);

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .title("Marker"));
        mMap.setOnMarkerDragListener(this);

        updateLocationUI();


        getDeviceLocation();
        Toast.makeText(this,mMap.getCameraPosition().toString(),Toast.LENGTH_SHORT).show();
        mMap.setOnMyLocationClickListener(new GoogleMap.OnMyLocationClickListener() {
            @Override
            public void onMyLocationClick(@NonNull Location location) {
                mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(),location.getLongitude())).title("marker"));
                CameraUpdate cameraUpdate=CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()),14f);
                mMap.animateCamera(cameraUpdate);
            }
        });
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(),16f));


            }
        });

    }
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }
    private void getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            mLastKnownLocation = (Location) task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), 4f));
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(36,55), 5f));
                            mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        }
                    }
                });

            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    public void show(View view) {
        Toast.makeText(this,mMap.getCameraPosition().toString(),Toast.LENGTH_SHORT).show();
        getWeatherData(mMap.getCameraPosition().target.latitude,mMap.getCameraPosition().target.longitude);
    }

    @Override
    public void onMapClick(LatLng latLng) {
mMap.clear();
        Toast.makeText(this,"latitude=="+latLng.latitude+"logitude=="+latLng.longitude,Toast.LENGTH_SHORT).show();
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("latitude=="+latLng.latitude+"logitude=="+latLng.longitude)
                .draggable(true));


        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,16f));
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(),16f));
    }

    private void getWeatherData(double lat,double lng) {

        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        final TextView tv_detail = findViewById(R.id.tv_detail);
        String url = "https://api.openweathermap.org/data/2.5/weather?lat="+lat+"&lon="+lng+"&appid=API_KEY_OPEN_WEATHER";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null,
                        new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    JSONArray jsonArray = response.getJSONArray("weather");
                                    JSONObject jsonObject = jsonArray.getJSONObject(0);


                                    tv_detail.setText(jsonObject.getString("description"));

                                } catch (JSONException e) {

                                    e.printStackTrace();

                                }
                            }
                        }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Open Weather Error", Toast.LENGTH_SHORT).show();

                    }
                });
        requestQueue.add(jsonObjectRequest);

    }

    private void getDarkSky(double lat,double lng,String unixDate) {
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        final TextView tv_dark = findViewById(R.id.tv_dark);
        String url = "https://api.darksky.net/forecast/API_KEY_DARK_SKY/"+lat+","+lng+","+unixDate;

        Log.v("DARK SKY",url);


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null,
                        new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    JSONObject jsonObject = response.getJSONObject("daily");
                                    JSONArray jsonArray=jsonObject.getJSONArray("data");
                                    JSONObject jsonObject1=jsonArray.getJSONObject(0);


                                    tv_dark.setText("precipIntensity "+jsonObject1.getString("precipIntensity")+"\nprecipType "+jsonObject1.getString("precipType"));

                                } catch (JSONException e) {

                                    e.printStackTrace();

                                }
                            }
                        }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "DARK sky Error", Toast.LENGTH_SHORT).show();

                    }
                });
       requestQueue.add(jsonObjectRequest);
    }

}
