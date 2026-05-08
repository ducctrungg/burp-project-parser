# USAGE.md

## How It Works

The extension parses Burp Suite project files from the command line using the Montoya API. The extension outputs proxy history and site map data as CSV. Response header/body search results are output as JSON.

## Basic Command

```bash
java -Djava.awt.headless=true -jar <path-to-burpsuite-pro.jar> --project-file=<path-to-project-file> <flags>
```

You may need `--add-opens=java.desktop/javax.swing=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED` depending on your Java version.

## Flags

| Flag | Description |
|------|-------------|
| `siteMap` | Print site map requests/responses as CSV |
| `proxyHistory` | Print proxy history requests/responses as CSV |
| `responseHeader=<regex>` | Search response headers matching a regex (JSON output) |
| `responseBody=<regex>` | Search response bodies matching a regex (JSON output) |
| `ignoreExt=<ext1,ext2,...>` | Skip URLs ending in given extensions (default: gif,jpg,jpeg,png,css,css2,mp3,mp4,wav,ico,map,woff,woff2,svg,ttf,pdf,otf,doc,docx). Use `ignoreExt=none` to include all. |
| `outputFile=<path>` | Write all output to a file (e.g. output.csv) |

## siteMap / proxyHistory Sub-Components

Include `response` in the flag to also export response data:

| Flag variant | Effect |
|---|---|
| `proxyHistory` | Request data only (7 columns) |
| `proxyHistory.response` | Request + response data (8 columns) |
| `siteMap` | Request data only (7 columns) |
| `siteMap.response` | Request + response data (8 columns) |

## CSV Output Format

### Without responses (7 columns)

```
No,Host,Request Method,URL,Headers,Body,Status Code
```

### With responses (8 columns)

```
No,Host,Request Method,URL,Headers,Body,Status Code,Response Body
```

| Column | Description |
|--------|-------------|
| No | Sequential request identifier (1, 2, 3...) |
| Host | Target hostname or IP address |
| Request Method | HTTP method (GET, POST, PUT, DELETE, etc.) |
| URL | Complete request URL including query parameters |
| Headers | All request headers (newline-separated within quoted field) |
| Body | Request body |
| Status Code | HTTP response status code (200, 404, 500, etc.) |
| Response Body | Response body (only when include responses enabled) |

## Examples

### Print proxy history (requests only)

```bash
java -Djava.awt.headless=true burpsuite_pro.jar --project-file=target.burp proxyHistory
```

### Print proxy history with responses

```bash
java -Djava.awt.headless=true burpsuite_pro.jar --project-file=target.burp proxyHistory.response
```

### Print site map and proxy history

```bash
java -Djava.awt.headless=true burpsuite_pro.jar --project-file=target.burp siteMap proxyHistory
```

### Print proxy history, ignore static files

```bash
java -Djava.awt.headless=true burpsuite_pro.jar --project-file=target.burp proxyHistory ignoreExt=gif,jpg,png,css
```

### Print all files (no ignore)

```bash
java -Djava.awt.headless=true burpsuite_pro.jar --project-file=target.burp proxyHistory ignoreExt=none
```

### Write results to CSV file

```bash
java -Djava.awt.headless=true burpsuite_pro.jar --project-file=target.burp proxyHistory outputFile=C:\results\output.csv
```

### Search response headers with regex (JSON output)

```bash
java -Djava.awt.headless=true burpsuite_pro.jar --project-file=target.burp responseHeader='.*(Servlet|nginx).*'
```

Output:
```
{"url":"https://example.com/something.css","header":"x-powered-by: Servlet/3.0"}
{"url":"https://spocs.getpocket.com:443/spocs","header":"Server: nginx"}
```

### Search response body with regex (JSON output)

```bash
java -Djava.awt.headless=true burpsuite_pro.jar --project-file=target.burp responseBody='.*<form.*'
```

## Tips

- Use a custom Burp config with only this extension loaded to speed startup:

```bash
--user-config-file=<path-to-config>
```

- Increase memory with `-Xmx`:

```bash
java -Djava.awt.headless=true -Xmx2G burpsuite_pro.jar --project-file=target.burp proxyHistory
```

- Flags can be combined (e.g. `siteMap proxyHistory.response`).
- Always use full paths to the project file.
- CSV output can be opened directly in Excel, Google Sheets, or any spreadsheet application.
- For large exports, use `outputFile` to save directly to disk instead of relying on console output.
