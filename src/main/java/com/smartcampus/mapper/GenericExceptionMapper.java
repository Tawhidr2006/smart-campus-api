package com.smartcampus.mapper;

import com.smartcampus.model.ErrorResponse;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global "safety net" exception mapper. Catches any Throwable that isn't
 * handled by a more specific mapper (e.g. NullPointerException,
 * IndexOutOfBoundsException, IllegalStateException).
 *
 * Crucially, the full stack trace is logged to the server's log only.
 * The response to the client is a generic HTTP 500 with NO internal
 * details - this prevents leaking class names, file paths, library versions
 * or other information that an attacker could use to reconnoiter the system.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        // If it's already a WebApplicationException (e.g. thrown deliberately
        // with a specific status), honour its response.
        if (ex instanceof WebApplicationException) {
            return ((WebApplicationException) ex).getResponse();
        }

        // Log the full trace on the server for debugging...
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by GenericExceptionMapper", ex);

        // ...but never expose it to the client.
        ErrorResponse body = new ErrorResponse(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal Server Error",
                "An unexpected error occurred. Please contact the API administrators."
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
