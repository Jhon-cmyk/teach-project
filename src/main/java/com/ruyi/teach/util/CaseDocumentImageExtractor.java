package com.ruyi.teach.util;

import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CaseDocumentImageExtractor {

    private CaseDocumentImageExtractor() {
    }

    public static List<ExtractedImage> extractDocxImages(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return List.of();
        }
        List<ExtractedImage> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        try (InputStream inputStream = new ByteArrayInputStream(bytes);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            int order = 0;
            for (XWPFPictureData picture : document.getAllPictures()) {
                byte[] data = picture.getData();
                if (data == null || data.length == 0) {
                    continue;
                }
                String hash = sha256Hex(data);
                if (!seen.add(hash)) {
                    continue;
                }
                ImageSize size = readImageSize(data);
                String extension = normalizeExtension(picture.suggestFileExtension());
                result.add(new ExtractedImage(
                        data,
                        "case-image-" + (++order) + "." + extension,
                        contentType(extension),
                        hash,
                        size.width(),
                        size.height(),
                        order
                ));
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return result;
    }

    private static ImageSize readImageSize(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                return new ImageSize(null, null);
            }
            return new ImageSize(image.getWidth(), image.getHeight());
        } catch (Exception ignored) {
            return new ImageSize(null, null);
        }
    }

    private static String normalizeExtension(String extension) {
        String value = extension == null ? "png" : extension.toLowerCase(Locale.ROOT).replace(".", "");
        return switch (value) {
            case "jpg", "jpeg" -> "jpg";
            case "gif" -> "gif";
            case "bmp" -> "bmp";
            case "webp" -> "webp";
            default -> "png";
        };
    }

    private static String contentType(String extension) {
        return switch (extension) {
            case "jpg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            default -> "image/png";
        };
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ignored) {
            return String.valueOf(bytes.length);
        }
    }

    private record ImageSize(Integer width, Integer height) {
    }

    public record ExtractedImage(byte[] bytes,
                                 String fileName,
                                 String contentType,
                                 String hash,
                                 Integer width,
                                 Integer height,
                                 Integer order) {
    }
}
