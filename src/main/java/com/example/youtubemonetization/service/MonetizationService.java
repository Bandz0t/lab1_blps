package com.example.youtubemonetization.service;

import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.MonetizationType;

public interface MonetizationService {

    Video chooseMonetization(Long videoId, MonetizationType monetizationType);
}
