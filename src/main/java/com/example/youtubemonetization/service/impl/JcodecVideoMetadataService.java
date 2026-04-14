package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.exception.BusinessException;
import com.example.youtubemonetization.service.VideoMetadataService;
import java.io.File;
import java.io.IOException;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class JcodecVideoMetadataService implements VideoMetadataService {

    @Override
    public int extractDurationSeconds(MultipartFile videoFile) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("video-metadata-", ".tmp");
            videoFile.transferTo(tempFile);

            MovieBox movie = MP4Util.parseMovie(tempFile);
            if (movie == null || movie.getTimescale() <= 0) {
                throw new BusinessException("Не удалось получить метаданные длительности видео");
            }

            double durationSeconds = (double) movie.getDuration() / movie.getTimescale();
            int seconds = (int) Math.ceil(durationSeconds);
            if (seconds <= 0) {
                throw new BusinessException("Длительность видео в метаданных некорректна");
            }
            return seconds;
        } catch (IOException | RuntimeException ex) {
            throw new BusinessException("Не удалось определить длительность видео из метаданных");
        } finally {
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }
}
