package com.campus.trade.file.util;

import com.campus.trade.file.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ImageMetadataUtil {

    private ImageMetadataUtil() {
    }

    public static ImageMetadata read(MultipartFile file) {
        try {
            if ("image/webp".equals(file.getContentType())) {
                return readWebp(file.getBytes());
            }

            try (InputStream inputStream = file.getInputStream()) {
                BufferedImage image = ImageIO.read(inputStream);
                if (image == null) {
                    throw new BusinessException("图片内容无效");
                }
                return new ImageMetadata(image.getWidth(), image.getHeight());
            }
        } catch (IOException e) {
            throw new BusinessException("图片内容读取失败");
        }
    }

    private static ImageMetadata readWebp(byte[] bytes) {
        if (bytes.length < 30) {
            throw new BusinessException("图片内容无效");
        }
        if (!"RIFF".equals(new String(bytes, 0, 4, StandardCharsets.US_ASCII))
                || !"WEBP".equals(new String(bytes, 8, 4, StandardCharsets.US_ASCII))) {
            throw new BusinessException("图片内容无效");
        }

        String chunkType = new String(bytes, 12, 4, StandardCharsets.US_ASCII);
        if ("VP8 ".equals(chunkType)) {
            return readLossyWebp(bytes);
        }
        if ("VP8L".equals(chunkType)) {
            return readLosslessWebp(bytes);
        }
        if ("VP8X".equals(chunkType)) {
            return readExtendedWebp(bytes);
        }
        throw new BusinessException("图片内容无效");
    }

    private static ImageMetadata readLossyWebp(byte[] bytes) {
        if (bytes.length < 30 || bytes[23] != (byte) 0x9D || bytes[24] != 0x01 || bytes[25] != 0x2A) {
            throw new BusinessException("图片内容无效");
        }

        int width = readLittleEndian16(bytes, 26) & 0x3FFF;
        int height = readLittleEndian16(bytes, 28) & 0x3FFF;
        return validate(width, height);
    }

    private static ImageMetadata readLosslessWebp(byte[] bytes) {
        if (bytes.length < 25 || bytes[20] != 0x2F) {
            throw new BusinessException("图片内容无效");
        }

        int bits = (bytes[21] & 0xFF)
                | ((bytes[22] & 0xFF) << 8)
                | ((bytes[23] & 0xFF) << 16)
                | ((bytes[24] & 0xFF) << 24);
        int width = (bits & 0x3FFF) + 1;
        int height = ((bits >> 14) & 0x3FFF) + 1;
        return validate(width, height);
    }

    private static ImageMetadata readExtendedWebp(byte[] bytes) {
        if (bytes.length < 30) {
            throw new BusinessException("图片内容无效");
        }

        int width = readLittleEndian24(bytes, 24) + 1;
        int height = readLittleEndian24(bytes, 27) + 1;
        return validate(width, height);
    }

    private static ImageMetadata validate(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new BusinessException("图片内容无效");
        }
        return new ImageMetadata(width, height);
    }

    private static int readLittleEndian16(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

    private static int readLittleEndian24(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16);
    }

    public record ImageMetadata(int width, int height) {
    }
}
