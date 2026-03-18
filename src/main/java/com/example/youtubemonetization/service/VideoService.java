package com.example.youtubemonetization.service;

import com.example.youtubemonetization.dto.request.EditVideoRequest;
import com.example.youtubemonetization.dto.request.VideoCreateRequest;
import com.example.youtubemonetization.entity.Video;
import java.util.List;

public interface VideoService {

    Video createVideo(VideoCreateRequest request);

    Video getVideo(Long id);

    List<Video> getVideos(Long authorId);

    Video editVideo(Long id, EditVideoRequest request);
}
