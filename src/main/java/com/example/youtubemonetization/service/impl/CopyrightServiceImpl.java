package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.dto.request.CopyrightCheckRequest;
import com.example.youtubemonetization.entity.Claim;
import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.ClaimStatus;
import com.example.youtubemonetization.enums.ClaimType;
import com.example.youtubemonetization.enums.CopyrightStatus;
import com.example.youtubemonetization.enums.UploadStatus;
import com.example.youtubemonetization.enums.ValidationStatus;
import com.example.youtubemonetization.exception.BusinessException;
import com.example.youtubemonetization.exception.RequestValidationException;
import com.example.youtubemonetization.service.ClaimDataService;
import com.example.youtubemonetization.service.CopyrightService;
import com.example.youtubemonetization.service.VideoDataService;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.SAXException;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CopyrightServiceImpl implements CopyrightService {

    private static final int MAX_SUBTITLE_SIZE = 200_000;

    private final VideoDataService videoDataService;
    private final ClaimDataService claimDataService;

    @Value("${copyright.banned-words:pirated,camrip,torrent,leak}")
    private List<String> bannedWords;

    @Override
    public Video processCopyrightCheck(Long videoId, CopyrightCheckRequest request) {
        Video video = videoDataService.getById(videoId);
        assertReadyForCheck(video);

        if (request.isHasViolation()) {
            if (request.getClaimType() == null) {
                throw new RequestValidationException("Для нарушения авторских прав необходимо указать claimType");
            }
            createClaim(video, request.getClaimType(), request.getDescription(), request.getDetectedFragment());
            video.setCopyrightStatus(CopyrightStatus.NEEDS_EDITING);
            video.setUploadStatus(UploadStatus.READY_FOR_REVIEW);
            return videoDataService.save(video);
        }

        closeOpenClaims(videoId);
        video.setCopyrightStatus(CopyrightStatus.CLEARED);
        return videoDataService.save(video);
    }

    @Override
    public Video processAutomaticCopyrightCheck(Long videoId) {
        Video video = videoDataService.getById(videoId);
        assertReadyForCheck(video);

        String subtitles = extractSubtitles(video.getFilePath());
        String violationWord = findViolationWord(subtitles);
        if (violationWord != null) {
            createClaim(
                    video,
                    ClaimType.OTHER,
                    "Автоматическая проверка субтитров обнаружила запрещённое слово: " + violationWord,
                    violationWord);
            video.setCopyrightStatus(CopyrightStatus.NEEDS_EDITING);
            video.setUploadStatus(UploadStatus.READY_FOR_REVIEW);
            return videoDataService.save(video);
        }

        closeOpenClaims(videoId);
        video.setCopyrightStatus(CopyrightStatus.CLEARED);
        video.setUploadStatus(UploadStatus.PUBLISHED);
        video.setPublishedAt(LocalDateTime.now());
        return videoDataService.save(video);
    }

    private void assertReadyForCheck(Video video) {
        if (video.getValidationStatus() != ValidationStatus.PASSED) {
            throw new BusinessException("Нельзя проверять авторские права до успешной технической валидации видео");
        }
    }

    private void createClaim(Video video, ClaimType claimType, String description, String fragment) {
        Claim claim = new Claim();
        claim.setVideo(video);
        claim.setClaimType(claimType == null ? ClaimType.OTHER : claimType);
        claim.setDescription(description);
        claim.setDetectedFragment(fragment);
        claim.setStatus(ClaimStatus.OPEN);
        claimDataService.create(claim);
    }

    private void closeOpenClaims(Long videoId) {
        List<Claim> claims = claimDataService.getByVideoId(videoId);
        claims.stream()
                .filter(claim -> claim.getStatus() == ClaimStatus.OPEN)
                .forEach(claim -> claimDataService.resolveClaim(claim.getId()));
    }

    private String findViolationWord(String subtitles) {
        if (subtitles == null || subtitles.isBlank()) {
            return null;
        }
        String normalized = subtitles.toLowerCase(Locale.ROOT);
        Set<String> uniqueWords = bannedWords.stream()
                .map(word -> word.toLowerCase(Locale.ROOT).trim())
                .filter(word -> !word.isBlank())
                .collect(java.util.stream.Collectors.toSet());

        for (String word : uniqueWords) {
            if (normalized.contains(word)) {
                return word;
            }
        }
        return null;
    }

    private String extractSubtitles(String filePath) {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            BodyContentHandler handler = new BodyContentHandler(MAX_SUBTITLE_SIZE);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(inputStream, handler, metadata, context);
            return handler.toString();
        } catch (IOException | TikaException | SAXException e) {
            log.warn("Не удалось извлечь текст из видео {}: {}", filePath, e.getMessage());
            return "";
        }
    }
}
