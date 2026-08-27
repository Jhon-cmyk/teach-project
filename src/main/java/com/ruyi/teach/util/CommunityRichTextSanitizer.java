package com.ruyi.teach.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 社区富文本白名单过滤器。仅保留基础排版、链接和 HTTPS 图片标签。
 */
public final class CommunityRichTextSanitizer {

    private static final Set<String> ALLOWED_TAGS = Set.of(
            "p", "br", "strong", "b", "em", "i", "u", "s",
            "ol", "ul", "li", "blockquote", "code", "pre",
            "h1", "h2", "h3", "h4", "a", "img"
    );
    private static final Pattern COMMENT_PATTERN = Pattern.compile("(?is)<!--.*?-->");
    private static final Pattern DANGEROUS_BLOCK_PATTERN = Pattern.compile(
            "(?is)<(script|style|iframe|object|embed|form|svg|math)[^>]*>.*?</\\1\\s*>"
    );
    private static final Pattern TAG_PATTERN = Pattern.compile("(?is)<(/?)([a-z][a-z0-9]*)([^>]*)>");
    private static final Pattern SRC_PATTERN = Pattern.compile("(?i)\\bsrc\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
    private static final Pattern HREF_PATTERN = Pattern.compile("(?i)\\bhref\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
    private static final Pattern ALT_PATTERN = Pattern.compile("(?i)\\balt\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
    private static final Pattern TEXT_PATTERN = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("(?i)<img\\b");

    private CommunityRichTextSanitizer() {
    }

    public static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String source = COMMENT_PATTERN.matcher(input.trim()).replaceAll("");
        source = DANGEROUS_BLOCK_PATTERN.matcher(source).replaceAll("");

        Matcher matcher = TAG_PATTERN.matcher(source);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String closing = matcher.group(1);
            String tag = matcher.group(2).toLowerCase(Locale.ROOT);
            String attributes = matcher.group(3);
            String replacement = sanitizeTag(closing, tag, attributes);
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString().trim();
    }

    public static boolean hasMeaningfulContent(String html) {
        if (html == null || html.isBlank()) {
            return false;
        }
        if (IMAGE_PATTERN.matcher(html).find()) {
            return true;
        }
        String text = TEXT_PATTERN.matcher(html).replaceAll("")
                .replace("&nbsp;", " ")
                .replace("&#160;", " ")
                .trim();
        return !text.isEmpty();
    }

    private static String sanitizeTag(String closing, String tag, String attributes) {
        if (!ALLOWED_TAGS.contains(tag)) {
            return "";
        }
        if (!closing.isEmpty()) {
            return "br".equals(tag) || "img".equals(tag) ? "" : "</" + tag + ">";
        }
        if ("br".equals(tag)) {
            return "<br>";
        }
        if ("img".equals(tag)) {
            String src = readAttribute(SRC_PATTERN, attributes);
            if (!isHttpsUrl(src)) {
                return "";
            }
            String alt = escapeAttribute(readAttribute(ALT_PATTERN, attributes));
            return "<img src=\"" + escapeAttribute(src) + "\" alt=\"" + alt + "\">";
        }
        if ("a".equals(tag)) {
            String href = readAttribute(HREF_PATTERN, attributes);
            if (!isSafeLink(href)) {
                return "<a>";
            }
            return "<a href=\"" + escapeAttribute(href)
                    + "\" target=\"_blank\" rel=\"noopener noreferrer\">";
        }
        return "<" + tag + ">";
    }

    private static String readAttribute(Pattern pattern, String attributes) {
        Matcher matcher = pattern.matcher(attributes == null ? "" : attributes);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1) != null ? matcher.group(1).trim() : matcher.group(2).trim();
    }

    private static boolean isHttpsUrl(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).startsWith("https://");
    }

    private static boolean isSafeLink(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://") || lower.startsWith("mailto:");
    }

    private static String escapeAttribute(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
