Smart Campus  API

A web service built with JAX-RS for managing rooms and sensors across the University of Westminster's Smart Campus.

- **Module:** 5COSC022W – Client-Server Architectures (2025/26)
- **Base URL:** `http://localhost:8080/api/v1`
- **Technology:** Java 11, JAX-RS 3.1 (Jersey), Embedded Grizzly HTTP, Jackson JSON
- **Persistence:** In-memory only (`ConcurrentHashMap` / `CopyOnWriteArrayList`) — no database, per coursework constraints.


## 1. API Design Overview

The API is structured around three core resources that mirror the physical structure of the campus:

Resource:       Path:                                    Purpose:

Discovery        `GET /api/v1`                             API metadata + HATEOAS links                          
Rooms            `/api/v1/rooms`                           CRUD on rooms                                         
Sensors          `/api/v1/sensors`                         CRUD on sensors, with `?type=` filtering              
Sensor Readings  `/api/v1/sensors/{sensorId}/readings`     Historical readings per sensor (sub-resource locator) 

### Endpoint Summary

Method: Path:                                   Description:                                         Success:  Error:          

GET     `/api/v1`                               Discovery document                                   200      —                   
GET     `/api/v1/rooms`                         List all rooms                                       200      —                   
POST    `/api/v1/rooms`                         Create a room                                        201      400, 409            
GET     `/api/v1/rooms/{roomId}`                Room details                                         200      404                 
DELETE  `/api/v1/rooms/{roomId}`                Decommission a room (blocked if it has sensors)      204      404, **409**        
GET     `/api/v1/sensors[?type=CO2]`            List sensors (optionally filtered by type)           200      —                   
POST    `/api/v1/sensors`                       Register a sensor (room must exist)                  201      400, 409, **422**   
GET     `/api/v1/sensors/{sensorId}`            Sensor details                                       200      404                 
GET     `/api/v1/sensors/{sensorId}/readings`   History of readings                                  200      404                 
POST    `/api/v1/sensors/{sensorId}/readings`   Append a reading (sensor must be ACTIVE)             201      404, **403**        

Any unhandled runtime error is caught by a global mapper and returned as **HTTP 500** with a generic JSON body (no stack traces leak to clients).


## 2. Build & Run

### Prerequisites
- **JDK 11+** (tested on OpenJDK 11 and 21)
- **Maven 3.8+**
- Any REST client (Postman)

### Build
```bash
# from the project root
mvn clean package
```

This produces two jars in `target/`:
- `smart-campus-api.jar` — thin jar
- `smart-campus-api-jar-with-dependencies.jar` — fat jar with every dependency bundled

### Run

**Option A — Maven (easiest during development):**
```bash
mvn exec:java
```

**Option B — fat jar:**
```bash
java -jar target/smart-campus-api-jar-with-dependencies.jar
```

On startup you should see:
```
INFO: Smart Campus API started.
INFO: Base URL: http://localhost:8080/api/v1
INFO: Discovery endpoint: http://localhost:8080/api/v1
```

Stop the server with `Ctrl + C`.

# Conceptual Report (Answers to Questions in the spec)

## Part 1 — Service Architecture & Setup

### 1.1 Default lifecycle of a JAX-RS resource class

By default, **JAX-RS instantiates a new instance of a resource class for every incoming HTTP request** (per-request scope). When the request completes, the instance is eligible for garbage collection. The JAX-RS runtime does *not* treat resource classes as singletons unless you explicitly annotate them with `@Singleton` (from `jakarta.inject`) or register them as an instance rather than a class.

This design has three important consequences for state management:

1. **Instance fields are effectively useless for shared data.** If `SensorRoomResource` had a field like `private Map<String, Room> rooms = new HashMap<>();`, each request would see an empty map because each request gets a fresh object. All data written by one request would be lost the moment the response is sent.

2. **Shared state must live outside the resource.** In this project, every resource obtains the `DataStore` singleton via `DataStore.getInstance()` in its constructor. The singleton is the only place that survives across requests.

