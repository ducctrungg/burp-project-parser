# AGENTS.md

This file provides guidance to opencode agent when working with code in this repository.

Burp Suite Extension for exporting project data (proxy history, site map) as CSV, plus regex search of responses as JSON.

## Architecture

- **Main Entry Point**: `src/main/java/Extension.java` - implements `BurpExtension` interface
- **GUI Panel**: `src/main/java/ParserPanel.java` - Swing JPanel registered as a Burp Suite tab
- **Config Model**: `src/main/java/ParsingConfig.java` - record holding all export options
- **Build System**: Gradle with Kotlin DSL, Java 21 compatibility
- **Dependencies**: Montoya API 2026.4 (compile-only), Gson 2.14.0 (bundled)

## Extension Pattern

GUI-only extension. In `initialize()`, registers a suite tab via `api.userInterface().registerSuiteTab()`. The tab lets users select which data to export, configure options, and save results to file.

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
3. Go to **"BurpSuite Extractor"** tab in the main window
4. Quick reload: Ctrl/Click the Loaded checkbox

## Docs

- `USAGE.md` - CSV format, column descriptions, usage
- `docs/montoya-api-examples.md` - Code patterns
- `docs/development-best-practices.md` - Dev guidelines
- `docs/resources.md` - External links

## Commit Convention

Follow [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/):

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.

## Current State

GUI extension. Main class registers "BurpSuite Extractor" tab. Processing methods: printProxyHistory, printHistory, processResponseHeaders, processResponseBodies. CSV output via writeCsvHeader/escapeCsv helpers.
