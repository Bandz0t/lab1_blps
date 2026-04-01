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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vosk.Model;
import org.vosk.Recognizer;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CopyrightServiceImpl implements CopyrightService {

    private static final int AUDIO_SAMPLE_RATE = 16_000;
    private static final int AUDIO_BUFFER_SIZE = 4_096;
    private static final Pattern VOSK_TEXT_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]*)\"");

    private final VideoDataService videoDataService;
    private final ClaimDataService claimDataService;

    @Value("${copyright.banned-words:good,morning,sure,everybody}")
    private List<String> bannedWords;
    @Value("${copyright.vosk-model-path:}")
    private String voskModelPath;
    @Value("${copyright.ffmpeg-binary:ffmpeg}")
    private String ffmpegBinary;

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
        if (voskModelPath == null || voskModelPath.isBlank()) {
            log.warn("Пропущено авто-распознавание: не задан путь к Vosk модели (copyright.vosk-model-path)");
            return "";
        }
        File modelDir = new File(voskModelPath);
        if (!modelDir.exists() || !modelDir.isDirectory()) {
            log.warn("Пропущено авто-распознавание: директория Vosk модели не найдена: {}", voskModelPath);
            return "";
        }

        Process process = null;
        try (Model model = new Model(voskModelPath);
             Recognizer recognizer = new Recognizer(model, AUDIO_SAMPLE_RATE)) {
            process = startAudioExtractionProcess(filePath);
            StringBuilder subtitles = new StringBuilder();
            byte[] buffer = new byte[AUDIO_BUFFER_SIZE];

            try (InputStream audioStream = process.getInputStream()) {
                int bytesRead;
                while ((bytesRead = audioStream.read(buffer)) != -1) {
                    if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                        subtitles.append(extractText(recognizer.getResult())).append(' ');
                    }
                }
            }

            subtitles.append(extractText(recognizer.getFinalResult()));
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("FFmpeg завершился с кодом {} при обработке файла {}", exitCode, filePath);
            }
            return subtitles.toString().trim();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Не удалось извлечь субтитры через Vosk из видео {}: {}", filePath, e.getMessage());
            return "";
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private Process startAudioExtractionProcess(String filePath) throws IOException {
        return new ProcessBuilder(
                ffmpegBinary,
                "-i",
                filePath,
                "-vn",
                "-ar",
                String.valueOf(AUDIO_SAMPLE_RATE),
                "-ac",
                "1",
                "-f",
                "s16le",
                "-")
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
    }

    private String extractText(String voskJson) {
        if (voskJson == null || voskJson.isBlank()) {
            return "";
        }
        Matcher matcher = VOSK_TEXT_PATTERN.matcher(voskJson);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1)
                .replace("\\n", " ")
                .replace("\\\"", "\"")
                .trim();
    }
}
