package com.ruyi.teach.service;

import com.ruyi.teach.client.RemoteResourceClient;
import com.ruyi.teach.exception.BusinessException;
import com.ruyi.teach.exception.ErrorCode;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LessonPlanExportService {

    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)");
    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^\\s*\\d+[.)、]\\s+(.+)$");
    private static final Pattern HORIZONTAL_RULE_PATTERN = Pattern.compile("^\\s*[-*_]{3,}\\s*$");
    private static final Pattern INLINE_MARKDOWN_PATTERN = Pattern.compile("(\\*\\*([^*\\n]+?)\\*\\*)|`([^`\\n]+?)`");
    private static final int MAX_IMAGE_BYTES = 12 * 1024 * 1024;
    private static final double IMAGE_MAX_WIDTH_POINTS = 432.0;
    private static final double IMAGE_MAX_HEIGHT_POINTS = 280.0;
    private static final double PIXEL_TO_POINTS = 72.0 / 96.0;

    @Value("${plan.export.font-path:}")
    private String configuredFontPath;

    @Resource
    private RemoteResourceClient remoteResourceClient;

    public ExportedPlan export(String format, String title, String markdown) {
        String normalizedFormat = StringUtils.defaultIfBlank(format, "docx").toLowerCase(Locale.ROOT);
        String safeTitle = sanitizeFileName(StringUtils.defaultIfBlank(title, "AI lesson plan"));
        try {
            if ("pdf".equals(normalizedFormat)) {
                return new ExportedPlan(
                        exportPdf(StringUtils.defaultIfBlank(title, "AI lesson plan"), markdown),
                        "application/pdf",
                        safeTitle + ".pdf"
                );
            }
            return new ExportedPlan(
                    exportDocx(StringUtils.defaultIfBlank(title, "AI lesson plan"), markdown),
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    safeTitle + ".docx"
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "lesson plan export failed");
        }
    }

    private byte[] exportDocx(String title, String markdown) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontFamily("Microsoft YaHei");
            titleRun.setFontSize(18);

            boolean inCodeBlock = false;
            for (MarkdownBlock block : parseMarkdown(markdown)) {
                switch (block.type()) {
                    case HEADING -> addDocxHeading(document, block.text(), block.level());
                    case LIST_ITEM -> addDocxListItem(document, block.text());
                    case TABLE -> addDocxTable(document, block.tableRows());
                    case IMAGE -> addDocxImage(document, block.alt(), block.url());
                    case HORIZONTAL -> {
                    }
                    case CODE -> {
                        inCodeBlock = !inCodeBlock;
                    }
                    case PARAGRAPH -> addDocxParagraph(document, block.text(), inCodeBlock);
                    default -> {
                    }
                }
            }
            document.write(out);
            return out.toByteArray();
        }
    }

    private void addDocxHeading(XWPFDocument document, String text, int level) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(level == 1 ? 280 : 180);
        paragraph.setSpacingAfter(100);
        writeInlineRuns(paragraph, text, level == 1 ? 16 : level == 2 ? 14 : 12, false, true);
    }

    private void addDocxParagraph(XWPFDocument document, String text, boolean code) {
        if (StringUtils.isBlank(text)) {
            return;
        }
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(80);
        if (code) {
            addInlineRun(paragraph, text, 10, true, false);
            return;
        }
        writeInlineRuns(paragraph, text, code ? 10 : 11, code, false);
    }

    private void addDocxListItem(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setIndentationLeft(420);
        paragraph.setSpacingAfter(50);
        XWPFRun bullet = paragraph.createRun();
        bullet.setText("• ");
        bullet.setFontFamily("Microsoft YaHei");
        bullet.setFontSize(11);
        writeInlineRuns(paragraph, text, 11, false, false);
    }

    private void addDocxTable(XWPFDocument document, List<List<String>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        XWPFTable table = document.createTable(rows.size(), rows.get(0).size());
        for (int i = 0; i < rows.size(); i++) {
            XWPFTableRow row = table.getRow(i);
            for (int j = 0; j < rows.get(i).size(); j++) {
                XWPFTableCell cell = row.getCell(j);
                cell.removeParagraph(0);
                XWPFParagraph paragraph = cell.addParagraph();
                writeInlineRuns(paragraph, rows.get(i).get(j), 10, false, i == 0);
            }
        }
    }

    private void addDocxImage(XWPFDocument document, String alt, String url) {
        ImageBytes image = downloadImage(url);
        if (image == null) {
            addDocxParagraph(document, "[Image unavailable: " + StringUtils.defaultIfBlank(alt, url) + "]", false);
            return;
        }
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingBefore(100);
        paragraph.setSpacingAfter(120);
        XWPFRun run = paragraph.createRun();
        try (ByteArrayInputStream in = new ByteArrayInputStream(image.bytes())) {
            double originalWidthPoints = Math.max(1, image.width()) * PIXEL_TO_POINTS;
            double originalHeightPoints = Math.max(1, image.height()) * PIXEL_TO_POINTS;
            double ratio = Math.min(
                    1.0,
                    Math.min(IMAGE_MAX_WIDTH_POINTS / originalWidthPoints, IMAGE_MAX_HEIGHT_POINTS / originalHeightPoints)
            );
            double width = Math.max(120.0, originalWidthPoints * ratio);
            double height = Math.max(80.0, originalHeightPoints * ratio);
            run.addPicture(in, poiPictureType(image.contentType(), url), StringUtils.defaultIfBlank(alt, "case-image"),
                    Units.toEMU(width), Units.toEMU(height));
        } catch (Exception e) {
            addDocxParagraph(document, "[Image unavailable: " + StringUtils.defaultIfBlank(alt, url) + "]", false);
        }
    }

    private byte[] exportPdf(String title, String markdown) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFont font = loadPdfFont(document);
            boolean fallbackLatinFont = font instanceof PDType1Font;
            PdfWriter writer = new PdfWriter(document, font, fallbackLatinFont);
            writer.writeText(title, 18, true);
            writer.newLine(8);
            boolean inCodeBlock = false;
            for (MarkdownBlock block : parseMarkdown(markdown)) {
                switch (block.type()) {
                    case HEADING -> writer.writeText(plainInlineText(block.text()), block.level() == 1 ? 16 : 13, true);
                    case LIST_ITEM -> writer.writeText("• " + plainInlineText(block.text()), 11, false);
                    case TABLE -> {
                        for (List<String> row : block.tableRows()) {
                            writer.writeText(plainInlineText(String.join("    ", row)), 10, false);
                        }
                    }
                    case IMAGE -> writer.writeImage(block.alt(), block.url());
                    case HORIZONTAL -> writer.newLine(6);
                    case CODE -> inCodeBlock = !inCodeBlock;
                    case PARAGRAPH -> writer.writeText(plainInlineText(block.text()), inCodeBlock ? 10 : 11, false);
                    default -> {
                    }
                }
            }
            writer.close();
            document.save(out);
            return out.toByteArray();
        }
    }

    private List<MarkdownBlock> parseMarkdown(String markdown) {
        List<MarkdownBlock> blocks = new ArrayList<>();
        String[] lines = normalizeMarkdown(markdown).split("\n");
        List<List<String>> tableRows = new ArrayList<>();
        boolean inCode = false;
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine;
            String trimmed = line.trim();
            if (line.trim().startsWith("```")) {
                flushTable(blocks, tableRows);
                blocks.add(MarkdownBlock.code());
                inCode = !inCode;
                continue;
            }
            if (!inCode) {
                Matcher image = IMAGE_PATTERN.matcher(line.trim());
                if (image.matches()) {
                    flushTable(blocks, tableRows);
                    blocks.add(MarkdownBlock.image(image.group(1), image.group(2)));
                    continue;
                }
                if (HORIZONTAL_RULE_PATTERN.matcher(trimmed).matches()) {
                    flushTable(blocks, tableRows);
                    blocks.add(MarkdownBlock.horizontal());
                    continue;
                }
                if (isTableLine(line)) {
                    List<String> row = parseTableRow(line);
                    if (!isSeparatorRow(row)) {
                        tableRows.add(row);
                    }
                    continue;
                }
                flushTable(blocks, tableRows);
                if (StringUtils.isBlank(trimmed)) {
                    continue;
                }
                if (trimmed.startsWith("#")) {
                    int level = Math.min(3, countLeading(trimmed, '#'));
                    blocks.add(MarkdownBlock.heading(cleanInlineSource(trimmed.substring(level).trim()), level));
                } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                    blocks.add(MarkdownBlock.listItem(cleanInlineSource(trimmed.substring(2).trim())));
                } else {
                    Matcher ordered = ORDERED_LIST_PATTERN.matcher(trimmed);
                    if (ordered.matches()) {
                        blocks.add(MarkdownBlock.listItem(cleanInlineSource(ordered.group(1).trim())));
                    } else {
                        blocks.add(MarkdownBlock.paragraph(cleanInlineSource(line)));
                    }
                }
            } else {
                blocks.add(MarkdownBlock.paragraph(line));
            }
        }
        flushTable(blocks, tableRows);
        return blocks;
    }

    private String normalizeMarkdown(String markdown) {
        String normalized = StringUtils.defaultString(markdown)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        normalized = normalized
                .replaceAll("([^\\n])\\s*(#{1,6}\\s+)", "$1\n$2")
                .replaceAll("(?m)^\\s*[\"“”]+\\s*(#{1,6}\\s+)", "$1")
                .replaceAll("(?m)^\\s*[-*_]{3,}\\s*$", "---")
                .replaceAll("\\n{3,}", "\n\n");
        return normalized.trim();
    }

    private void flushTable(List<MarkdownBlock> blocks, List<List<String>> tableRows) {
        if (!tableRows.isEmpty()) {
            blocks.add(MarkdownBlock.table(new ArrayList<>(tableRows)));
            tableRows.clear();
        }
    }

    private boolean isTableLine(String line) {
        String trimmed = StringUtils.defaultString(line).trim();
        return trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.chars().filter(ch -> ch == '|').count() >= 2;
    }

    private List<String> parseTableRow(String line) {
        String trimmed = line.trim();
        trimmed = trimmed.substring(1, trimmed.length() - 1);
        List<String> cells = new ArrayList<>();
        for (String cell : trimmed.split("\\|")) {
            cells.add(cleanInlineSource(cell.trim()));
        }
        return cells;
    }

    private boolean isSeparatorRow(List<String> row) {
        return row.stream().allMatch(cell -> cell.replace(":", "").replace("-", "").trim().isEmpty());
    }

    private String stripInlineMarkdown(String text) {
        return plainInlineText(text);
    }

    private String cleanInlineSource(String text) {
        return StringUtils.defaultString(text)
                .replaceAll("\\*\\*\\s*【案例参考】\\s*\\*\\*", "")
                .replaceAll("\\*\\*\\s*【参考：E\\d+(?:[、,，\\s]*E\\d+)*】\\s*\\*\\*", "")
                .replaceAll("【(?:案例参考|参考：E\\d+(?:[、,，\\s]*E\\d+)*)】\\s*", "")
                .replaceAll("(?m)^\\s*\\*{4,}\\s*", "")
                .replaceAll("(?m)^\\s*\\*{2}\\s+", "")
                .replaceAll("[ \\t]+\\*{4,}(?=[ \\t]*(?:\\R|$))", "")
                .trim();
    }

    private String plainInlineText(String text) {
        String cleaned = cleanInlineSource(text);
        return INLINE_MARKDOWN_PATTERN.matcher(cleaned).replaceAll(match -> {
            String bold = match.group(2);
            String code = match.group(3);
            return Matcher.quoteReplacement(StringUtils.defaultString(bold != null ? bold : code));
        });
    }

    private void writeInlineRuns(XWPFParagraph paragraph, String text, int fontSize, boolean codeDefault, boolean boldDefault) {
        String cleaned = cleanInlineSource(text);
        Matcher matcher = INLINE_MARKDOWN_PATTERN.matcher(cleaned);
        int cursor = 0;
        while (matcher.find()) {
            addInlineRun(paragraph, cleaned.substring(cursor, matcher.start()), fontSize, codeDefault, boldDefault);
            if (matcher.group(2) != null) {
                addInlineRun(paragraph, matcher.group(2), fontSize, false, true);
            } else {
                addInlineRun(paragraph, matcher.group(3), fontSize, true, boldDefault);
            }
            cursor = matcher.end();
        }
        addInlineRun(paragraph, cleaned.substring(cursor), fontSize, codeDefault, boldDefault);
    }

    private void addInlineRun(XWPFParagraph paragraph, String text, int fontSize, boolean code, boolean bold) {
        if (StringUtils.isEmpty(text)) {
            return;
        }
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontFamily(code ? "Consolas" : "Microsoft YaHei");
        run.setFontSize(fontSize);
        run.setBold(bold);
    }

    private int countLeading(String text, char ch) {
        int count = 0;
        while (count < text.length() && text.charAt(count) == ch) {
            count++;
        }
        return count;
    }

    private ImageBytes downloadImage(String url) {
        try {
            URI uri = URI.create(url);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                Path path = Path.of(uri);
                byte[] bytes = Files.readAllBytes(path);
                return buildImageBytes(bytes, Files.probeContentType(path));
            }
            return remoteResourceClient.withStream(
                    "lesson-plan-image",
                    url,
                    Duration.ofSeconds(15),
                    remote -> {
                byte[] bytes = remote.body().readNBytes(MAX_IMAGE_BYTES + 1);
                return buildImageBytes(bytes, remote.contentType());
                    }
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private ImageBytes buildImageBytes(byte[] bytes, String contentType) throws Exception {
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            return null;
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            return null;
        }
        return new ImageBytes(bytes, contentType, image.getWidth(), image.getHeight());
    }

    private int poiPictureType(String contentType, String url) {
        String lower = (StringUtils.defaultString(contentType) + " " + StringUtils.defaultString(url)).toLowerCase(Locale.ROOT);
        if (lower.contains("png")) {
            return Document.PICTURE_TYPE_PNG;
        }
        if (lower.contains("gif")) {
            return Document.PICTURE_TYPE_GIF;
        }
        if (lower.contains("bmp")) {
            return Document.PICTURE_TYPE_BMP;
        }
        return Document.PICTURE_TYPE_JPEG;
    }

    private PDFont loadPdfFont(PDDocument document) throws Exception {
        for (String path : fontCandidates()) {
            if (StringUtils.isBlank(path)) {
                continue;
            }
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                try {
                    return PDType0Font.load(document, file);
                } catch (Exception ignored) {
                }
            }
        }
        return PDType1Font.HELVETICA;
    }

    private List<String> fontCandidates() {
        List<String> result = new ArrayList<>();
        result.add(configuredFontPath);
        result.add("C:/Windows/Fonts/msyh.ttf");
        result.add("C:/Windows/Fonts/simhei.ttf");
        result.add("C:/Windows/Fonts/msyh.ttc");
        result.add("/usr/share/fonts/truetype/wqy/wqy-microhei.ttc");
        result.add("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc");
        return result;
    }

    private String sanitizeFileName(String value) {
        String safe = StringUtils.defaultIfBlank(value, "AI lesson plan").replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
        return StringUtils.abbreviate(safe, 80);
    }

    private record ImageBytes(byte[] bytes, String contentType, int width, int height) {
    }

    public record ExportedPlan(byte[] bytes, String contentType, String fileName) {
    }

    private enum MarkdownType {
        HEADING, PARAGRAPH, LIST_ITEM, TABLE, IMAGE, HORIZONTAL, CODE
    }

    private record MarkdownBlock(MarkdownType type, String text, int level, String alt, String url, List<List<String>> tableRows) {
        static MarkdownBlock heading(String text, int level) {
            return new MarkdownBlock(MarkdownType.HEADING, text, level, null, null, null);
        }

        static MarkdownBlock paragraph(String text) {
            return new MarkdownBlock(MarkdownType.PARAGRAPH, text, 0, null, null, null);
        }

        static MarkdownBlock listItem(String text) {
            return new MarkdownBlock(MarkdownType.LIST_ITEM, text, 0, null, null, null);
        }

        static MarkdownBlock image(String alt, String url) {
            return new MarkdownBlock(MarkdownType.IMAGE, null, 0, alt, url, null);
        }

        static MarkdownBlock horizontal() {
            return new MarkdownBlock(MarkdownType.HORIZONTAL, null, 0, null, null, null);
        }

        static MarkdownBlock table(List<List<String>> rows) {
            return new MarkdownBlock(MarkdownType.TABLE, null, 0, null, null, rows);
        }

        static MarkdownBlock code() {
            return new MarkdownBlock(MarkdownType.CODE, null, 0, null, null, null);
        }
    }

    private class PdfWriter {
        private final PDDocument document;
        private final PDFont font;
        private final boolean fallbackLatinFont;
        private final float margin = 54;
        private final float pageWidth = PDRectangle.A4.getWidth();
        private final float pageHeight = PDRectangle.A4.getHeight();
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        PdfWriter(PDDocument document, PDFont font, boolean fallbackLatinFont) throws Exception {
            this.document = document;
            this.font = font;
            this.fallbackLatinFont = fallbackLatinFont;
            newPage();
        }

        void writeText(String text, int fontSize, boolean bold) throws Exception {
            String value = safeText(StringUtils.defaultString(text));
            if (StringUtils.isBlank(value)) {
                newLine(8);
                return;
            }
            for (String paragraph : value.split("\n")) {
                for (String line : wrap(paragraph, fontSize, pageWidth - margin * 2)) {
                    ensureSpace(fontSize + 8);
                    stream.beginText();
                    stream.setFont(font, fontSize);
                    stream.newLineAtOffset(margin, y);
                    stream.showText(line);
                    stream.endText();
                    y -= fontSize + 7;
                }
                newLine(3);
            }
            newLine(bold ? 4 : 2);
        }

        void writeImage(String alt, String url) throws Exception {
            ImageBytes image = downloadImage(url);
            if (image == null) {
                writeText("[Image unavailable: " + StringUtils.defaultIfBlank(alt, url) + "]", 10, false);
                return;
            }
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, image.bytes(), StringUtils.defaultIfBlank(alt, "case-image"));
            float maxWidth = pageWidth - margin * 2;
            float maxHeight = 300;
            float ratio = Math.min(maxWidth / Math.max(1, image.width()), maxHeight / Math.max(1, image.height()));
            ratio = Math.min(1.0f, ratio);
            float width = image.width() * ratio;
            float height = image.height() * ratio;
            ensureSpace(height + 18);
            stream.drawImage(pdImage, margin, y - height, width, height);
            y -= height + 14;
        }

        void newLine(float amount) throws Exception {
            y -= amount;
            ensureSpace(20);
        }

        void close() throws Exception {
            if (stream != null) {
                stream.close();
            }
        }

        private void newPage() throws Exception {
            if (stream != null) {
                stream.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = pageHeight - margin;
        }

        private void ensureSpace(float needed) throws Exception {
            if (y - needed < margin) {
                newPage();
            }
        }

        private List<String> wrap(String text, int fontSize, float maxWidth) throws Exception {
            List<String> result = new ArrayList<>();
            String value = StringUtils.defaultString(text);
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                String candidate = line + String.valueOf(ch);
                if (line.length() > 0 && textWidth(candidate, fontSize) > maxWidth) {
                    result.add(line.toString());
                    line.setLength(0);
                }
                line.append(ch);
            }
            if (!line.isEmpty()) {
                result.add(line.toString());
            }
            return result.isEmpty() ? List.of("") : result;
        }

        private float textWidth(String text, int fontSize) throws Exception {
            return font.getStringWidth(safeText(text)) / 1000 * fontSize;
        }

        private String safeText(String text) {
            if (!fallbackLatinFont) {
                return text;
            }
            return text.replaceAll("[^\\x20-\\x7E]", "?");
        }
    }
}
