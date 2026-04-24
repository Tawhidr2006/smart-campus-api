package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.storage.DataStore;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sensor Resource at /api/v1/sensors
 *
 * Manages the sensor collection:
 * - GET /            : list all sensors (optional filter: ?type=CO2)
 * - POST /           : register a new sensor (validates roomId exists)
 * - GET /{sensorId}  : fetch a specific sensor
 *
 * It also acts as a sub-resource locator, delegating
 *   /{sensorId}/readings/*
 * to a dedicated SensorReadingResource instance.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore dataStore = DataStore.getInstance();

    /**
     * GET /api/v1/sensors[?type=...]
     * Returns all sensors, optionally filtered by type.
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = dataStore.getAllSensors();

        if (type != null && !type.trim().isEmpty()) {
            sensors = sensors.stream()
                    .filter(s -> type.equalsIgnoreCase(s.getType()))
                    .collect(Collectors.toList());
        }

        return Response.ok(sensors).build();
    }

    /**
     * POST /api/v1/sensors
     * Register a new sensor. The referenced roomId must exist - otherwise
     * a LinkedResourceNotFoundException is thrown (-> HTTP 422).
     */
    @POST
    public Response createSensor(Sensor newSensor, @Context UriInfo uriInfo) {
        if (newSensor.getId() == null || newSensor.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Sensor id is required")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (dataStore.sensorExists(newSensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Sensor with id '" + newSensor.getId() + "' already exists")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // Dependency validation: the referenced room MUST exist
        String roomId = newSensor.getRoomId();
        if (roomId == null || roomId.trim().isEmpty()) {
            throw new LinkedResourceNotFoundException("Room", "(null)");
        }
        Room room = dataStore.getRoom(roomId);
        if (room == null) {
            throw new LinkedResourceNotFoundException("Room", roomId);
        }

        // Default status if not supplied
        if (newSensor.getStatus() == null || newSensor.getStatus().trim().isEmpty()) {
            newSensor.setStatus("ACTIVE");
        }

        dataStore.saveSensor(newSensor);
        room.addSensorId(newSensor.getId());

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(newSensor.getId())
                .build();
        return Response.created(location)
                .entity(newSensor)
                .build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Sensor '" + sensorId + "' not found")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * Sub-resource locator: delegates /api/v1/sensors/{sensorId}/readings/*
     * to a dedicated SensorReadingResource.
     *
     * Note the absence of @GET / @POST here - JAX-RS uses the presence of
     * @Path alone (no HTTP method annotation) to identify a locator.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
        if (!dataStore.sensorExists(sensorId)) {
            throw new NotFoundException("Sensor '" + sensorId + "' not found");
        }
        return new SensorReadingResource(sensorId);
    }
}
