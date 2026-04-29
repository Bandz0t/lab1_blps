package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.CopyrightStatus;
import com.example.youtubemonetization.enums.MonetizationStatus;
import com.example.youtubemonetization.enums.MonetizationType;
import com.example.youtubemonetization.enums.UploadStatus;
import com.example.youtubemonetization.exception.BusinessException;
import com.example.youtubemonetization.security.AccessGuard;
import com.example.youtubemonetization.security.SecurityPrivileges;
import com.example.youtubemonetization.service.MonetizationService;
import com.example.youtubemonetization.service.VideoDataService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MonetizationServiceImpl implements MonetizationService {

    private final VideoDataService videoDataService;
    private final AccessGuard accessGuard;

    @Override
    public Video chooseMonetization(Long videoId, MonetizationType monetizationType) {
        Video video = videoDataService.getById(videoId);
        accessGuard.requireOwnOrAll(video.getAuthor().getId(), SecurityPrivileges.VIDEO_MONETIZE_OWN, SecurityPrivileges.VIDEO_MONETIZE_ALL);
        if (video.getCopyrightStatus() != CopyrightStatus.CLEARED) {
            throw new BusinessException("Монетизацию можно выбрать только после успешной проверки авторских прав");
        }
        if (video.getPublishedAt() != null) {
            throw new BusinessException("Видео уже опубликовано и монетизация уже обработана");
        }

        video.setMonetizationType(monetizationType);
        video.setMonetizationStatus(isMonetizationAllowed(video) ? MonetizationStatus.ENABLED : MonetizationStatus.DISABLED);
        video.setPublishedAt(LocalDateTime.now());
        video.setUploadStatus(UploadStatus.PUBLISHED);
        return videoDataService.save(video);
    }

    private boolean isMonetizationAllowed(Video video) {
        String text = (video.getTitle() + " " + String.valueOf(video.getDescription())).toLowerCase();
        return !text.contains("shock")
                && !text.contains("violent")
                && !text.contains("18+")
                && !text.contains("drugs")
                && video.getDurationSeconds() != null
                && video.getDurationSeconds() >= 30;
    }
}
