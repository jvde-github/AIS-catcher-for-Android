package com.jvdegithub.aiscatcher;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class LocationHelper implements LocationListener {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private Context context;
    private LocationManager locationManager;
    private boolean isUpdatingLocation = false;
    public LocationHelper(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5 * 60 * 1000, 0, this);
            isUpdatingLocation = true;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Location Update", String.format("New Location Received: (Latitude: %s, Longitude: %s)",
                location.getLatitude(), location.getLongitude()));
        AisCatcherJava.setLatLon((float) location.getLatitude(), (float) location.getLongitude());
    }

    public void removeLocationUpdates() {
        if (isUpdatingLocation) {
            locationManager.removeUpdates(this);
            isUpdatingLocation = false;
        }
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                requestLocationUpdates();
            } else {
                // no permission
            }
        }
    }
}