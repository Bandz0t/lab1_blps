package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.service.VideoMetadataService;
import java.io.File;
import java.io.IOException;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieHeaderBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class JcodecVideoMetadataService implements VideoMetadataService {

    @Override
    public int extractDurationSeconds(MultipartFile videoFile) {
        File tempFile = null;

        try {
            tempFile = File.createTempFile("video-metadata-", ".mp4");
            videoFile.transferTo(tempFile);

            MovieBox movie = MP4Util.parseMovie(tempFile);
            if (movie == null) {
                throw new IllegalArgumentException("Unable to parse MP4 metadata: movie box is missing.");
            }

            MovieHeaderBox movieHeader = NodeBox.findFirst(movie, MovieHeaderBox.class, "mvhd");
            if (movieHeader == null || movieHeader.getTimescale() <= 0) {
                throw new IllegalArgumentException("Unable to parse MP4 metadata: movie header is missing or invalid.");
            }

            double durationSeconds = (double) movieHeader.getDuration() / movieHeader.getTimescale();
            return (int) Math.ceil(durationSeconds);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read uploaded video file.", e);
        } finally {
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }
}