3. **Concurrent access is the norm, not the exception.** Because the Grizzly container handles requests on multiple worker threads, two different request threads can enter `createSensor(...)` simultaneously. A plain `HashMap` or `ArrayList` would suffer from lost updates or even structural corruption (infinite loops on resize). To prevent this the `DataStore` uses:
   - `ConcurrentHashMap<String, Room>` and `ConcurrentHashMap<String, Sensor>` — thread-safe `put`/`get`/`remove` without external locking.
   - `CopyOnWriteArrayList<SensorReading>` for each sensor's reading log — safe iteration under concurrent appends, which is ideal for a write-occasionally, read-often history log.

In short: resources are disposable request handlers; persistent state must be isolated in thread-safe structures that outlive any single request.

### 1.2 Why HATEOAS matters

HATEOAS ("Hypermedia as the Engine of Application State") is the principle that responses should include **links the client can follow** to discover related resources and next-possible actions, rather than requiring the client to hard-code every URL.

The discovery endpoint in this project is a minimal example — `GET /api/v1` returns a `_links` map pointing to `/rooms` and `/sensors`. A fully HATEOAS-driven response to `GET /api/v1/rooms/LIB-301` could additionally embed links such as `sensors`, `self`, and `delete`.

Benefits to client developers over static documentation:

- **Loose coupling / evolvability.** The server can change URL structures (e.g. move `/rooms` to `/facilities/rooms` or introduce versioning) without breaking clients that follow links rather than hard-coded strings. Static docs would require every client to be redeployed.
- **Discoverability.** A new developer can explore the API interactively starting from the root, in the same way a human browses a website — no need to keep docs and code in sync.
- **Context-sensitive actions.** Links can be conditionally included. A room with zero sensors could expose a `delete` link; a room with sensors would omit it. The client doesn't need to duplicate that business rule.
- **Reduced documentation drift.** Documentation is a secondary artifact that goes stale. The hypermedia response is generated from the same code that serves the data, so it is always in sync.

The trade-off is increased response size and client complexity if taken to extremes; in practice most APIs adopt a pragmatic subset (links on collection/resource representations) rather than full HAL/Siren specs.


## Part 2 — Room Management

### 2.1 IDs-only vs full-object lists

When `GET /api/v1/rooms` returns full `Room` objects rather than a list of IDs, there are two competing considerations:

**Full objects (the approach taken here):**
- **Pros:** Clients that want to render a list of rooms with their names, capacities, and sensor counts can do so in a single HTTP round-trip. This is the common case for dashboards and listing UIs, so it minimises perceived latency.
- **Cons:** The payload scales linearly with the number of rooms *and* with the depth of each room's data. For 10,000 rooms each holding 50 sensor IDs, the response could easily reach several megabytes. This costs network bandwidth, server serialisation time, and client memory.

**IDs-only:**
- **Pros:** Tiny payload; a collection of 10,000 IDs is still well under a megabyte.
- **Cons:** The client is forced into an "N+1" fetch pattern — one request for the list, then one request per room for details. For a UI that needs 20 visible rooms, that is 21 round-trips instead of 1.

**Hybrid approaches** used in production APIs:
- **Pagination** (`?page=1&pageSize=50`) bounds the size of any single response.
- **Field selection** (`?fields=id,name`) lets the client ask for a narrower projection.
- **Sparse fieldsets** (JSON:API) combine both.
- **Expansion** (`?expand=sensors`) lets the client opt in to nested data only when needed.

For this coursework I return full objects without nested sensor objects (only sensor IDs), which strikes a reasonable middle ground for the small campus scale modelled here.

### 2.2 Is `DELETE` idempotent in this implementation?

**Yes — `DELETE /api/v1/rooms/{roomId}` is idempotent** in the REST sense, with an important nuance.

Idempotent means "the observable state on the server is the same whether the client calls the operation once or many times." It does **not** mean "the response body is identical every time."

Behaviour of this implementation when the same `DELETE /api/v1/rooms/ENG-205` is sent repeatedly:

Call:   State before:    State after:    Response:                                       

1st     room exists      room removed     `204 No Content` (success)                     
2nd     room absent      room absent      `404 Not Found` (with a JSON error body)       
3rd     room absent      room absent      `404 Not Found` (identical to the 2nd response)

The state converges after the first request — calls 2, 3, 4… all leave the server exactly as it was. That is the formal definition of idempotency. The fact that the HTTP status changes from 204 to 404 does not break idempotency, because the server state is the relevant invariant, not the response.

