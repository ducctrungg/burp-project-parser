# USAGE.md

## How It Works

The extension parses Burp Suite project files from the command line using the Montoya API. It outputs results as JSON to the system console (Burp > Extensions > [extension] > set Output and Errors to **system console**).

## Basic Command

```bash
java -jar -Djava.awt.headless=true <path-to-burpsuite-pro.jar> --project-file=<path-to-project-file> <flags>
```

You may need `--add-opens=java.desktop/javax.swing=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED` depending on your Java version.

## Flags

| Flag | Description |
|------|-------------|
| `auditItems` | Print all audit items (scan issues) |
| `siteMap` | Print all requests/responses from the site map |
| `proxyHistory` | Print all requests/responses from the proxy history |
| `responseHeader=<regex>` | Search response headers matching a regex |
| `responseBody=<regex>` | Search response bodies matching a regex |
| `outputFile=<path>` | Write all output to a file in addition to console |

## siteMap / proxyHistory Sub-Components

Speed up parsing by selecting which parts of requests/responses to output. Separate with commas:

- `request.headers`
- `request.body`
- `response.headers`
- `response.body`

Example — print only request body and headers from proxy history:

```bash
java -jar -Djava.awt.headless=true <burpsuite_pro.jar> --project-file=<project> proxyHistory.request.headers,proxyHistory.request.body
```

## Examples

### Print audit items

```bash
java -jar -Djava.awt.headless=true burpsuite_pro.jar --project-file=target.burp auditItems
```

### Print site map and proxy history

```bash
java -jar -Djava.awt.headless=true burpsuite_pro.jar --project-file=target.burp siteMap proxyHistory
```

### Search response headers with regex

```bash
java -jar -Djava.awt.headless=true burpsuite_pro.jar --project-file=target.burp responseHeader='.*(Servlet|nginx).*'
```

Output:
```
{"url":"https://example.com/something.css","header":"x-powered-by: Servlet/3.0"}
{"url":"https://spocs.getpocket.com:443/spocs","header":"Server: nginx"}
```

### Search response body with regex

```bash
java -jar -Djava.awt.headless=true burpsuite_pro.jar --project-file=target.burp responseBody='.*<form.*'
```

To clean up results (trim around match):

```bash
java -jar -Djava.awt.headless=true burpsuite_pro.jar --project-file=target.burp responseBody='.*<form.*' | grep -o -P -- "url\\":.{0,100}|.{0,80}<form.{0,80}"
```

## Tips

- Use a custom Burp config with only this extension loaded to speed startup:

```bash
--user-config-file=<path-to-config>
```

- Increase memory with `-Xmx`:

```bash
java -jar -Djava.awt.headless=true -Xmx2G burpsuite_pro.jar --project-file=target.burp auditItems
```

- Flags can be combined (e.g. `auditItems siteMap`).
- Always use full paths to the project file.

### Write results to file

```bash
java -Djava.awt.headless=true burpsuite_pro.jar --project-file=target.burp auditItems proxyHistory outputFile=C:\results\output.json
```

Output is written to the file in real time while also logged to the Burp console.
