package com.campus.trade.file.service.impl;

import com.campus.trade.file.config.FileStorageProperties;
import com.campus.trade.file.exception.BusinessException;
import com.campus.trade.file.service.StorageService;
import com.campus.trade.file.service.StoredFileInfo;
import com.campus.trade.file.util.FileBizType;
import com.campus.trade.file.util.FileConstants;
import com.campus.trade.file.util.FileExtensionUtil;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final int PREVIEW_MAX_EDGE = 1280;
    private static final int THUMBNAIL_MAX_EDGE = 320;
    private static final String WEBP_EXT = "webp";
    private static final String WEBP_MIME = "image/webp";
    private static final String PREVIEW_SUFFIX = ".preview";
    private static final String THUMBNAIL_SUFFIX = ".thumb";

    private final Path rootPath;

    public LocalStorageService(FileStorageProperties fileStorageProperties) {
        this.rootPath = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    @Override
    public StoredFileInfo store(MultipartFile file, String bizType) {
        String ext = FileExtensionUtil.resolveExtension(file);
        String datePath = DATE_FORMATTER.format(LocalDate.now());
        String fileBaseName = UUID.randomUUID().toString();
        String baseDir = FileBizType.resolveStorageDir(bizType) + "/" + datePath + "/";
        String originalRelativePath = baseDir + fileBaseName + "." + ext;
        Path originalPath = rootPath.resolve(originalRelativePath).normalize();
        validateTargetPath(originalPath);

        try {
            Files.createDirectories(originalPath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, originalPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(500, "文件存储失败");
        }

        BufferedImage sourceImage = readImageQuietly(file);
        String previewRelativePath = null;
        String thumbnailRelativePath = null;
        if (sourceImage != null) {
            previewRelativePath = writeVariant(sourceImage, baseDir, fileBaseName, PREVIEW_SUFFIX, ext, PREVIEW_MAX_EDGE);
            thumbnailRelativePath = writeVariant(sourceImage, baseDir, fileBaseName, THUMBNAIL_SUFFIX, ext, THUMBNAIL_MAX_EDGE);
        }

        return new StoredFileInfo(
                FileConstants.STORAGE_LOCAL,
                originalRelativePath.replace("\\", "/"),
                originalRelativePath.replace("\\", "/"),
                ext,
                normalizeRelativePath(previewRelativePath),
                normalizeRelativePath(thumbnailRelativePath)
        );
    }

    @Override
    public Resource loadAsResource(String storagePath) {
        try {
            Path filePath = rootPath.resolve(storagePath).normalize();
            validateTargetPath(filePath);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(404, "文件不存在");
            }
            return resource;
        } catch (IOException e) {
            throw new BusinessException(404, "文件不存在");
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path filePath = rootPath.resolve(storagePath).normalize();
            validateTargetPath(filePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new BusinessException(500, "文件删除失败");
        }
    }

    private void validateTargetPath(Path targetPath) {
        if (!targetPath.startsWith(rootPath)) {
            throw new BusinessException(400, "文件路径非法");
        }
    }

    private BufferedImage readImageQuietly(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            return null;
        }
    }

    private String writeVariant(BufferedImage sourceImage,
                                String baseDir,
                                String fileBaseName,
                                String suffix,
                                String originalExt,
                                int maxEdge) {
        BufferedImage resizedImage = resize(sourceImage, maxEdge);
        if (resizedImage == null) {
            return null;
        }

        String preferredExt = resolvePreferredVariantExt(resizedImage, originalExt);
        String relativePath = baseDir + fileBaseName + suffix + "." + preferredExt;
        Path targetPath = rootPath.resolve(relativePath).normalize();
        validateTargetPath(targetPath);

        try {
            Files.createDirectories(targetPath.getParent());
            writeImage(resizedImage, preferredExt, targetPath);
            return relativePath;
        } catch (IOException e) {
            throw new BusinessException(500, "图片变体生成失败");
        }
    }

    private BufferedImage resize(BufferedImage sourceImage, int maxEdge) {
        if (sourceImage == null) {
            return null;
        }
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }

        int longestEdge = Math.max(width, height);
        if (longestEdge <= maxEdge) {
            return copyImage(sourceImage);
        }

        double scale = (double) maxEdge / (double) longestEdge;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        int imageType = sourceImage.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D graphics = resizedImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return resizedImage;
    }

    private BufferedImage copyImage(BufferedImage sourceImage) {
        int imageType = sourceImage.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage copied = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), imageType);
        Graphics2D graphics = copied.createGraphics();
        try {
            graphics.drawImage(sourceImage, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copied;
    }

    private String resolvePreferredVariantExt(BufferedImage image, String originalExt) {
        if (canWriteFormat(WEBP_MIME)) {
            return WEBP_EXT;
        }
        if (image.getColorModel().hasAlpha() && canWriteFormat("png")) {
            return "png";
        }
        if (canWriteFormat("jpg")) {
            return "jpg";
        }
        return originalExt == null || originalExt.isBlank() ? "png" : originalExt;
    }

    private boolean canWriteFormat(String formatOrMimeType) {
        if (formatOrMimeType.contains("/")) {
            return ImageIO.getImageWritersByMIMEType(formatOrMimeType).hasNext();
        }
        return ImageIO.getImageWritersByFormatName(formatOrMimeType).hasNext();
    }

    private void writeImage(BufferedImage image, String ext, Path targetPath) throws IOException {
        String normalizedExt = ext == null ? "png" : ext.toLowerCase();
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(Files.newOutputStream(targetPath))) {
            if (WEBP_EXT.equals(normalizedExt)) {
                ImageWriter writer = ImageIO.getImageWritersByMIMEType(WEBP_MIME).next();
                writer.setOutput(imageOutputStream);
                writer.write(null, new IIOImage(image, null, null), writer.getDefaultWriteParam());
                writer.dispose();
                return;
            }

            if ("jpg".equals(normalizedExt) || "jpeg".equals(normalizedExt)) {
                BufferedImage rgbImage = image.getColorModel().hasAlpha()
                        ? stripAlpha(image)
                        : image;
                ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
                writer.setOutput(imageOutputStream);
                ImageWriteParam writeParam = writer.getDefaultWriteParam();
                if (writeParam.canWriteCompressed()) {
                    writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    writeParam.setCompressionQuality(0.82f);
                }
                writer.write(null, new IIOImage(rgbImage, null, null), writeParam);
                writer.dispose();
                return;
            }

            ImageWriter writer = ImageIO.getImageWritersByFormatName(normalizedExt).next();
            writer.setOutput(imageOutputStream);
            writer.write(null, new IIOImage(image, null, null), writer.getDefaultWriteParam());
            writer.dispose();
        }
    }

    private BufferedImage stripAlpha(BufferedImage image) {
        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgbImage;
    }

    private String normalizeRelativePath(String relativePath) {
        if (relativePath == null) {
            return null;
        }
        return relativePath.replace("\\", "/");
    }
}
