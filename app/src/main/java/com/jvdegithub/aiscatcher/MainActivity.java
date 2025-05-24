/*
 *     AIS-catcher for Android
 *     Copyright (C)  2022-2023 jvde.github@gmail.com.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.jvdegithub.aiscatcher;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.webkit.WebViewCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jvdegithub.aiscatcher.databinding.ActivityMainBinding;
import com.jvdegithub.aiscatcher.tools.LogBook;
import com.jvdegithub.aiscatcher.ui.main.StatisticsFragment;
import com.jvdegithub.aiscatcher.ui.main.WebViewMapFragment;

import java.io.IOException;
import java.net.ServerSocket;

public class MainActivity<binding> extends AppCompatActivity implements AisCatcherJava.AisCallback, DeviceManager.DeviceCallback {

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private boolean firstRun = true;

    private LogBook logbook;
    private LocationHelper locationHelper;
    public static int port = 0;
    static {

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            port = serverSocket.getLocalPort();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.loadLibrary("AIScatcherNDK");
        AisCatcherJava.Init(port);
    }

    boolean legacyVersion = true;

    private StatisticsFragment stat_fragment;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* ask for permission to post notifications if needed ..... */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS}, 101);
            }
        }

        com.jvdegithub.aiscatcher.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        setDarkMode(true);

        logbook = LogBook.getInstance();

        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);

        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        legacyVersion = shouldUseLegacyMode();

        Fragment fragment;

        if (legacyVersion) {
            stat_fragment = new StatisticsFragment();
            fragment = stat_fragment;
        } else {
            fragment = new WebViewMapFragment();
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();

        if(Settings.setDefaultOnFirst(this)) {
            onOpening();
        }

        locationHelper = new LocationHelper(this);
        locationHelper.requestLocationUpdates();

        bottomNavigationView = binding.bottombar;
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_play:
                    onPlayStop();
                    return true;
                case R.id.action_clear:
                    onClear();
                    return true;
                case R.id.action_source:
                    onSource();
                    return true;
                case R.id.action_web:
                    onWeb();
                    return true;
            }
            return false;
        });

        DeviceManager.Init(this);

        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("sFORCEDARK"))
                    setDarkMode(false);
                else if (key.equals("sKEEPSCREENON"))
                    applyKeepScreenOnSetting();
            }
        };

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        }

        // ugly to have the callback in mainactivity, to be cleaned up
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == locationHelper.PERMISSION_REQUEST_CODE) {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    locationHelper.requestLocationUpdates();
                }
            }
        }

        @Override
        protected void onDestroy () {
            super.onDestroy();
            locationHelper.removeLocationUpdates();
            PreferenceManager.getDefaultSharedPreferences(this)
                    .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);

        }

        public void setDarkMode(boolean only_force) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean sForceDark = sharedPreferences.getBoolean("sFORCEDARK", false);
            if(!only_force || sForceDark)
                AppCompatDelegate.setDefaultNightMode(sForceDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        private void onWeb () {

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://aiscatcher.org"));
            startActivity(browserIntent);
        }

        protected void AutoStart() {
            if(!Settings.getAutoStart(this)) return;
            if(DeviceManager.getDeviceType() != DeviceManager.DeviceType.RTLSDR &&
                    DeviceManager.getDeviceType() != DeviceManager.DeviceType.AIRSPY &&
                    DeviceManager.getDeviceType() != DeviceManager.DeviceType.AIRSPYHF)
                return;

            if (!AisService.isRunning(getApplicationContext())) {
                onPlayStop();
            }
        }
        protected void onResume () {

            super.onResume();

            applyKeepScreenOnSetting();
            updateUIonSource();
            if (AisService.isRunning(getApplicationContext())) {
                updateUIwithStart();
            } else {
                updateUIwithStop();
            }

            if(firstRun) {
                firstRun = false;
                AutoStart();
            }
        }

        private void applyKeepScreenOnSetting() {
            boolean keepScreenOn = Settings.getKeepScreenOn(this);
            if (keepScreenOn) {
                getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }

        @Override
        protected void onStart () {
            super.onStart();

            AisCatcherJava.registerCallback(this);
            DeviceManager.register(this);
            LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, new IntentFilter("message"));
        }

        @Override
        protected void onStop () {
            super.onStop();

            AisCatcherJava.unregisterCallback();
            DeviceManager.unregister();
            LocalBroadcastManager.getInstance(this).unregisterReceiver(bReceiver);
        }

        private void onPlayStop () {
            if (!AisService.isRunning(getApplicationContext())) {
                if (Settings.Apply(this)) {
                    int fd = DeviceManager.openDevice();
                    if (fd != -1) {
                        Intent serviceIntent = new Intent(MainActivity.this, AisService.class);
                        serviceIntent.putExtra("source", DeviceManager.getDeviceCode());
                        serviceIntent.putExtra("USB", fd);
                        serviceIntent.putExtra("CGFWIDE", Settings.getCGFSetting(this));
                        serviceIntent.putExtra("MODELTYPE", Settings.getModelType(this));
                        serviceIntent.putExtra("FPDS", Settings.getFixedPointDownsampling(this) ? 1 : 0);
                        serviceIntent.putExtra("USB", fd);
                        ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
                        updateUIwithStart();
                    } else
                        Toast.makeText(MainActivity.this, "Cannot open USB device. Give permission first and try again.", Toast.LENGTH_LONG).show();
                } else
                    Toast.makeText(MainActivity.this, "Invalid setting", Toast.LENGTH_LONG).show();

            } else {
                AisCatcherJava.forceStop();
            }
        }

        private BroadcastReceiver bReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                onAisServiceClosing();
            }
        };

        @Override
        public boolean onOptionsItemSelected (MenuItem item){
            switch (item.getItemId()) {
                case R.id.action_settings:
                    Intent myIntent = new Intent(MainActivity.this, Settings.class);
                    MainActivity.this.startActivity(myIntent);
                    return true;
                case R.id.action_default:
                    Settings.setDefault(this);
                    return true;
                case R.id.action_credit:
                    onCredit();
                    return true;
                case R.id.action_abouts:
                    onAbout();
                    return true;
                case R.id.action_logs:
                    onLogs();
                    return true;
            }
            return super.onOptionsItemSelected(item);

        }
        private void onCredit () {

            Spanned html = Html.fromHtml((String) getText(R.string.license_text));
            showDialog(html, "Licenses");
        }

        private void onLogs () {

            Spanned html = Html.fromHtml(logbook.getLogAsString());
            showDialog(html, "Log");
        }

        private void onAbout () {
            String disclaimerText = getString(R.string.disclaimer_text);
            String androidVersion = String.format(Build.VERSION.RELEASE);
            String versionName = String.valueOf(BuildConfig.VERSION_NAME);
            String versionCode = String.valueOf(BuildConfig.VERSION_CODE);
            String libraryVersion = AisCatcherJava.getLibraryVersion();
            String appID = String.valueOf(BuildConfig.APPLICATION_ID);
            String debug = String.valueOf(BuildConfig.BUILD_TYPE);
            WebView webView = new WebView(this);
            String userAgentString = webView.getSettings().getUserAgentString();

            String formattedHtml = String.format(disclaimerText,versionName,versionCode,
                libraryVersion,appID,debug,androidVersion, userAgentString);
            Spanned html = Html.fromHtml(formattedHtml);

            showDialog(html, "About");
        }

        private void onOpening () {
            onAbout();
        }

        private void showDialog (@Nullable CharSequence msg, String title)
        {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.90);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(title);
            builder.setMessage(msg);
            builder.setCancelable(true);
            builder.setPositiveButton("OK", null);
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            alertDialog.getWindow().setLayout(width, height); //Controlling width and height.
        }
        private void onClear () {
            AisCatcherJava.Reset();
        }

        private void onSource () {

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            String[] devs = DeviceManager.getDeviceStrings();

            builder.setTitle("Select Device");
            builder.setItems(devs, (dialog, select) -> DeviceManager.setDevice(select));
            builder.show();
        }

        private void updateUIwithStart () {
            MenuItem item = bottomNavigationView.getMenu().findItem(R.id.action_play);

            item.setIcon(R.drawable.ic_baseline_stop_circle_40);
            item.setTitle("Stop");
            bottomNavigationView.getMenu().findItem(R.id.action_source).setEnabled(false);
            Settings.setEnabled(false);
        }

        private void updateUIwithStop () {
            MenuItem item = bottomNavigationView.getMenu().findItem(R.id.action_play);
            item.setIcon(R.drawable.ic_baseline_play_circle_filled_40);
            item.setTitle("Start");
            bottomNavigationView.getMenu().findItem(R.id.action_source).setEnabled(true);
            Settings.setEnabled(true);
        }

        private void updateUIonSource () {
            MenuItem item = bottomNavigationView.getMenu().findItem(R.id.action_source);
            item.setTitle(DeviceManager.getDeviceTypeDescription());
        }

        @Override
        public boolean onCreateOptionsMenu (Menu menu){

            getMenuInflater().inflate(R.menu.toolbar_menu, menu);
            return true;
        }

        @Override
        public void onConsole ( final String line){
            Log.d("AIS-catcher library",line);
            logbook.addLog(line);
        }

        @Override
        public void onNMEA ( final String line){
            //nmea_fragment.Update(line);
        }

        @Override
        public void onMessage ( final String line){
            //map_fragment.Update(line);
        }

        @Override
        public void onError ( final String line){
            runOnUiThread(() -> Toast.makeText(MainActivity.this, line, Toast.LENGTH_LONG).show());
        }

        public void onAisServiceClosing () {
            DeviceManager.closeDevice();
            runOnUiThread(this::updateUIwithStop);
        }

        @Override
        public void onUpdate () {
            if (legacyVersion)
                stat_fragment.Update();
        }

        @Override
        public void onSourceChange () {
            updateUIonSource();
        }


    private boolean shouldUseLegacyMode() {
            // Original API level check
            int currentApiVersion = android.os.Build.VERSION.SDK_INT;
            if (currentApiVersion < Build.VERSION_CODES.Q) {
                return true;
            }

            return false;
        }
    }