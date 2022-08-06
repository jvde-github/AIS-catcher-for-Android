package com.jvdegithub.aiscatcher.ui.main;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.jvdegithub.aiscatcher.Logs;
import com.jvdegithub.aiscatcher.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment {

    private TextView logview;
    private ScrollView scrollView;

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootview = inflater.inflate(R.layout.fragment_map, container, false);
        logview = rootview.findViewById(R.id.log);
        scrollView = rootview.findViewById(R.id.scrollView);

        return rootview;
    }

    public void Update(String str) {
        if (logview != null && scrollView != null) {

            getActivity().runOnUiThread(() -> {
                logview.setText(str);
                scrollView.fullScroll(View.FOCUS_DOWN);
            });
        }
    }
}