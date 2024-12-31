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
            e.printStackTrace();
        }
    }

    public List<BatteryLog> readLogs() {
        List<BatteryLog> logs = new ArrayList<>();
        try {
            FileInputStream fis = context.openFileInput(LOG_FILE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                logs.add(new BatteryLog(
                        Long.parseLong(parts[0]),
                        Integer.parseInt(parts[1]),
                        Float.parseFloat(parts[2]),
                        Integer.parseInt(parts[3]),
                        parts[4],
                        parts[5]
                ));
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public boolean exportToCSV(File exportFile) {
        try {
            FileWriter writer = new FileWriter(exportFile);
            writer.append("Date,Time,Battery Level,Temperature,Voltage,Status,Health\n");

            List<BatteryLog> logs = readLogs();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss", Locale.US);

            for (BatteryLog log : logs) {
                String date = dateFormat.format(new Date(log.getTimestamp()));
                writer.append(String.format(Locale.US, "%s,%d,%.1f,%d,%s,%s\n",
                        date,
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

    public void clearLogs() {
        context.deleteFile(LOG_FILE);
    }
}
