package com.project.googletestapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
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
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.Geometry;
import com.google.maps.model.TravelMode;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    //Camera zoom level
    private static final int ZOOM_LEVEL = 15;

    private Button mNavigateButtonWalking;
    private Button mNavigationButtonCycling;

    private PlaceAutocompleteFragment mSourceAutocomplete;
    private PlaceAutocompleteFragment mDestinationAutocomplete;

    private LatLng mSourceLatLng = null;
    private LatLng mDestinationLatLng = null;

    //It should be linked hash map, since we can compare each step with current location in an order, so that display proper HTML instruction (from source to destination)
    private LinkedHashMap<List<LatLng>, String> mInstructionsMap = new LinkedHashMap<>();

    private Marker mDestinationMarker = null;

    private Toast mInstructionToast = null;

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

        initializeViews();

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initializeListeners();
    }

    private void initializeViews() {
        mNavigateButtonWalking = (Button) findViewById(R.id.navigateButtonWalking);
        mNavigationButtonCycling = (Button) findViewById(R.id.navigateButtonCycling);

        mSourceAutocomplete = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment_source);
        mDestinationAutocomplete = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment_destination);

        mInstructionToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG);
        mInstructionToast.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT, 0, 0);
    }

    private void initializeListeners() {
        addNavigateButtonListener();
        addAutoCompleteListeners();
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

        //Once connected, move camera to current location if possible
        if (mLastLocation != null && mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())));
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
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(Utils.LocationToAndroidLatLng(mLastLocation)));
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
        mLocationRequest.setInterval(1000 * 5);
        mLocationRequest.setFastestInterval(1000 * 3);
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

        Toast.makeText(getApplicationContext(), "location update", Toast.LENGTH_SHORT).show();

        //TODO: Check current location and display html instruction as an overlay until switch to another step
        for (Map.Entry<List<LatLng>, String> entry: mInstructionsMap.entrySet()) {
            List<LatLng> step = entry.getKey();
            String instruction = entry.getValue();

            Log.d(TAG, "sss");

            //If current location is on a path of the whole navigation route
            if ( PolyUtil.isLocationOnPath(Utils.LocationToAndroidLatLng(mCurrentLocation), step, false) ) {
                //Update instruction to user
                mInstructionToast.setText(instruction);
                mInstructionToast.show();
                break;
            }
        }

        //TODO: End navigation with notification, maybe need some tolerance
        if (Utils.LocationToAndroidLatLng(mCurrentLocation) == mDestinationLatLng) {
            mInstructionToast.setText("Done navigation");
            mInstructionToast.show();

            //TODO: some cleaning
        }
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

    private void navigationButtonOnClickEvent(boolean isCycling) {
        if (mSourceLatLng == null || mDestinationLatLng == null) {
            Toast.makeText(getApplicationContext(), "Please select source and destination!", Toast.LENGTH_SHORT).show();
            return;
        }

        //TODO: Remove old polylines and destination marker, polyline-instruction map
        mMap.clear();
        mInstructionsMap.clear();
        mInstructionToast.cancel();

        //Add destination marker
        mDestinationMarker = mMap.addMarker(new MarkerOptions()
                .position(mDestinationLatLng)
                .title("Your destination"));

        //Zoom camera to source location. Running on UI thread so that the zoom process won't be interrupted
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mSourceLatLng, ZOOM_LEVEL));
            }
        });

        //Draw polyline based on source and destination
        GeoApiContext context = new GeoApiContext().setApiKey(GOOGLE_MAPS_SERVER_API_KEY);
        DirectionsApiRequest request = DirectionsApi.newRequest(context)
                .origin(new com.google.maps.model.LatLng(mSourceLatLng.latitude, mSourceLatLng.longitude))
                .destination(new com.google.maps.model.LatLng(mDestinationLatLng.latitude, mDestinationLatLng.longitude))
                .mode((isCycling) ? TravelMode.BICYCLING : TravelMode.WALKING);
        request.setCallback(new com.google.maps.PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.d(TAG, "Request successfully");
                DirectionsRoute[] routes = result.routes;
                //There maybe many routes
                for (DirectionsRoute route : routes) {
                    //There maybe many legs in one route
                    for (DirectionsLeg leg : route.legs) {
                        //Same for start address and start location
                        Log.d(TAG, "Distance: " + leg.distance.humanReadable
                                + " Duration: " + leg.duration.humanReadable
                                + " End address: " + leg.endAddress
                                + " End location: " + leg.endLocation.toString());

                        for (final DirectionsStep step : leg.steps) {
                            //Note: cannot toast here, no idea why
                            Log.d(TAG, "HTML instructions: " + step.htmlInstructions + "\n");
                            Log.d(TAG, String.valueOf(step.polyline.decodePath().size()));

                            //Note: Error would error if drawing poly lines not on the main thread
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //Draw polyline
                                    List<LatLng> listOfLatlng = Utils.ListOfMapsLatLngToAndroidLatLng(step.polyline.decodePath());
                                    //By default geodesic is false
                                    mMap.addPolyline(new PolylineOptions()
                                            .addAll(listOfLatlng)
                                            .width(5)
                                            .color(Color.BLUE));

                                    //TODO: Save polyline in combination with html instruction
                                    mInstructionsMap.put(listOfLatlng, step.htmlInstructions);
                                }
                            });
                        }
                    }
                    Log.d(TAG, route.legs[0].distance.humanReadable);
                }
            }

            @Override
            public void onFailure(Throwable e) {
                Log.d(TAG, "Request directions api error");
                e.printStackTrace();
            }
        });
    }

    private void addNavigateButtonListener() {
        mNavigationButtonCycling.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationButtonOnClickEvent(true);
            }
        });

        mNavigateButtonWalking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationButtonOnClickEvent(false);
            }
        });
    }

    //place_id is a unique identifier that can be used with other Google APIs.
    //For example, you can use the place_id from a Google Place Autocomplete response to calculate directions to a local business
    private void addAutoCompleteListeners() {
        mSourceAutocomplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: " + place.getName());

                String placeDetailsStr = place.getName() + "\n"
                        + place.getId() + "\n"
                        + place.getLatLng().toString() + "\n"
                        + place.getAddress() + "\n"
                        + place.getAttributions();

                Log.d(TAG, placeDetailsStr);

                mSourceLatLng = place.getLatLng();

                //TODO: fix issue that onLocationChanged callback is not called when using autocomplete fragments
                startLocationUpdates();
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        mDestinationAutocomplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i(TAG, "Place: " + place.getName());

                String placeDetailsStr = place.getName() + "\n"
                        + place.getId() + "\n"
                        + place.getLatLng().toString() + "\n"
                        + place.getAddress() + "\n"
                        + place.getAttributions();

                mDestinationLatLng = place.getLatLng();

                //TODO: fix issue that onLocationChanged callback is not called when using autocomplete fragments
                startLocationUpdates();
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }
}
