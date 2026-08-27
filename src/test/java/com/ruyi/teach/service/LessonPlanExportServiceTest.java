package com.ruyi.teach.service;

import org.apache.poi.util.Units;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonPlanExportServiceTest {

    @Test
    void exportDocxEmbedsMarkdownImages() throws Exception {
        LessonPlanExportService service = new LessonPlanExportService();
        Path image = Files.createTempFile("lesson-plan-image", ".png");
        Files.write(image, pngBytes(40, 28));
        String markdown = "# Test Plan\n\nTeaching content.\n\n![case image](" + image.toUri() + ")";

        LessonPlanExportService.ExportedPlan exported = service.export("docx", "Test Plan", markdown);

        assertTrue(exported.bytes().length > 0);
        assertTrue(zipContainsWordMedia(exported.bytes()));
    }

    @Test
    void exportDocxCleansMarkdownSyntaxForWord() throws Exception {
        LessonPlanExportService service = new LessonPlanExportService();
        String markdown = """
                # 冒泡排序

                - **知识与技能**：能说出冒泡排序的基本原理。


                教师总结：“大数像气泡一样逐渐浮到末尾。”### 2. 明确学习目标（2分钟）

                ---

                普通段落包含 `swap` 操作说明。
                """;

        LessonPlanExportService.ExportedPlan exported = service.export("docx", "冒泡排序", markdown);
        String documentXml = wordDocumentXml(exported.bytes());

        assertFalse(documentXml.contains("**"));
        assertFalse(documentXml.contains("###"));
        assertFalse(documentXml.contains("---"));
        assertTrue(documentXml.contains("知识与技能"));
        assertTrue(documentXml.contains("明确学习目标"));
        assertTrue(documentXml.contains("<w:b"));
    }

    @Test
    void exportDocxConstrainsWideImages() throws Exception {
        LessonPlanExportService service = new LessonPlanExportService();
        Path image = Files.createTempFile("lesson-plan-wide-image", ".png");
        Files.write(image, pngBytes(1400, 760));
        String markdown = "# Test Plan\n\n![wide image](" + image.toUri() + ")";

        LessonPlanExportService.ExportedPlan exported = service.export("docx", "Test Plan", markdown);
        List<Long> widths = imageWidths(exported.bytes());

        assertFalse(widths.isEmpty());
        assertTrue(widths.stream().allMatch(width -> width <= Units.toEMU(432.0)));
    }

    @Test
    void exportPdfSurvivesMissingImages() throws Exception {
        LessonPlanExportService service = new LessonPlanExportService();
        Path missingImage = Files.createTempFile("lesson-plan-missing-image", ".png");
        Files.delete(missingImage);
        String markdown = "# Test Plan\n\n![missing](" + missingImage.toUri() + ")\n\nStill readable.";

        LessonPlanExportService.ExportedPlan exported = service.export("pdf", "Test Plan", markdown);

        assertTrue(new String(exported.bytes(), 0, 4).startsWith("%PDF"));
    }

    private boolean zipContainsWordMedia(byte[] bytes) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().startsWith("word/media/")) {
                    return true;
                }
            }
        }
        return false;
    }

    private String wordDocumentXml(byte[] bytes) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        return "";
    }

    private List<Long> imageWidths(byte[] bytes) throws Exception {
        Pattern extent = Pattern.compile("<wp:extent cx=\"(\\d+)\"");
        Matcher matcher = extent.matcher(wordDocumentXml(bytes));
        List<Long> widths = new ArrayList<>();
        while (matcher.find()) {
            widths.add(Long.parseLong(matcher.group(1)));
        }
        return widths;
    }

    private byte[] pngBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.RED);
        graphics.fillOval(width / 5, height / 6, Math.max(8, width / 2), Math.max(8, height / 2));
        graphics.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }
}
