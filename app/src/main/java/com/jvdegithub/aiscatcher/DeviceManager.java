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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Xml;

import androidx.core.util.Pair;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashSet;


public class DeviceManager {

    static Context context = null;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    enum DeviceType {NONE, RTLTCP, RTLSDR, AIRSPY, AIRSPYHF, HACKRF, SPYSERVER }

    public interface DeviceCallback {

        void onSourceChange();
    }

    private static DeviceManager.DeviceCallback callback = null;

    static class Device {

        private final UsbDevice device;
        private final int UID;
        private final String description;
        private final DeviceType type;

        Device(UsbDevice d, String e, DeviceType t, int u) {
            device = d;
            description = e;
            type = t;
            UID = u;
        }

        String getDescription() {
            return description;
        }

        int getUID() {
            return UID;
        }

        UsbDevice getDevice() {
            return device;
        }

        DeviceType getType() {
            return type;
        }
    }

    private static final ArrayList<Device> devices = new ArrayList<>();

    static int deviceIndex = 0;
    static int deviceUID = 0;
    static DeviceType deviceType = DeviceType.NONE;

    static UsbDeviceConnection usbDeviceConnection = null;

    public static void register(DeviceCallback cb) {
        callback = cb;
        registerUSBBroadCast();
    }

    public static void unregister() {
        unregisterUSBBroadCast();
        callback = null;
    }

    public static void onSourceChanged() {

        if (callback != null)
            callback.onSourceChange();
    }

