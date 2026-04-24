package com.smartcampus.exception;

/**
 * Thrown when a client POSTs a payload that references a linked resource
 * which does not exist (e.g. a Sensor with a roomId that has no corresponding
 * Room). Mapped to HTTP 422 Unprocessable Entity.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    public LinkedResourceNotFoundException(String resourceType, String resourceId) {
        super("Referenced " + resourceType + " with id '" + resourceId + "' does not exist.");
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }
}
