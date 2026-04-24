package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.storage.DataStore;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;

/**
 * Room Management Resource at /api/v1/rooms
 *
 * Manages the lifecycle of Rooms:
 * - GET / : list all rooms
 * - POST / : create a new room
 * - GET /{roomId} : fetch a specific room's metadata
 * - DELETE /{roomId} : decommission a room (blocked if sensors still assigned)
 *
 * Note: JAX-RS resource classes are, by default, instantiated per-request.
 * All shared state (rooms map) is stored in the DataStore singleton to prevent
 * data loss and race conditions across concurrent requests.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorRoomResource {

    private final DataStore dataStore = DataStore.getInstance();

    /**
     * GET /api/v1/rooms
     * Returns a comprehensive list of all rooms.
     */
    @GET
    public Response getAllRooms() {
        List<Room> rooms = dataStore.getAllRooms();
        return Response.ok(rooms).build();
    }

    /**
     * POST /api/v1/rooms
     * Create a new room.
     * Request body must contain at minimum: id, name, capacity
     *
     * Returns 201 Created with a Location header pointing to the new
     * resource's URI - standard REST convention that lets clients follow
     * the header to fetch the created entity without guessing the URL.
     */
    @POST
    public Response createRoom(Room newRoom, @Context UriInfo uriInfo) {
        if (newRoom.getId() == null || newRoom.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Room id is required")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if (dataStore.roomExists(newRoom.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Room with id '" + newRoom.getId() + "' already exists")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        dataStore.saveRoom(newRoom);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(newRoom.getId())
                .build();
        return Response.created(location)
                .entity(newRoom)
                .build();
    }

    /**
     * GET /api/v1/rooms/{roomId}
     * Fetch detailed metadata for a specific room.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Room '" + roomId + "' not found")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        return Response.ok(room).build();
    }

    /**
     * DELETE /api/v1/rooms/{roomId}
     * Decommission a room.
     *
     * Business Logic: A room cannot be deleted if it still has active sensors
     * assigned to it. If deletion is attempted on a room with sensors,
     * RoomNotEmptyException is thrown (mapped to HTTP 409 Conflict).
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Room '" + roomId + "' not found")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // Business logic: prevent orphaning sensors
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }

        dataStore.removeRoom(roomId);
        return Response.noContent().build();
    }
}
