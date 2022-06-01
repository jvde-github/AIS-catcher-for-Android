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

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class AisService extends Service {

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
        PendingIntent contentIntent = PendingIntent.getActivity(this.getApplicationContext(), 0, notificationIntent, 0);

        notification.setContentIntent(contentIntent)
                .setContentText(msg)
                .setContentTitle("AIS-catcher")
                .setSmallIcon(R.drawable.ic_notif_launcher);

        return notification.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int source = (int) intent.getExtras().get("source");
        int fd = (int) intent.getExtras().get("USB");

        int r = AisCatcherJava.createReceiver(source, fd);

        if(r == 0)
        {
            String msg = "Receiver running - " + DeviceManager.getDeviceType() + " @ " + AisCatcherJava.getSampleRate() / 1000 + "K";
            startForeground(1001, buildNotification(msg));

            new Thread(
                    () -> {

                        AisCatcherJava.Run();
                        AisCatcherJava.Close();

                        stopForeground(true);
                        stopSelf();
                        sendBroadcast();
                    }).start();
        }
        else
        {
            String msg = "Receiver creation failed";
            startForeground(1001, buildNotification(msg));

            stopForeground(true);
            stopSelf();
            sendBroadcast();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}