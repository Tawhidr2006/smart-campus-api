package com.smartcampus.exception;

/**
 * Thrown when a client attempts to delete a Room that still has active
 * sensors assigned to it. Mapped to HTTP 409 Conflict.
 */
public class RoomNotEmptyException extends RuntimeException {

    private final String roomId;
    private final int sensorCount;

    public RoomNotEmptyException(String roomId, int sensorCount) {
        super("Room '" + roomId + "' cannot be deleted while it still has "
                + sensorCount + " sensor(s) assigned to it.");
        this.roomId = roomId;
        this.sensorCount = sensorCount;
    }

    public String getRoomId() {
        return roomId;
    }

    public int getSensorCount() {
        return sensorCount;
    }
}
