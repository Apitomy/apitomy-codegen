# Apitomy Codegen

Welcome to **Apitomy Codegen** — an open source API design code generator that produces high-quality Java code from OpenAPI specifications.

## What is Apitomy Codegen?

Apitomy Codegen reads an OpenAPI 3.x specification and generates:

- **JAX-RS interfaces** — fully annotated resource interfaces ready for you to implement
- **Data model beans** — Jackson-annotated Java classes from your schema definitions
- **Quarkus-optimized projects** — optional full project scaffolding with Dockerfiles, config, and Maven wrapper

You write the OpenAPI spec and the business logic. Apitomy Codegen handles the boilerplate.

## Key Features

- **OpenAPI 3.x Support** — full support for modern OpenAPI specifications
- **Interface-Based Design** — generated interfaces define the API contract; you implement the behavior
- **Maven Plugin** — integrates code generation into your build lifecycle
- **Highly Configurable** — 14+ project settings, 10+ OpenAPI extensions for fine-grained control
- **Reactive Support** — optional `CompletionStage<>` wrapping for async endpoints
- **Bean Validation** — optional JSR-303 annotations from OpenAPI constraints
- **Production Ready** — battle-tested in enterprise environments

## Project Structure

- **`core/`** — core code generation engine (can also be used as a library)
- **`maven-plugin/`** — Maven plugin for build integration
- **`maven-plugin-tests/`** — integration tests for the Maven plugin

## Quick Links

- [Getting Started](getting-started.md) — generate your first API in minutes
- [Maven Plugin](user-guide/maven-plugin.md) — plugin setup and configuration
- [Configuration Reference](user-guide/configuration-reference.md) — all project settings
- [OpenAPI Extensions](user-guide/openapi-extensions.md) — customize generation from your spec
- [Generated Output](user-guide/generated-output.md) — what the generator produces
- [Examples](user-guide/examples.md) — practical recipes for common use cases
- [GitHub Repository](https://github.com/Apitomy/apitomy-codegen) — source code and issues

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](https://github.com/Apitomy/apitomy-codegen/blob/main/LICENSE) file for details.

## Community

- **Issues**: Report bugs and request features on [GitHub Issues](https://github.com/Apitomy/apitomy-codegen/issues)
- **Discussions**: [GitHub Discussions](https://github.com/Apitomy/apitomy-codegen/discussions)
- **Contributing**: See the [Contributing Guidelines](https://github.com/Apitomy/apitomy-codegen/blob/main/CONTRIBUTING.md)

---

*Part of the [Apitomy](https://www.apitomy.io) project. Licensed under the Apache License 2.0.*
