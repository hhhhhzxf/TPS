package com.tps.service;

import com.tps.dto.file.UploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    public UploadResponse upload(MultipartFile file) {
        validateImage(file);
        String original = file.getOriginalFilename();
        String ext = getExtension(original);
        String filename = UUID.randomUUID() + ext;
        Path dest = Paths.get(uploadDir, filename);
        try {
            Files.createDirectories(dest.getParent());
            file.transferTo(dest);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
        String path = "/img/" + filename;
        return new UploadResponse(toAbsoluteUrl(path), path);
    }

    public String toAbsoluteUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String baseUrl = publicBaseUrl;
        if (baseUrl == null || baseUrl.isBlank()) {
            try {
                baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            } catch (IllegalStateException ignored) {
                return url;
            }
        }
        return baseUrl.replaceAll("/+$", "") + (url.startsWith("/") ? url : "/" + url);
    }

    private void validateImage(MultipartFile file) {
        String ext = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("仅支持 jpg、png、webp、gif 图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("文件类型不支持");
        }
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(12);
            if (!hasValidImageHeader(header)) {
                throw new IllegalArgumentException("文件内容不是有效图片");
            }
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }

    private boolean hasValidImageHeader(byte[] header) {
        if (header.length < 4) {
            return false;
        }
        boolean jpg = (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8;
        boolean png = header.length >= 8
                && (header[0] & 0xff) == 0x89 && header[1] == 0x50 && header[2] == 0x4e && header[3] == 0x47
                && header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a;
        boolean gif = header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46;
        boolean webp = header.length >= 12
                && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50;
        return jpg || png || gif || webp;
    }
}
