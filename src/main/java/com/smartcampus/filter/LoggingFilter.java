package com.smartcampus.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Cross-cutting filter that logs every incoming request and outgoing response.
 * Implements both ContainerRequestFilter and ContainerResponseFilter so that
 * the same class handles both sides of the interaction.
 *
 * Using a filter means we capture 100% of traffic - including requests that
 * never reach a resource method (e.g. 404s, malformed input rejected by
 * JAX-RS before dispatch) - without cluttering the resource classes.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getRequestUri().toString();
        LOGGER.info("--> " + method + " " + path);
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getRequestUri().toString();
        int status = responseContext.getStatus();
        LOGGER.info("<-- " + method + " " + path + " : " + status);
    }
}
