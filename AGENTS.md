# AGENTS.md

This file provides guidance to opencode agent when working with code in this repository.

Burp Suite Extension template project using the Montoya API (minimal starter project).

## Architecture

- **Main Entry Point**: `src/main/java/Extension.java` - implements `BurpExtension` interface
- **Build System**: Gradle with Kotlin DSL, Java 21 compatibility
- **Dependencies**: Montoya API 2025.10 (compile-only), no runtime dependencies
- **Extension Pattern**: Single-class extension that initializes through `initialize(MontoyaApi montoyaApi)` method

## Build Commands

```bash
./gradlew build    # Build and test
./gradlew jar      # Create extension JAR
./gradlew clean    # Clean artifacts
```

JAR output: `build/libs/` - load directly into Burp Suite.

## Loading in Burp

1. Build JAR: `./gradlew jar`
2. Burp: Extensions > Installed > Add > Select JAR
3. Quick reload: Ctrl/Click the Loaded checkbox

## Docs

- `docs/bapp-store-requirements.md` - BApp Store requirements
- `docs/montoya-api-examples.md` - Code patterns
- `docs/development-best-practices.md` - Dev guidelines
- `docs/resources.md` - External links

## Current State

Template project. Main class sets extension name to "My Extension" with a TODO placeholder.
