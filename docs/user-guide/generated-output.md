# Generated Output

This page explains what Apitomy Codegen generates and how the generated code is structured.

## Overview

Apitomy Codegen reads your OpenAPI specification and produces:

1. **JAX-RS Interfaces** — one per resource (grouped by the first path segment)
2. **Bean Classes** — one per schema defined in `#/components/schemas`
3. **OpenAPI spec file** — optionally included as `META-INF/openapi.json`

When `codeOnly` is `false`, additional project scaffold files are also generated (see [Full Project Generation](#full-project-generation)).

## JAX-RS Interfaces

Each group of paths that share a common first segment produces a separate Java interface. For example, paths `/beers`, `/beers/{beerId}`, and `/beers/{beerId}/reviews` all produce a single `BeersResource` interface.

### Interface Characteristics

- Interfaces are placed in the configured `javaPackage` (e.g. `com.example.api`)
- Each method corresponds to an OpenAPI operation
- Method names come from `operationId` in the spec
- JAX-RS annotations (`@GET`, `@POST`, `@Path`, `@Produces`, `@Consumes`) are derived from the operation
- MicroProfile `@Operation` annotations include `summary`, `description`, and `operationId` (unless `generatesOpenApi` is `false`)

### Example

Given this OpenAPI path:

```yaml
/beers/{beerId}:
  get:
    operationId: getBeer
    summary: Get Info About a Beer
    description: Returns full information about a single beer.
    parameters:
      - name: beerId
        in: path
        required: true
        schema:
          type: integer
    responses:
      "200":
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/Beer"
```

Apitomy generates:

```java
@Path("/beers")
public interface BeersResource {

    @Operation(
        description = "Returns full information about a single beer.",
        summary = "Get Info About a Beer",
        operationId = "getBeer"
    )
    @Path("/{beerId}")
    @GET
    @Produces("application/json")
    Beer getBeer(@PathParam("beerId") int beerId);
}
```

### Parameter Handling

| OpenAPI Parameter | Generated Code |
|-------------------|----------------|
| `in: path` | `@PathParam("name")` |
| `in: query` | `@QueryParam("name")` |
| `in: header` | `@HeaderParam("name")` |
| Request body (`required: true`) | `@NotNull TypeName data` |
| Request body (`required: false`) | `TypeName data` |

### Return Types

By default, methods return the Java type mapped from the response schema. This can be modified with:

- **[`reactive`](configuration-reference.md#reactive)** — wraps in `CompletionStage<T>`
- **[`genericReturnType`](configuration-reference.md#genericreturntype)** — wraps in a custom generic type like `RestResponse<T>`
- **[`x-codegen-async`](openapi-extensions.md#x-codegen-async)** — makes individual operations async
- **[`x-codegen-returnType`](openapi-extensions.md#x-codegen-returntype)** — overrides the return type for a specific operation

### Implementing Interfaces

You provide the business logic by implementing the generated interface:

```java
@ApplicationScoped
public class BeersResourceImpl implements BeersResource {

    @Override
    public Beer getBeer(int beerId) {
        // Your implementation here
    }
}
```

The framework (e.g. Quarkus, WildFly) discovers your implementation class and handles HTTP routing based on the JAX-RS annotations from the interface.

## Bean Classes

Each schema in `#/components/schemas` that has `type: object` generates a Java bean class. Schemas with `type: string` and an `enum` array generate Java enum classes.

### Bean Characteristics

- Beans are placed in a `beans` sub-package (e.g. `com.example.api.beans`)
- Generated using the [jsonschema2pojo](https://www.jsonschema2pojo.org/) library
- Include Jackson annotations for JSON serialization: `@JsonProperty`, `@JsonInclude`, `@JsonPropertyOrder`
- Include `@Generated("jsonschema2pojo")` annotation
- Follow standard JavaBean conventions with getters and setters

### Type Mapping

| OpenAPI Type | Format | Java Type |
|-------------|--------|-----------|
| `string` | — | `String` |
| `string` | `date` | `Date` |
| `string` | `date-time` | `Date` (with `@JsonFormat`) |
| `string` | `byte` | `byte[]` |
| `integer` | — | `Integer` |
| `integer` | `int32` | `Integer` |
| `integer` | `int64` | `Long` |
| `number` | — | `Double` |
| `number` | `float` | `Float` |
| `number` | `double` | `Double` |
| `boolean` | — | `Boolean` |
| `array` | — | `List<T>` |
| `object` | — | Generated bean class |
| `$ref` | — | Referenced bean class |

### Required Properties

Properties listed in the schema's `required` array are annotated with a `(Required)` Javadoc comment. When [`useJsr303`](configuration-reference.md#usejsr303) is enabled, they also get `@NotNull` annotations.

### Enum Generation

String schemas with `enum` values generate Java enums:

```yaml
Status:
  type: string
  enum: [active, inactive, pending]
```

Generates:

```java
public enum Status {
    ACTIVE, INACTIVE, PENDING
}
```

Enum constant casing is controlled by [`capitalizeEnumValues`](configuration-reference.md#capitalizeenumvalues).

### Customizing Beans

Beans can be customized using:

- **[`x-codegen-annotations`](openapi-extensions.md#x-codegen-annotations)** — add annotations to specific beans
- **[`x-codegen.bean-annotations`](openapi-extensions.md#x-codegenbean-annotations)** — add annotations to all beans globally
- **[`x-codegen-extendsClass`](openapi-extensions.md#x-codegen-extendsclass)** — specify a superclass
- **[`x-codegen-package`](openapi-extensions.md#x-codegen-package)** — override the package
- **[`generateBuilders`](configuration-reference.md#generatebuilders)** — add builder methods

## Multipart Form Data

When an operation's request body uses `multipart/form-data` content type, the generator creates a dedicated bean class for the multipart form fields rather than using individual parameters.

## Output Directory Structure

### Code-Only Mode (default)

With `codeOnly=true` (the Maven plugin default), only Java source files are generated:

```
<outputDir>/
└── com/example/api/
    ├── BeersResource.java
    ├── BreweriesResource.java
    └── beans/
        ├── Beer.java
        ├── Brewery.java
        └── Style.java
```

### With `includeSpec=true`

```
<outputDir>/
├── com/example/api/
│   ├── BeersResource.java
│   └── beans/
│       └── Beer.java
└── META-INF/
    └── openapi.json
```

## Full Project Generation

When `codeOnly` is `false`, the generator produces a complete, runnable project. This mode is primarily used for scaffolding new projects rather than for Maven plugin integration.

### JAX-RS Target

Generates a standard JAX-RS project:

- `pom.xml` with required dependencies
- `JaxRsApplication.java` — JAX-RS application class
- All interfaces and beans
- OpenAPI spec in `META-INF/openapi.json`

### Quarkus Target

Generates a Quarkus-ready project with additional files:

- `pom.xml` with Quarkus BOM and dependencies
- `src/main/resources/application.properties`
- `src/main/docker/Dockerfile.jvm`, `.native`, `.legacy-jar`, `.native-micro`
- Maven wrapper (`mvnw`, `mvnw.cmd`)
- `.gitignore`, `.dockerignore`
- `README.md`
