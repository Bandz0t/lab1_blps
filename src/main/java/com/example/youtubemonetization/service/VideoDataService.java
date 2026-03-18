package com.example.youtubemonetization.service;

import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.CopyrightStatus;
import com.example.youtubemonetization.enums.MonetizationStatus;
import com.example.youtubemonetization.enums.UploadStatus;
import com.example.youtubemonetization.enums.ValidationStatus;
import java.util.List;

public interface VideoDataService {

    Video save(Video video);

    Video getById(Long id);

    List<Video> findAll();

    List<Video> getByAuthorId(Long authorId);

    List<Video> getByUploadStatus(UploadStatus uploadStatus);

    Video updateValidationStatus(Long videoId, ValidationStatus status);

    Video updateCopyrightStatus(Long videoId, CopyrightStatus status);

    Video updateMonetizationStatus(Long videoId, MonetizationStatus status);

    Video publish(Long videoId);
}
