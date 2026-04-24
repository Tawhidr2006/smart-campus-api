package com.smartcampus.app;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Entry point. Starts an embedded Grizzly HTTP server that hosts the
 * JAX-RS application at http://localhost:8080/
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static final String BASE_URI = "http://localhost:8080/";

    public static HttpServer startServer() {
        // Register the Application subclass so @ApplicationPath is honoured
        ResourceConfig rc = ResourceConfig.forApplicationClass(SmartCampusApplication.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();

        
        LOGGER.info("Smart Campus API started.");
        LOGGER.info("Base URL: " + BASE_URI + "api/v1");
        LOGGER.info("Discovery endpoint: " + BASE_URI + "api/v1");
        LOGGER.info("Press Ctrl+C to stop the server.");
        

        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Smart Campus API...");
            server.shutdownNow();
        }));

        // Keep the main thread alive
        Thread.currentThread().setName("smart-campus-main");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
