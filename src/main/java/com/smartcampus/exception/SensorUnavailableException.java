package com.smartcampus.exception;

/**
 * Thrown when a client attempts to POST a reading to a Sensor that is not
 * in ACTIVE state (i.e. is MAINTENANCE or OFFLINE).
 * Mapped to HTTP 403 Forbidden.
 */
public class SensorUnavailableException extends RuntimeException {

    private final String sensorId;
    private final String status;

    public SensorUnavailableException(String sensorId, String status) {
        super("Sensor '" + sensorId + "' is currently in '" + status
                + "' state and cannot accept new readings.");
        this.sensorId = sensorId;
        this.status = status;
    }

    public String getSensorId() {
        return sensorId;
    }

    public String getStatus() {
        return status;
    }
}
