package com.smartcampus.model;

/**
 * Standardised JSON error body returned by Exception Mappers.
 * This ensures the API never leaks raw Java stack traces.
 */
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private long timestamp;

    public ErrorResponse() {
    }

    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
