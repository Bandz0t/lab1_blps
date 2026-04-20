package ru.ifmo.se.lab1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.lab1.domain.Channel;
import ru.ifmo.se.lab1.domain.MonetizationStatus;
import ru.ifmo.se.lab1.domain.Video;
import ru.ifmo.se.lab1.domain.VideoStatus;
import ru.ifmo.se.lab1.dto.CreateVideoRequest;
import ru.ifmo.se.lab1.dto.VideoDto;
import ru.ifmo.se.lab1.exception.BusinessRuleException;
import ru.ifmo.se.lab1.exception.ResourceNotFoundException;
import ru.ifmo.se.lab1.repository.VideoRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoRepository videoRepository;
    private final ChannelService channelService;
    private final UserService userService;

    @Transactional
    public VideoDto uploadVideo(Long channelId, CreateVideoRequest request) {
        Channel channel = channelService.getDomainChannel(channelId);
        Video video = new Video();
        video.setChannel(channel);
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setStatus(VideoStatus.UPLOADED);
        video = videoRepository.save(video);
        return mapToDto(video);
    }

    @Transactional
    public VideoDto updateVideo(Long id, CreateVideoRequest request) {
        Video video = getDomainVideo(id);
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video = videoRepository.save(video);
        return mapToDto(video);
    }

    @Transactional
    public VideoDto publishVideo(Long id) {
        Video video = getDomainVideo(id);
        if (video.getStatus() != VideoStatus.PROCESSED) {
            throw new BusinessRuleException("Video cannot be published from current status: " + video.getStatus());
        }
        video.setStatus(VideoStatus.PUBLISHED);
        video = videoRepository.save(video);
        return mapToDto(video);
    }

    @Transactional
    public VideoDto processVideoSystem(Long id, boolean passCopyright) {
        Video video = getDomainVideo(id);
        if (video.getStatus() != VideoStatus.UPLOADED) {
            throw new BusinessRuleException("Only UPLOADED videos can be processed");
        }
        video.setStatus(passCopyright ? VideoStatus.PROCESSED : VideoStatus.COPYRIGHT_STRIKE);
        video = videoRepository.save(video);
        return mapToDto(video);
    }

    @Transactional(readOnly = true)
    public VideoDto getVideo(Long id) {
        return mapToDto(getDomainVideo(id));
    }

    public Video getDomainVideo(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
    }

    @Transactional
    public void addView(Long id) {
        Video video = getDomainVideo(id);
        if (video.getStatus() != VideoStatus.PUBLISHED) {
            throw new BusinessRuleException("Can only add views to PUBLISHED videos");
        }
        video.setViewsCount(video.getViewsCount() + 1);
        if (video.getMonetizationStatus() == MonetizationStatus.APPROVED) {
            // For example, 0.01 money per view
            userService.addBalance(video.getChannel().getUser().getId(), new BigDecimal("0.01"));
        }
        videoRepository.save(video);
    }

    public VideoDto mapToDto(Video video) {
        VideoDto dto = new VideoDto();
        dto.setId(video.getId());
        dto.setChannelId(video.getChannel().getId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setStatus(video.getStatus());
        dto.setMonetizationStatus(video.getMonetizationStatus());
        dto.setViewsCount(video.getViewsCount());
        dto.setCreatedAt(video.getCreatedAt());
        return dto;
    }
}
