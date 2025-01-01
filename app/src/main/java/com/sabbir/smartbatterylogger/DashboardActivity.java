package com.sabbir.smartbatterylogger;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.sabbir.smartbatterylogger.adapter.BatteryLogAdapter;
import com.sabbir.smartbatterylogger.model.BatteryLog;
import com.sabbir.smartbatterylogger.utils.FileManager;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {
    private FileManager fileManager;
    private BatteryLogAdapter adapter;
    private LineChart chart;
    private Handler handler;
    private TextView tvCurrentLevel, tvCurrentTemp, tvCurrentVoltage, tvCurrentHealth;
    private Button btnExport, btnStopService;
    private boolean isServiceStopped = false;
    private static final int UPDATE_INTERVAL = 60000; // 1 minute
    private static final int STATUS_UPDATE_INTERVAL = 1000; // 1 second
    private Handler statusHandler = new Handler();
    private Runnable statusUpdateRunnable;
    private static final int PERMISSION_REQUEST_CODE = 123;

    private CircularProgressBar circularProgressBar;
    private TextView tvProgressPercentage;

    private boolean isServiceRunning = false;

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);


        fileManager = new FileManager(this);
        initializeViews();
        setupRecyclerView();
        setupChart();
        setupButtons();
        checkServiceStatus();
        handler = new Handler();
        // Only start updates if service is running
        if (isServiceRunning) {
            startPeriodicUpdate();
            startStatusUpdates();
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start your service
                startService();
            } else {
                // Permission denied
                Toast.makeText(this, "Notification permission required for battery monitoring", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with export
                if (exportData()) {
                    exportChartImage();
                    clearAllData();
                    isServiceRunning = false;
                    btnExport.setEnabled(false);
                    btnStopService.setText("Start Service");
                }
            } else {
                Toast.makeText(this, "Storage permission needed to export data", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkServiceStatus() {
        isServiceRunning = isServiceRunning(BatteryLoggerService.class);
        if (isServiceRunning) {
            btnStopService.setText("Stop Service");
            btnExport.setEnabled(false);
            updateData();
        } else {
            btnStopService.setText("Start Service");
            // Enable export button if there's data to export
            List<BatteryLog> logs = fileManager.readLogs();
            btnExport.setEnabled(!logs.isEmpty());
            if (logs.isEmpty()) {
                clearAllData();
            }
        }
    }

    private boolean isServiceRunning(Class<BatteryLoggerService> batteryLoggerServiceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (batteryLoggerServiceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void initializeViews() {
        tvCurrentLevel = findViewById(R.id.tvCurrentLevel);
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvCurrentVoltage = findViewById(R.id.tvCurrentVoltage);
        tvCurrentHealth = findViewById(R.id.tvCurrentHealth);
        btnExport = findViewById(R.id.btnExport);
        btnStopService = findViewById(R.id.btnStopService);
        circularProgressBar = findViewById(R.id.circularProgressBar);
        tvProgressPercentage = findViewById(R.id.tvProgressPercentage);
    }

    private void setupButtons() {
        btnExport.setEnabled(false);

        btnStopService.setOnClickListener(v -> {
            if (isServiceRunning) {
                stopBatteryService();
                btnStopService.setText("Start Service");
                btnExport.setEnabled(true);
                isServiceRunning = false;
            } else {
                startService();
                btnStopService.setText("Stop Service");
                btnExport.setEnabled(false);
                isServiceRunning = true;
                // Immediately update data when service starts
                updateData();
                // Start periodic updates
                startPeriodicUpdate();
            }
        });

        btnExport.setOnClickListener(v -> {
            if (exportData()) {
                exportChartImage();
                clearAllData();
                isServiceRunning = false;
                btnExport.setEnabled(false);
                btnStopService.setText("Start Service");
            }
        });
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, BatteryLoggerService.class);
        startService(serviceIntent);

        // Add small delay to allow first battery reading
        new Handler().postDelayed(() -> {
            updateData();
            startPeriodicUpdate();
            startStatusUpdates();
        }, 1000); // 1-second delay

        Toast.makeText(this, "Battery logging service started", Toast.LENGTH_SHORT).show();
    }

    private void stopBatteryService() {
        Intent serviceIntent = new Intent(this, BatteryLoggerService.class);
        stopService(serviceIntent);
        isServiceRunning = false;
        // Enable export button if there's data to export
        List<BatteryLog> logs = fileManager.readLogs();
        btnExport.setEnabled(!logs.isEmpty());
        Toast.makeText(this, "Battery logging service stopped", Toast.LENGTH_SHORT).show();
    }

    private void clearAllData() {
        chart.clear();
        chart.invalidate();
        adapter.clearData();
      /*  tvCurrentLevel.setText("");
        tvCurrentTemp.setText("");
        tvCurrentVoltage.setText("");
        tvCurrentHealth.setText("");*/
        fileManager.clearLogs();
        //Toast.makeText(this, "Data exported and cleared successfully", Toast.LENGTH_SHORT).show();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BatteryLogAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setupChart() {
        chart = findViewById(R.id.chartBattery);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
    }

    private void updateChart() {
        List<BatteryLog> logs = fileManager.readLogs();
        ArrayList<Entry> levelEntries = new ArrayList<>();
        ArrayList<Entry> tempEntries = new ArrayList<>();

        for (int i = 0; i < logs.size(); i++) {
            BatteryLog log = logs.get(i);
            levelEntries.add(new Entry(i, log.getLevel()));
            tempEntries.add(new Entry(i, log.getTemperature()));
        }

        LineDataSet levelDataSet = new LineDataSet(levelEntries, "Battery Level (%)");
        levelDataSet.setColor(Color.GREEN);
        levelDataSet.setCircleColor(Color.GREEN);

        LineDataSet tempDataSet = new LineDataSet(tempEntries, "Temperature (°C)");
        tempDataSet.setColor(Color.RED);
        tempDataSet.setCircleColor(Color.RED);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(levelDataSet);
        dataSets.add(tempDataSet);

        LineData lineData = new LineData(dataSets);
        chart.setData(lineData);
        chart.invalidate();
    }

    private void exportChartImage() {
        chart.setDrawingCacheEnabled(true);
        Bitmap chartBitmap = chart.getDrawingCache();

        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_hhmmss", Locale.getDefault()).format(new Date());
            String fileName = "battery_chart_" + timestamp + ".png";
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File batteryLogsDir = new File(picturesDir, "BatteryLogs");
            if (!batteryLogsDir.exists()) {
                batteryLogsDir.mkdirs();
            }

            File imageFile = new File(batteryLogsDir, fileName);
            FileOutputStream fos = new FileOutputStream(imageFile);
            chartBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Toast.makeText(this, "Chart exported to Pictures/BatteryLogs/" + fileName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to export chart: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        chart.setDrawingCacheEnabled(false);
    }

    private boolean exportData() {
        // Check if we have permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission if we don't have it
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
            return false;
        } else {
            // We have permission, proceed with export
            return performExport();
        }
    }

    private boolean performExport() {
        String folderName = "Battery Logger";
        String deviceName = Build.MODEL.replaceAll("\\s+", "_");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = String.format("%s_%s.csv", deviceName, timestamp);

        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), folderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File exportFile = new File(folder, fileName);
        if (fileManager.exportToCSV(exportFile)) {
            Toast.makeText(this, "Data exported to Documents/" + folderName + "/" + fileName, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private void startStatusUpdates() {
        statusUpdateRunnable = () -> {
            updateCurrentStatus();
            statusHandler.postDelayed(statusUpdateRunnable, STATUS_UPDATE_INTERVAL);
        };
        statusHandler.post(statusUpdateRunnable);
    }

    private void updateCurrentStatus() {
        List<BatteryLog> logs = fileManager.readLogs();
        if (!logs.isEmpty()) {
            BatteryLog latest = logs.get(logs.size() - 1);
            int batteryLevel = latest.getLevel();
            tvCurrentLevel.setText(String.format("Battery Level: %d%%", latest.getLevel()));
            tvCurrentTemp.setText(String.format("Temperature: %.1f°C", latest.getTemperature()));
            tvCurrentVoltage.setText(String.format("Voltage: %dmV", latest.getVoltage()));
            tvCurrentHealth.setText(String.format("Health: %s", latest.getHealth()));

            circularProgressBar.setProgress(batteryLevel);
            tvProgressPercentage.setText(batteryLevel + "%");
        }
    }

    private void startPeriodicUpdate() {
        // Remove any existing callbacks first
        handler.removeCallbacksAndMessages(null);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isServiceRunning) {
                    updateData();
                    handler.postDelayed(this, UPDATE_INTERVAL);
                }
            }
        }, UPDATE_INTERVAL);
    }

    private void updateData() {
        List<BatteryLog> logs = fileManager.readLogs();
        adapter.updateLogs(logs);
        updateChart();
        updateCurrentStatus();

        // Force UI refresh
        if (chart != null) {
            chart.invalidate();
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateData();
        if (isServiceStopped) {
            btnExport.setEnabled(true);
            btnStopService.setEnabled(false);
        } else {
            btnExport.setEnabled(false);
            btnStopService.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        statusHandler.removeCallbacks(statusUpdateRunnable);
    }


    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                startActivityForResult(intent, PERMISSION_REQUEST_CODE);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int write = ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return write == PackageManager.PERMISSION_GRANTED;
        }
    }

}