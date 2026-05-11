import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public record ParsingConfig(
        boolean proxyHistory,
        boolean proxyHistoryResponse,
        boolean siteMap,
        boolean siteMapResponse,
        boolean responseHeader,
        String responseHeaderRegex,
        boolean responseBody,
        String responseBodyRegex,
        String outputFile,
        Set<String> ignoredExtensions,
        boolean ignoreContentType,
        Set<String> ignoredContentTypes) {

    public static final String DEFAULT_IGNORED_EXTENSIONS =
            "gif,jpg,jpeg,png,css,css2,mp3,mp4,wav,ico,map,woff,woff2,svg,ttf,pdf,otf,doc,docx";

    public static final String DEFAULT_IGNORED_CONTENT_TYPES =
            "text/javascript,application/javascript,application/x-javascript,image/png,image/jpeg,image/gif,image/svg+xml,image/webp,image/x-icon,text/css,audio/mpeg,audio/ogg,video/mp4,font/woff,font/woff2,font/ttf,font/otf,application/pdf,application/font-woff,application/font-woff2,application/octet-stream";

    public static Set<String> parseExtensions(String input) {
        return parseCommaSeparated(input);
    }

    public static Set<String> parseContentTypes(String input) {
        return parseCommaSeparated(input);
    }

    private static Set<String> parseCommaSeparated(String input) {
        if (input == null || input.isBlank()) return Collections.emptySet();
        Set<String> result = new HashSet<>();
        for (String part : input.split(",")) {
            String trimmed = part.strip().toLowerCase();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
