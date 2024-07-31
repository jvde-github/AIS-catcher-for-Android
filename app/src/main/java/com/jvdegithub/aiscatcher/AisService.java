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

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class AisService extends Service {

    PowerManager.WakeLock wakeLock;

    public interface ServiceCallback {
        void onClose();
    }

    private void sendBroadcast (){
        Intent intent = new Intent ("message"); //put the same message as in the filter you used in the activity when registering the receiver
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (AisService.class.getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }

        return false;
    }

    private Notification buildNotification(String msg) {
        Notification.Builder notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final String CHANNELID = "Foreground Service AIS-catcher";
            NotificationChannel channel = new NotificationChannel(CHANNELID, CHANNELID,
                    NotificationManager.IMPORTANCE_HIGH);

            getSystemService(NotificationManager.class).createNotificationChannel(channel);
            notification = new Notification.Builder(this, CHANNELID);
        } else {
            notification = new Notification.Builder(this);
        }

        Intent notificationIntent = new Intent(this.getApplicationContext(), MainActivity.class);
        int f = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            f = PendingIntent.FLAG_MUTABLE;
        PendingIntent contentIntent = PendingIntent.getActivity(this.getApplicationContext(), 0, notificationIntent, f);

        notification.setContentIntent(contentIntent)
                .setContentText(msg)
                .setContentTitle("AIS-catcher")
                .setSmallIcon(R.drawable.ic_notif_launcher);

        return notification.build();
    }


    public void acquireLocks() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AIS-catcher:WakeLock");
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        }
    }

    public void releaseLocks() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent != null) {

            int source = (int) intent.getExtras().get("source");
            int fd = (int) intent.getExtras().get("USB");
            int cgfwide = (int) intent.getExtras().get("CGFWIDE");
            int modeltype = (int) intent.getExtras().get("MODELTYPE");
            int FPDS = (int) intent.getExtras().get("FPDS");

            int r = AisCatcherJava.createReceiver(source, fd, cgfwide, modeltype, FPDS);

            if (r == 0) {
                String msg = "Receiver running - " + DeviceManager.getDeviceTypeDescription() + " @ " + AisCatcherJava.getRateDescription();
                startForeground(1001, buildNotification(msg));

                new Thread(() -> {
                    try {
                        acquireLocks();
                        AisCatcherJava.Run();
                        AisCatcherJava.Close();
                        sendBroadcast();

                    } finally {
                        releaseLocks();

                        stopForeground(true);
                        stopSelf();
                    }
                }).start();
            } else {
                String msg = "Receiver creation failed";
                startForeground(1001, buildNotification(msg));

                stopForeground(true);
                stopSelf();
                sendBroadcast();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}