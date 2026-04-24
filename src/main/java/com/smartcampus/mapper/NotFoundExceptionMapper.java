package com.smartcampus.mapper;

import com.smartcampus.model.ErrorResponse;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Catches JAX-RS's built-in NotFoundException and any manually-thrown
 * NotFoundException to ensure 404 responses come back as structured JSON
 * rather than a default server HTML page.
 */
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException ex) {
        ErrorResponse body = new ErrorResponse(
                Response.Status.NOT_FOUND.getStatusCode(),
                "Not Found",
                ex.getMessage() == null ? "The requested resource could not be found."
                                         : ex.getMessage()
        );
        return Response.status(Response.Status.NOT_FOUND)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
