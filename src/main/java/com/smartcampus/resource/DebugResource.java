package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Diagnostic endpoint used purely to demonstrate the global
 * ExceptionMapper&lt;Throwable&gt; safety net (rubric 5.4).
 *
 * GET /api/v1/debug/boom deliberately throws a NullPointerException.
 * The GenericExceptionMapper intercepts it, logs the stack trace on the
 * server, and returns a generic HTTP 500 JSON body to the client - with
 * no stack-trace leakage.
 *
 * This mirrors the rubric's explicit video-demo requirement for 5.4:
 *   "Trigger a runtime error (e.g., NullPointerException) and show
 *    generic HTTP 500 without a raw stack trace."
 */
@Path("/debug")
@Produces(MediaType.APPLICATION_JSON)
public class DebugResource {

    /**
     * Deliberately throws NullPointerException. Never returns normally.
     */
    @GET
    @Path("/boom")
    public Response triggerRuntimeError() {
        String nullString = null;
        // The next line dereferences null -> NullPointerException
        // which bubbles up to GenericExceptionMapper<Throwable>.
        int length = nullString.length();
        // Unreachable, but required for the method signature.
        return Response.ok("length=" + length).build();
    }
}
