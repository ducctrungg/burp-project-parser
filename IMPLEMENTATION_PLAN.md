# Implementation Plan: GUI Tab + Ignore Static Files

## Overview

Add a Burp Suite tab (`registerSuiteTab`) that lets users interactively choose which flags to run and where to save output, alongside the existing headless CLI mode.

## Mode Detection

In `initialize()`, detect if CLI flags are present:
- **CLI flags present** → run headless CLI path (existing behavior, unchanged)
- **No CLI flags** → register Suite tab for interactive GUI use

## New File: `ParserPanel.java`

A Swing `JPanel` with these sections:

### Flag Checkboxes (top)
- `auditItems` — checkbox
- `proxyHistory` — checkbox
- `siteMap` — checkbox

### Search Section (middle)
- `responseHeader` — checkbox + text field for regex
- `responseBody` — checkbox + text field for regex

### Output File Section (middle)
- Text field for file path
- "Browse..." button that opens `JFileChooser` for file save

### Actions (bottom)
- "Run" button — executes parsing on background thread via `SwingWorker`
- Status label — shows "Running...", "Complete", or error messages

### Styling
- Panel organized with `BorderLayout` / `GridBagLayout`
- `api.userInterface().applyThemeToComponent(panel)` for Burp-native look

## Modified: `Extension.java`

### Refactored `initialize()`
```java
String[] args = api.burpSuite().commandLineArguments().toArray(new String[0]);
if (containsAny(args, "...")) {
    // CLI path: existing headless logic
    runFromCli(args);
} else {
    // GUI path: register Suite tab
    api.userInterface().registerSuiteTab("Project Parser", new ParserPanel(api));
}
```

### Extracted `runFromCli(String[] args)`
Contains all the current CLI processing logic (flag parsing, proxy history, audit items, site map, response search, output file, shutdown).

### Reusable `runParser(args)` (used by both CLI and GUI)
Extracts the core parsing logic that both paths can call: proxy history, audit items, site map, response search, file output.

## Data Flow

### GUI Run button:
1. User checks flags, sets regex, chooses output file
2. User clicks "Run"
3. `SwingWorker.doInBackground()` calls a new `executeParsing()` method
4. Results stream to Burp Output tab + output file
5. Status label updates on EDT via `SwingWorker.done()`

### CLI path:
1. Burp loads extension headless with `--project-file=...`
2. `runFromCli(args)` executes the same `executeParsing()` logic
3. Extension unloads and Burp shuts down

## Thread Safety

- GUI Run button triggers `SwingWorker` to keep UI responsive
- File writing synchronized via existing `writeOutput`/`writeError` methods
- No shared mutable state between GUI threads

---

# Phase 2: Ignore Static File Extensions

## Overview

Allow users to skip URLs ending in common static file extensions (gif, jpg, png, css, etc.) when extracting data from proxy history and site map.

## Changes

### `ParsingConfig.java`

- Add `Set<String> ignoredExtensions` field
- Default ignored extensions constant: `gif,jpg,jpeg,png,css,mp3,mp4,wav,ico,map,woff,woff2,svg,ttf,pdf,otf,doc,docx`
- Parse new CLI flag: `ignoreExt=<comma-separated-list>`
  - No flag → use default set
  - `ignoreExt=none` → empty set (no filtering)
- Add `parseExtensions(String input)` helper, store as lowercased `Set<String>`

### `ParserPanel.java`

- New "Ignore Static Files" section between "Extract Data From" and "Search Responses"
- Single-line `JTextField` pre-filled with default extensions
- Label: "Skip URLs ending in these extensions (comma-separated):"
- Pass text field value into `ParsingConfig`

### `Extension.java`

- Add `isIgnored(String url, Set<String> ignoredExtensions)` helper:
  - Strip query (`?`) and fragment (`#`)
  - Get last path segment after `/`
  - Extract extension after last `.`
  - Check if lowercased extension is in the ignored set
- Apply filter in: `printProxyHistory()`, `printHistory()`, `processResponseHeaders()`, `processResponseBodies()`
- Pass `config.ignoredExtensions()` from `executeParsing()`

### URL matching logic

```
isIgnored(url, extensions):
  path = url before '?' or '#'
  lastSegment = path after last '/'
  ext = lastSegment after last '.' (lowercased)
  return ext is not empty AND extensions.contains(ext)
```

### CLI flag format

```bash
java -jar burpsuite_pro.jar --project-file=target.burp proxyHistory ignoreExt=gif,jpg,png,css
```

Omitted → uses default list. `ignoreExt=none` → no filtering.

## Build Validation

```bash
./gradlew clean build
```
