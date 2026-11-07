package kg.bakai.stopfactor.util;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NameTemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z0-9_]+)}");
    private static final Pattern OPTIONAL_SEGMENT = Pattern.compile("\\[([^\\[\\]]*)]");

    private NameTemplateRenderer() {
    }

    public static String render(String template, Map<String, Object> params) {
        Matcher segments = OPTIONAL_SEGMENT.matcher(template);
        StringBuilder withSegments = new StringBuilder();
        while (segments.find()) {
            String segment = segments.group(1);
            boolean complete = placeholders(segment).stream().allMatch(key -> params.get(key) != null);
            segments.appendReplacement(withSegments, Matcher.quoteReplacement(complete ? segment : ""));
        }
        segments.appendTail(withSegments);

        Matcher placeholders = PLACEHOLDER.matcher(withSegments);
        StringBuilder rendered = new StringBuilder();
        while (placeholders.find()) {
            Object value = params.get(placeholders.group(1));
            placeholders.appendReplacement(rendered, Matcher.quoteReplacement(value != null ? value.toString() : ""));
        }
        placeholders.appendTail(rendered);
        return rendered.toString().replaceAll("\\s{2,}", " ").trim();
    }

    public static Set<String> placeholders(String template) {
        Set<String> keys = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return keys;
    }
}
