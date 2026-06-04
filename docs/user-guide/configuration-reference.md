# Configuration Reference

This page documents all configuration properties available in the Apitomy Codegen Maven plugin's `<projectSettings>` block.

## Project Settings

All properties are configured inside the `<projectSettings>` element of the Maven plugin configuration:

```xml
<plugin>
    <groupId>io.apitomy</groupId>
    <artifactId>apitomy-codegen-maven-plugin</artifactId>
    <version>1.3.0</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <projectSettings>
                    <!-- Properties go here -->
                </projectSettings>
                <inputSpec>src/main/resources/openapi.json</inputSpec>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Property Summary

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| [`javaPackage`](#javapackage) | `String` | `org.example.api` | Base Java package for generated code |
| [`reactive`](#reactive) | `boolean` | `false` | Wrap return types in `CompletionStage<>` |
| [`genericReturnType`](#genericreturntype) | `String` | `null` | Wrap return types in a custom generic type |
| [`classNamePrefix`](#classnameprefix) | `String` | `""` | Prefix added to all generated class names |
| [`classNameSuffix`](#classnamesuffix) | `String` | `""` | Suffix added to all generated class names |
| [`useJsr303`](#usejsr303) | `boolean` | `false` | Add Bean Validation annotations to generated beans |
| [`generateBuilders`](#generatebuilders) | `boolean` | `false` | Generate builder methods on bean classes |
| [`capitalizeEnumValues`](#capitalizeenumvalues) | `boolean` | `true` | Uppercase enum constant names |
| [`generatesOpenApi`](#generatesopenapi) | `boolean` | `true` | Add MicroProfile `@Operation` annotations |
| [`codeOnly`](#codeonly) | `boolean` | `true` | Generate only Java sources (no project scaffold) |
| [`includeSpec`](#includespec) | `boolean` | `false` | Include the OpenAPI spec file in output |
| [`mavenFileStructure`](#mavenfilestructure) | `boolean` | `false` | Use `src/main/java/` directory layout |
| [`groupId`](#groupid) | `String` | `org.example` | Maven groupId (when generating full project) |
| [`artifactId`](#artifactid) | `String` | `example-api` | Maven artifactId (when generating full project) |

!!! note "Default values"
    The defaults listed above are the **Maven plugin defaults**. When using the core library programmatically, some defaults differ (e.g. `codeOnly` defaults to `false`, `includeSpec` defaults to `true`, `mavenFileStructure` defaults to `true`).

---

## Property Details

### `javaPackage`

The base Java package for all generated interfaces and bean classes.

- **Type:** `String`
- **Default:** `org.example.api`

Generated interfaces are placed directly in this package. Bean classes are placed in a `beans` sub-package (e.g. `com.example.api.beans`).

```xml
<projectSettings>
    <javaPackage>com.example.myapi</javaPackage>
</projectSettings>
```

**Generated structure:**

```
com/example/myapi/
├── UsersResource.java
├── OrdersResource.java
└── beans/
    ├── User.java
    └── Order.java
```

Individual schemas can override their package using the [`x-codegen-package`](openapi-extensions.md#x-codegen-package) OpenAPI extension.

---

### `reactive`

When enabled, all generated interface method return types are wrapped in `CompletionStage<>`.

- **Type:** `boolean`
- **Default:** `false`

```xml
<projectSettings>
    <javaPackage>com.example.api</javaPackage>
    <reactive>true</reactive>
</projectSettings>
```

**Without `reactive`:**

```java
@GET
@Produces("application/json")
List<Beer> listAllBeers();
```

**With `reactive` enabled:**

```java
@GET
@Produces("application/json")
CompletionStage<List<Beer>> listAllBeers();
```

Methods that would return `void` become `CompletionStage<Void>`.

!!! tip
    When targeting Quarkus with reactive endpoints, consider using [`genericReturnType`](#genericreturntype) with `io.smallrye.mutiny.Uni` instead.

---

### `genericReturnType`

Wraps all generated interface method return types in a specified generic type. This is useful for framework-specific response wrappers.

- **Type:** `String`
- **Default:** `null` (disabled)

The value should be the fully qualified class name of a generic type that takes one type parameter.

```xml
<projectSettings>
    <javaPackage>com.example.api</javaPackage>
    <genericReturnType>org.jboss.resteasy.reactive.RestResponse</genericReturnType>
</projectSettings>
```

**Generated code:**

```java
@GET
@Produces("application/json")
RestResponse<List<Beer>> listAllBeers();

@DELETE
@Path("/{beerId}")
RestResponse<Void> deleteBeer(@PathParam("beerId") int beerId);
```

Can be combined with `reactive` for types like `CompletionStage<RestResponse<T>>`.

---

### `classNamePrefix`

A prefix added to all generated class names (interfaces and beans).

- **Type:** `String`
- **Default:** `""` (no prefix)

```xml
<projectSettings>
    <javaPackage>org.example.api</javaPackage>
    <classNamePrefix>Test</classNamePrefix>
</projectSettings>
```

**Effect:** `BeersResource` becomes `TestBeersResource`, `Beer` becomes `TestBeer`.

---

### `classNameSuffix`

A suffix added to all generated class names (interfaces and beans).

- **Type:** `String`
- **Default:** `""` (no suffix)

```xml
<projectSettings>
    <javaPackage>org.example.api</javaPackage>
    <classNameSuffix>Dto</classNameSuffix>
</projectSettings>
```

**Effect:** `BeersResource` becomes `BeersResourceDto`, `Beer` becomes `BeerDto`.

---

### `useJsr303`

When enabled, adds [Bean Validation](https://beanvalidation.org/) (JSR 303 / Jakarta Validation) annotations to generated bean properties based on OpenAPI schema constraints.

- **Type:** `boolean`
- **Default:** `false`

```xml
<projectSettings>
    <javaPackage>com.example.api</javaPackage>
    <useJsr303>true</useJsr303>
