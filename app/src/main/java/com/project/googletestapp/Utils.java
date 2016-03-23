package com.project.googletestapp;


import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yzuo on 22.3.2016.
 */
public class Utils {
    public static LatLng MapsLatLngToAndroidLatLng(com.google.maps.model.LatLng latLng) {
        return new LatLng(latLng.lat, latLng.lng);
    }

    public static List<LatLng> ListOfMapsLatLngToAndroidLatLng(List<com.google.maps.model.LatLng> latLngList) {
        List returnLatLngList = new ArrayList<LatLng>();

        for (com.google.maps.model.LatLng latLng: latLngList) {
            returnLatLngList.add(new LatLng(latLng.lat, latLng.lng));
        }
        return returnLatLngList;
    }

    public static com.google.maps.model.LatLng LocationToMapsLatLng(Location location) {
        return new com.google.maps.model.LatLng(location.getLatitude(), location.getLongitude());
    }

    public static LatLng LocationToAndroidLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }
}
