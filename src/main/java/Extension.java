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
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Extension implements BurpExtension {
    private MontoyaApi api;
    private Logging logging;
    private Proxy proxy;
    private SiteMap siteMap;
    private PrintWriter fileWriter;

    @Override
    public void initialize(MontoyaApi montoyaApi) {
        this.api = montoyaApi;
        this.logging = api.logging();
        this.proxy = api.proxy();
        this.siteMap = api.siteMap();

        montoyaApi.extension().setName("BurpSuite Project File Parser");

        api.extension().registerUnloadingHandler(() -> {
            if (fileWriter != null) {
                fileWriter.close();
            }
        });

        String[] args = api.burpSuite().commandLineArguments().toArray(new String[0]);

        if (hasAnyActionFlag(args)) {
            ParsingConfig config = ParsingConfig.fromCliArgs(args);
            writeOutput(String.join(" ", args));
            openOutputFile(config.outputFile());
            executeParsing(config);
            logging.logToOutput("{\"Message\":\"Project File Parsing Complete\"}");
            closeOutputFile();
            api.extension().unload();
            api.burpSuite().shutdown();
        } else {
            api.userInterface().registerSuiteTab("BurpSuite Extractor", new ParserPanel(api, this::executeFromGui));
        }
    }

    private void executeFromGui(ParsingConfig config) {
        writeOutput("Running with selected options...");
        openOutputFile(config.outputFile());
        executeParsing(config);
        closeOutputFile();
    }

    private void executeParsing(ParsingConfig config) {
        Set<String> ignored = config.ignoredExtensions();
        if (config.proxyHistory()) {
            printProxyHistory(proxy.history(), config.proxyHistoryResponse(), ignored);
        }
        if (config.siteMap()) {
            printHistory(siteMap.requestResponses(), config.siteMapResponse(), ignored);
        }
        if (config.responseHeader()) {
            processResponseHeaders(proxy.history(), config.responseHeaderRegex(), ignored);
        }
        if (config.responseBody()) {
            processResponseBodies(proxy.history(), config.responseBodyRegex(), ignored);
        }
    }

    private void openOutputFile(String path) {
        if (path == null || path.isEmpty()) return;
        try {
            fileWriter = new PrintWriter(new FileWriter(path), true);
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

    private void printHistory(List<HttpRequestResponse> history, boolean includeResponse, Set<String> ignored) {
        writeCsvHeader(includeResponse);
        int no = 1;
        for (HttpRequestResponse reqRes : history) {
            try {
                String url = reqRes.request().url();
                if (isIgnored(url, ignored)) continue;

                String host = extractHost(url);
                String method = reqRes.request().method();
                String headers = formatHeaders(reqRes.request().headers());
                String body = reqRes.request().bodyToString();
                String statusCode = "";
                String responseBody = "";

                if (reqRes.response() != null) {
                    statusCode = String.valueOf(reqRes.response().statusCode());
                    responseBody = reqRes.response().bodyToString();
                }

                String line = no + ","
                        + escapeCsv(host) + ","
                        + method + ","
                        + escapeCsv(url) + ","
                        + escapeCsv(headers) + ","
                        + escapeCsv(body) + ","
                        + statusCode;

                if (includeResponse) {
                    line += "," + escapeCsv(responseBody);
                }

                writeOutput(line);
                no++;
            } catch (Exception e) {
                writeOutput("Error processing request/response: " + e.getMessage());
            }
        }
    }

    private void printProxyHistory(List<ProxyHttpRequestResponse> history, boolean includeResponse, Set<String> ignored) {
        writeCsvHeader(includeResponse);
        int no = 1;
        for (ProxyHttpRequestResponse reqRes : history) {
            try {
                String url = reqRes.request().url() + reqRes.request().query();
                if (isIgnored(url, ignored)) continue;

                String host = extractHost(url);
                String method = reqRes.request().method();
                String headers = formatHeaders(reqRes.request().headers());
                String body = reqRes.request().bodyToString();
                String statusCode = "";
                String responseBody = "";

                if (reqRes.response() != null) {
                    statusCode = String.valueOf(reqRes.response().statusCode());
                    responseBody = reqRes.response().bodyToString();
                }

                String line = no + ","
                        + escapeCsv(host) + ","
                        + method + ","
                        + escapeCsv(url) + ","
                        + escapeCsv(headers) + ","
                        + escapeCsv(body) + ","
                        + statusCode;

                if (includeResponse) {
                    line += "," + escapeCsv(responseBody);
                }

                writeOutput(line);
                no++;
            } catch (Exception e) {
                writeOutput("Error processing request/response: " + e.getMessage());
            }
        }
    }

    private void processResponseHeaders(List<ProxyHttpRequestResponse> history, String regex, Set<String> ignored) {
        Pattern pattern = Pattern.compile(regex);
        for (ProxyHttpRequestResponse reqRes : history) {
            try {
                if (reqRes.response() == null) continue;
                String url = reqRes.request().url();
                if (isIgnored(url, ignored)) continue;
                for (HttpHeader header : reqRes.response().headers()) {
                    if (pattern.matcher(header.toString()).find()) {
                        JsonObject output = new JsonObject();
                        output.addProperty("url", url);
                        output.addProperty("header", header.toString());
                        writeOutput(output.toString());
                    }
                }
            } catch (Exception e) {
                writeError(e.getMessage());
            }
        }
    }

    private void processResponseBodies(List<ProxyHttpRequestResponse> history, String regex, Set<String> ignored) {
        Pattern pattern = Pattern.compile(regex);
        for (ProxyHttpRequestResponse reqRes : history) {
            try {
                if (reqRes.response() == null) continue;
                String url = reqRes.request().url();
                if (isIgnored(url, ignored)) continue;
                String body = reqRes.response().bodyToString();
                if (pattern.matcher(body).find()) {
                    JsonObject output = new JsonObject();
                    output.addProperty("url", url);
                    output.addProperty("body", body);
                    writeOutput(output.toString());
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

    private boolean hasAnyActionFlag(String[] args) {
        for (String arg : args) {
            if (arg.contains("proxyHistory") || arg.contains("siteMap")
                    || arg.startsWith("responseHeader=") || arg.startsWith("responseBody=")
                    || arg.startsWith("ignoreExt=") || arg.startsWith("outputFile=")) {
                return true;
            }
        }
        return false;
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
}
