package com.sabbir.smartbatterylogger;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.BuildConfig;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupVersionAndCopyright();

        new Handler().postDelayed(() -> {
            startActivity(new Intent(MainActivity.this, DashboardActivity.class));
            finish();
        }, SPLASH_DURATION);
    }

    private void setupVersionAndCopyright() {
        TextView versionText = findViewById(R.id.versionName);
        TextView copyrightText = findViewById(R.id.developerName);

        // Get version from package info
        String versionName;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "1.1.2"; // Fallback to default version
        }

        versionText.setText("Version " + versionName);

        // Set copyright with current year
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String copyright = String.format("© %d SABBIR AHMED. All rights reserved.", currentYear);
        copyrightText.setText(copyright);
    }
}