package com.sabbir.smartbatterylogger.adapter;

//import static android.os.Build.VERSION_CODES.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sabbir.smartbatterylogger.R;
import com.sabbir.smartbatterylogger.model.BatteryLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BatteryLogAdapter extends RecyclerView.Adapter<BatteryLogAdapter.ViewHolder> {
    private List<BatteryLog> logs;
    private SimpleDateFormat dateFormat;

    public BatteryLogAdapter() {
        this.logs = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.US);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_battery_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BatteryLog log = logs.get(position);
        holder.bind(log);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public void updateLogs(List<BatteryLog> newLogs) {
        this.logs = newLogs;
        notifyDataSetChanged();
    }

    public void clearData() {
        logs.clear();
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateTime, tvLevel, tvTemp, tvVoltage, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvTemp = itemView.findViewById(R.id.tvTemp);
            tvVoltage = itemView.findViewById(R.id.tvVoltage);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        void bind(BatteryLog log) {
            tvDateTime.setText(dateFormat.format(new Date(log.getTimestamp())));
            tvLevel.setText(String.format("Level: %d%%", log.getLevel()));
            tvTemp.setText(String.format("Temp: %.1f°C", log.getTemperature()));
            tvVoltage.setText(String.format("Voltage: %dmV", log.getVoltage()));
            tvStatus.setText(String.format("Status: %s", log.getStatus()));
        }
    }
}
