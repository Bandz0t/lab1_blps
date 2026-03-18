package com.example.youtubemonetization.service;

import com.example.youtubemonetization.entity.Video;

public interface ValidationService {

    Video validateVideo(Long videoId);
}
