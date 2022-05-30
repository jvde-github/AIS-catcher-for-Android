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

package com.jvdegithub.aiscatcher.ui.main;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.jvdegithub.aiscatcher.Logs;
import com.jvdegithub.aiscatcher.R;

public class ConsoleLogFragment extends Fragment {

    private TextView logview;
    private ScrollView scrollView;

    public static ConsoleLogFragment newInstance() {
        return new ConsoleLogFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootview = inflater.inflate(R.layout.fragment_log, container, false);
        logview = rootview.findViewById(R.id.log);
        scrollView = rootview.findViewById(R.id.scrollView);
        logview.setText(Logs.getStatus());

        rootview.findViewById(R.id.copylog).setOnClickListener(v -> {

            ClipboardManager clipboard = ContextCompat.getSystemService(getContext(), ClipboardManager.class);
            ClipData clip = ClipData.newPlainText("aiscatcher", logview.getText().toString());
            clipboard.setPrimaryClip(clip);
        });

        return rootview;
    }

    public void Update(String str) {
        if (logview != null && scrollView != null) {

            getActivity().runOnUiThread(() -> {
                logview.setText(Logs.getStatus());
                scrollView.fullScroll(View.FOCUS_DOWN);
            });
        }
    }
}