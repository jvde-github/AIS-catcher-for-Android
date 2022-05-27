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

public class AisCatcherJava {

    public interface AisCallback {

        void onNMEA(final String line);

        void onConsole(final String line);

        void onError(final String line);

        void onClose();

        void onUpdate();
    }

    private static AisCallback callback = null;

    static native int InitNative();

    static native int createReceiver(int source, int FD);

    static native int Run();

    static native int Close();

    static native int forceStop();

    static native boolean isStreaming();

    static native int applySetting(String dev, String Setting, String Param);

    static native int createUDP(String h, String p);

    static native int getSampleRate();

    public static class Statistics {
        private static int Data = 0;
        private static int Total = 0;
        private static int ChA = 0;
        private static int ChB = 0;
        private static int Msg123 = 0;
        private static int Msg5 = 0;
        private static int Msg1819 = 0;
        private static int Msg24 = 0;
        private static int MsgOther = 0;

        public static int getData() {
            return Data;
        }

        public static int getTotal() {
            return Total;
        }

        public static int getChA() {
            return ChA;
        }

        public static int getChB() {
            return ChB;
        }

        public static int getMsg123() {
            return Msg123;
        }

        public static int getMsg5() {
            return Msg5;
        }

        public static int getMsg1819() {
            return Msg1819;
        }

        public static int getMsg24() {
            return Msg24;
        }

        public static int getMsgOther() {
            return MsgOther;
        }

        private static native void Init();

        static native void Reset();
    }

    public static void Init() {
        InitNative();
        Statistics.Init();
    }

    public static void registerCallback(AisCallback m) {
        callback = m;
    }

    public static void unregisterCallback() {
        callback = null;
    }

    public static void Reset() {

        Logs.Clear();
        Statistics.Reset();
    }

    private static void onNMEA(String nmea) {

        Logs.Nmea.Update(nmea);
        if (callback != null)
            callback.onNMEA(nmea);
    }

    public static void onStatus(String str) {

        Logs.Status.Update(str);
        if (callback != null)
            callback.onConsole(str);
    }

    public static void onError(String str) {

        if (callback != null)
            callback.onError(str);
    }

    public static void onClose() {

        if (callback != null)
            callback.onClose();
    }

    public static void onUpdate() {

        if (callback != null)
            callback.onUpdate();
    }

}

