# Implementation Plan: Log Extractor

## Overview

GUI-only Burp Suite extension that registers a "Log Extractor" tab via `registerSuiteTab()`. Users select data sources, configure filters, choose output format (CSV or SQLite), and export results. Processing runs on a SwingWorker background thread.

## Architecture

### `Extension.java` — Entry point
- Implements `BurpExtension`
- `initialize()` registers the Suite tab with a `ParserPanel` instance via `this::runParsing`
- `runParsing()` dispatches to CSV or SQLite path based on file extension
- All processing logic: `printProxyHistory()`, `printHistory()`, `processResponseHeaders()`, `processResponseBodies()` — each branches on `dbMode`
- CSV: `writeCsvHeader()`, `escapeCsv()`, `writeOutput()` (file only, no per-line log)
- SQLite: `openDb()`/`closeDb()`, `createDbTables()`, transaction-batched insert methods
- Helpers: `extractHost()`, `formatHeaders()`, `isIgnored()`, `isIgnoredByContentType()`, `checkDbBatchCommit()`
- Cleanup: `cleanup()` handles both `closeOutputFile()` and `closeDb()`

### `ParserPanel.java` — Swing UI
- JPanel with checkboxes for proxy history/site map with "+ Response" sub-options
- Regex search text fields for response headers/bodies
- Static file filter: URL extensions + Content-Type checkbox with editable lists
- Output section: format combo (CSV/SQLite) above file path with Browse button
- Run button triggers `SwingWorker` background processing, indeterminate progress bar

### `ParsingConfig.java` — Data model
- Record: `proxyHistory`, `proxyHistoryResponse`, `siteMap`, `siteMapResponse`, `responseHeader`, `responseHeaderRegex`, `responseBody`, `responseBodyRegex`, `outputFile`, `ignoredExtensions`, `ignoreContentType`, `ignoredContentTypes`
- `isDbOutput()` — detects SQLite mode by `.db` extension
- `parseExtensions()`, `parseContentTypes()` — comma-separated string parsers (shared `parseCommaSeparated()`)
- Constants: `DEFAULT_IGNORED_EXTENSIONS`, `DEFAULT_IGNORED_CONTENT_TYPES`

## Data Flow

1. User selects options and clicks "Run"
2. `SwingWorker.doInBackground()` calls `Extension.runParsing(config)`
3. `runParsing` opens the output (CSV file or SQLite connection) based on `config.isDbOutput()`
4. `executeParsing()` calls enabled processing methods
5. Each method:
   - **CSV**: writes lines to file via `writeOutput()` (no Burp log per line)
   - **SQLite**: inserts rows via `PreparedStatement` with batched commits (every 500 rows)
6. Summary logged to Burp Output tab at end of each method
7. Output file/connection closed; status label updated on EDT via `SwingWorker.done()`

## SQLite Tables

```
proxy_history         (id PK, host, request_method, url, headers, body, status_code, response_body BLOB)
site_map              (id PK, host, request_method, url, headers, body, status_code, response_body BLOB)
response_headers_search (id PK, url, header)
response_bodies_search  (id PK, url, body BLOB)
```

## Performance

| Optimization | Detail |
|---|---|
| No per-line Burp logging | `writeOutput()` writes to file only; summaries logged once per source |
| SQLite transaction batching | `setAutoCommit(false)`, commit every 500 rows via `checkDbBatchCommit()` |
| Conditional body extraction | `bodyToString()` only in CSV mode, `body().getBytes()` only in DB mode; skipped when response body not needed |
| Buffered CSV I/O | `PrintWriter(BufferedWriter(FileWriter))` |
| Binary-safe BLOB storage | Response body stored as raw bytes via `response.body().getBytes()` |

## Files

| File | Purpose |
|------|---------|
| `Extension.java` | Entry point + all processing/export logic |
| `ParserPanel.java` | Swing UI panel |
| `ParsingConfig.java` | Configuration data record |

## Build

```bash
./gradlew clean build
```
