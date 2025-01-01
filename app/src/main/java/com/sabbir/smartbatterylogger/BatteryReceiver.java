package com.sabbir.smartbatterylogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import com.sabbir.smartbatterylogger.model.BatteryLog;
import com.sabbir.smartbatterylogger.utils.FileManager;

public abstract class BatteryReceiver extends BroadcastReceiver {
    private FileManager fileManager;
    private int lastLevel = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (fileManager == null) {
            fileManager = new FileManager(context);
        }

        if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

            // Only log if battery level has changed
            if (level != lastLevel) {
                lastLevel = level;
                float temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f;
                int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);

                BatteryLog log = new BatteryLog(
                        System.currentTimeMillis(),
                        level,
                        temperature,
                        voltage,
                        getBatteryStatusString(status),
                        getBatteryHealthString(health)
                );

                fileManager.saveLog(log);
            }
        }
    }

    private String getBatteryStatusString(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "Discharging";
            case BatteryManager.BATTERY_STATUS_FULL: return "Full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "Not Charging";
            default: return "Unknown";
        }
    }

    private String getBatteryHealthString(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Overheat";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Over Voltage";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "Failed";
            default: return "Unknown";
        }
    }


    public abstract void onBatteryInfoReceived(BatteryLog batteryLog);
}


