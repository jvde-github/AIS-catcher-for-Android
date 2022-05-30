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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

import com.jvdegithub.aiscatcher.tools.InputFilterIP;
import com.jvdegithub.aiscatcher.tools.InputFilterMinMax;

import java.util.Objects;

public class Settings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    static void setDefault(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString("rRATE", "288K").commit();
        preferences.edit().putBoolean("rRTLAGC", false).commit();
        preferences.edit().putString("rTUNER", "Auto").commit();
        preferences.edit().putBoolean("rBIASTEE", false).commit();
        preferences.edit().putString("rFREQOFFSET", "0").commit();

        preferences.edit().putString("tRATE", "240K").commit();
        preferences.edit().putString("tTUNER", "Auto").commit();
        preferences.edit().putString("tHOST", "192.168.1.233").commit();
        preferences.edit().putString("tPORT", "12345").commit();

        preferences.edit().putBoolean("u1SWITCH", true).commit();
        preferences.edit().putString("u1HOST", "127.0.0.1").commit();
        preferences.edit().putString("u1PORT", "10110").commit();

        preferences.edit().putBoolean("u2SWITCH", false).commit();
        preferences.edit().putString("u2HOST", "192.168.1.239").commit();
        preferences.edit().putString("u2PORT", "4002").commit();

        preferences.edit().putInt("mLINEARITY", 17).commit();
        preferences.edit().putString("mRATE", "2500K").commit();
        preferences.edit().putBoolean("mBIASTEE", false).commit();

        preferences.edit().putString("hRATE", "192K").commit();
    }

    static boolean setDefaultOnFirst(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean pref_set = preferences.getBoolean("pref_set", false);
        if (!pref_set) setDefault(context);
        preferences.edit().putBoolean("pref_set", true).commit();
        return !pref_set;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements
            SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.preferences, rootKey);

            ((EditTextPreference) getPreferenceManager().findPreference("tPORT")).setOnBindEditTextListener(validatePort);
            ((EditTextPreference) getPreferenceManager().findPreference("rFREQOFFSET")).setOnBindEditTextListener(validatePPM);
            ((EditTextPreference) getPreferenceManager().findPreference("tHOST")).setOnBindEditTextListener(validateIP);
            ((EditTextPreference) getPreferenceManager().findPreference("u1HOST")).setOnBindEditTextListener(validateIP);
            ((EditTextPreference) getPreferenceManager().findPreference("u2HOST")).setOnBindEditTextListener(validateIP);
            ((EditTextPreference) getPreferenceManager().findPreference("u1PORT")).setOnBindEditTextListener(validatePort);
            ((EditTextPreference) getPreferenceManager().findPreference("u2PORT")).setOnBindEditTextListener(validatePort);
            ((SeekBarPreference) getPreferenceManager().findPreference("mLINEARITY")).setUpdatesContinuously(true);

            setSummaries();
        }

        private void setSummaries() {
            setSummaryText(new String[]{"tPORT","tHOST","u1HOST","u1PORT","u2HOST","u2PORT", "rFREQOFFSET"});
            setSummaryList(new String[]{"rTUNER","rRATE","tRATE","tTUNER","mRATE","hRATE"});
            setSummarySeekbar(new String[]{"mLINEARITY"});
        }

        private void setSummaryText(String[] settings) {

            for (String s : settings) {
                EditTextPreference e = findPreference(s);
                e.setSummary(e.getText());
            }
        }

        private void setSummaryList(String[] settings) {
            for (String s : settings) {
                ListPreference e = findPreference(s);
                e.setSummary(e.getEntry());
            }
        }

        private void setSummarySeekbar(String[] settings) {
            for(String s:settings) {
                SeekBarPreference e = findPreference(s);
                e.setSummary(String.valueOf(e.getValue()));
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            setSummaries();
        }

        EditTextPreference.OnBindEditTextListener validatePPM = editText -> {
            editText.selectAll();
            editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            editText.setFilters(new InputFilter[]{new InputFilterMinMax(-150,150)});
        };

        EditTextPreference.OnBindEditTextListener validatePort = editText -> {
            editText.selectAll();
            editText.setInputType(InputType.TYPE_CLASS_NUMBER );
            editText.setFilters(new InputFilter[]{new InputFilterMinMax(0,65536)});
        };

        EditTextPreference.OnBindEditTextListener validateInteger = editText -> {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            editText.selectAll();
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
        };

        EditTextPreference.OnBindEditTextListener validateIP = editText -> {
            editText.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
            editText.selectAll();
            editText.setFilters(new InputFilter[]{new InputFilterIP()});
        };
    }

    static public boolean Apply(Context context) {

        if (!SetDevice(new String[]{"rRATE", "rTUNER", "rFREQOFFSET", "tRATE", "tTUNER", "tHOST", "tPORT", "mRATE", "hRATE"}, context))
            return false;
        if (!SetDeviceBoolean(new String[]{"rRTLAGC", "rBIASTEE", "mBIASTEE"}, "ON", "OFF", context))
            return false;
        if (!SetDeviceInteger(new String[]{"mLINEARITY"}, context)) return false;

        if (!SetUDPoutput("u1", context)) return false;
        if (!SetUDPoutput("u2", context)) return false;
        return true;
    }

    static private boolean SetDevice(String[] settings, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        for (String s : settings) {
            String p = preferences.getString(s, "");
            if (Objects.equals(p, "")) return false;
            if (AisCatcherJava.applySetting(s.substring(0, 1), s.substring(1), p) != 0)
                return false;
        }
        return true;
    }

    static private boolean SetDeviceBoolean(String[] settings, String st, String sf, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        for (String s : settings) {
            boolean b = preferences.getBoolean(s, true);
            if (AisCatcherJava.applySetting(s.substring(0, 1), s.substring(1), b ? st : sf) != 0)
                return false;
        }
        return true;
    }

    static private boolean SetDeviceInteger(String[] settings, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        for (String s : settings) {
            String p = String.valueOf(preferences.getInt(s, 0));
            if (Objects.equals(p, "")) return false;
            if (AisCatcherJava.applySetting(s.substring(0, 1), s.substring(1), p) != 0)
                return false;
        }
        return true;
    }

    static private boolean SetUDPoutput(String s, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

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