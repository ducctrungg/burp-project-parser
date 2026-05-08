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
        Set<String> ignoredExtensions) {

    public static final String DEFAULT_IGNORED_EXTENSIONS =
            "gif,jpg,jpeg,png,css,css2,mp3,mp4,wav,ico,map,woff,woff2,svg,ttf,pdf,otf,doc,docx";

    public static Set<String> parseExtensions(String input) {
        if (input == null || input.isBlank()) return Collections.emptySet();
        Set<String> result = new HashSet<>();
        for (String ext : input.split(",")) {
            String trimmed = ext.strip().toLowerCase();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