This matters practically because a flaky network, proxy retry, or a user double-clicking a "delete" button will not cause cumulative damage. Compare this with `POST /api/v1/sensors/CO2-205/readings`, which is **not** idempotent — each call appends a new reading and mutates `currentValue` — meaning the client must never blindly retry a failed reading POST without a safeguard like an idempotency key.

One caveat: the protective `RoomNotEmptyException` (409) is raised *before* the delete. So a `DELETE` on a populated room remains a no-op and is therefore still idempotent — the server state is unchanged both after call 1 and after call 2.

## Part 3 — Sensor Operations & Linking

### CW question 3.1 What happens when `@Consumes(MediaType.APPLICATION_JSON)` is violated

The `@Consumes` annotation tells JAX-RS which media types the method is willing to accept in the request body. It is enforced by the runtime **before** the method is called.

Consider `POST /api/v1/sensors` annotated with `@Consumes(MediaType.APPLICATION_JSON)`:

- **If a client sends `Content-Type: text/plain`** — JAX-RS finds that no method on the matching resource declares that it consumes `text/plain`, and responds with **`415 Unsupported Media Type`**. The method body never runs. No exception is raised in user code — this is a framework-level rejection.

- **If a client sends `Content-Type: application/xml`** — Same outcome: `415 Unsupported Media Type`. Even if an XML provider were on the classpath, the annotation explicitly limits this endpoint to JSON.

The purpose of `@Consumes` is twofold: (a) **content negotiation** — the runtime picks the correct reader for the incoming body, and (b) **defensive filtering** — incompatible content types are rejected at the HTTP boundary rather than producing cryptic parse errors deep inside the handler.

### 3.2 `@QueryParam` vs. path-based filtering

The coursework implements filtering as `GET /api/v1/sensors?type=CO2` rather than `GET /api/v1/sensors/type/CO2`. The query-parameter approach is generally considered superior for searching and filtering **collections** for several reasons:

1. **URLs identify resources; query strings refine them.** In REST, the URI path is meant to identify a single resource or a canonical collection. `/sensors` is the canonical collection of all sensors. Filtering doesn't identify a *different* resource — it asks for a projection of the same one. Burying filter criteria in the path (`/sensors/type/CO2`) pretends the filtered result is a distinct resource, which it isn't — `/sensors/type/CO2/type/ACTIVE` quickly becomes absurd.

2. **Composability.** Query parameters can be combined and reordered freely: `?type=CO2&status=ACTIVE&minValue=400`. A path-based scheme forces an ordering (`/type/CO2/status/ACTIVE` vs `/status/ACTIVE/type/CO2` — are they the same URL? cache-identical?) and explodes combinatorially as filters multiply.

3. **Optional by nature.** Query parameters are optional by convention — a client can safely omit them and still hit the same endpoint. `@QueryParam` handles `null` gracefully. Path segments are positional and mandatory in whatever position they occupy, making partial filtering awkward.

4. **Cache and routing friendliness.** Intermediate caches and routers treat the path as the resource identifier and the query string as a modifier. `GET /sensors` with different query strings still resolves to the same handler, which simplifies server-side routing and client-side URL construction.

5. **Industry convention.** Every major API (GitHub, Stripe, Twilio, AWS) uses query parameters for list filtering. Clients expect `?type=...` and tools (API explorers, HTTP clients) render them as form fields automatically.

6. **Clean separation from sub-resources.** Paths are reserved for hierarchy (`/sensors/{id}`, `/sensors/{id}/readings`). Mixing filter segments into that tree blurs the distinction between "navigate to a sub-resource" and "narrow the collection."

Path segments remain appropriate for **identification** — `GET /sensors/CO2-001` identifies exactly one entity. Query parameters are appropriate for **filtering, sorting, and pagination** across a collection. The two mechanisms are complementary, not alternative.


## Part 4 — Deep Nesting with Sub-Resources

### 4.1 Architectural benefits of the Sub-Resource Locator pattern

A **sub-resource locator** is a method on a parent resource that is annotated with `@Path` but no HTTP-method annotation (no `@GET`, `@POST`, etc.). JAX-RS calls it to obtain an instance of another class, and then continues matching the rest of the request path against that returned object.

In this project, `SensorResource.readings(String sensorId)` is the locator, and it returns a `SensorReadingResource` bound to a specific sensor.

