package com.campus.trade.file.util;

import org.springframework.web.multipart.MultipartFile;

public final class FileExtensionUtil {

    private FileExtensionUtil() {
    }

    public static String resolveExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            int index = originalName.lastIndexOf('.');
            if (index >= 0 && index < originalName.length() - 1) {
                return originalName.substring(index + 1).toLowerCase();
            }
        }

        String contentType = file.getContentType();
        if ("image/jpeg".equals(contentType)) {
            return "jpg";
        }
        if ("image/png".equals(contentType)) {
            return "png";
        }
        if ("image/webp".equals(contentType)) {
            return "webp";
        }
        return "bin";
    }
}
