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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.jvdegithub.aiscatcher.databinding.ActivityMainBinding;
import com.jvdegithub.aiscatcher.ui.main.ConsoleLogFragment;
import com.jvdegithub.aiscatcher.ui.main.MapFragment;
import com.jvdegithub.aiscatcher.ui.main.NMEALogFragment;
import com.jvdegithub.aiscatcher.ui.main.SectionsPagerAdapter;
import com.jvdegithub.aiscatcher.ui.main.StatisticsFragment;

public class MainActivity<binding> extends AppCompatActivity implements AisCatcherJava.AisCallback, DeviceManager.DeviceCallback {

    static {
        System.loadLibrary("AIScatcherNDK");
        AisCatcherJava.Init();
    }

    private ConsoleLogFragment log_fragment;
    private NMEALogFragment nmea_fragment;
    //private MapFragment map_fragment;
    private StatisticsFragment stat_fragment;
    private BottomNavigationView bottomNavigationView;

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
        //map_fragment = (MapFragment) sectionsPagerAdapter.instantiateItem(viewPager, 3);
        sectionsPagerAdapter.finishUpdate(viewPager);

        if(Settings.setDefaultOnFirst(this)) {
            onOpening();
        }

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
                case R.id.action_webclient:
                    onWeb();
                    return true;
            }
            return false;
        });

        DeviceManager.Init(this);

    }

    protected void onResume() {

        super.onResume();

        updateUIonSource();
        if (AisService.isRunning(getApplicationContext())) {
            updateUIwithStart();
        } else {
            updateUIwithStop();
        }

        log_fragment.Update("");
        nmea_fragment.Update("");

    }

    @Override
    protected void onStart() {
        super.onStart();

        AisCatcherJava.registerCallback(this);
        DeviceManager.register(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, new IntentFilter("message"));
    }

    @Override
    protected void onStop() {
        super.onStop();

        AisCatcherJava.unregisterCallback();
        DeviceManager.unregister();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bReceiver);
    }

    private void onWeb() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://localhost:8100"));
        startActivity(browserIntent);
    }

    private void onPlayStop() {
        if (!AisService.isRunning(getApplicationContext())) {
            if (Settings.Apply(this)) {
                int fd = DeviceManager.openDevice();
                if(fd!=-1) {
                    Intent serviceIntent = new Intent(MainActivity.this, AisService.class);
                    serviceIntent.putExtra("source", DeviceManager.getDeviceCode());
                    serviceIntent.putExtra("USB", fd);
                    serviceIntent.putExtra("CGFWIDE", Settings.getCGFSetting(this));
                    serviceIntent.putExtra("MODELTYPE", Settings.getModelType(this));
                    serviceIntent.putExtra("FPDS", Settings.getFixedPointDownsampling(this)?1:0);
                    serviceIntent.putExtra("SERVER_PORT", Settings.getServerPort(this));
                    serviceIntent.putExtra("SERVER", Settings.getServerSwitch(this)?1:0);

                    serviceIntent.putExtra("USB", fd);
                    ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
                    updateUIwithStart();
                }
                else
                    Toast.makeText(MainActivity.this, "Cannot open USB device. Give permission first and try again.", Toast.LENGTH_LONG).show();
            } else
                Toast.makeText(MainActivity.this, "Invalid setting", Toast.LENGTH_LONG).show();

        } else {
            AisCatcherJava.forceStop();
        }
    }

    private BroadcastReceiver bReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            onAisServiceClosing();
        }
    };

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
    private void onCredit() {

        Spanned html = Html.fromHtml((String)getText(R.string.license_text));
        showDialog(html,"Licenses");
    }

    private void onAbout() {
        Spanned html = Html.fromHtml((String)getText(R.string.disclaimer_text));
        showDialog(html,"About");
    }

    private void onOpening() {
        Spanned html = Html.fromHtml((String)getText(R.string.disclaimer_text));
        showDialog(html,"Welcome!");
    }

    private void showDialog(@Nullable CharSequence msg, String title)
    {
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.90);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setCancelable(true);
        builder.setPositiveButton("OK",null);
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

    private void updateUIwithStart() {
        MenuItem item = bottomNavigationView.getMenu().findItem(R.id.action_play);
        item.setIcon(R.drawable.ic_baseline_stop_circle_40);
        item.setTitle("Stop");

        bottomNavigationView.getMenu().findItem(R.id.action_source).setEnabled(false);
    }

    private void updateUIwithStop() {
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
    public void onMessage(final String line) {
        //map_fragment.Update(line);
    }

    @Override
    public void onError(final String line) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, line, Toast.LENGTH_LONG).show());
    }

    public void onAisServiceClosing() {
        DeviceManager.closeDevice();
        runOnUiThread(this::updateUIwithStop);
    }

    @Override
    public void onUpdate() {
        stat_fragment.Update();
    }

    @Override
    public void onSourceChange() {
        updateUIonSource();
    }
}