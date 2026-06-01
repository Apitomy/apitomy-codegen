[![Verify Build Workflow](https://github.com/Apitomy/apitomy-codegen/actions/workflows/verify.yaml/badge.svg)](https://github.com/Apitomy/apitomy-codegen/actions/workflows/verify.yaml?query=branch%3Amain)

# Apitomy Codegen

**Open Source API Design Code Generator**

Apitomy Codegen is a powerful tool that automatically generates high-quality code from OpenAPI specifications. It specializes in generating JAX-RS server stubs, Quarkus-optimized applications, data models, and client libraries, helping developers quickly scaffold robust API implementations.

## 🚀 Features

- **OpenAPI 3.x Support** - Full support for modern OpenAPI specifications
- **Quarkus Native** - Optimized for Quarkus applications with native compilation support
- **JAX-RS Generation** - Generate server stubs and client libraries
- **Multiple Integration Options** - Maven plugin or programmatic API
- **Highly Configurable** - Extensive customization options for generated code
- **Production Ready** - Battle-tested in enterprise environments

## 📦 Project Structure

- **`core/`** - Core code generation engine
- **`maven-plugin/`** - Maven plugin for build integration
- **`maven-plugin-tests/`** - Integration tests for the Maven plugin

## 🔧 Building the Project

### Prerequisites

- **Java 11 or later** (tested with Java 21)
- **Maven 3.6+** (tested with Maven 3.9+)

### Build Commands

```bash
# Clone the repository
git clone https://github.com/Apitomy/apitomy-codegen.git
cd apitomy-codegen

# Build all modules
./mvnw clean install

# Build without running tests (faster)
./mvnw clean install -DskipTests

# Run tests only
./mvnw test

# Build specific module
./mvnw clean install -pl core

# Generate Javadocs
./mvnw javadoc:javadoc
```

### Build Profiles

- **Default Profile** - Standard build with all tests
- **Release Profile** - Includes source jars, javadocs, and GPG signing
  ```bash
  ./mvnw clean install -Prelease
  ```

## 📚 Documentation

### Quick Start

For a quick start guide and examples, see our [Getting Started Guide](docs/getting-started.md).

### Full Documentation

Comprehensive documentation is available at: [docs/](docs/)

- **[Project Overview](docs/index.md)** - Introduction and key concepts
- **[Getting Started](docs/getting-started.md)** - Hello World tutorial
- **[User Guide](docs/user-guide/)** - Additional documentation

### Contributing to Documentation

We use [MkDocs](https://www.mkdocs.org/) with the Material theme for our documentation.

#### Prerequisites for Documentation

```bash
# Install MkDocs and required plugins
pip install mkdocs mkdocs-material mkdocstrings
```

#### Working with Documentation

```bash
# Preview documentation locally (auto-reload on changes)
mkdocs serve
# Visit http://127.0.0.1:8000 in your browser

# Build static documentation
mkdocs build

# Deploy to GitHub Pages (maintainers only)
mkdocs gh-deploy
```

#### Documentation Structure

- All documentation files are in `docs/` directory
- Written in Markdown format
- Configuration in `mkdocs.yml` (repo root)
- Supports code syntax highlighting, admonitions, and more

#### Contributing Documentation

1. Fork the repository
2. Create a new branch: `git checkout -b docs/your-feature`
3. Make your changes in the `docs/` directory
4. Preview locally with `mkdocs serve` from the repo root
5. Submit a pull request

## 🛠️ Usage

### Maven Plugin

Add to your `pom.xml`:

```xml
<plugin>
    <groupId>io.apitomy</groupId>
    <artifactId>apitomy-codegen-maven-plugin</artifactId>
    <version>1.2.11.Final</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <projectSettings>
                    <javaPackage>org.example.api</javaPackage>
                </projectSettings>
                <inputSpec>src/main/resources/openapi.json</inputSpec>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details on:

- Code style and conventions
- Submitting pull requests
- Reporting issues
- Development setup

## 📄 License

This project is licensed under the [Apache License 2.0](LICENSE).

## 🔗 Links

- **Website**: [https://www.apitomy.io/](https://www.apitomy.io/)
- **Documentation**: [https://www.apitomy.io/codegen/docs/](https://www.apitomy.io/codegen/docs/)
- **Issues**: [GitHub Issues](https://github.com/Apitomy/apitomy-codegen/issues)
- **Discussions**: [GitHub Discussions](https://github.com/Apitomy/apitomy-codegen/discussions)

## 👥 Maintainers

- **Eric Wittmann** ([@EricWittmann](https://github.com/EricWittmann)) - Project Lead
- **Jakub Senko** ([@JakubSenko](https://github.com/JakubSenko))
- **Carles Arnal** ([@CarlesArnal](https://github.com/CarlesArnal))

---

*Part of the [Apitomy](https://www.apitomy.io) project. Licensed under the
[Apache License 2.0](LICENSE).*
