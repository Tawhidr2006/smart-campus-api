package com.smartcampus.model;

/**
 * Represents a physical sensor deployed within a Room.
 * Each Sensor has a status (ACTIVE, MAINTENANCE, or OFFLINE) and tracks
 * its most recent reading via currentValue.
 */
public class Sensor {
    private String id;              // Unique identifier, e.g., "TEMP-001"
    private String type;            // Category, e.g., "Temperature", "Occupancy", "CO2"
    private String status;          // "ACTIVE", "MAINTENANCE", or "OFFLINE"
    private double currentValue;    // The most recent measurement recorded
    private String roomId;          // Foreign key linking to the Room where the sensor is located

    public Sensor() {
    }

    public Sensor(String id, String type, String status, double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
