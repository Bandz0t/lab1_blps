package com.example.youtubemonetization.mapper;

import com.example.youtubemonetization.dto.response.MonetizationResponse;
import com.example.youtubemonetization.dto.response.VideoResponse;
import com.example.youtubemonetization.dto.response.VideoStatusResponse;
import com.example.youtubemonetization.entity.Video;
import org.springframework.stereotype.Component;

@Component
public class VideoMapper {

    public VideoResponse toResponse(Video video) {
        return VideoResponse.builder()
                .id(video.getId())
                .authorId(video.getAuthor().getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .filePath(video.getFilePath())
                .format(video.getFormat())
                .sizeBytes(video.getSizeBytes())
                .durationSeconds(video.getDurationSeconds())
                .uploadStatus(video.getUploadStatus())
                .validationStatus(video.getValidationStatus())
                .copyrightStatus(video.getCopyrightStatus())
                .monetizationStatus(video.getMonetizationStatus())
                .monetizationType(video.getMonetizationType())
                .publishedAt(video.getPublishedAt())
                .processInstanceId(video.getProcessInstanceId())
                .build();
    }

    public VideoStatusResponse toStatusResponse(Video video, String currentStep) {
        return VideoStatusResponse.builder()
                .videoId(video.getId())
                .processInstanceId(video.getProcessInstanceId())
                .uploadStatus(video.getUploadStatus())
                .validationStatus(video.getValidationStatus())
                .copyrightStatus(video.getCopyrightStatus())
                .monetizationStatus(video.getMonetizationStatus())
                .currentStep(currentStep)
                .published(video.getPublishedAt() != null)
                .build();
    }

    public MonetizationResponse toMonetizationResponse(Video video) {
        return MonetizationResponse.builder()
                .videoId(video.getId())
                .monetizationType(video.getMonetizationType())
                .monetizationStatus(video.getMonetizationStatus())
                .published(video.getPublishedAt() != null)
                .build();
    }
}
