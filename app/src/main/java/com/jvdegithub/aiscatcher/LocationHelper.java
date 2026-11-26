package com.jvdegithub.aiscatcher;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.GnssStatus;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class LocationHelper implements LocationListener {

    public static final int PERMISSION_REQUEST_CODE = 1;
    private Context context;
    private LocationManager locationManager;
    private boolean isUpdatingLocation = false;
    private OnNmeaMessageListener nmeaListener = null;
    private GnssStatus.Callback statusCallback = null;
    private HandlerThread gnssThread = null;
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
            addNmeaListener();
            if (isUpdatingLocation) return;
            boolean hasGps = locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER);
            // also while it is off: the updates start when it is switched on
            if (hasGps) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30 * 1000, 0, this);
                isUpdatingLocation = true;
            }
            if (!(hasGps && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30 * 1000, 0, this);
                isUpdatingLocation = true;

                Toast.makeText(context, "GPS not available. Using network for location updates.", Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        Log.d("Location Update", String.format("New Location Received: (Latitude: %s, Longitude: %s)",
                location.getLatitude(), location.getLongitude()));
        AisCatcherJava.setLatLon((float) location.getLatitude(), (float) location.getLongitude());
    }


    private void addNmeaListener() {
        if (nmeaListener != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        nmeaListener = (message, timestamp) -> AisCatcherJava.feedGPS(message);
        statusCallback = new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                final int n = status.getSatelliteCount();
                int[] constellation = new int[n], svid = new int[n];
                float[] cn0 = new float[n], elevation = new float[n], azimuth = new float[n], carrier = new float[n];
                boolean[] used = new boolean[n];
                for (int i = 0; i < n; i++) {
                    constellation[i] = status.getConstellationType(i);
                    svid[i] = status.getSvid(i);
                    cn0[i] = status.getCn0DbHz(i);
                    elevation[i] = status.getElevationDegrees(i);
                    azimuth[i] = status.getAzimuthDegrees(i);
                    used[i] = status.usedInFix(i);
                    carrier[i] = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && status.hasCarrierFrequencyHz(i) ? status.getCarrierFrequencyHz(i) : 0;
                }
                AisCatcherJava.feedSky(constellation, svid, cn0, elevation, azimuth, used, carrier);
            }
        };
        // off the UI thread: the native side takes the tracker's locks
        gnssThread = new HandlerThread("gnss");
        gnssThread.start();
        Handler handler = new Handler(gnssThread.getLooper());
        try {
            locationManager.addNmeaListener(nmeaListener, handler);
            locationManager.registerGnssStatusCallback(statusCallback, handler);
        } catch (SecurityException e) {
            removeNmeaListener();
        }
    }

    private void removeNmeaListener() {
        if (nmeaListener == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        locationManager.removeNmeaListener(nmeaListener);
        locationManager.unregisterGnssStatusCallback(statusCallback);
        gnssThread.quitSafely();
        nmeaListener = null;
        statusCallback = null;
        gnssThread = null;
    }

    public void removeLocationUpdates() {
        removeNmeaListener();
        if (isUpdatingLocation) {
            locationManager.removeUpdates(this);
            isUpdatingLocation = false;
        }
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    // the GPS switched on: from the network's positions to its own
    @Override
    public void onProviderEnabled(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider) && isUpdatingLocation) {
            locationManager.removeUpdates(this);
            isUpdatingLocation = false;
            requestLocationUpdates();
        }
    }

    @Override
    public void onProviderDisabled(String provider) { }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                requestLocationUpdates();
            } else {
            }
        }
    }
}