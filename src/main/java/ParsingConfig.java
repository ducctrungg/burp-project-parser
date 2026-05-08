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

    public static ParsingConfig fromCliArgs(String[] args) {
        String ignoreExt = containsPrefix(args, "ignoreExt=");
        Set<String> extensions;
        if (ignoreExt == null) {
            extensions = parseExtensions(DEFAULT_IGNORED_EXTENSIONS);
        } else if (ignoreExt.equals("none")) {
            extensions = Collections.emptySet();
        } else {
            extensions = parseExtensions(ignoreExt);
        }

        return new ParsingConfig(
                contains(args, "proxyHistory"),
                containsAny(args, "proxyHistory.response", "proxyHistory.both"),
                contains(args, "siteMap"),
                containsAny(args, "siteMap.response", "siteMap.both"),
                containsPrefix(args, "responseHeader=") != null,
                containsPrefix(args, "responseHeader="),
                containsPrefix(args, "responseBody=") != null,
                containsPrefix(args, "responseBody="),
                containsPrefix(args, "outputFile="),
                extensions);
    }

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

    private static boolean contains(String[] args, String flag) {
        for (String arg : args) {
            if (arg.contains(flag)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String[] args, String... flags) {
        for (String flag : flags) {
            if (contains(args, flag)) {
                return true;
            }
        }
        return false;
    }

    private static String containsPrefix(String[] args, String prefix) {
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return null;
    }
}
