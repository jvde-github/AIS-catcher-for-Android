/*
 *     AIS-catcher for Android
 *     Copyright (C)  2022 jvde.github@gmail.com.
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

import com.jvdegithub.aiscatcher.databinding.ActivityMainBinding;
import com.jvdegithub.aiscatcher.ui.main.ConsoleLogFragment;
import com.jvdegithub.aiscatcher.ui.main.NMEALogFragment;
import com.jvdegithub.aiscatcher.ui.main.StatisticsFragment;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.jvdegithub.aiscatcher.ui.main.SectionsPagerAdapter;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class MainActivity<binding> extends AppCompatActivity implements AisCatcherJava.AisCallback, DeviceManager.DeviceCallback {

    private ConsoleLogFragment log_fragment;
    private NMEALogFragment nmea_fragment;
    private StatisticsFragment stat_fragment;
    private BottomNavigationView bottomNavigationView;

    static {
        System.loadLibrary("AIScatcherNDK");
        AisCatcherJava.Init();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.jvdegithub.aiscatcher.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        ViewPager viewPager = binding.viewPager;
        TabLayout tabs = binding.tabs;
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

        viewPager.setAdapter(sectionsPagerAdapter);
        tabs.setupWithViewPager(viewPager);

        sectionsPagerAdapter.startUpdate(viewPager);
        stat_fragment = (StatisticsFragment) sectionsPagerAdapter.instantiateItem(viewPager, 0);
        log_fragment = (ConsoleLogFragment) sectionsPagerAdapter.instantiateItem(viewPager, 1);
        nmea_fragment = (NMEALogFragment) sectionsPagerAdapter.instantiateItem(viewPager, 2);
        sectionsPagerAdapter.finishUpdate(viewPager);

        Settings.setDefaultOnFirst(this);

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
            }
            return false;
        });

        DeviceManager.Init(this);
        updateUIonSource();

        if (AisService.isRunning(getApplicationContext())) {
            UpdateUIonStart();
        } else {
            UpdateUIonStop();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        AisCatcherJava.registerCallback(this);
        DeviceManager.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        AisCatcherJava.unregisterCallback();
        DeviceManager.unregister();
    }

    private void onPlayStop() {
        if (!AisService.isRunning(getApplicationContext())) {

            String valid_until = "15/07/2022";
            Date valDate = new SimpleDateFormat("dd/MM/yyyy").parse(valid_until, new ParsePosition(0));

            if (new Date().after(valDate)) {

                Toast.makeText(MainActivity.this, "Test version valid until " + valid_until + ". Please download official version.", Toast.LENGTH_LONG).show();

            } else {
                if (ApplySettings()) {
                    int fd = DeviceManager.openDevice();
                    Intent serviceIntent = new Intent(MainActivity.this, AisService.class);
                    serviceIntent.putExtra("source", DeviceManager.getDeviceCode());
                    serviceIntent.putExtra("USB", fd);
                    ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
                    UpdateUIonStart();
                } else
                    Toast.makeText(MainActivity.this, "Invalid setting", Toast.LENGTH_LONG).show();
            }
        } else {
            AisCatcherJava.forceStop();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
        }
        return super.onOptionsItemSelected(item);

    }

    static final String LICENSES = "AIS-catcher for Android - GPL license\n\n" +
            "libusb 1.0.26 - LGPL-2.1 license\n" +
            "https://github.com/libusb/libusb\n\n" +
            "rtl-sdr -  GPL-2.0 license\n" +
            "https://github.com/osmocom/rtl-sdr\n" +
            "As modified: https://github.com/jvde-github/rtl-sdr\n\n" +
            "AIS-catcher v0.25 - MIT license\n" +
            "https://github.com/jvde-github/AIS-catcher\n";

    private void onCredit() {

        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.90);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Credits");
        builder.setMessage(LICENSES);
        builder.setCancelable(true);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().setLayout(width, height); //Controlling width and height.
    }

    static final String ABOUT = "Purpose\n" +
            "The aim of AIS-catcher is to provide a platform to facilitate continuous improvement of receiver models. Any suggestions, observation or sharing of recordings for setups where the current models are struggling is highly appreciated! The algorithm behind the default receiver model was created by investigating signals and trying different ways to get a coherent model running whilst keeping it simple at the same time. If I have some more free time I will try to expand the documentation and implement some improvement ideas.\n" +
            "\n" +
            "Disclaimer\n" +
            "AIS-catcher is created for research and educational purposes under the MIT license. It is a hobby project and not tested and designed for reliability and correctness. You can play with the software but it is the user's responsibility to use it prudently. So, DO NOT rely upon this software in any way including for navigation and/or safety of life or property purposes. There are variations in the legislation concerning radio reception in the different administrations around the world. It is your responsibility to determine whether or not your local administration permits the reception and handling of AIS messages from ships. It is specifically forbidden to use this software for any illegal purpose whatsoever. This is hobby and research software for use only in those regions where such use is permitted.";

    private void onAbout() {

        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.90);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("About");
        builder.setMessage(ABOUT);
        builder.setCancelable(true);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getWindow().setLayout(width, height); //Controlling width and height.
    }

    private void onClear() {
        AisCatcherJava.Reset();
    }

    private void onSource() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        String[] devs = DeviceManager.getDeviceStrings();

        builder.setTitle("Select Device");
        builder.setItems(devs, (dialog, select) -> DeviceManager.setDevice(select));
        builder.show();
    }

    private void UpdateUIonStart() {
        MenuItem item = bottomNavigationView.getMenu().findItem(R.id.action_play);
        item.setIcon(R.drawable.ic_baseline_stop_circle_40);
        item.setTitle("Stop");

        bottomNavigationView.getMenu().findItem(R.id.action_source).setEnabled(false);
    }

    private void UpdateUIonStop() {
        MenuItem item = bottomNavigationView.getMenu().findItem(R.id.action_play);
        item.setIcon(R.drawable.ic_baseline_play_circle_filled_40);
        item.setTitle("Start");
        bottomNavigationView.getMenu().findItem(R.id.action_source).setEnabled(true);
    }

    private void updateUIonSource() {

        MenuItem item = bottomNavigationView.getMenu().findItem(R.id.action_source);
        item.setTitle(DeviceManager.getDeviceTypeString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }


    @Override
    public void onConsole(final String line) {
        log_fragment.Update(line);

    }

    @Override
    public void onNMEA(final String line) {
        nmea_fragment.Update(line);
    }

    @Override
    public void onError(final String line) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, line, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onClose() {
        DeviceManager.closeDevice();
        runOnUiThread(this::UpdateUIonStop);
    }

    @Override
    public void onUpdate() {
        stat_fragment.Update();
    }

    @Override
    public void onSourceChange() {
        updateUIonSource();
    }

    public boolean ApplySettings() {

        if (!SetDevice("rRATE")) return false;
        if (!SetDeviceBoolean("rRTLAGC", "ON", "OFF")) return false;
        if (!SetDevice("rTUNER")) return false;
        if (!SetDeviceBoolean("rBIASTEE", "ON", "OFF")) return false;
        if (!SetDevice("rFREQOFFSET")) return false;
        if (!SetDevice("tRATE")) return false;
        if (!SetDevice("tTUNER")) return false;
        if (!SetDevice("tHOST")) return false;
        if (!SetDevice("tPORT")) return false;
        if (!SetDevice("mRATE")) return false;
        if (!SetDeviceInteger("mLINEARITY")) return false;
        if (!SetDeviceBoolean("mBIASTEE", "ON", "OFF")) return false;
        if (!SetDevice("hRATE")) return false;

        if (!SetUDPoutput("u1")) return false;
        return SetUDPoutput("u2");
    }

    public boolean SetDevice(String s) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String p = preferences.getString(s, "");
        if (!Objects.equals(p, ""))
            return AisCatcherJava.applySetting(s.substring(0, 1), s.substring(1), p) == 0;

        return true;
    }

    public boolean SetDeviceBoolean(String s, String st, String sf) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean b = preferences.getBoolean(s, true);
        return AisCatcherJava.applySetting(s.substring(0, 1), s.substring(1), b ? st : sf) == 0;
    }

    public boolean SetDeviceInteger(String s) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String p = String.valueOf(preferences.getInt(s, 0));
        if (!Objects.equals(p, ""))
            return AisCatcherJava.applySetting(s.substring(0, 1), s.substring(1), p) == 0;

        return true;
    }

    public boolean SetUDPoutput(String s) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Log.i("SET UDP", s);

        boolean b = preferences.getBoolean(s + "SWITCH", true);
        if (b) {
            String host = preferences.getString(s + "HOST", "");
            String port = preferences.getString(s + "PORT", "");
            return AisCatcherJava.createUDP(host, port) == 0;

        }
        return true;
    }
}