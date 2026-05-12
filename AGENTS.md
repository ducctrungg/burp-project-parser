# AGENTS.md

This file provides guidance to opencode agent when working with code in this repository.

Burp Suite Extension ("Log Extractor") for exporting proxy history and site map data as CSV or SQLite, plus regex search of responses.

## Architecture

- **Main Entry Point**: `src/main/java/Extension.java` - implements `BurpExtension` interface
- **GUI Panel**: `src/main/java/ParserPanel.java` - Swing JPanel registered as a Burp Suite tab
- **Config Model**: `src/main/java/ParsingConfig.java` - record holding all export options
- **Build System**: Gradle with Kotlin DSL, Java 21 compatibility
- **Dependencies**: Montoya API 2026.4 (compile-only), Gson 2.14.0 (bundled), SQLite JDBC 3.49.1.0 (bundled)

## Extension Pattern

GUI-only extension. In `initialize()`, registers a suite tab via `api.userInterface().registerSuiteTab()`. The tab lets users select data sources (proxy history, site map), configure options (include responses, regex search, static file filters), choose output format (CSV / SQLite), and save results to file. Processing runs on a SwingWorker background thread.

## Build Commands

```bash
./gradlew build    # Build and test
./gradlew jar      # Create extension JAR
./gradlew clean    # Clean artifacts
```

JAR output: `build/libs/` - fat JAR with bundled dependencies. Load directly into Burp Suite.

## Loading in Burp

1. Build JAR: `./gradlew jar`
2. Burp: Extensions > Installed > Add > Select JAR
3. Go to **"Log Extractor"** tab in the main window
4. Quick reload: Ctrl/Click the Loaded checkbox

## Key Implementation Details

### Output Formats
- **CSV** (`.csv`): UTF-8 with BOM, BufferedWriter for performance, per-line writes (no line-by-line Burp logging — summary only)
- **SQLite** (`.db`): 4 tables — `proxy_history`, `site_map`, `response_headers_search`, `response_bodies_search`. Response bodies stored as BLOB via `response.body().getBytes()`. Autocommit disabled; batched commits every 500 rows.

### Static File Filtering
- **Extension-based**: checks URL path for file extension in ignored set (`isIgnored()`)
- **Content-Type-based**: checks response `Content-Type` header, strips charset params (`isIgnoredByContentType()`)
- Both filters are additive (OR logic)

### Performance Optimizations
- `writeOutput()` writes to file only (no `logging.logToOutput()` per line — prevents EDT flooding)
- SQLite: `setAutoCommit(false)` + commit every 500 rows (`checkDbBatchCommit()`)
- Conditional extraction: `bodyToString()` only in CSV mode, `body().getBytes()` only in DB mode; skipped entirely when `includeResponse` is false
- Buffered I/O: `PrintWriter(BufferedWriter(FileWriter))` for CSV

### Binary Data
- Montoya API `HttpResponse.body()` returns `ByteArray` (not `byte[]`). Use `.body().getBytes()`.
- `bodyToString()` and `body().getBytes()` are independent — both can be called separately.

### Classloader
- Burp's custom classloader does not support JDBC SPI auto-discovery. SQLite driver must be loaded explicitly via `Class.forName("org.sqlite.JDBC")`.

## Docs

- `USAGE.md` - CSV/SQLite format, table descriptions, GUI usage
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

GUI extension. Main class registers "Log Extractor" tab. Processing methods: `printProxyHistory`, `printHistory`, `processResponseHeaders`, `processResponseBodies`. Output: CSV (UTF-8 BOM, BufferedWriter) or SQLite (batched transactions, BLOB response bodies). Filtering: URL extension + Content-Type (additive). Summary logging to Burp output (per-line logging disabled for performance).
