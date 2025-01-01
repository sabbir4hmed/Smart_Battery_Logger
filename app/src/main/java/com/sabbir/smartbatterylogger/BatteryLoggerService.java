package com.sabbir.smartbatterylogger;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.sabbir.smartbatterylogger.model.BatteryLog;

public class BatteryLoggerService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "battery_logger_channel";
    private BatteryReceiver batteryReceiver;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();  // Fixed: Changed from createNotification() to createNotificationChannel()
        notificationManager = getSystemService(NotificationManager.class);
        batteryReceiver = new BatteryReceiver() {
            @Override
            public void onBatteryInfoReceived(BatteryLog batteryLog)  {
                updateNotification(batteryLog);
            }
        };
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Battery Logger Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void updateNotification(BatteryLog batteryLog) {
        Notification notification = createNotification(batteryLog);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        BatteryLog initialBatteryLog = new BatteryLog(
                System.currentTimeMillis(), // timestamp
                0,                         // level
                0.0f,                      // temperature
                0,                         // voltage
                "Unknown",                 // status
                "Unknown"                  // health
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(initialBatteryLog),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, createNotification(initialBatteryLog));
        }
        return START_STICKY;
    }

    private Notification createNotification(BatteryLog batteryLog) {
        // Create an intent that opens DashboardActivity
        Intent notificationIntent = new Intent(this, DashboardActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Create status bar text
        @SuppressLint("DefaultLocale") String statusBarText = String.format("%d%% | %.1f°C", batteryLog.getLevel(), batteryLog.getTemperature());

        // Create expanded notification text
        @SuppressLint("DefaultLocale") String expandedText = String.format("Battery Level: %d%%\nTemperature: %.1f°C\nVoltage: %dmV\nStatus: %s\nHealth: %s", batteryLog.getLevel(), batteryLog.getTemperature(), batteryLog.getVoltage(), batteryLog.getStatus(), batteryLog.getHealth());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Battery Monitor Running")
                .setContentText(statusBarText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(expandedText))
                .setSmallIcon(R.drawable.ic_battery)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        return builder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryReceiver);
        stopForeground(true);
    }
}