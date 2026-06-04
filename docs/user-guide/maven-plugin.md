# Maven Plugin

The Apitomy Codegen Maven plugin integrates code generation into your Maven build lifecycle, generating JAX-RS interfaces and data model beans from an OpenAPI specification.

## Maven Coordinates

```xml
<plugin>
    <groupId>io.apitomy</groupId>
    <artifactId>apitomy-codegen-maven-plugin</artifactId>
    <version>1.3.0</version>
</plugin>
```

## Goal: `generate`

The plugin provides a single goal, `generate`, which reads an OpenAPI specification file and produces Java source code.

**Default lifecycle phase:** none (must be explicitly bound, typically to `generate-sources`)

## Basic Configuration

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
                    <javaPackage>com.example.api</javaPackage>
                </projectSettings>
                <inputSpec>src/main/resources/openapi.json</inputSpec>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Configuration Parameters

### `inputSpec` (required)

Path to the OpenAPI specification file. Supports JSON and YAML formats. Can be an absolute path or relative to the Maven project base directory.

```xml
<inputSpec>src/main/resources/openapi.json</inputSpec>
```

### `outputDir`

Directory where generated source code is written. The plugin automatically registers this directory as a compile source root, so generated code is included in compilation.

**Default:** `${project.build.directory}/generated-sources/jaxrs`

```xml
<outputDir>${project.build.directory}/generated-sources/apitomy</outputDir>
```

### `projectSettings`

Configuration block controlling what code is generated and how. See the [Configuration Reference](configuration-reference.md) for all available properties.

**Minimal example** (only `javaPackage` is typically needed):

```xml
<projectSettings>
    <javaPackage>com.example.api</javaPackage>
</projectSettings>
```

**Full example** with all properties shown:

```xml
<projectSettings>
    <javaPackage>com.example.api</javaPackage>
    <reactive>false</reactive>
    <genericReturnType>org.jboss.resteasy.reactive.RestResponse</genericReturnType>
    <classNamePrefix></classNamePrefix>
    <classNameSuffix></classNameSuffix>
    <useJsr303>true</useJsr303>
    <generateBuilders>false</generateBuilders>
    <capitalizeEnumValues>true</capitalizeEnumValues>
    <generatesOpenApi>true</generatesOpenApi>
    <codeOnly>true</codeOnly>
    <includeSpec>false</includeSpec>
    <mavenFileStructure>false</mavenFileStructure>
    <groupId>com.example</groupId>
    <artifactId>my-api</artifactId>
</projectSettings>
```

## Required Dependencies

The generated code uses annotations from several libraries. Add these dependencies to your project:

=== "Standalone JAX-RS"

    ```xml
    <dependencies>
        <dependency>
            <groupId>jakarta.ws.rs</groupId>
            <artifactId>jakarta.ws.rs-api</artifactId>
            <version>4.0.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
            <version>2.18.0</version>
        </dependency>
        <dependency>
            <groupId>org.eclipse.microprofile.openapi</groupId>
            <artifactId>microprofile-openapi-api</artifactId>
            <version>4.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
            <version>3.1.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
    ```

=== "Quarkus"

    ```xml
    <dependencies>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-rest</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-rest-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>
    </dependencies>
    ```

!!! note "MicroProfile OpenAPI"
    If you set `generatesOpenApi` to `false`, the MicroProfile OpenAPI dependency is not required.

## Multiple Specifications

To generate code from multiple OpenAPI specs, add multiple `<execution>` blocks. Use separate output directories or Java packages to avoid class name collisions:

```xml
<plugin>
    <groupId>io.apitomy</groupId>
    <artifactId>apitomy-codegen-maven-plugin</artifactId>
    <version>1.3.0</version>
    <executions>
        <execution>
            <id>generate-users-api</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <projectSettings>
                    <javaPackage>com.example.users</javaPackage>
                </projectSettings>
                <inputSpec>src/main/resources/users-api.json</inputSpec>
                <outputDir>${project.build.directory}/generated-sources/users</outputDir>
            </configuration>
        </execution>
        <execution>
            <id>generate-orders-api</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <projectSettings>
                    <javaPackage>com.example.orders</javaPackage>
                </projectSettings>
                <inputSpec>src/main/resources/orders-api.json</inputSpec>
                <outputDir>${project.build.directory}/generated-sources/orders</outputDir>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Build Lifecycle Integration

The plugin is typically bound to the `generate-sources` phase so that generated code is available for compilation:

```
validate → generate-sources (codegen runs here) → compile → test → package
```

When using with Quarkus, make sure the Apitomy Codegen plugin is declared **before** the Quarkus Maven plugin in your `pom.xml`, so code is generated before Quarkus processes it.

## IDE Support

The generated code is placed in `target/generated-sources/jaxrs` by default. Most IDEs (IntelliJ IDEA, Eclipse, VS Code) automatically recognize Maven-registered source roots. If your IDE doesn't pick up the generated sources:

- **IntelliJ IDEA:** Right-click `target/generated-sources/jaxrs` → Mark Directory as → Generated Sources Root
- **Eclipse:** Run Maven → Update Project (Alt+F5)
- **VS Code:** The Java extension picks up source roots automatically after a Maven reload
