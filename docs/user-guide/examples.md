# Examples

Practical recipes for common Apitomy Codegen use cases.

## Reactive Endpoints

Wrap all generated return types in `CompletionStage<>` for non-blocking, asynchronous endpoints:

```xml
<projectSettings>
    <javaPackage>com.example.api</javaPackage>
    <reactive>true</reactive>
</projectSettings>
```

**Generated interface:**

```java
@Path("/beers")
public interface BeersResource {

    @GET
    @Produces("application/json")
    CompletionStage<List<Beer>> listAllBeers();

    @POST
    @Consumes("application/json")
    CompletionStage<Void> addBeer(@NotNull Beer data);

    @DELETE
    @Path("/{beerId}")
    CompletionStage<Void> deleteBeer(@PathParam("beerId") int beerId);
}
```

## Generic Return Type Wrapper

Wrap return types in a framework-specific response type like RESTEasy Reactive's `RestResponse`:

```xml
<projectSettings>
    <javaPackage>com.example.api</javaPackage>
    <genericReturnType>org.jboss.resteasy.reactive.RestResponse</genericReturnType>
</projectSettings>
```

**Generated interface:**

```java
@GET
@Produces("application/json")
RestResponse<List<Beer>> listAllBeers();

@DELETE
@Path("/{beerId}")
RestResponse<Void> deleteBeer(@PathParam("beerId") int beerId);
```

This lets your implementation control HTTP status codes and headers:

```java
@Override
public RestResponse<Void> deleteBeer(int beerId) {
    beerService.delete(beerId);
    return RestResponse.noContent();
}
```

### Combining Reactive + Generic Return Type

Both can be used together:

```xml
<projectSettings>
    <javaPackage>com.example.api</javaPackage>
    <reactive>true</reactive>
    <genericReturnType>org.jboss.resteasy.reactive.RestResponse</genericReturnType>
</projectSettings>
```

Produces `CompletionStage<RestResponse<T>>` return types.

## Adding Lombok Annotations to All Beans

Use the `x-codegen.bean-annotations` extension in your OpenAPI spec to add Lombok (or any other) annotations globally:

```json
{
  "openapi": "3.0.3",
  "info": { "title": "My API", "version": "1.0.0" },
  "paths": { },
  "components": {
    "schemas": {
      "User": {
        "type": "object",
        "properties": {
          "name": { "type": "string" },
          "email": { "type": "string" }
        }
      }
    }
  },
  "x-codegen": {
    "bean-annotations": [
      "lombok.ToString",
      {
        "annotation": "lombok.EqualsAndHashCode",
        "excludeEnums": true
      }
    ]
  }
}
```

**Generated bean:**

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("jsonschema2pojo")
@lombok.ToString
@lombok.EqualsAndHashCode
public class User {
    // ...
}
```

The `excludeEnums: true` option on `@EqualsAndHashCode` means that annotation won't be added to generated enum classes.

## Per-Schema Annotations

Add annotations to specific schemas using `x-codegen-annotations`:

```json
{
  "components": {
    "schemas": {
      "Event": {
        "type": "object",
        "x-codegen-annotations": [
          "io.quarkus.runtime.annotations.RegisterForReflection"
        ],
        "properties": {
          "name": { "type": "string" }
        }
      }
    }
  }
}
```

## Schema Inheritance

Use `x-codegen-extendsClass` to make a generated bean extend an existing class:

```json
{
  "components": {
    "schemas": {
      "RuleViolationError": {
        "type": "object",
        "x-codegen-extendsClass": "com.example.api.beans.Error",
        "x-codegen-annotations": ["lombok.AllArgsConstructor", "lombok.Builder"],
        "properties": {
          "causes": {
            "type": "array",
            "items": { "$ref": "#/components/schemas/RuleViolationCause" }
          }
        }
      }
    }
  }
}
```

**Generated bean:**

```java
@lombok.AllArgsConstructor
@lombok.Builder
public class RuleViolationError extends Error {
    // Only properties specific to this subclass
}
```

## Context Root Prefix

Prepend a base path to all generated `@Path` annotations without modifying every path in your spec:

```json
{
  "paths": {
    "/users": {
      "get": { "operationId": "listUsers", "..." : "..." }
    },
    "/orders": {
      "get": { "operationId": "listOrders", "..." : "..." }
    },
    "x-codegen-contextRoot": "/api/v2"
  }
}
```

**Generated interfaces:**

```java
@Path("/api/v2/users")
public interface UsersResource { }

