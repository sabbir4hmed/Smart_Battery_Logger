package com.sabbir.smartbatterylogger;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.sabbir.smartbatterylogger.adapter.BatteryLogAdapter;
import com.sabbir.smartbatterylogger.model.BatteryLog;
import com.sabbir.smartbatterylogger.utils.FileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {
    private FileManager fileManager;
    private BatteryLogAdapter adapter;
    private LineChart chart;
    private Handler handler;
    private TextView tvCurrentLevel, tvCurrentTemp, tvCurrentVoltage, tvCurrentHealth;
    private static final int UPDATE_INTERVAL = 60000; // 1 minute

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        fileManager = new FileManager(this);
        initializeViews();
        setupRecyclerView();
        setupChart();
        setupExportButton();

        handler = new Handler();
        startPeriodicUpdate();
    }

    private void initializeViews() {
        tvCurrentLevel = findViewById(R.id.tvCurrentLevel);
        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvCurrentVoltage = findViewById(R.id.tvCurrentVoltage);
        tvCurrentHealth = findViewById(R.id.tvCurrentHealth);
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
        levelDataSet.setColor(Color.BLUE);
        levelDataSet.setCircleColor(Color.BLUE);

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

    private void updateCurrentStatus() {
        List<BatteryLog> logs = fileManager.readLogs();
        if (!logs.isEmpty()) {
            BatteryLog latest = logs.get(logs.size() - 1);
            tvCurrentLevel.setText(String.format("Battery Level: %d%%", latest.getLevel()));
            tvCurrentTemp.setText(String.format("Temperature: %.1f°C", latest.getTemperature()));
            tvCurrentVoltage.setText(String.format("Voltage: %dmV", latest.getVoltage()));
            tvCurrentHealth.setText(String.format("Health: %s", latest.getHealth()));
        }
    }

    private void setupExportButton() {
        Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(v -> exportData());
    }

    private void exportData() {
        File exportFile = fileManager.exportToCSV();
        if (exportFile != null) {
            Toast.makeText(this, "Data exported to: " + exportFile.getPath(),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void startPeriodicUpdate() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateData();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        }, UPDATE_INTERVAL);
    }

    private void updateData() {
        List<BatteryLog> logs = fileManager.readLogs();
        adapter.updateLogs(logs);
        updateChart();
        updateCurrentStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}