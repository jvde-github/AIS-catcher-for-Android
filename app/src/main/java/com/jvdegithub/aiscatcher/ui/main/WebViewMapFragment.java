package com.jvdegithub.aiscatcher.ui.main;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.jvdegithub.aiscatcher.MainActivity;
import com.jvdegithub.aiscatcher.R;
import com.jvdegithub.aiscatcher.tools.LogBook;

public class WebViewMapFragment extends Fragment {

    private WebView webView;
    private LogBook logbook;

    public static WebViewMapFragment newInstance() {
        return new WebViewMapFragment();
    }

    private boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        logbook = LogBook.getInstance();

        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        webView = rootView.findViewById(R.id.webmap);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        if(!isOnline()) {
            RelativeLayout offlineBar = rootView.findViewById(R.id.offline_bar);
            offlineBar.setVisibility(View.VISIBLE);
            logbook.addLog("Device is offline.");
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                webView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                webView.setVisibility(View.VISIBLE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                logbook.addLog(String.format("W(%d): %s",
                        consoleMessage.lineNumber(), consoleMessage.message() ));

                return true;
            }
        });


        webView.loadUrl("http://localhost:" + MainActivity.port + "?welcome=false&android=true");

        return rootView;
    }
}