package com.sabbir.smartbatterylogger;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private Button btnStartService, btnStopService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize buttons
        btnStartService = findViewById(R.id.btnStartService);
        btnStopService = findViewById(R.id.btnStopService);

        // Set click listeners
        btnStartService.setOnClickListener(v -> {
            startBatteryService();
            startActivity(new Intent(this, DashboardActivity.class));
        });

        btnStopService.setOnClickListener(v -> stopBatteryService());
    }

    private void startBatteryService() {
        Intent serviceIntent = new Intent(this, BatteryLoggerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopBatteryService() {
        stopService(new Intent(this, BatteryLoggerService.class));
    }
}