package com.smartcampus.mapper;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.ErrorResponse;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity
 * with a JSON error body.
 *
 * 422 is preferred over 404 because the request's URL (e.g. POST /api/v1/sensors)
 * is perfectly valid - it's the CONTENT of the JSON body that references a
 * non-existent resource, which is a semantic rather than routing error.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    private static final int UNPROCESSABLE_ENTITY = 422;

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        ErrorResponse body = new ErrorResponse(
                UNPROCESSABLE_ENTITY,
                "Unprocessable Entity",
                ex.getMessage()
        );
        return Response.status(UNPROCESSABLE_ENTITY)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
