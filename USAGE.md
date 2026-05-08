# USAGE.md

## How It Works

The extension adds a **"BurpSuite Extractor"** tab to the main Burp Suite window. From this tab you can:
- Export proxy history and site map data as CSV
- Search response headers/bodies by regex (JSON output)
- Filter out static files by extension
- Save output directly to a file

## Usage

1. Load the extension in Burp Suite
2. Go to the **"BurpSuite Extractor"** tab in the main window

### Extract Data From

Check which data sources to include:

| Option | Description |
|--------|-------------|
| **Proxy History** | Export requests from the proxy history |
| &nbsp;&nbsp; include responses | Also include response status code and body in the export |
| **Site Map** | Export requests from the site map |
| &nbsp;&nbsp; include responses | Also include response status code and body in the export |

### Search Responses

| Option | Description |
|--------|-------------|
| **Response Header regex** | Search response headers matching a regex (JSON output) |
| **Response Body regex** | Search response bodies matching a regex (JSON output) |

### Ignore Static Files

Skip URLs ending in common static file extensions. The default list is:
`gif,jpg,jpeg,png,css,css2,mp3,mp4,wav,ico,map,woff,woff2,svg,ttf,pdf,otf,doc,docx`

Edit the comma-separated list to add or remove extensions, or clear the field to include everything.

### Output

- **Save to**: Optionally write results to a CSV file (click "Browse..." to choose). Output is also logged to the **Burp Extensions Output** tab.
- **Run**: Start the export. A progress indicator shows while running.

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

CSV output can be opened directly in Excel, Google Sheets, or any spreadsheet application.

## Tips

- For large projects, check "include responses" only when you need response data — this speeds up parsing significantly.
- Use the **Ignore Static Files** list to skip images, CSS, fonts, and other non-essential resources.
