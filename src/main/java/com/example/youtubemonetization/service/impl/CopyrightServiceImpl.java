package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.dto.request.CopyrightCheckRequest;
import com.example.youtubemonetization.entity.Claim;
import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.ClaimStatus;
import com.example.youtubemonetization.enums.ClaimType;
import com.example.youtubemonetization.enums.CopyrightStatus;
import com.example.youtubemonetization.enums.ValidationStatus;
import com.example.youtubemonetization.exception.BusinessException;
import com.example.youtubemonetization.exception.RequestValidationException;
import com.example.youtubemonetization.service.ClaimDataService;
import com.example.youtubemonetization.service.CopyrightService;
import com.example.youtubemonetization.service.VideoDataService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CopyrightServiceImpl implements CopyrightService {

    private final VideoDataService videoDataService;
    private final ClaimDataService claimDataService;

    @Override
    public Video processCopyrightCheck(Long videoId, CopyrightCheckRequest request) {
        Video video = videoDataService.getById(videoId);
        if (video.getValidationStatus() != ValidationStatus.PASSED) {
            throw new BusinessException("Нельзя проверять авторские права до успешной технической валидации видео");
        }

        if (request.isHasViolation()) {
            if (request.getClaimType() == null) {
                throw new RequestValidationException("Для нарушения авторских прав необходимо указать claimType");
            }
            Claim claim = new Claim();
            claim.setVideo(video);
            claim.setClaimType(request.getClaimType() == null ? ClaimType.OTHER : request.getClaimType());
            claim.setDescription(request.getDescription());
            claim.setDetectedFragment(request.getDetectedFragment());
            claim.setStatus(ClaimStatus.OPEN);
            claimDataService.create(claim);
            video.setCopyrightStatus(CopyrightStatus.NEEDS_EDITING);
        } else {
            List<Claim> claims = claimDataService.getByVideoId(videoId);
            claims.stream()
                    .filter(claim -> claim.getStatus() == ClaimStatus.OPEN)
                    .forEach(claim -> claimDataService.resolveClaim(claim.getId()));
            video.setCopyrightStatus(CopyrightStatus.CLEARED);
        }
        return videoDataService.save(video);
    }
}
