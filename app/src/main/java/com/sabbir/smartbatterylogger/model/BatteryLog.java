package com.sabbir.smartbatterylogger.model;

public class BatteryLog {
    private long timestamp;
    private int level;
    private float temperature;
    private int voltage;
    private String status;
    private String health;

    public BatteryLog(long timestamp, int level, float temperature, int voltage, String status, String health) {
        this.timestamp = timestamp;
        this.level = level;
        this.temperature = temperature;
        this.voltage = voltage;
        this.status = status;
        this.health = health;
    }



    // Getters
    public long getTimestamp() { return timestamp; }
    public int getLevel() { return level; }
    public float getTemperature() { return temperature; }
    public int getVoltage() { return voltage; }
    public String getStatus() { return status; }
    public String getHealth() { return health; }
}

