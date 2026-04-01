package com.example.youtubemonetization.service;

import org.springframework.web.multipart.MultipartFile;

public interface VideoMetadataService {

    int extractDurationSeconds(MultipartFile videoFile);
}
