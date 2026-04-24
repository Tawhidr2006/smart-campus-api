package com.smartcampus.storage;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory, thread-safe data store used throughout the application.
 *
 * Because JAX-RS resource classes are, by default, instantiated per-request
 * (see the Part 1 question in the report), any shared mutable state must
 * live in a thread-safe structure outside the resource. This singleton
 * uses ConcurrentHashMap and CopyOnWriteArrayList so multiple concurrent
 * HTTP requests cannot corrupt the campus data.
 *
 * Per the coursework constraints, no database technology is used - only
 * Java collections (HashMap / ArrayList variants).
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    // sensorId -> list of readings for that sensor
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private DataStore() {
        seedSampleData();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    //  Room operations 

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }

    public Room getRoom(String id) {
        return rooms.get(id);
    }

    public void saveRoom(Room room) {
        rooms.put(room.getId(), room);
    }

    public boolean roomExists(String id) {
        return rooms.containsKey(id);
    }

    public Room removeRoom(String id) {
        return rooms.remove(id);
    }

    //  Sensor operations 

    public List<Sensor> getAllSensors() {
        return new ArrayList<>(sensors.values());
    }

    public Sensor getSensor(String id) {
        return sensors.get(id);
    }

    public void saveSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        // Ensure the reading log is initialised
        readings.putIfAbsent(sensor.getId(), new CopyOnWriteArrayList<>());
    }

    public boolean sensorExists(String id) {
        return sensors.containsKey(id);
    }

    public Sensor removeSensor(String id) {
        readings.remove(id);
        return sensors.remove(id);
    }

    //  Reading operations 

    public List<SensorReading> getReadings(String sensorId) {
        return readings.getOrDefault(sensorId, new CopyOnWriteArrayList<>());
    }

    public void addReading(String sensorId, SensorReading reading) {
        readings.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>()).add(reading);
    }

    //  Seed demo data 

    private void seedSampleData() {
        Room lib = new Room("LIB-301", "Library Quiet Study", 40);
        Room lab = new Room("LAB-101", "Computer Science Lab 101", 30);
        saveRoom(lib);
        saveRoom(lab);

        Sensor tempSensor = new Sensor("TEMP-001", "Temperature", "ACTIVE", 21.5, "LIB-301");
        Sensor co2Sensor  = new Sensor("CO2-001",  "CO2",         "ACTIVE", 420.0, "LIB-301");
        Sensor occSensor  = new Sensor("OCC-001",  "Occupancy",   "MAINTENANCE", 0.0, "LAB-101");

        saveSensor(tempSensor);
        saveSensor(co2Sensor);
        saveSensor(occSensor);

        lib.addSensorId("TEMP-001");
        lib.addSensorId("CO2-001");
        lab.addSensorId("OCC-001");
    }
}
