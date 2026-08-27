package com.ruyi.teach.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

public final class CaseDocumentTextExtractor {

    private static final Pattern SCRIPT_STYLE_PATTERN = Pattern.compile("(?is)<(script|style|noscript)[^>]*>.*?</\\1>");
    private static final Pattern TAG_PATTERN = Pattern.compile("(?is)<[^>]+>");

    private CaseDocumentTextExtractor() {
    }

    public static String extractHtmlText(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        try {
            return extractHtml(new ByteArrayInputStream(bytes));
        } catch (Exception ignored) {
            return "";
        }
    }

    public static String extractText(byte[] bytes, String fileName) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        String lowerName = StringUtils.defaultString(fileName).toLowerCase(Locale.ROOT);
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            if (lowerName.endsWith(".docx")) {
                return extractDocx(in);
            }
            if (lowerName.endsWith(".doc")) {
                return extractDoc(in);
            }
            if (lowerName.endsWith(".pdf")) {
                return extractPdf(in);
            }
            return "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String extractPdf(InputStream in) throws Exception {
        try (PDDocument document = PDDocument.load(in)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private static String extractDocx(InputStream in) throws Exception {
        try (XWPFDocument document = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private static String extractDoc(InputStream in) throws Exception {
        try (HWPFDocument document = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private static String extractHtml(InputStream in) throws Exception {
        String html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        String text = SCRIPT_STYLE_PATTERN.matcher(html).replaceAll(" ");
        text = text.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</div>", "\n")
                .replaceAll("(?i)</li>", "\n");
        text = TAG_PATTERN.matcher(text).replaceAll(" ");
        text = decodeHtml(text);
        return text.replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n\\s*\\n+", "\n")
                .trim();
    }

    private static String decodeHtml(String text) {
        return StringUtils.defaultString(text)
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }
}
