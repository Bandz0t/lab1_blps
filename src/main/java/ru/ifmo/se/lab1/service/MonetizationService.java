package ru.ifmo.se.lab1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.lab1.domain.*;
import ru.ifmo.se.lab1.dto.MonetizationRequestDto;
import ru.ifmo.se.lab1.exception.BusinessRuleException;
import ru.ifmo.se.lab1.exception.ResourceNotFoundException;
import ru.ifmo.se.lab1.repository.MonetizationRequestRepository;
import ru.ifmo.se.lab1.repository.VideoRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MonetizationService {
    private final MonetizationRequestRepository requestRepository;
    private final VideoService videoService;
    private final VideoRepository videoRepository;
    private final UserService userService;

    @Transactional
    public MonetizationRequestDto applyForMonetization(Long videoId) {
        Video video = videoService.getDomainVideo(videoId);
        if (video.getStatus() == VideoStatus.COPYRIGHT_STRIKE) {
            throw new BusinessRuleException("Cannot monetize a video with a copyright strike");
        }
        if (video.getMonetizationStatus() != MonetizationStatus.NONE) {
            throw new BusinessRuleException("Monetization already applied or processed");
        }

        MonetizationRequest request = new MonetizationRequest();
        request.setVideo(video);
        request.setStatus(RequestStatus.PENDING);
        request = requestRepository.save(request);

        video.setMonetizationStatus(MonetizationStatus.PENDING);
        videoRepository.save(video);

        return mapToDto(request);
    }

    @Transactional
    public MonetizationRequestDto moderateRequest(Long requestId, Long moderatorId, boolean approve) {
        MonetizationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Monetization request not found"));
        
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessRuleException("Request already resolved");
        }

        User moderator = userService.getDomainUser(moderatorId);
        if (moderator.getRole() != Role.MODERATOR) {
            throw new BusinessRuleException("User is not a moderator");
        }

        request.setModerator(moderator);
        request.setStatus(approve ? RequestStatus.APPROVED : RequestStatus.REJECTED);
        request.setResolvedAt(LocalDateTime.now());
        
        Video video = request.getVideo();
        video.setMonetizationStatus(approve ? MonetizationStatus.APPROVED : MonetizationStatus.REJECTED);
        videoRepository.save(video);

        return mapToDto(requestRepository.save(request));
    }

    private MonetizationRequestDto mapToDto(MonetizationRequest req) {
        MonetizationRequestDto dto = new MonetizationRequestDto();
        dto.setId(req.getId());
        dto.setVideoId(req.getVideo().getId());
        dto.setStatus(req.getStatus());
        dto.setModeratorId(req.getModerator() != null ? req.getModerator().getId() : null);
        dto.setCreatedAt(req.getCreatedAt());
        dto.setResolvedAt(req.getResolvedAt());
        return dto;
    }
}
