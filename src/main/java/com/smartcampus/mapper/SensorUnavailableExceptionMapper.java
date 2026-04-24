package com.smartcampus.mapper;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.ErrorResponse;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps SensorUnavailableException to HTTP 403 Forbidden with a JSON error body.
 */
@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException ex) {
        ErrorResponse body = new ErrorResponse(
                Response.Status.FORBIDDEN.getStatusCode(),
                "Sensor Unavailable",
                ex.getMessage()
        );
        return Response.status(Response.Status.FORBIDDEN)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
