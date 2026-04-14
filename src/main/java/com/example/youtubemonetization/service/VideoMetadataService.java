package com.example.youtubemonetization.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Reads technical metadata from uploaded video files.
 */
public interface VideoMetadataService {

    /**
     * Extracts duration in full seconds from MP4 metadata.
     *
     * @param videoFile uploaded video file
     * @return duration in seconds
     */
    int extractDurationSeconds(MultipartFile videoFile);
}
