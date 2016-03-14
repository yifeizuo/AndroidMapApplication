package com.project.jilin.googletestapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.TravelMode;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

//https://developers.google.com/maps/web-services/client-library#terms_and_conditions
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;

    private LocationRequest mLocationRequest;
    //https://developers.google.com/android/guides/api-client#Starting
    private GoogleApiClient mGoogleApiClient;


    private static final String TAG = "rtsd-2016";
    private Location mLastLocation;
    private static final int REQUEST_CHECK_SETTINGS = 1;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "2";
    //track whether the user has turned location updates on or off
    private boolean mRequestingLocationUpdates = true;
    private static final String LOCATION_KEY = "3";
    private Location mCurrentLocation;
    private static final String LAST_UPDATED_TIME_STRING_KEY = "4";
    private String mLastUpdateTime;
    private Button mNavigateButton;
    private static final String GOOGLE_MAPS_SERVER_API_KEY = "AIzaSyAGFZeAMuorbEdxNitNHt7l1PIbsvveQ4I";

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        //Requests location updates from the FusedLocationApi.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        break;
                }
                break;
        }
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
        Log.d(TAG, "googleapiclient connected");
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
        Log.d(TAG, "googleapiclient disconnected");
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mNavigateButton = (Button) findViewById(R.id.navigateButton);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        addNavigateButtonListener();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        outState.putParcelable(LOCATION_KEY, mCurrentLocation);
        outState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No permission for getting location");
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        if (mLastLocation != null) {
            Log.d(TAG, "Latitude: " + String.valueOf(mLastLocation.getLatitude()));
            Log.d(TAG, "Logitude: " + String.valueOf(mLastLocation.getLongitude()));
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
    }


    // My Location layer does not return any data. If you wish to access location data programmatically, use the Location API.
    private void enableMyLocationLayer() {
        //Aiming at Android 6 or later runtime permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    Log.d(TAG, "my location button is clicked");

                    if (mLastLocation == null) {
                        checkLocationSettings();
                    } else {
                        //TODO - It requires another click to activate camera move to current location. Perhaps await for PendingResult?
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())));
                    }

                    return false;
                }
            });
        } else {
            // Show rationale and request permission.
            Log.d(TAG, "permission is not granted");
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");

        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        createLocationRequest();

        //checkLocationSettings();

        enableMyLocationLayer();
    }

    //Example: https://developers.google.com/android/reference/com/google/android/gms/location/SettingsApi
    //PendingResult: https://developers.google.com/android/reference/com/google/android/gms/common/api/PendingResult#public-methods
    private void checkLocationSettings() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                Status status = locationSettingsResult.getStatus();
                //LocationSettingsStates locationSettingsStates = locationSettingsResult.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location requests here.
                        Log.d(TAG, "SUCCESS settings");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                        Log.d(TAG, "RESOLUTION_REQUIRED settings");
                        try {
                            status.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the settings so we won't show the dialog.
                        Log.d(TAG, "SETTINGS_CHANGE_UNAVAILABLE settings");
                        break;
                }
            }
        });
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000 * 10);
        mLocationRequest.setFastestInterval(1000 * 5);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
                //setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocationis not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(
                        LAST_UPDATED_TIME_STRING_KEY);
            }
            //updateUI();
            Log.d(TAG, "mRequestingLocationUpdates: " + mRequestingLocationUpdates + "\nmCurrentLocation: " + mCurrentLocation.toString()
                + "\nmLastUpdateTime: " + mLastUpdateTime.toString());
        }
    }

    //For requestLocationUpdates
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "*** location updated ***");
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        //updateUI ---> lat, long, mLastUpdateTime
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void addNavigateButtonListener() {

        mNavigateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GeoApiContext context = new GeoApiContext().setApiKey(GOOGLE_MAPS_SERVER_API_KEY);
                if (mCurrentLocation == null)
                    return;
                DirectionsApiRequest request = DirectionsApi.newRequest(context)
                        .origin(new com.google.maps.model.LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                        .destination(new com.google.maps.model.LatLng(65.0115702,25.4669972))
                        .mode(TravelMode.WALKING);

                //Async request
                //Same as the link: by default mode is driving, so we need to set it to walking or bicycling
                //<a href="https://maps.googleapis.com/maps/api/directions/json?origin=Erkki%20Koiso-Kanttilan%20katu%203,%2090570%20Oulu,%20Finland&destination=Saaristonkatu%206,%2090100%20Oulu,%20Finland&mode=walking&key=AIzaSyAGFZeAMuorbEdxNitNHt7l1PIbsvveQ4I">
                request.setCallback(new com.google.maps.PendingResult.Callback<DirectionsResult>() {
                    @Override
                    public void onResult(DirectionsResult result) {
                        Log.d(TAG, "Request successfully");
                        DirectionsRoute[] routes = result.routes;
                        //There maybe many routes
                        for (DirectionsRoute route: routes) {
                            //There maybe many legs in one route
                            for(DirectionsLeg leg: route.legs) {
                                //Same for start address and start location
                                Log.d(TAG, "Distance: " + leg.distance.humanReadable
                                    + " Duration: " + leg.duration.humanReadable
                                    + " End address: " + leg.endAddress
                                    + " End location: " + leg.endLocation.toString());
                                for (DirectionsStep step: leg.steps) {
                                    //step.distance.humanReadable
                                    //step.duration.humanReadable
                                    //step.endLocation.toString(), step.startLocation.toString()

                                    //step.polyline
                                    //Note: cannot toast here, no idea why
                                    Log.d(TAG, "HTML instructions: " + step.htmlInstructions + "\n");
                                    Log.d(TAG, String.valueOf(step.polyline.decodePath().size()));

                                    /* How to draw polylines:
                                    1. Get decoded path of a list of latlng from step
                                    2. Add this list to draw a poly line
                                    Codes:
                                    List<com.google.maps.model.LatLng> listOfLatlng = step.polyline.decodePath();
                                    mMap.addPolyline(new PolylineOptions().addAll((Iterable) listOfLatlng));
                                    */

                                }
                            }
                            Log.d(TAG, route.legs[0].distance.humanReadable);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        Log.d(TAG, "Request directions api error");
                    }
                });


            }
        });
    }
}
