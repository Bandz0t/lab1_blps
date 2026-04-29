package com.example.youtubemonetization.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(prefix = "storage", name = "minio-enabled", havingValue = "false", matchIfMissing = true)
public class LocalVideoStorageService implements VideoStorageService {

    private static final Path STORAGE_DIR = Path.of(System.getProperty("java.io.tmpdir"), "youtube-monetization-videos");

    @Override
    public String upload(MultipartFile file) {
        try {
            Files.createDirectories(STORAGE_DIR);
            String originalName = file.getOriginalFilename() == null ? "video.bin" : file.getOriginalFilename();
            String objectKey = UUID.randomUUID() + "-" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = STORAGE_DIR.resolve(objectKey);
            try (InputStream stream = file.getInputStream()) {
                Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return objectKey;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить видео локально", e);
        }
    }

    @Override
    public StoredVideoObject open(String objectKey) {
        try {
            Path path = STORAGE_DIR.resolve(objectKey);
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return new StoredVideoObject(Files.newInputStream(path), contentType, Files.size(path));
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать видео", e);
        }
    }
}
