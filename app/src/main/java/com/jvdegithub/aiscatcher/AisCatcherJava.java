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

    private static AisCallback callback = null;

    private static final TextLog NmeaLog = new TextLog();
    private static final TextLog ConsoleLog = new TextLog();

    private static int Statistics_Data = 0;
    private static int Statistics_Total = 0;
    private static int Statistics_ChA = 0;
    private static int Statistics_ChB = 0;
    private static int Statistics_Msg123 = 0;
    private static int Statistics_Msg5 = 0;
    private static int Statistics_Msg1819 = 0;
    private static int Statistics_Msg24 = 0;
    private static int Statistics_MsgOther = 0;

    static native int Init();

    static native int createReceiver(int source, int FD);

    static native int Run();

    static native int Close();

    static native int forceStop();

    static native boolean isStreaming();

    static native int applySetting(String dev, String Setting, String Param);

    static native int createUDP(String h, String p);

    static native void resetStatistics();

    static native int getSampleRate();


    public static void registerCallback(AisCallback m) {
        callback = m;
    }

    public static void unregisterCallback() {
        callback = null;
    }

    public static String getNmeaText() {
        return NmeaLog.getText();
    }

    public static String getConsoleText() {
        return ConsoleLog.getText();
    }

    public static int getStatistics_Data() {
        return Statistics_Data;
    }

    public static int getStatistics_Total() {
        return Statistics_Total;
    }

    public static int getStatistics_ChA() {
        return Statistics_ChA;
    }

    public static int getStatistics_ChB() {
        return Statistics_ChB;
    }

    public static int getStatistics_Msg123() {
        return Statistics_Msg123;
    }

    public static int getStatistics_Msg5() {
        return Statistics_Msg5;
    }

    public static int getStatistics_Msg1819() {
        return Statistics_Msg1819;
    }

    public static int getStatistics_Msg24() {
        return Statistics_Msg24;
    }

    public static int getStatistics_MsgOther() {
        return Statistics_MsgOther;
    }


    public interface AisCallback {

        void onNMEA(final String line);

        void onConsole(final String line);

        void onError(final String line);

        void onClose();

        void onUpdate();

        void onSourceChange();
    }

    public static void Reset() {

        ConsoleLog.Clear();
        NmeaLog.Clear();

        resetStatistics();
    }

    private static void callbackNMEA(String nmea) {

        NmeaLog.Update(nmea);
        if (callback != null)
            callback.onNMEA(nmea);
    }

    public static void callbackConsole(String str) {

        ConsoleLog.Update(str);
        if (callback != null)
            callback.onConsole(str);
    }

    public static void callbackError(String str) {

        if (callback != null)
            callback.onError(str);
    }

    public static void callbackClose() {

        if (callback != null)
            callback.onClose();
    }

    public static void callbackUpdate() {

        if (callback != null)
            callback.onUpdate();
    }

    public static void callbackSourceChanged() {

        if (callback != null)
            callback.onSourceChange();
    }
}

