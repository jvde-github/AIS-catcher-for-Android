package com.jvdegithub.aiscatcher.tools;

import java.util.ArrayList;
import java.util.List;

public class LogBook {
    private static LogBook instance;
    private List<String> logs;
    private static final int MAX_LOGS = 50;

    private LogBook() {
        logs = new ArrayList<>();
    }

    public static LogBook getInstance() {
        if (instance == null) {
            instance = new LogBook();
        }
        return instance;
    }

    public void addLog(String log) {
        if (logs.size() >= MAX_LOGS) {
            logs.remove(0);
        }
        logs.add(log);
        notifyLogUpdate(log);
    }

    public List<String> getLogs() {
        return logs;
    }

    public String getLogAsString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String log : logs) {
            stringBuilder.append(log).append("<br/>");        }
        return stringBuilder.toString();
    }

    public interface LogUpdateListener {
        void onLogUpdated(String log);
    }

    private LogUpdateListener logUpdateListener;

    public void setLogUpdateListener(LogUpdateListener listener) {
        logUpdateListener = listener;
    }

    private void notifyLogUpdate(String log) {
        if (logUpdateListener != null) {
            logUpdateListener.onLogUpdated(log);
        }
    }

}