The key architectural benefits:

1. **Single responsibility per class.** `SensorResource` handles sensor lifecycle; `SensorReadingResource` handles reading history. Without the locator pattern, a single class would need methods for both `/sensors`, `/sensors/{id}`, `/sensors/{id}/readings`, `/sensors/{id}/readings/{rid}` — quickly turning into a several-hundred-line "god class" that violates SRP.

2. **Context is carried cleanly.** The `sensorId` is a constructor argument of `SensorReadingResource`, so every method in that class already has it in scope as `this.sensorId`. If readings were handled inside `SensorResource`, every reading method would need `@PathParam("sensorId")` parameters and would keep re-fetching the parent sensor, duplicating code.

3. **Validation is centralised.** The locator is the perfect place to validate that the parent exists. Our implementation throws `NotFoundException` if the sensor doesn't exist — so every method in `SensorReadingResource` can safely assume the parent exists without rechecking.

4. **Easier testing.** `SensorReadingResource` can be instantiated directly in unit tests by calling `new SensorReadingResource("TEMP-001")`, without spinning up a web server or dealing with the parent's concerns.

5. **Better composability at scale.** If a third level of nesting were added (`/sensors/{sid}/readings/{rid}/annotations`), we would simply add another locator on `SensorReadingResource`. The structure scales linearly; nesting everything in one class scales combinatorially.

6. **Dynamic dispatch is possible.** The locator is a regular Java method, so it can return *different* sub-resource instances based on runtime conditions — for example, returning a `ReadOnlyReadingResource` when the sensor is offline, or a `SimulatedReadingResource` for sensors flagged as virtual. A pure annotation-based scheme can't do that.

The trade-off is one extra hop conceptually for readers of the code, but that is offset many times over by the maintainability gains on any non-trivial API.

## Part 5 — Advanced Error Handling, Exception Mapping & Logging

### 5.2 Why HTTP 422 is more accurate than 404 for missing referenced resources

HTTP status codes divide into classes: `4xx` means "the client did something wrong," but the *kind* of wrong matters for correct client behaviour.

- **404 Not Found** means *"the URL you asked for does not correspond to any resource on this server."* It describes a failure of **routing**. The correct client response is typically to check the URL for typos or ask the server what resources exist.

- **422 Unprocessable Entity** means *"I understood the URL, I understood the content type, I parsed your JSON fine, but the content is semantically invalid in a way that routing and syntax checks can't catch."* It describes a failure of **content validation**.

Consider `POST /api/v1/sensors` with a body `{ "id":"X", "roomId":"ZZZ" }`:

- The URL `/api/v1/sensors` *does* exist — there's a resource method registered on it.
- The HTTP method `POST` is allowed on it.
- The `Content-Type: application/json` header matches `@Consumes`.
- The JSON is well-formed and deserialises successfully into a `Sensor`.
- The *only* problem is that `roomId=ZZZ` references a room that doesn't exist.

Returning 404 here is misleading because:
- The URL the client invoked is perfectly valid (not "not found").
- A client retrying with exactly the same URL would still fail, so the "fix your URL" reflex that 404 triggers is wrong advice.
- A debugging developer looking at `POST /api/v1/sensors → 404` would reasonably assume the endpoint itself is missing — sending them down the wrong diagnostic path.

422 is precise: *"your syntax is fine, your request target is fine, but the content of your body references a resource that doesn't exist."* The client knows to look at the body, not the URL. Some APIs prefer **400 Bad Request** here (RFC 9110 permits it), but 422 is more specific and is widely adopted by modern APIs (GitHub, Stripe, Twilio) for exactly this case. The `LinkedResourceNotFoundExceptionMapper` in this project returns 422 with a message that names the missing reference, making the fix obvious.

### 5.4 Security risks of exposing Java stack traces

Unhandled exceptions in a naïvely-configured Java web app return the full stack trace to the caller — often as HTML. From a cybersecurity standpoint this is a **high-severity information-disclosure vulnerability** (see OWASP A05:2021 Security Misconfiguration). An attacker running reconnaissance against the API can harvest:

1. **Framework & version fingerprints.** Class names like `org.glassfish.jersey.server.internal.routing.RoutingStage$1` or `com.fasterxml.jackson.databind.JsonMappingException` reveal Jersey and Jackson are in use. An attacker then looks up known CVEs for those specific versions (Jackson in particular has a long history of deserialisation vulnerabilities tied to specific 2.x minor versions).

