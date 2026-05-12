import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.proxy.Proxy;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import burp.api.montoya.sitemap.SiteMap;
import com.google.gson.JsonObject;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Extension implements BurpExtension {
    private MontoyaApi api;
    private Logging logging;
    private Proxy proxy;
    private SiteMap siteMap;
    private PrintWriter fileWriter;
    private Connection dbConnection;
    private boolean dbMode;
    private PreparedStatement psProxyHistory;
    private PreparedStatement psSiteMap;
    private PreparedStatement psResponseHeader;
    private PreparedStatement psResponseBody;

    @Override
    public void initialize(MontoyaApi montoyaApi) {
        this.api = montoyaApi;
        this.logging = api.logging();
        this.proxy = api.proxy();
        this.siteMap = api.siteMap();

        montoyaApi.extension().setName("Log Extractor");

        api.extension().registerUnloadingHandler(this::cleanup);

        api.userInterface().registerSuiteTab("Log Extractor",
                new ParserPanel(api, this::runParsing));
    }

    private void executeParsing(ParsingConfig config) {
        Set<String> ignoredExt = config.ignoredExtensions();
        boolean checkContentType = config.ignoreContentType();
        Set<String> ignoredContentTypes = config.ignoredContentTypes();
        if (config.proxyHistory()) {
            printProxyHistory(proxy.history(), config.proxyHistoryResponse(), ignoredExt, checkContentType, ignoredContentTypes);
        }
        if (config.siteMap()) {
            printHistory(siteMap.requestResponses(), config.siteMapResponse(), ignoredExt, checkContentType, ignoredContentTypes);
        }
        if (config.responseHeader()) {
            processResponseHeaders(proxy.history(), config.responseHeaderRegex(), ignoredExt, checkContentType, ignoredContentTypes);
        }
        if (config.responseBody()) {
            processResponseBodies(proxy.history(), config.responseBodyRegex(), ignoredExt, checkContentType, ignoredContentTypes);
        }
    }

    private void runParsing(ParsingConfig config) {
        if (config.isDbOutput()) {
            openDb(config.outputFile());
        } else {
            openOutputFile(config.outputFile());
        }
        executeParsing(config);
        if (dbMode) {
            closeDb();
        } else {
            closeOutputFile();
        }
    }

    private void openOutputFile(String path) {
        if (path == null || path.isEmpty()) return;
        try {
            fileWriter = new PrintWriter(new FileWriter(path, StandardCharsets.UTF_8), true);
            fileWriter.write('\uFEFF');
            dbMode = false;
        } catch (IOException e) {
            writeError("Failed to open output file: " + e.getMessage());
        }
    }

    private void closeOutputFile() {
        if (fileWriter != null) {
            fileWriter.close();
            fileWriter = null;
        }
    }

    private void openDb(String path) {
        if (path == null || path.isEmpty()) return;
        try {
            dbConnection = DriverManager.getConnection("jdbc:sqlite:" + path);
            dbMode = true;
            createDbTables();
            psProxyHistory = dbConnection.prepareStatement(
                "INSERT INTO proxy_history(host,request_method,url,headers,body,status_code,response_body) VALUES(?,?,?,?,?,?,?)");
            psSiteMap = dbConnection.prepareStatement(
                "INSERT INTO site_map(host,request_method,url,headers,body,status_code,response_body) VALUES(?,?,?,?,?,?,?)");
            psResponseHeader = dbConnection.prepareStatement(
                "INSERT INTO response_headers_search(url,header) VALUES(?,?)");
            psResponseBody = dbConnection.prepareStatement(
                "INSERT INTO response_bodies_search(url,body) VALUES(?,?)");
        } catch (SQLException e) {
            writeError("Failed to open DB: " + e.getMessage());
        }
    }

    private void closeDb() {
        try {
            if (psProxyHistory != null) { psProxyHistory.close(); psProxyHistory = null; }
            if (psSiteMap != null) { psSiteMap.close(); psSiteMap = null; }
            if (psResponseHeader != null) { psResponseHeader.close(); psResponseHeader = null; }
            if (psResponseBody != null) { psResponseBody.close(); psResponseBody = null; }
            if (dbConnection != null) { dbConnection.close(); dbConnection = null; }
        } catch (SQLException e) {
            writeError("Failed to close DB: " + e.getMessage());
        }
        dbMode = false;
    }

    private void cleanup() {
        closeOutputFile();
        closeDb();
    }

    private void createDbTables() throws SQLException {
        try (Statement stmt = dbConnection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS proxy_history(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "host TEXT,request_method TEXT,url TEXT,headers TEXT,body TEXT," +
                "status_code INTEGER,response_body BLOB)");
            stmt.execute("CREATE TABLE IF NOT EXISTS site_map(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "host TEXT,request_method TEXT,url TEXT,headers TEXT,body TEXT," +
                "status_code INTEGER,response_body BLOB)");
            stmt.execute("CREATE TABLE IF NOT EXISTS response_headers_search(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "url TEXT,header TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS response_bodies_search(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "url TEXT,body BLOB)");
        }
    }

    private void insertProxyHistoryDb(String host, String method, String url, String headers, String body, int statusCode, byte[] responseBytes) {
        try {
            psProxyHistory.setString(1, host);
            psProxyHistory.setString(2, method);
            psProxyHistory.setString(3, url);
            psProxyHistory.setString(4, headers);
            psProxyHistory.setString(5, body);
            psProxyHistory.setInt(6, statusCode);
            if (responseBytes != null) {
                psProxyHistory.setBytes(7, responseBytes);
            } else {
                psProxyHistory.setNull(7, Types.BLOB);
            }
            psProxyHistory.executeUpdate();
        } catch (SQLException e) {
            writeError("DB insert error: " + e.getMessage());
        }
    }

    private void insertSiteMapDb(String host, String method, String url, String headers, String body, int statusCode, byte[] responseBytes) {
        try {
            psSiteMap.setString(1, host);
            psSiteMap.setString(2, method);
            psSiteMap.setString(3, url);
            psSiteMap.setString(4, headers);
            psSiteMap.setString(5, body);
            psSiteMap.setInt(6, statusCode);
            if (responseBytes != null) {
                psSiteMap.setBytes(7, responseBytes);
            } else {
                psSiteMap.setNull(7, Types.BLOB);
            }
            psSiteMap.executeUpdate();
        } catch (SQLException e) {
            writeError("DB insert error: " + e.getMessage());
        }
    }

    private void insertResponseHeaderDb(String url, String header) {
        try {
            psResponseHeader.setString(1, url);
            psResponseHeader.setString(2, header);
            psResponseHeader.executeUpdate();
        } catch (SQLException e) {
            writeError("DB insert error: " + e.getMessage());
        }
    }

    private void insertResponseBodyDb(String url, byte[] body) {
        try {
            psResponseBody.setString(1, url);
            if (body != null) {
                psResponseBody.setBytes(2, body);
            } else {
                psResponseBody.setNull(2, Types.BLOB);
            }
            psResponseBody.executeUpdate();
        } catch (SQLException e) {
            writeError("DB insert error: " + e.getMessage());
        }
    }

    private void writeCsvHeader(boolean includeResponse) {
        String header = "No,Host,Request Method,URL,Headers,Body,Status Code";
        if (includeResponse) {
            header += ",Response Body";
        }
        writeOutput(header);
    }

    private String escapeCsv(String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String extractHost(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return "";
        }
    }

    private String formatHeaders(List<HttpHeader> headers) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headers.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(headers.get(i).toString());
        }
        return sb.toString();
    }

    private void printHistory(List<HttpRequestResponse> history, boolean includeResponse,
                              Set<String> ignoredExt, boolean checkContentType, Set<String> ignoredContentTypes) {
        if (!dbMode) writeCsvHeader(includeResponse);
        int no = 1;
        for (HttpRequestResponse reqRes : history) {
            try {
                String url = reqRes.request().url();
                if (isIgnored(url, ignoredExt)) continue;

                String host = extractHost(url);
                String method = reqRes.request().method();
                String headers = formatHeaders(reqRes.request().headers());
                String body = reqRes.request().bodyToString();
                int statusCode = 0;
                String responseBodyCsv = "";
                byte[] responseBytes = null;

                if (reqRes.response() != null) {
                    if (checkContentType && isIgnoredByContentType(reqRes.response(), ignoredContentTypes)) continue;
                    statusCode = reqRes.response().statusCode();
                    responseBodyCsv = reqRes.response().bodyToString();
                    responseBytes = reqRes.response().body().getBytes();
                } else if (checkContentType) {
                    continue;
                }

                if (dbMode) {
                    insertSiteMapDb(host, method, url, headers, body, statusCode, responseBytes);
                } else {
                    String line = no + "," + escapeCsv(host) + "," + method + ","
                            + escapeCsv(url) + "," + escapeCsv(headers) + ","
                            + escapeCsv(body) + "," + statusCode;
                    if (includeResponse) {
                        line += "," + escapeCsv(responseBodyCsv);
                    }
                    writeOutput(line);
                }
                no++;
            } catch (Exception e) {
                writeOutput("Error processing request/response: " + e.getMessage());
            }
        }
    }

    private void printProxyHistory(List<ProxyHttpRequestResponse> history, boolean includeResponse,
                                   Set<String> ignoredExt, boolean checkContentType, Set<String> ignoredContentTypes) {
        if (!dbMode) writeCsvHeader(includeResponse);
        int no = 1;
        for (ProxyHttpRequestResponse reqRes : history) {
            try {
                String url = reqRes.request().url() + reqRes.request().query();
                if (isIgnored(url, ignoredExt)) continue;

                String host = extractHost(url);
                String method = reqRes.request().method();
                String headers = formatHeaders(reqRes.request().headers());
                String body = reqRes.request().bodyToString();
                int statusCode = 0;
                String responseBodyCsv = "";
                byte[] responseBytes = null;

                if (reqRes.response() != null) {
                    if (checkContentType && isIgnoredByContentType(reqRes.response(), ignoredContentTypes)) continue;
                    statusCode = reqRes.response().statusCode();
                    responseBodyCsv = reqRes.response().bodyToString();
                    responseBytes = reqRes.response().body().getBytes();
                } else if (checkContentType) {
                    continue;
                }

                if (dbMode) {
                    insertProxyHistoryDb(host, method, url, headers, body, statusCode, responseBytes);
                } else {
                    String line = no + "," + escapeCsv(host) + "," + method + ","
                            + escapeCsv(url) + "," + escapeCsv(headers) + ","
                            + escapeCsv(body) + "," + statusCode;
                    if (includeResponse) {
                        line += "," + escapeCsv(responseBodyCsv);
                    }
                    writeOutput(line);
                }
                no++;
            } catch (Exception e) {
                writeOutput("Error processing request/response: " + e.getMessage());
            }
        }
    }

    private void processResponseHeaders(List<ProxyHttpRequestResponse> history, String regex,
                                        Set<String> ignoredExt, boolean checkContentType, Set<String> ignoredContentTypes) {
        Pattern pattern = Pattern.compile(regex);
        for (ProxyHttpRequestResponse reqRes : history) {
            try {
                if (reqRes.response() == null) continue;
                String url = reqRes.request().url();
                if (isIgnored(url, ignoredExt)) continue;
                if (checkContentType && isIgnoredByContentType(reqRes.response(), ignoredContentTypes)) continue;
                for (HttpHeader header : reqRes.response().headers()) {
                    if (pattern.matcher(header.toString()).find()) {
                        if (dbMode) {
                            insertResponseHeaderDb(url, header.toString());
                        } else {
                            JsonObject output = new JsonObject();
                            output.addProperty("url", url);
                            output.addProperty("header", header.toString());
                            writeOutput(output.toString());
                        }
                    }
                }
            } catch (Exception e) {
                writeError(e.getMessage());
            }
        }
    }

    private void processResponseBodies(List<ProxyHttpRequestResponse> history, String regex,
                                       Set<String> ignoredExt, boolean checkContentType, Set<String> ignoredContentTypes) {
        Pattern pattern = Pattern.compile(regex);
        for (ProxyHttpRequestResponse reqRes : history) {
            try {
                if (reqRes.response() == null) continue;
                String url = reqRes.request().url();
                if (isIgnored(url, ignoredExt)) continue;
                if (checkContentType && isIgnoredByContentType(reqRes.response(), ignoredContentTypes)) continue;
                String bodyStr = reqRes.response().bodyToString();
                if (pattern.matcher(bodyStr).find()) {
                    if (dbMode) {
                        insertResponseBodyDb(url, reqRes.response().body().getBytes());
                    } else {
                        JsonObject output = new JsonObject();
                        output.addProperty("url", url);
                        output.addProperty("body", bodyStr);
                        writeOutput(output.toString());
                    }
                }
            } catch (Exception e) {
                writeError(e.getMessage());
            }
        }
    }

    private void writeOutput(String message) {
        logging.logToOutput(message);
        if (fileWriter != null) {
            fileWriter.println(message);
        }
    }

    private void writeError(String message) {
        logging.logToError(message);
        if (fileWriter != null) {
            fileWriter.println("[ERROR] " + message);
        }
    }

    private boolean isIgnored(String url, Set<String> ignored) {
        if (ignored == null || ignored.isEmpty()) return false;
        int queryIdx = url.indexOf('?');
        int fragIdx = url.indexOf('#');
        int end = url.length();
        if (queryIdx != -1) end = Math.min(end, queryIdx);
        if (fragIdx != -1) end = Math.min(end, fragIdx);
        String path = url.substring(0, end);
        int lastSlash = path.lastIndexOf('/');
        String fileName = (lastSlash != -1) ? path.substring(lastSlash + 1) : path;
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) return false;
        String ext = fileName.substring(lastDot + 1).toLowerCase();
        return ignored.contains(ext);
    }

    private boolean isIgnoredByContentType(HttpResponse response, Set<String> ignoredContentTypes) {
        if (response == null || ignoredContentTypes == null || ignoredContentTypes.isEmpty()) return false;
        for (HttpHeader header : response.headers()) {
            if ("content-type".equalsIgnoreCase(header.name())) {
                String value = header.value();
                int semicolon = value.indexOf(';');
                String mimeType = (semicolon != -1) ? value.substring(0, semicolon).strip().toLowerCase()
                                                     : value.strip().toLowerCase();
                return ignoredContentTypes.contains(mimeType);
            }
        }
        return false;
    }
}
