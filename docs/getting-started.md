# Getting Started

This guide walks you through generating Java code from an OpenAPI specification using the Apitomy Codegen Maven plugin.

## Prerequisites

- **Java 11 or later** (tested with Java 11, 17, and 21)
- **Maven 3.6+**

## Step 1: Write an OpenAPI Specification

Create a file called `src/main/resources/openapi.json`:

```json
{
  "openapi": "3.0.3",
  "info": {
    "title": "Beer API",
    "version": "1.0.0"
  },
  "paths": {
    "/beers": {
      "get": {
        "operationId": "listBeers",
        "summary": "List all beers",
        "responses": {
          "200": {
            "description": "A list of beers",
            "content": {
              "application/json": {
                "schema": {
                  "type": "array",
                  "items": { "$ref": "#/components/schemas/Beer" }
                }
              }
            }
          }
        }
      },
      "post": {
        "operationId": "addBeer",
        "summary": "Add a beer",
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "schema": { "$ref": "#/components/schemas/Beer" }
            }
          }
        },
        "responses": {
          "201": { "description": "Beer created" }
        }
      }
    },
    "/beers/{beerId}": {
      "get": {
        "operationId": "getBeer",
        "summary": "Get a beer by ID",
        "parameters": [
          {
            "name": "beerId",
            "in": "path",
            "required": true,
            "schema": { "type": "integer" }
          }
        ],
        "responses": {
          "200": {
            "description": "A single beer",
            "content": {
              "application/json": {
                "schema": { "$ref": "#/components/schemas/Beer" }
              }
            }
          }
        }
      }
    }
  },
  "components": {
    "schemas": {
      "Beer": {
        "type": "object",
        "required": ["name", "style"],
        "properties": {
          "id": { "type": "integer" },
          "name": { "type": "string" },
          "style": { "type": "string" },
          "abv": { "type": "number", "format": "double" }
        }
      }
    }
  }
}
```

## Step 2: Add the Maven Plugin

Add the Apitomy Codegen plugin to your `pom.xml`:

```xml
<build>
    <plugins>
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
                            <javaPackage>com.example.beer</javaPackage>
                        </projectSettings>
                        <inputSpec>src/main/resources/openapi.json</inputSpec>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

You'll also need these dependencies for the generated code to compile:

```xml
<dependencies>
    <!-- JAX-RS API -->
    <dependency>
        <groupId>jakarta.ws.rs</groupId>
        <artifactId>jakarta.ws.rs-api</artifactId>
        <version>4.0.0</version>
        <scope>provided</scope>
    </dependency>

    <!-- Jackson annotations (used by generated beans) -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-annotations</artifactId>
        <version>2.18.0</version>
    </dependency>

    <!-- MicroProfile OpenAPI annotations (used by generated interfaces) -->
    <dependency>
        <groupId>org.eclipse.microprofile.openapi</groupId>
        <artifactId>microprofile-openapi-api</artifactId>
        <version>4.0</version>
        <scope>provided</scope>
    </dependency>

    <!-- Bean Validation API (for @NotNull on request bodies) -->
    <dependency>
        <groupId>jakarta.validation</groupId>
        <artifactId>jakarta.validation-api</artifactId>
        <version>3.1.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

!!! tip "Using Quarkus?"
    If you're using Quarkus, these dependencies are already managed by the Quarkus BOM. See [Using with Quarkus](#using-with-quarkus) below.

## Step 3: Generate Code

```bash
mvn clean generate-sources
```

The plugin generates code into `target/generated-sources/jaxrs/` and automatically registers it as a compile source root:

```
target/generated-sources/jaxrs/
└── com/example/beer/
    ├── BeersResource.java          # JAX-RS interface
    └── beans/
        └── Beer.java               # Data model bean
```

### Generated Interface

The generated `BeersResource.java` is a JAX-RS **interface** with all the annotations and method signatures derived from your OpenAPI spec:

```java
@Path("/beers")
public interface BeersResource {

    @Operation(summary = "List all beers", operationId = "listBeers")
    @GET
    @Produces("application/json")
    List<Beer> listBeers();

    @Operation(summary = "Add a beer", operationId = "addBeer")
    @POST
    @Consumes("application/json")
    void addBeer(@NotNull Beer data);

    @Operation(summary = "Get a beer by ID", operationId = "getBeer")
    @Path("/{beerId}")
    @GET
    @Produces("application/json")
    Beer getBeer(@PathParam("beerId") int beerId);
}
```

### Generated Bean

The `Beer.java` bean class includes Jackson annotations for JSON serialization:

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "name", "style", "abv"})
@Generated("jsonschema2pojo")
public class Beer {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("style")
    private String style;

    @JsonProperty("abv")
    private Double abv;

    // Getters and setters...
}
```

## Step 4: Implement the Interface

Create an implementation class that implements the generated interface:

```java
package com.example.beer;

import com.example.beer.beans.Beer;
import java.util.*;

public class BeersResourceImpl implements BeersResource {

    private final Map<Integer, Beer> beers = new LinkedHashMap<>();
    private int nextId = 1;

    @Override
    public List<Beer> listBeers() {
        return new ArrayList<>(beers.values());
    }

    @Override
    public void addBeer(Beer data) {
        data.setId(nextId++);
        beers.put(data.getId(), data);
    }

    @Override
    public Beer getBeer(int beerId) {
        return beers.get(beerId);
    }
}
```

Your implementation handles the business logic while the generated interface handles the JAX-RS wiring — paths, HTTP methods, content types, and parameter binding are all derived from the OpenAPI spec.

## Using with Quarkus

Quarkus is the recommended runtime for Apitomy Codegen. Here's a minimal `pom.xml` for a Quarkus project:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                           http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>beer-api</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <quarkus.platform.version>3.17.0</quarkus.platform.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.quarkus.platform</groupId>
                <artifactId>quarkus-bom</artifactId>
                <version>${quarkus.platform.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

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

    <build>
        <plugins>
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
                                <javaPackage>com.example.beer</javaPackage>
                            </projectSettings>
                            <inputSpec>src/main/resources/openapi.json</inputSpec>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>io.quarkus.platform</groupId>
                <artifactId>quarkus-maven-plugin</artifactId>
                <version>${quarkus.platform.version}</version>
                <extensions>true</extensions>
                <executions>
                    <execution>
                        <goals>
                            <goal>build</goal>
                            <goal>generate-code</goal>
                            <goal>generate-code-tests</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

Make your implementation a CDI bean:

```java
package com.example.beer;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BeersResourceImpl implements BeersResource {
    // ... implementation
}
```

Run with Quarkus dev mode:

```bash
mvn quarkus:dev
```

Test it:

```bash
curl http://localhost:8080/beers
```

## What's Next?

- **[Maven Plugin Reference](user-guide/maven-plugin.md)** — all plugin configuration options
- **[Configuration Reference](user-guide/configuration-reference.md)** — detailed documentation of all project settings
- **[OpenAPI Extensions](user-guide/openapi-extensions.md)** — customize code generation from your API contract
- **[Generated Output](user-guide/generated-output.md)** — understand what the generator produces
- **[Examples](user-guide/examples.md)** — practical recipes for common use cases