    public static int openDevice() {

        int fd = 0;

        AisCatcherJava.onStatus("Opening Device Connection\n");

        if (devices.get(deviceIndex).type != DeviceType.RTLTCP && devices.get(deviceIndex).type != DeviceType.SPYSERVER) {

            try {
                UsbManager mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

                if(mUsbManager.hasPermission(devices.get(deviceIndex).getDevice())) {
                    UsbDeviceConnection conn = mUsbManager.openDevice(devices.get(deviceIndex).getDevice());
                    fd = conn.getFileDescriptor();
                    AisCatcherJava.onStatus("Device SN: " + conn.getSerial() + ", FD: " + fd + "\n");
                }
                else
                {
                    AisCatcherJava.onStatus("No permission to USB device\n");
                    int f = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        f = PendingIntent.FLAG_MUTABLE;

                    PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), f);
                    mUsbManager.requestPermission(devices.get(deviceIndex).getDevice(),permissionIntent);
                    AisCatcherJava.onStatus("Permission requested\n");
                    return -1;

                }
            } catch (Exception e){
                AisCatcherJava.onStatus(e.toString());
                return -1;
            }
        }
        return fd;
    }

    public static DeviceType getDeviceType() {
        return deviceType;
    }

    public static int getDeviceCode() {
        switch (deviceType) {
            case RTLTCP:
                return 0;
            case RTLSDR:
                return 1;
            case AIRSPY:
                return 2;
            case AIRSPYHF:
                return 3;
            case SPYSERVER:
                return 4;
        }
        return 0;
    }

    public static void closeDevice() {

        AisCatcherJava.onStatus("Closing connection\n");

        if (devices.get(deviceIndex).getType() != DeviceType.RTLTCP && devices.get(deviceIndex).getType() != DeviceType.SPYSERVER && usbDeviceConnection != null) {
            usbDeviceConnection.close();
        }
        usbDeviceConnection = null;
    }

    public static void Init(Context m) {
        context = m;
        refreshList(false);
    }

    private static HashSet<Pair<Integer, Integer>> getSupportedDevices() {
        HashSet<Pair<Integer, Integer>> pairSet = new HashSet<>();
        try {
            final XmlResourceParser xml = context.getResources().getXml(R.xml.usb_device_filter);

            xml.next();
            int eventType;
            while ((eventType = xml.getEventType()) != XmlPullParser.END_DOCUMENT) {

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (xml.getName().equals("usb-device")) {
                            final AttributeSet as = Xml.asAttributeSet(xml);
                            final Integer vendorId = Integer.valueOf( as.getAttributeValue(null, "vendor-id"));
                            final Integer productId = Integer.valueOf( as.getAttributeValue(null, "product-id"));
                            pairSet.add(new Pair<>(vendorId, productId));
                        }
                        break;
                }
                xml.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return pairSet;
    }

    private static boolean refreshList(boolean add) {

        devices.clear();

        devices.add(new Device(null, "SpyServer", DeviceType.SPYSERVER, 0));
        devices.add(new Device(null, "RTL-TCP", DeviceType.RTLTCP, 0));

        final HashSet<Pair<Integer, Integer>> supported = getSupportedDevices();
        UsbManager mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        for (UsbDevice device : mUsbManager.getDeviceList().values()) {

            if (supported.contains(new Pair<>(device.getVendorId(), device.getProductId()))) {

                Device dev;
                if (device.getVendorId() == 7504 && device.getProductId() == 24737)
                    dev = new Device(device, "Airspy", DeviceType.AIRSPY, device.getDeviceId());
                else if (device.getVendorId() == 7504 && device.getProductId() == 24713)
                    dev = new Device(device, "HackRF", DeviceType.HACKRF, device.getDeviceId());
                else if (device.getVendorId() == 1003 && device.getProductId() == 32780)
                    dev = new Device(device, "Airspy HF+", DeviceType.AIRSPYHF, device.getDeviceId());
                else
                    dev = new Device(device, "RTL-SDR", DeviceType.RTLSDR, device.getDeviceId());

                devices.add(dev);
            }
            else
            {
                AisCatcherJava.onStatus("Warning: not supported USB devices connected - VID: " + device.getVendorId() + " PID " + device.getProductId()  + "\n");
            }
        }

        int nDev = devices.size();
        int select = nDev - 1;
        boolean changed = true;

        if (!add && (deviceType != DeviceType.RTLTCP && deviceType != DeviceType.SPYSERVER))
        //if (!(add && deviceType == DeviceType.RTLTCP && deviceType == DeviceType.SPYSERVER))
            for (int i = 0; i < devices.size(); i++)
                if (devices.get(i).getType() == deviceType && devices.get(i).getUID() == deviceUID) {
                    select = i;
                    changed = false;
                }

        setDevice(select);

        if (changed) onSourceChanged();
        return changed;
    }

    public static void setDevice(int select) {
        if(select>=devices.size() || select < 0)
            select = devices.size()-1;

        deviceIndex = select;
        Device dev = devices.get(select);
        deviceType = dev.getType();
        deviceUID = dev.getUID();

        AisCatcherJava.setDeviceDescription(dev.getDescription(),"","");

        onSourceChanged();
    }

    public static String getDeviceTypeString() {
        switch (getDeviceType()) {
            case RTLTCP:
                return "RTLTCP";
            case RTLSDR:
                return "RTLSDR";
            case AIRSPY:
                return "AIRSPY";
            case AIRSPYHF:
                return "AIRSPYHF";
            case HACKRF:
                return "HACKRF";
            case SPYSERVER:
                return "SPYSERVER";
        }
        return "NONE";
    }

    public static String[] getDeviceStrings() {

        refreshList(false);

        String[] devs = new String[devices.size()];
        int idx = 0;

        for (Device dev : devices) {

            devs[idx] = (idx + 1) + ": " + dev.description;
            idx++;
        }

        return devs;
    }

    public static void registerUSBBroadCast() {

        IntentFilter filter = new IntentFilter();

        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(ACTION_USB_PERMISSION);

        context.registerReceiver(mUsbReceiver, filter);
    }

    public static void unregisterUSBBroadCast() {
        context.unregisterReceiver(mUsbReceiver);
    }

    static BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            boolean add = false;

            String action = intent.getAction();
            String action_clean = "";

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                action_clean = "USB device connected";
                add = true;
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                action_clean = "USB device disconnected";
            } else if (ACTION_USB_PERMISSION.equals(action)) {
                action_clean = "USB device granted extra permission. Try to start again.";
            }

            AisCatcherJava.onStatus("Android: " + action_clean + ".\n");
            refreshList(add);
        }
    };
}
