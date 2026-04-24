package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.storage.DataStore;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

/**
 * Sub-resource for readings of a specific sensor, reached via the
 * SensorResource locator:
 *
 *   GET  /api/v1/sensors/{sensorId}/readings   -> history for the sensor
 *   POST /api/v1/sensors/{sensorId}/readings   -> append a new reading
 *
 * This class is NOT annotated with @Path at the class level on purpose -
 * it is instantiated and bound by the parent resource's locator method.
 *
 * Side effect of POST: updates the parent Sensor's currentValue so that the
 * latest metric is immediately visible through the /sensors/{id} endpoint
 * (data-consistency requirement in the spec).
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final DataStore dataStore = DataStore.getInstance();
    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns the historical log of readings for this sensor.
     */
    @GET
    public Response getReadings() {
        List<SensorReading> readings = dataStore.getReadings(sensorId);
        return Response.ok(readings).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     * Append a new reading.
     *
     * - Sensors in MAINTENANCE or OFFLINE state cannot accept readings
     *   -> SensorUnavailableException (HTTP 403)
     * - On success, the parent Sensor's currentValue is updated to the
     *   new reading's value.
     */
    @POST
    public Response addReading(SensorReading incoming) {
        Sensor sensor = dataStore.getSensor(sensorId);
        // The locator already verified existence, but guard anyway
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Sensor '" + sensorId + "' not found")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // State-constraint check
        String status = sensor.getStatus();
        if (!"ACTIVE".equalsIgnoreCase(status)) {
            throw new SensorUnavailableException(sensorId, status);
        }

        // Populate server-generated fields
        if (incoming.getId() == null || incoming.getId().trim().isEmpty()) {
            incoming.setId(UUID.randomUUID().toString());
        }
        if (incoming.getTimestamp() <= 0) {
            incoming.setTimestamp(System.currentTimeMillis());
        }

        dataStore.addReading(sensorId, incoming);

        // Side effect: update the parent Sensor's currentValue
        sensor.setCurrentValue(incoming.getValue());
        dataStore.saveSensor(sensor);

        return Response.status(Response.Status.CREATED)
                .entity(incoming)
                .build();
    }
}
