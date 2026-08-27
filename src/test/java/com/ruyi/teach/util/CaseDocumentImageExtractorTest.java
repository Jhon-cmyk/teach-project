package com.ruyi.teach.util;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseDocumentImageExtractorTest {

    @Test
    void extractDocxImagesDeduplicatesEmbeddedPictures() throws Exception {
        byte[] image = pngBytes();
        byte[] docx;
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFRun run = document.createParagraph().createRun();
            run.addPicture(new ByteArrayInputStream(image), Document.PICTURE_TYPE_PNG, "case.png", Units.toEMU(120), Units.toEMU(90));
            run.addPicture(new ByteArrayInputStream(image), Document.PICTURE_TYPE_PNG, "case-duplicate.png", Units.toEMU(120), Units.toEMU(90));
            document.write(out);
            docx = out.toByteArray();
        }

        List<CaseDocumentImageExtractor.ExtractedImage> images = CaseDocumentImageExtractor.extractDocxImages(docx);

        assertEquals(1, images.size());
        assertEquals("image/png", images.get(0).contentType());
        assertTrue(images.get(0).bytes().length > 0);
        assertNotNull(images.get(0).hash());
    }

    private byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 32, 24);
        graphics.setColor(Color.BLUE);
        graphics.fillRect(4, 4, 24, 16);
        graphics.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }
}
