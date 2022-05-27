package com.jvdegithub.aiscatcher;

public class Logs {
    public static final TextLog Nmea = new TextLog();
    public static final TextLog Status = new TextLog();

    public static void Clear() {
        Nmea.Clear();
        Status.Clear();
    }


    public static String getNmea() {
        return Logs.Nmea.getText();
    }

    public static String getStatus() {
        return Logs.Status.getText();
    }
}
