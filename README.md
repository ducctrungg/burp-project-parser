# Log Extractor

A Burp Suite extension for exporting proxy history and site map data via a GUI tab. Uses the Montoya API (2026.4). Outputs data as CSV or SQLite database, with regex search of response headers/bodies.

## Features

- Export Proxy History and Site Map to **CSV** or **SQLite (.db)**
- Regex search of response headers and bodies
- Filter by URL extension and/or response Content-Type
- Binary-safe response body storage (BLOB in SQLite)
- Background processing with SwingWorker (non-blocking UI)

## Prerequisites

- JDK 21
- Burp Suite Professional

## Build

```bash
./gradlew clean build   # Compile and run tests
./gradlew jar           # Create JAR (output: build/libs/)
```

## Load in Burp

1. Build the JAR: `./gradlew jar`
2. Burp > Extensions > Installed > Add > Select JAR
3. Go to the **"Log Extractor"** tab in the main Burp window
4. Quick reload: `Ctrl`/`⌘` + click the **Loaded** checkbox

## Usage

See [USAGE.md](USAGE.md) for CSV/SQLite format, table descriptions, and full GUI usage.

## Dependencies

- `net.portswigger.burp.extensions:montoya-api:2026.4` (compile-only, provided by Burp)
- `com.google.code.gson:gson:2.14.0` (bundled in JAR)
- `org.xerial:sqlite-jdbc:3.49.1.0` (bundled in JAR)

## Docs

- [USAGE.md](USAGE.md) — CSV and SQLite format, column/table descriptions, GUI usage
- [docs/montoya-api-examples.md](docs/montoya-api-examples.md) — API patterns
- [docs/development-best-practices.md](docs/development-best-practices.md) — dev guidelines

## Credits

- [BuffaloWill/burpsuite-project-file-parser](https://github.com/BuffaloWill/burpsuite-project-file-parser)
- [NonManuall/SAVER_LOGGER](https://github.com/NonManuall/SAVER_LOGGER)
