package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.CopyrightStatus;
import com.example.youtubemonetization.enums.MonetizationStatus;
import com.example.youtubemonetization.enums.UploadStatus;
import com.example.youtubemonetization.enums.ValidationStatus;
import com.example.youtubemonetization.exception.EntityNotFoundException;
import com.example.youtubemonetization.repository.VideoRepository;
import com.example.youtubemonetization.service.VideoDataService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VideoDataServiceImpl implements VideoDataService {

    private final VideoRepository videoRepository;

    @Override
    public Video save(Video video) {
        return videoRepository.save(video);
    }

    @Override
    @Transactional(readOnly = true)
    public Video getById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Video with id=" + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Video> findAll() {
        return videoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Video> getByAuthorId(Long authorId) {
        return videoRepository.findByAuthorId(authorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Video> getByUploadStatus(UploadStatus uploadStatus) {
        return videoRepository.findByUploadStatus(uploadStatus);
    }

    @Override
    public Video updateValidationStatus(Long videoId, ValidationStatus status) {
        Video video = getById(videoId);
        video.setValidationStatus(status);
        return videoRepository.save(video);
    }

    @Override
    public Video updateCopyrightStatus(Long videoId, CopyrightStatus status) {
        Video video = getById(videoId);
        video.setCopyrightStatus(status);
        return videoRepository.save(video);
    }

    @Override
    public Video updateMonetizationStatus(Long videoId, MonetizationStatus status) {
        Video video = getById(videoId);
        video.setMonetizationStatus(status);
        return videoRepository.save(video);
    }

    @Override
    public Video publish(Long videoId) {
        Video video = getById(videoId);
        video.setUploadStatus(UploadStatus.PUBLISHED);
        video.setPublishedAt(LocalDateTime.now());
        return videoRepository.save(video);
    }
}
