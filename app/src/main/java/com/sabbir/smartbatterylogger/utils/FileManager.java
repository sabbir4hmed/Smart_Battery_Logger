package com.sabbir.smartbatterylogger.utils;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.sabbir.smartbatterylogger.model.BatteryLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileManager {
    private static final String LOG_FILE = "battery_logs.txt";
    private static final String CSV_DIRECTORY = "BatteryLogs";
    private Context context;

    public FileManager(Context context) {
        this.context = context;
        initializeLogFile();
    }

    private void initializeLogFile() {
        try {
            File file = new File(context.getFilesDir(), LOG_FILE);
            if (!file.exists()) {
                FileOutputStream fos = context.openFileOutput(LOG_FILE, Context.MODE_PRIVATE);
                fos.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error initializing log file", e);
        }
    }

    public void saveLog(BatteryLog log) {
        try {
            FileOutputStream fos = context.openFileOutput(LOG_FILE, Context.MODE_APPEND);
            String logLine = String.format(Locale.US, "%d,%d,%.1f,%d,%s,%s\n",
                    log.getTimestamp(),
                    log.getLevel(),
                    log.getTemperature(),
                    log.getVoltage(),
                    log.getStatus(),
                    log.getHealth());
            fos.write(logLine.getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Error saving log", e);
        }
    }

    public List<BatteryLog> readLogs() {
        List<BatteryLog> logs = new ArrayList<>();
        File file = new File(context.getFilesDir(), LOG_FILE);

        if (!file.exists()) {
            return logs;
        }

        try {
            FileInputStream fis = context.openFileInput(LOG_FILE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 6) {
                    logs.add(new BatteryLog(
                            Long.parseLong(parts[0]),
                            Integer.parseInt(parts[1]),
                            Float.parseFloat(parts[2]),
                            Integer.parseInt(parts[3]),
                            parts[4],
                            parts[5]
                    ));
                }
            }
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Error reading logs", e);
        }
        return logs;
    }

    public void clearLogs() {
        context.deleteFile(LOG_FILE);
        initializeLogFile(); // Create a fresh empty file
    }

    public boolean exportToCSV(File exportFile) {
        try {
            FileWriter writer = new FileWriter(exportFile);

            // Write device information header
            writer.append("Device Model: ").append(Build.MODEL).append("\n");
            writer.append("Build Number: ").append(Build.DISPLAY).append("\n");
            writer.append("Android Version: ").append(Build.VERSION.RELEASE).append("\n\n");

            // Write CSV headers
            writer.append("Date,Time,Battery Level,Temperature(°C),Voltage(mV),Status,Health\n");

            // Get and format all logs
            List<BatteryLog> logs = readLogs();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

            for (BatteryLog log : logs) {
                Date logDate = new Date(log.getTimestamp());
                String date = dateFormat.format(logDate);
                String time = timeFormat.format(logDate);

                writer.append(String.format(Locale.US, "%s,%s,%d,%.1f,%d,%s,%s\n",
                        date,
                        time,
                        log.getLevel(),
                        log.getTemperature(),
                        log.getVoltage(),
                        log.getStatus(),
                        log.getHealth()));
            }

            writer.flush();
            writer.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error exporting to CSV", e);
            return false;
        }
    }
}