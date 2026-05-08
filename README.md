# BurpSuite Project File Parser

A Burp Suite extension for exporting proxy history and site map data via a GUI tab. Uses the Montoya API (2026.4). Outputs data as CSV and supports regex search of response headers/bodies as JSON.

## Prerequisites

- JDK 21
- Burp Suite Professional (for the `burpsuite_pro.jar`)

## Build

```bash
./gradlew clean build   # Compile and run tests
./gradlew jar           # Create JAR (output: build/libs/)
```

## Load in Burp

1. Build the JAR: `./gradlew jar`
2. Burp > Extensions > Installed > Add > Select JAR
3. Go to the **"BurpSuite Extractor"** tab in the main Burp window
4. Quick reload: `Ctrl`/`⌘` + click the **Loaded** checkbox

## Usage

See [USAGE.md](USAGE.md) for CSV format, column descriptions, and GUI usage.

## Dependencies

- `net.portswigger.burp.extensions:montoya-api:2026.4` (compile-only, provided by Burp)
- `com.google.code.gson:gson:2.14.0` (bundled in JAR)

## Docs

- [USAGE.md](USAGE.md) — CSV format, column descriptions, GUI usage
- [docs/montoya-api-examples.md](docs/montoya-api-examples.md) — API patterns
- [docs/development-best-practices.md](docs/development-best-practices.md) — dev guidelines
- [docs/bapp-store-requirements.md](docs/bapp-store-requirements.md) — BApp Store submission

## Credits

- [BuffaloWill/burpsuite-project-file-parser](https://github.com/BuffaloWill/burpsuite-project-file-parser)
- [NonManuall/SAVER_LOGGER](https://github.com/NonManuall/SAVER_LOGGER)
