package com.smartcampus.app;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.mapper.GenericExceptionMapper;
import com.smartcampus.mapper.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.mapper.NotFoundExceptionMapper;
import com.smartcampus.mapper.RoomNotEmptyExceptionMapper;
import com.smartcampus.mapper.SensorUnavailableExceptionMapper;
import com.smartcampus.resource.DebugResource;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.SensorResource;
import com.smartcampus.resource.SensorRoomResource;

import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application subclass. Registers all resources, exception mappers,
 * and filters under the versioned /api/v1 path.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // Resource classes
        classes.add(DiscoveryResource.class);
        classes.add(SensorRoomResource.class);
        classes.add(SensorResource.class);
        classes.add(DebugResource.class);

        // Exception mappers
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(NotFoundExceptionMapper.class);
        classes.add(GenericExceptionMapper.class);

        // Filters
        classes.add(LoggingFilter.class);

        return classes;
    }
}