</projectSettings>
```

OpenAPI constraints are mapped to validation annotations:

| OpenAPI Constraint | Validation Annotation |
|---|---|
| `required` | `@NotNull` |
| `minLength` / `maxLength` | `@Size(min=, max=)` |
| `minimum` | `@Min` or `@DecimalMin` |
| `maximum` | `@Max` or `@DecimalMax` |
| `pattern` | `@Pattern` |
| `minimum` with `exclusiveMinimum` | `@DecimalMin(inclusive=false)` |
| `maximum` with `exclusiveMaximum` | `@DecimalMax(inclusive=false)` |

!!! note
    Validation annotations on interface method parameters (e.g. `@NotNull` on request bodies, `@Positive` from custom constraints) are always generated regardless of this setting.

---

### `generateBuilders`

When enabled, generates builder-pattern methods on bean classes in addition to the standard getters and setters.

- **Type:** `boolean`
- **Default:** `false`

```xml
<projectSettings>
    <javaPackage>com.example.api</javaPackage>
    <generateBuilders>true</generateBuilders>
</projectSettings>
```

---

### `capitalizeEnumValues`

Controls whether generated Java enum constants are converted to uppercase.

- **Type:** `boolean`
- **Default:** `true`

When `true` (default), an OpenAPI enum value like `"active"` becomes `ACTIVE` in the generated Java enum. When `false`, the original casing from the OpenAPI spec is preserved (with minimal sanitization for Java identifier rules).

```xml
<projectSettings>
    <capitalizeEnumValues>false</capitalizeEnumValues>
</projectSettings>
```

---

### `generatesOpenApi`

Controls whether MicroProfile OpenAPI `@Operation` annotations are added to generated JAX-RS interface methods.

- **Type:** `boolean`
- **Default:** `true`

When enabled, each generated method includes an `@Operation` annotation with the `summary`, `description`, and `operationId` from the OpenAPI spec:

```java
@Operation(
    description = "Returns all beers in the database.",
    summary = "Get All Beers",
    operationId = "listAllBeers"
)
@GET
@Produces("application/json")
List<Beer> listAllBeers();
```

Set to `false` if you don't use MicroProfile OpenAPI and want to avoid the dependency:

```xml
<projectSettings>
    <generatesOpenApi>false</generatesOpenApi>
</projectSettings>
```

---

### `codeOnly`

Controls whether only Java source code is generated, or a complete project scaffold including `pom.xml` and supporting files.

- **Type:** `boolean`
- **Default:** `true` (Maven plugin), `false` (core library)

When `true`, only Java interfaces and bean classes are generated. When `false`, the output also includes a `pom.xml`, and for Quarkus targets: Dockerfiles, `application.properties`, Maven wrapper, `.gitignore`, and a README.

```xml
<projectSettings>
    <codeOnly>true</codeOnly>
</projectSettings>
```

!!! tip
    When using the Maven plugin, `codeOnly` should almost always be `true` since you already have your own `pom.xml`. Full project generation is intended for scaffolding new projects from scratch.

---

### `includeSpec`

Controls whether the original OpenAPI specification file is included in the generated output.

- **Type:** `boolean`
- **Default:** `false` (Maven plugin), `true` (core library)

When enabled, the OpenAPI spec is written to `META-INF/openapi.json` (or `src/main/resources/META-INF/openapi.json` when `mavenFileStructure` is `true`).

```xml
<projectSettings>
    <includeSpec>true</includeSpec>
</projectSettings>
```

This is useful when using frameworks like Quarkus with SmallRye OpenAPI, which can serve the spec at runtime from `META-INF/openapi.json`.

---

### `mavenFileStructure`

Controls whether generated files use Maven's standard directory layout (`src/main/java/...`).

- **Type:** `boolean`
- **Default:** `false` (Maven plugin), `true` (core library)

When `false`, Java files are placed directly under the package directory structure. When `true`, the output includes the `src/main/java/` prefix.

```xml
<projectSettings>
    <mavenFileStructure>true</mavenFileStructure>
</projectSettings>
```

!!! tip
    When using the Maven plugin with a custom `<outputDir>`, keep this `false` (the default) since the plugin already sets the output directory as a compile source root.

---

### `groupId`

The Maven `groupId` used when generating a full project scaffold (`codeOnly=false`).

- **Type:** `String`
- **Default:** `org.example`

```xml
<projectSettings>
    <codeOnly>false</codeOnly>
    <groupId>com.example</groupId>
</projectSettings>
```

---

### `artifactId`

The Maven `artifactId` used when generating a full project scaffold (`codeOnly=false`).

- **Type:** `String`
- **Default:** `example-api`

```xml
<projectSettings>
    <codeOnly>false</codeOnly>
    <artifactId>my-api</artifactId>
</projectSettings>
```

---

## Plugin-Level Configuration

In addition to `<projectSettings>`, the Maven plugin accepts these top-level configuration properties:

### `inputSpec`

**(Required)** Path to the OpenAPI specification file (JSON or YAML).

- **Type:** `File`
- Can be an absolute path or relative to the Maven project base directory.

```xml
<configuration>
    <inputSpec>src/main/resources/openapi.json</inputSpec>
</configuration>
```

### `outputDir`

The directory where generated code is written.

- **Type:** `File`
- **Default:** `${project.build.directory}/generated-sources/jaxrs`

The plugin automatically registers this directory as a compile source root.

```xml
<configuration>
    <outputDir>${project.build.directory}/generated-sources/apitomy</outputDir>
</configuration>
```
