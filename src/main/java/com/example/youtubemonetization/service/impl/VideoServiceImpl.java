package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.dto.request.EditVideoRequest;
import com.example.youtubemonetization.dto.request.VideoCreateRequest;
import com.example.youtubemonetization.entity.Claim;
import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.ClaimStatus;
import com.example.youtubemonetization.enums.CopyrightStatus;
import com.example.youtubemonetization.enums.MonetizationStatus;
import com.example.youtubemonetization.enums.MonetizationType;
import com.example.youtubemonetization.enums.UploadStatus;
import com.example.youtubemonetization.enums.ValidationStatus;
import com.example.youtubemonetization.exception.BusinessException;
import com.example.youtubemonetization.service.ClaimDataService;
import com.example.youtubemonetization.service.ProcessService;
import com.example.youtubemonetization.service.UserDataService;
import com.example.youtubemonetization.service.VideoDataService;
import com.example.youtubemonetization.service.VideoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "transactionManager")
public class VideoServiceImpl implements VideoService {

    private final VideoDataService videoDataService;
    private final UserDataService userDataService;
    private final ClaimDataService claimDataService;
    private final ProcessService processService;

    @Override
    public Video createVideo(VideoCreateRequest request) {
        if (request.getFilePath() == null || request.getFilePath().isBlank()) {
            throw new BusinessException("Путь к файлу обязателен");
        }
        if (request.getFormat() == null || request.getFormat().isBlank()) {
            throw new BusinessException("Формат обязателен");
        }
        if (request.getSizeBytes() == null || request.getSizeBytes() <= 0) {
            throw new BusinessException("Размер файла должен быть положительным");
        }
        if (request.getAuthorId() == null) {
            throw new BusinessException("Author is required");
        }
        User author = userDataService.getById(request.getAuthorId());
        Video video = new Video();
        video.setAuthor(author);
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setFilePath(request.getFilePath());
        video.setFormat(request.getFormat());
        video.setSizeBytes(request.getSizeBytes());
        video.setDurationSeconds(request.getDurationSeconds());
        video.setUploadStatus(UploadStatus.UPLOADED);
        video.setValidationStatus(ValidationStatus.PENDING);
        video.setCopyrightStatus(CopyrightStatus.PENDING);
        video.setMonetizationStatus(MonetizationStatus.PENDING);
        video.setMonetizationType(MonetizationType.NONE);
        Video saved = videoDataService.saveAndFlush(video);
        if (saved.getId() == null) {
            throw new BusinessException("Video was saved without generated id");
        }
        processService.startVideoUploadProcess(saved.getId());
        return videoDataService.getById(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Video getVideo(Long id) {
        if (id == null) {
            throw new BusinessException("Video id is required");
        }
        return videoDataService.getById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Video> getVideos(Long authorId) {
        return authorId == null ? videoDataService.findAll() : videoDataService.getByAuthorId(authorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Video> getAllVideos() {
        return videoDataService.findAll();
    }

    @Override
    public Video editVideo(Long id, EditVideoRequest request) {
        Video video = videoDataService.getById(id);
        if (video.getCopyrightStatus() != CopyrightStatus.NEEDS_EDITING) {
            throw new BusinessException("Редактирование доступно только для видео с найденным нарушением авторских прав");
        }

        video.setFilePath(request.getNewFilePath());
        if (request.getComment() != null && !request.getComment().isBlank()) {
            String previousDescription = video.getDescription() == null ? "" : video.getDescription() + System.lineSeparator();
            video.setDescription(previousDescription + "[EDIT COMMENT] " + request.getComment());
        }
        for (Claim claim : claimDataService.getByVideoId(id)) {
            if (claim.getStatus() == ClaimStatus.OPEN) {
                claimDataService.resolveClaim(claim.getId());
            }
        }
        video.setCopyrightStatus(CopyrightStatus.PENDING);
        return videoDataService.save(video);
    }
}
