package com.ruyi.teach.util;

import com.ruyi.teach.model.entity.TeachingCaseAsset;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CaseDocumentPreviewExtractor {

    private static final int MAX_HTML_LENGTH = 180_000;

    private CaseDocumentPreviewExtractor() {
    }

    public static String extractDocxHtml(byte[] bytes, List<TeachingCaseAsset> imageAssets) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        Map<String, TeachingCaseAsset> assetByHash = new HashMap<>();
        if (imageAssets != null) {
            for (TeachingCaseAsset asset : imageAssets) {
                if (asset != null && StringUtils.isNotBlank(asset.getHash()) && StringUtils.isNotBlank(asset.getUrl())) {
                    assetByHash.put(asset.getHash(), asset);
                }
            }
        }

        StringBuilder html = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            for (IBodyElement element : document.getBodyElements()) {
                if (element.getElementType() == BodyElementType.PARAGRAPH && element instanceof XWPFParagraph paragraph) {
                    appendParagraph(html, paragraph, assetByHash);
                } else if (element.getElementType() == BodyElementType.TABLE && element instanceof XWPFTable table) {
                    appendTable(html, table);
                }
                if (html.length() > MAX_HTML_LENGTH) {
                    html.append("<p class=\"preview-truncated\">内容较长，已截断显示。</p>");
                    break;
                }
            }
        } catch (Exception ignored) {
            return "";
        }
        return html.toString();
    }

    private static void appendParagraph(StringBuilder html, XWPFParagraph paragraph, Map<String, TeachingCaseAsset> assetByHash) {
        StringBuilder content = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            for (XWPFPicture picture : run.getEmbeddedPictures()) {
                byte[] data = picture.getPictureData() == null ? null : picture.getPictureData().getData();
                String hash = data == null ? "" : CaseDocumentImageExtractor.sha256Hex(data);
                TeachingCaseAsset asset = assetByHash.get(hash);
                if (asset != null && StringUtils.isNotBlank(asset.getUrl())) {
                    content.append("<figure class=\"case-preview-figure\"><img src=\"")
                            .append(escapeAttr(asset.getUrl()))
                            .append("\" alt=\"")
                            .append(escapeAttr(StringUtils.defaultIfBlank(asset.getTitle(), "案例图片")))
                            .append("\"/><figcaption>")
                            .append(escapeText(StringUtils.defaultIfBlank(asset.getCaption(), asset.getTitle())))
                            .append("</figcaption></figure>");
                }
            }
            String text = run.text();
            if (StringUtils.isBlank(text)) {
                continue;
            }
            String escaped = escapeText(text).replace("\n", "<br/>");
            if (run.isBold()) {
                escaped = "<strong>" + escaped + "</strong>";
            }
            content.append(escaped);
        }
        if (StringUtils.isBlank(content.toString())) {
            return;
        }
        String style = paragraph.getStyle();
        if (StringUtils.defaultString(style).toLowerCase().contains("heading")) {
            html.append("<h3>").append(content).append("</h3>");
        } else {
            html.append("<p>").append(content).append("</p>");
        }
    }

    private static void appendTable(StringBuilder html, XWPFTable table) {
        html.append("<table class=\"case-preview-table\"><tbody>");
        for (XWPFTableRow row : table.getRows()) {
            html.append("<tr>");
            for (XWPFTableCell cell : row.getTableCells()) {
                html.append("<td>");
                String text = cell.getText();
                html.append(escapeText(text).replace("\n", "<br/>"));
                html.append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
    }

    private static String escapeText(String text) {
        return StringUtils.defaultString(text)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String escapeAttr(String text) {
        return escapeText(text).replace("\"", "&quot;");
    }
}