@Path("/api/v2/orders")
public interface OrdersResource { }
```

## Custom Return Type per Operation

Override the return type for a specific operation using `x-codegen-returnType` on the media type:

```json
{
  "paths": {
    "/files/{fileId}": {
      "get": {
        "operationId": "downloadFile",
        "responses": {
          "200": {
            "content": {
              "application/octet-stream": {
                "schema": { "type": "string", "format": "binary" },
                "x-codegen-returnType": "jakarta.ws.rs.core.Response"
              }
            }
          }
        }
      }
    }
  }
}
```

**Generated method:**

```java
@GET
@Path("/{fileId}")
@Produces("application/octet-stream")
Response downloadFile(@PathParam("fileId") String fileId);
```

## Selective Async Operations

Make specific operations async without enabling `reactive` globally:

```yaml
paths:
  /reports:
    post:
      operationId: generateReport
      x-codegen-async: true
      requestBody:
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/ReportRequest"
      responses:
        "202":
          description: Report generation started
  /health:
    get:
      operationId: healthCheck
      responses:
        "200":
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/HealthStatus"
```

**Generated interface:**

```java
// Async — returns CompletionStage
@POST
@Consumes("application/json")
CompletionStage<Void> generateReport(@NotNull ReportRequest data);

// Synchronous — returns directly
@GET
@Produces("application/json")
HealthStatus healthCheck();
```

## Class Name Prefix/Suffix

Namespace generated classes to avoid collisions when generating from multiple specs:

```xml
<!-- First API -->
<execution>
    <id>internal-api</id>
    <configuration>
        <projectSettings>
            <javaPackage>com.example.api</javaPackage>
            <classNamePrefix>Internal</classNamePrefix>
        </projectSettings>
        <inputSpec>internal-api.json</inputSpec>
        <outputDir>${project.build.directory}/generated-sources/internal</outputDir>
    </configuration>
</execution>

<!-- Second API -->
<execution>
    <id>public-api</id>
    <configuration>
        <projectSettings>
            <javaPackage>com.example.api</javaPackage>
            <classNamePrefix>Public</classNamePrefix>
        </projectSettings>
        <inputSpec>public-api.json</inputSpec>
        <outputDir>${project.build.directory}/generated-sources/public</outputDir>
    </configuration>
</execution>
```

Produces `InternalUsersResource`, `InternalUser` vs. `PublicUsersResource`, `PublicUser`.

## Bean Validation (JSR-303)

Enable validation annotations on generated beans based on OpenAPI schema constraints:

```xml
<projectSettings>
    <javaPackage>com.example.api</javaPackage>
    <useJsr303>true</useJsr303>
</projectSettings>
```

With this OpenAPI schema:

```yaml
User:
  type: object
  required: [name, email]
  properties:
    name:
      type: string
      minLength: 1
      maxLength: 100
    email:
      type: string
      pattern: "^[\\w.-]+@[\\w.-]+\\.\\w+$"
    age:
      type: integer
      minimum: 0
      maximum: 150
```

The generated bean includes validation annotations:

```java
public class User {

    @NotNull
    @Size(min = 1, max = 100)
    @JsonProperty("name")
    private String name;

    @NotNull
    @Pattern(regexp = "^[\\w.-]+@[\\w.-]+\\.\\w+$")
    @JsonProperty("email")
    private String email;

    @Min(0)
    @Max(150)
    @JsonProperty("age")
    private Integer age;
}
```

## Custom Date-Time Format

Override the default `@JsonFormat` pattern for a specific date-time property:

```json
{
  "components": {
    "schemas": {
      "Event": {
        "type": "object",
        "properties": {
          "createdAt": {
            "type": "string",
            "format": "date-time"
          },
          "scheduledAt": {
            "type": "string",
            "format": "date-time",
            "x-codegen-formatPattern": "yyyy-MM-dd HH:mm:ss"
          }
        }
      }
    }
  }
}
```

**Generated bean:**

```java
// Default pattern
@JsonFormat(shape = JsonFormat.Shape.STRING,
    pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
@JsonProperty("createdAt")
private Date createdAt;

// Custom pattern
@JsonFormat(shape = JsonFormat.Shape.STRING,
    pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
@JsonProperty("scheduledAt")
private Date scheduledAt;
```

To suppress date-time formatting entirely, use the global extension:

```json
{
  "x-codegen": {
    "suppress-date-time-formatting": true
  }
}
```

## Inline Simple Types

Prevent a simple schema from generating its own class by marking it as inline:

```json
{
  "components": {
    "schemas": {
      "UserId": {
        "type": "string",
        "x-codegen-inline": true
      },
      "User": {
        "type": "object",
        "properties": {
          "id": { "$ref": "#/components/schemas/UserId" },
          "name": { "type": "string" }
        }
      }
    }
  }
}
```

Instead of generating a `UserId` class, references to it are replaced with `String` directly.

## Custom Bean Package

Place specific beans in a different package:

```json
{
  "components": {
    "schemas": {
      "AuditEvent": {
        "type": "object",
        "x-codegen-package": "com.example.api.audit",
        "properties": {
          "action": { "type": "string" },
          "timestamp": { "type": "string", "format": "date-time" }
        }
      }
    }
  }
}
```

The `AuditEvent` class is generated in `com.example.api.audit` instead of the default `com.example.api.beans`.
