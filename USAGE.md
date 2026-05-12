# USAGE.md

## How It Works

The extension adds a **"Log Extractor"** tab to the main Burp Suite window. From this tab you can:
- Export proxy history and site map data as **CSV** or **SQLite database**
- Search response headers/bodies by regex
- Filter out static files by URL extension and/or response Content-Type
- Save output directly to a file

## Usage

1. Load the extension in Burp Suite
2. Go to the **"Log Extractor"** tab in the main window

### Data Sources

Check which data sources to include:

| Option | Description |
|--------|-------------|
| **Proxy History** | Export requests from the proxy history |
| &nbsp;&nbsp; + Response | Include response status code and body in the export |
| **Site Map** | Export requests from the site map |
| &nbsp;&nbsp; + Response | Include response status code and body in the export |

### Regex Search

| Option | Description |
|--------|-------------|
| **Header** | Search response headers matching a regex |
| **Body** | Search response bodies matching a regex |

Search results are output as JSON (CSV mode) or stored in the `response_headers_search` / `response_bodies_search` tables (SQLite mode).

### Filter Static Files

Skip requests/responses matching static content. Two filtering methods are available:

**By extension** — skip URLs with matching file extensions. Default list:
`gif,jpg,jpeg,png,css,css2,mp3,mp4,wav,ico,map,woff,woff2,svg,ttf,pdf,otf,doc,docx`

**By Content-Type** (optional checkbox) — skip responses whose `Content-Type` header matches a type in the list. Default:
`text/javascript,application/javascript,application/x-javascript,image/png,image/jpeg,image/gif,image/svg+xml,image/webp,image/x-icon,text/css,audio/mpeg,audio/ogg,video/mp4,font/woff,font/woff2,font/ttf,font/otf,application/pdf,application/font-woff,application/font-woff2,application/octet-stream`

A request is skipped if it matches **either** filter (additive OR logic). Content-Type values are matched after stripping parameters (e.g., `text/html; charset=utf-8` → `text/html`).

### Output

| Setting | Description |
|---------|-------------|
| **Format** | Choose output format: CSV (.csv) or SQLite (.db). Switching updates the file extension in the Save field. |
| **File** | Path to save output. Click **Browse...** to choose a location. Output is also logged to the Burp Extensions Output tab (summary only — line-by-line logging is disabled for performance). |
| **Run** | Start the export on a background thread. A progress indicator shows while running. |

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
| Response Body | Response body (only when + Response enabled) |

CSV files are written with UTF-8 BOM encoding for correct Unicode display in Excel and other tools.

## SQLite Output Format

When SQLite (.db) is selected, the following tables are created:

### `proxy_history` / `site_map`

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER PK | Auto-increment identifier |
| host | TEXT | Target hostname |
| request_method | TEXT | HTTP method |
| url | TEXT | Request URL |
| headers | TEXT | Request headers (newline-separated) |
| body | TEXT | Request body |
| status_code | INTEGER | Response status code |
| response_body | BLOB | Response body (raw bytes, preserves binary data) |

### `response_headers_search`

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER PK | Auto-increment |
| url | TEXT | Request URL |
| header | TEXT | Matching response header |

### `response_bodies_search`

| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER PK | Auto-increment |
| url | TEXT | Request URL |
| body | BLOB | Raw response body bytes |

Response bodies are stored as BLOB to faithfully preserve binary content (images, zip files, etc.) without encoding corruption.

## Tips

- For large projects, disable "include responses" when you don't need response data — this skips expensive body extraction.
- Use the **Filter Static Files** options to skip images, CSS, fonts, and other non-essential resources.
- SQLite mode with transaction batching (auto-commit disabled, commit every 500 rows) is significantly faster than CSV for very large exports.