2. **Internal package structure.** Package names like `com.smartcampus.storage.DataStore` expose business domain layering and can hint at other attack surface (e.g. an `admin` package that might be reachable on a different endpoint).

3. **File-system paths.** If the stack trace includes line numbers with source paths (e.g. `/opt/app/lib/...`), the attacker learns the deployment layout. This aids path traversal attempts and helps build exploit payloads.

4. **Database / ORM details.** A leaked `SQLException` reveals the database vendor, table and column names, and sometimes the exact SQL that failed — a massive gift for SQL-injection probing.

5. **Authentication or session internals.** A `NullPointerException` in a security filter that leaks variable names (`currentUser`, `sessionToken`) signals what the auth flow looks like.

6. **Dependency versions.** A `ClassNotFoundException` or `NoClassDefFoundError` can reveal which libraries are on the classpath — and their versions map directly to public vulnerability databases.

7. **Business logic hints.** Messages like `"roomId ZZZ not found in inventoryStore"` tell the attacker the internal data-store naming and what probing strings to try.

The mitigation, implemented in this project, is the `GenericExceptionMapper<Throwable>`. It:
- **Logs the full stack trace server-side** (via `Logger.log(Level.SEVERE, ...)`) so operators can debug.
- **Returns only a generic JSON body** (`"Internal Server Error" / "An unexpected error occurred"`) to the client.
- **Never includes class names, file paths, or exception messages** in the response.

For defence-in-depth, the response also avoids echoing user input back in error messages (which could turn a simple typo into an XSS vector if rendered by a browser-based client).

### 5.5 Why JAX-RS filters beat inline `Logger.info()` calls

Logging is a textbook **cross-cutting concern** — a behaviour that logically belongs to every endpoint but has nothing to do with their business logic. Implementing it inside each resource method has predictable problems.

Inline approach:
```java
@GET
public Response getAllRooms() {
    LOGGER.info("GET /api/v1/rooms");
    try {
        List<Room> rooms = dataStore.getAllRooms();
        LOGGER.info("Returning " + rooms.size() + " rooms");
        return Response.ok(rooms).build();
    } catch (Exception e) {
        LOGGER.severe("Error: " + e.getMessage());
        throw e;
    }
}
```
Multiply this across ~10 methods in this project — or several hundred in a real API — and the cost is significant.

The filter-based approach (this project's `LoggingFilter`, implementing both `ContainerRequestFilter` and `ContainerResponseFilter`) gives:

1. **Single point of truth.** One class logs every request and every response. Adding a new field (e.g. `X-Request-ID`) means editing one place, not every handler.

2. **Uniform coverage.** Filters run for *every* incoming request, including ones that 404 because no matching resource method exists, or 415 because the content type is wrong. Inline `Logger` calls in resource methods only cover requests that actually reached those methods — observability gaps form exactly where debugging is hardest.

3. **No chance of forgetting.** A developer adding a new endpoint cannot "forget" to log; the filter handles it automatically.

4. **Clean resource code.** Resource methods focus on business logic and return types. They don't drown in logging noise, and the actual logic remains readable.

5. **Easy to extend or disable.** Want to add timing? The filter can stamp the request with `System.nanoTime()` on the way in and compute latency on the way out. Want to silence logging in tests? Don't register the filter in the test `Application` subclass. Neither is possible with inline logging without touching every endpoint.

6. **Consistent format.** All log lines follow the same pattern (`--> METHOD URI` / `<-- METHOD URI : STATUS`), so log aggregation tools (Splunk, ELK, Datadog) can parse them with a single regex.

7. **Separation of concerns at the architectural level.** The JAX-RS filter chain is an explicit extension point for exactly this kind of concern — alongside authentication, rate limiting, request-ID propagation, and compression. Using it as designed keeps the architecture legible.

This is essentially Aspect-Oriented Programming at the HTTP boundary: the resource classes describe *what* the API does; the filters describe *how* every request is handled regardless of what it does.


## Author
Mohammed Tawhid Rahman
Submitted for **5COSC022W — Client-Server Architectures**, 2025/26.
University of Westminster, School of Computer Science and Engineering.
