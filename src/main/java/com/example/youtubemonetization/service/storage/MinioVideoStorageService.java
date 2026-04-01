package com.example.youtubemonetization.service.storage;

import com.example.youtubemonetization.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import java.io.InputStream;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(type = "io.minio.MinioClient")
public class MinioVideoStorageService implements VideoStorageService {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    @Override
    public String upload(MultipartFile file) {
        String objectKey = generateObjectKey(file);
        try (InputStream inputStream = file.getInputStream()) {
            ensureBucketExists();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(storageProperties.getBucket())
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return objectKey;
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сохранить видео в MinIO", e);
        }
    }

    @Override
    public StoredVideoObject open(String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(storageProperties.getBucket())
                            .object(objectKey)
                            .build()
            );
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(storageProperties.getBucket())
                            .object(objectKey)
                            .build()
            );
            return new StoredVideoObject(stream, stat.contentType(), stat.size());
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось прочитать видео из MinIO", e);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(storageProperties.getBucket()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(storageProperties.getBucket()).build());
        }
    }

    private String generateObjectKey(MultipartFile file) {
        String originalName = file.getOriginalFilename() == null ? "video.bin" : file.getOriginalFilename();
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return UUID.randomUUID() + "-" + safeName;
    }
}
