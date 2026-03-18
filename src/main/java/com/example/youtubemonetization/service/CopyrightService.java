package com.example.youtubemonetization.service;

import com.example.youtubemonetization.dto.request.CopyrightCheckRequest;
import com.example.youtubemonetization.entity.Video;

public interface CopyrightService {

    Video processCopyrightCheck(Long videoId, CopyrightCheckRequest request);
}
