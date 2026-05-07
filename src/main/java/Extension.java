import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.proxy.Proxy;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.sitemap.SiteMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        writeOutput(String.join(" ", args));

        boolean proceed = false;

        if (containsAny(args, "auditItems", "proxyHistory", "siteMap", "responseHeader", "responseBody", "outputFile")) {
            proceed = true;
        } else {
            writeOutput("{\"Message\":\"No flags provided, assuming the initial load of extension.\"}");
            return;
        }

        for (String arg : args) {
            if (arg.startsWith("outputFile=")) {
                try {
                    fileWriter = new PrintWriter(new FileWriter(arg.substring("outputFile=".length())), true);
                } catch (IOException e) {
                    writeError("Failed to open output file: " + e.getMessage());
                }
            }
        }

        if (contains(args, "proxyHistory")) {
            printProxyHistory(proxy.history(), args);
        }

        if (contains(args, "auditItems")) {
            printAuditItems();
        }

        if (contains(args, "siteMap")) {
            printHistory(siteMap.requestResponses(), args);
        }

        boolean responseHeader = false;
        boolean responseBody = false;
        String regex = "";

        for (String arg : args) {
            if (arg.startsWith("responseHeader=")) {
                responseHeader = true;
                regex = arg.split("=")[1];
            } else if (arg.startsWith("responseBody=")) {
                responseBody = true;
                regex = arg.split("=")[1];
            }
        }

        if (responseHeader || responseBody) {
            processResponses(responseHeader, responseBody, regex);
        }

        if (proceed) {
            logging.logToOutput("{\"Message\":\"Project File Parsing Complete\"}");
            api.extension().unload();
            api.burpSuite().shutdown();
        }
    }

    private void printHistory(List<HttpRequestResponse> history, String[] args) {
        for (HttpRequestResponse reqRes : history) {
            try {
                JsonObject jsonOutput = new JsonObject();

                HttpRequest request = reqRes.request();
                JsonObject jsonRequest = new JsonObject();
                jsonRequest.addProperty("url", request.url());
                jsonRequest.add("headers", headersToJsonArray(request.headers()));
                jsonRequest.addProperty("body", request.bodyToString());
                jsonOutput.add("request", jsonRequest);

                if (containsAny(args, "response", "both") && reqRes.response() != null) {
                    HttpResponse response = reqRes.response();
                    JsonObject jsonResponse = new JsonObject();
                    jsonResponse.add("headers", headersToJsonArray(response.headers()));
                    jsonResponse.addProperty("body", response.bodyToString());
                    jsonOutput.add("response", jsonResponse);
                }

                if (!jsonOutput.entrySet().isEmpty()) {
                    writeOutput(jsonOutput.toString());
                }
            } catch (Exception e) {
                writeOutput("Error processing request/response: " + e.getMessage());
            }
        }
    }

    private void printProxyHistory(List<ProxyHttpRequestResponse> history, String[] args) {
        boolean containsResponse = containsAny(args, "response", "both");

        for (ProxyHttpRequestResponse reqRes : history) {
            try {
                JsonObject jsonOutput = new JsonObject();

                HttpRequest request = reqRes.request();
                JsonObject jsonRequest = new JsonObject();
                jsonRequest.addProperty("url", request.url()+request.query());
                jsonRequest.add("headers", headersToJsonArray(request.headers()));
                jsonRequest.addProperty("body", request.bodyToString());
                jsonOutput.add("request", jsonRequest);

                if (containsResponse && reqRes.response() != null) {
                    HttpResponse response = reqRes.response();
                    JsonObject jsonResponse = new JsonObject();
                    jsonResponse.add("response-headers", headersToJsonArray(response.headers()));
                    jsonResponse.addProperty("response-body", response.bodyToString());
                    jsonOutput.add("response", jsonResponse);
                }

                if (!jsonOutput.entrySet().isEmpty()) {
                    writeOutput(jsonOutput.toString());
                }
            } catch (Exception e) {
                writeOutput("Error processing request/response: " + e.getMessage());
            }
        }
    }

    private JsonElement headersToJsonArray(List<HttpHeader> headers) {
        JsonArray jsonHeaders = new JsonArray();
        for (HttpHeader header : headers) {
            jsonHeaders.add(header.toString());
        }
        return jsonHeaders;
    }

    private void printAuditItems() {
        List<AuditIssue> issues = siteMap.issues();

        for (AuditIssue issue : issues) {
            issueToJson(issue);
        }
    }

    private void processResponses(boolean responseHeader, boolean responseBody, String regex) {
        Pattern pattern = Pattern.compile(regex);

        for (ProxyHttpRequestResponse reqRes : proxy.history()) {
            try {
                if (reqRes.response() == null) continue;

                String url = reqRes.request().url();
                List<HttpHeader> responseHeaders = reqRes.response().headers();
                String responseBodyStr = reqRes.response().bodyToString();

                if (responseHeader) {
                    for (HttpHeader header : responseHeaders) {
                        if (pattern.matcher(header.toString()).find()) {
                            JsonObject output = new JsonObject();
                            output.addProperty("url", url);
                            output.addProperty("header", header.toString());
                            writeOutput(output.toString());
                        }
                    }
                }

                if (responseBody) {
                    if (pattern.matcher(responseBodyStr).find()) {
                        JsonObject output = new JsonObject();
                        output.addProperty("url", url);
                        output.addProperty("body", responseBodyStr);
                        writeOutput(output.toString());
                    }
                }

            } catch (Exception e) {
                writeError(e.getMessage());
            }
        }
    }

    private void issueToJson(AuditIssue auditIssue) {
        Map<String, Object> issueMap = new HashMap<>();

        issueMap.put("name", auditIssue.name());
        issueMap.put("severity", auditIssue.severity().toString());
        issueMap.put("confidence", auditIssue.confidence().toString());
        issueMap.put("host", auditIssue.httpService().host());
        issueMap.put("port", auditIssue.httpService().port());
        issueMap.put("protocol", auditIssue.httpService().secure() ? "https" : "http");
        issueMap.put("url", auditIssue.baseUrl());

        Gson gson = new Gson();

        String json = gson.toJson(issueMap);
        writeOutput(json);
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

    // Package-private for testing
    boolean contains(String[] args, String flag) {
        for (String arg : args) {
            if (arg.contains(flag)) {
                return true;
            }
        }
        return false;
    }

    // Package-private for testing
    boolean containsAny(String[] args, String... flags) {
        for (String flag : flags) {
            if (contains(args, flag)) {
                return true;
            }
        }
        return false;
    }
}