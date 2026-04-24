package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discovery (root) endpoint at GET /api/v1
 *
 * Returns API metadata plus a HATEOAS-style map of primary resource collections,
 * allowing clients to discover available endpoints without prior knowledge
 * of the URL layout.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response getApiInfo(@Context UriInfo uriInfo) {
        String base = uriInfo.getBaseUri().toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("apiName", "Smart Campus Sensor & Room Management API");
        info.put("version", "1.0.0");
        info.put("description",
                "RESTful service for managing rooms and sensors across the university's Smart Campus.");

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("name", "Smart Campus Admin");
        contact.put("email", "smartcampus-admin@westminster.ac.uk");
        contact.put("organisation", "University of Westminster - School of Computer Science and Engineering");
        info.put("contact", contact);

        // HATEOAS-style map of primary resource collections
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",    base);
        links.put("rooms",   base + "/rooms");
        links.put("sensors", base + "/sensors");
        info.put("_links", links);

        return Response.ok(info).build();
    }
}
