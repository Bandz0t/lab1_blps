package com.example.youtubemonetization.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface VideoStorageService {

    String upload(MultipartFile file);

    StoredVideoObject open(String objectKey);
}
