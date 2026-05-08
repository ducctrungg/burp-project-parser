# Implementation Plan: GUI-Only Burp Suite Project File Parser

## Overview

GUI-only Burp Suite extension that registers a "BurpSuite Extractor" tab via `registerSuiteTab()`. Users select data sources, configure options, and export results to CSV or JSON.

## Architecture

### `Extension.java` — Entry point
- Implements `BurpExtension`
- `initialize()` registers the Suite tab with a `ParserPanel` instance
- Contains all processing logic: `printProxyHistory()`, `printHistory()`, `processResponseHeaders()`, `processResponseBodies()`
- Helper methods: `escapeCsv()`, `formatHeaders()`, `extractHost()`, `writeCsvHeader()`, `isIgnored()`
- `writeOutput()` / `writeError()` send to both Burp log and optional file

### `ParserPanel.java` — Swing UI
- JPanel with checkboxes for proxy history / site map / include responses
- Regex search text fields for response headers/bodies
- Ignore static files text field (comma-separated extensions)
- Output file path + Browse button (JFileChooser)
- Run button with SwingWorker for background processing
- Status label and progress bar

### `ParsingConfig.java` — Data model
- Record holding all user-selected options
- `parseExtensions()` helper for parsing ignore extension lists
- `DEFAULT_IGNORED_EXTENSIONS` constant

## Data Flow

1. User selects options in the tab and clicks "Run"
2. `SwingWorker.doInBackground()` calls `Extension.executeParsing(config)`
3. Results stream to Burp Output tab + optional output file
4. Status label updates on EDT via `SwingWorker.done()`

## Files

| File | Purpose |
|------|---------|
| `Extension.java` | Entry point + processing logic |
| `ParserPanel.java` | Swing UI panel |
| `ParsingConfig.java` | Configuration data record |

## Build

```bash
./gradlew clean build
```
