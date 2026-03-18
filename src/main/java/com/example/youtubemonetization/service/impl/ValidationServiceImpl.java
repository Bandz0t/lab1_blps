package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.UploadStatus;
import com.example.youtubemonetization.enums.ValidationStatus;
import com.example.youtubemonetization.service.ValidationService;
import com.example.youtubemonetization.service.VideoDataService;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ValidationServiceImpl implements ValidationService {

    private static final Set<String> SUPPORTED_FORMATS = Set.of("mp4", "mov", "avi");
    private static final long MAX_SIZE_BYTES = 2L * 1024L * 1024L * 1024L;

    private final VideoDataService videoDataService;

    @Override
    public Video validateVideo(Long videoId) {
        Video video = videoDataService.getById(videoId);
        boolean valid = SUPPORTED_FORMATS.contains(video.getFormat().toLowerCase(Locale.ROOT))
                && video.getSizeBytes() <= MAX_SIZE_BYTES;

        video.setValidationStatus(valid ? ValidationStatus.PASSED : ValidationStatus.FAILED);
        video.setUploadStatus(valid ? UploadStatus.READY_FOR_REVIEW : UploadStatus.REJECTED);
        return videoDataService.save(video);
    }
}
