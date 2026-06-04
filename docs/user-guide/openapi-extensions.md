# OpenAPI Extensions Reference

Apitomy Codegen uses [OpenAPI specification extensions](https://spec.openapis.org/oas/v3.0.3.html#specification-extensions) (vendor extensions) to customize code generation directly from your API contract.

All extensions use the `x-codegen` prefix.

## Extension Summary

| Extension | Placement | Description |
|-----------|-----------|-------------|
| [`x-codegen`](#x-codegen) | Document root | Global codegen configuration block |
| [`x-codegen-contextRoot`](#x-codegen-contextroot) | `paths` object | Base path prefix for all generated JAX-RS resources |
| [`x-codegen-package`](#x-codegen-package) | Schema definition | Custom Java package for a specific bean class |
| [`x-codegen-annotations`](#x-codegen-annotations) | Schema definition | Extra Java annotations on a specific bean class |
| [`x-codegen-extendsClass`](#x-codegen-extendsclass) | Schema definition | Superclass for a generated bean class |
| [`x-codegen-type`](#x-codegen-type) | Schema definition | Override the Java type for a schema (map types) |
| [`x-codegen-inline`](#x-codegen-inline) | Schema definition | Inline a schema instead of generating a separate class |
| [`x-codegen-async`](#x-codegen-async) | Operation | Mark an operation's return type as asynchronous |
| [`x-codegen-returnType`](#x-codegen-returntype) | Media type (in response) | Override the return type for a specific operation |
| [`x-codegen-formatPattern`](#x-codegen-formatpattern) | Schema property (`date-time`) | Custom date-time format pattern |

---

## Global Configuration

### `x-codegen`

A root-level configuration block that controls global code generation behavior. This extension is placed at the top level of the OpenAPI document (alongside `openapi`, `info`, `paths`, etc.).

**Sub-properties:**

| Property | Type | Description |
|----------|------|-------------|
| `bean-annotations` | Array | Annotations to add to all generated bean classes |
| `contextRoot` | String | Global context root path (alternative to `x-codegen-contextRoot` on `paths`) |
| `suppress-date-time-formatting` | Boolean | Disable `@JsonFormat` on date-time fields |

#### `x-codegen.bean-annotations`

Adds Java annotations to **all** generated bean classes. Each array element can be either:

- A **string** — the fully qualified annotation (e.g. `"lombok.ToString"`)
- An **object** with `annotation` and optional `excludeEnums` properties

=== "JSON"

    ```json
    {
      "x-codegen": {
        "bean-annotations": [
          "jakarta.enterprise.context.ApplicationScoped",
          {
            "annotation": "lombok.ToString(callSuper=true, includeFieldNames=true)",
            "excludeEnums": true
          }
        ]
      }
    }
    ```

=== "YAML"

    ```yaml
    x-codegen:
      bean-annotations:
        - jakarta.enterprise.context.ApplicationScoped
        - annotation: "lombok.ToString(callSuper=true, includeFieldNames=true)"
          excludeEnums: true
    ```

**Generated bean class:**

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("jsonschema2pojo")
@jakarta.enterprise.context.ApplicationScoped
@lombok.ToString(callSuper=true, includeFieldNames=true)
public class FooBean {
    // ...
}
```

When `excludeEnums` is `true`, the annotation is not added to generated Java enum classes.

#### `x-codegen.contextRoot`

Defines a global base path prefix that is prepended to all generated JAX-RS resource `@Path` annotations. This is an alternative to using [`x-codegen-contextRoot`](#x-codegen-contextroot) on the `paths` object.

```json
{
  "x-codegen": {
    "contextRoot": "/apis/studio/v1"
  }
}
```

**Generated resource:**

```java
@Path("/apis/studio/v1/users")
public interface UsersResource {
    // ...
}
```

#### `x-codegen.suppress-date-time-formatting`

By default, Apitomy Codegen generates `@JsonFormat` annotations on properties of type `string` with format `date-time`:

```java
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
@JsonProperty("shipDate")
private Date shipDate;
```

Set `suppress-date-time-formatting` to `true` to remove these `@JsonFormat` annotations:

```json
{
  "x-codegen": {
    "suppress-date-time-formatting": true
  }
}
```

---

## Path-Level Extensions

### `x-codegen-contextRoot`

Placed on the `paths` object, this extension defines a base path prefix that is prepended to all generated JAX-RS resource `@Path` annotations.

=== "JSON"

    ```json
    {
      "paths": {
        "/widgets": {
          "get": { "..." : "..." }
        },
        "x-codegen-contextRoot": "/api/v3"
      }
    }
    ```

=== "YAML"

    ```yaml
    paths:
      /widgets:
        get: ...
      x-codegen-contextRoot: /api/v3
    ```

**Without context root:**

```java
@Path("/widgets")
public interface WidgetsResource { }
```

**With context root `/api/v3`:**

```java
@Path("/api/v3/widgets")
public interface WidgetsResource { }
```

!!! tip
    Use this to align generated code with your application's deployment path without modifying every path in the OpenAPI spec.

---

## Schema-Level Extensions

### `x-codegen-package`

Overrides the Java package for a specific generated bean class. By default, beans are placed in the `beans` sub-package of your configured `javaPackage`.

```json
{
  "components": {
    "schemas": {
      "AuditEvent": {
        "type": "object",
        "x-codegen-package": "com.example.api.audit",
        "properties": {
          "action": { "type": "string" }
        }
      }
    }
  }
}
```

**Effect:** The `AuditEvent` class is generated in `com.example.api.audit` instead of the default `com.example.api.beans`.

---

### `x-codegen-annotations`

Adds Java annotations to a **specific** bean class. This is the per-schema equivalent of the global `x-codegen.bean-annotations`.

The value is an array of fully qualified annotation class names:

```json
{
  "components": {
    "schemas": {
      "MyBean": {
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

**Generated bean:**

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("jsonschema2pojo")
@io.quarkus.runtime.annotations.RegisterForReflection
public class MyBean {
    // ...
}
```

!!! note
    Per-schema annotations are applied in addition to any global `x-codegen.bean-annotations`.

---

### `x-codegen-extendsClass`

Makes a generated bean class extend a specified superclass.

```json
{
  "components": {
    "schemas": {
      "RuleViolationError": {
        "type": "object",
        "x-codegen-extendsClass": "com.example.api.beans.Error",
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
public class RuleViolationError extends Error {
    // Only properties specific to RuleViolationError
}
```

Can be combined with `x-codegen-annotations`:

```json
{
  "MySchema": {
    "type": "object",
    "x-codegen-extendsClass": "org.example.api.beans.AbstractSchema",
    "x-codegen-annotations": ["lombok.AllArgsConstructor", "lombok.Builder"],
    "properties": { "..." : "..." }
  }
}
```

---

### `x-codegen-type`

Overrides the Java type mapping for a schema. This is primarily used for map types where you want to use a specific Java map implementation.

Supported type values:

| Extension Value | Java Type |
|----------------|-----------|
| `map` | `java.util.Map<String, Object>` |

```json
{
  "components": {
    "schemas": {
      "Metadata": {
        "type": "object",
        "x-codegen-type": "map",
        "additionalProperties": true
      }
    }
  }
}
```

---

### `x-codegen-inline`

Marks a schema definition as "inline", meaning it will not generate a separate bean class. Instead, its type will be inlined wherever it is referenced.

```json
{
  "components": {
    "schemas": {
      "SimpleId": {
        "type": "string",
        "x-codegen-inline": true
      }
    }
  }
}
```

When `SimpleId` is referenced elsewhere (e.g. as a property type or parameter type), it will be replaced with `String` directly rather than generating a `SimpleId` class.

This is useful for trivial wrapper types that don't warrant their own class.

---

## Operation-Level Extensions

### `x-codegen-async`

Marks an individual operation's return type as asynchronous by wrapping it in `CompletionStage<>`. This is the per-operation equivalent of the global [`reactive`](configuration-reference.md#reactive) setting.

Place this extension on the operation object:

=== "JSON"

    ```json
    {
      "paths": {
        "/items": {
          "get": {
            "operationId": "listItems",
            "x-codegen-async": true,
            "responses": {
              "200": {
                "content": {
                  "application/json": {
                    "schema": {
                      "type": "array",
                      "items": { "$ref": "#/components/schemas/Item" }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    ```

=== "YAML"

    ```yaml
    paths:
      /items:
        get:
          operationId: listItems
          x-codegen-async: true
          responses:
            "200":
              content:
                application/json:
                  schema:
                    type: array
                    items:
                      $ref: "#/components/schemas/Item"
    ```

**Generated interface method:**

```java
@GET
@Produces("application/json")
CompletionStage<List<Item>> listItems();
```

!!! tip
    Use `x-codegen-async` when you want async behavior on specific operations rather than enabling [`reactive`](configuration-reference.md#reactive) globally.

---

### `x-codegen-returnType`

Overrides the Java return type of a generated JAX-RS interface method. Place this extension on the **media type object** inside a response.

```json
{
  "paths": {
    "/widgets/{widgetId}": {
      "get": {
        "operationId": "getWidget",
        "responses": {
          "200": {
            "content": {
              "application/json": {
                "schema": { "$ref": "#/components/schemas/Widget" },
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

**Generated interface method:**

```java
@GET
@Path("/{widgetId}")
@Produces("application/json")
Response getWidget(@PathParam("widgetId") String widgetId);
```

This is useful when you need full control over the HTTP response (status codes, headers, etc.) for specific operations.

---

## Property-Level Extensions

### `x-codegen-formatPattern`

Overrides the default date-time format pattern for a specific `date-time` property.

By default, `date-time` properties are annotated with:

```java
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
```

Use `x-codegen-formatPattern` to change the pattern:

```json
{
  "components": {
    "schemas": {
      "Event": {
        "type": "object",
        "properties": {
          "scheduledAt": {
            "type": "string",
            "format": "date-time",
            "x-codegen-formatPattern": "yyyy-MM-dd HH:mm:ss.SSSZ"
          }
        }
      }
    }
  }
}
```

**Generated property:**

```java
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSSZ", timezone = "UTC")
@JsonProperty("scheduledAt")
private Date scheduledAt;
```

!!! note
    To suppress date-time formatting entirely (remove all `@JsonFormat` annotations), use the global [`x-codegen.suppress-date-time-formatting`](#x-codegensuppress-date-time-formatting) setting instead.
