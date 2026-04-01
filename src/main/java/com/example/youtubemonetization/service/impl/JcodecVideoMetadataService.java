package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.exception.BusinessException;
import com.example.youtubemonetization.service.VideoMetadataService;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class JcodecVideoMetadataService implements VideoMetadataService {

    @Override
    public int extractDurationSeconds(MultipartFile videoFile) {
        try (SeekableByteChannel channel = NIOUtils.readableChannel(videoFile.getInputStream())) {
            MovieBox movie = MP4Util.parseMovie(channel);
            if (movie == null || movie.getMovieHeader() == null) {
                throw new BusinessException("Не удалось получить метаданные длительности видео");
            }
            double durationSeconds = (double) movie.getMovieHeader().getDuration() / movie.getMovieHeader().getTimescale();
            int seconds = (int) Math.ceil(durationSeconds);
            if (seconds <= 0) {
                throw new BusinessException("Длительность видео в метаданных некорректна");
            }
            return seconds;
        } catch (IOException | RuntimeException ex) {
            throw new BusinessException("Не удалось определить длительность видео из метаданных");
        }
    }
}
