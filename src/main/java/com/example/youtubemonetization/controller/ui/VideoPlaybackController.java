package com.example.youtubemonetization.controller.ui;

import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.service.UserDataService;
import com.example.youtubemonetization.service.VideoService;
import com.example.youtubemonetization.service.storage.StoredVideoObject;
import com.example.youtubemonetization.service.storage.VideoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoPlaybackController {

    private final VideoService videoService;
    private final UserDataService userDataService;
    private final VideoStorageService videoStorageService;

    @GetMapping("/{id}/stream")
    public ResponseEntity<InputStreamResource> stream(@PathVariable Long id, Authentication authentication) {
        Video video = videoService.getVideo(id);
        User user = userDataService.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Текущий пользователь не найден"));
        if (!video.getAuthor().getId().equals(user.getId())
                && !"ADMIN".equals(user.getRole())
                && !"MODERATOR".equals(user.getRole())) {
            return ResponseEntity.status(403).build();
        }

        StoredVideoObject object = videoStorageService.open(video.getFilePath());
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (object.contentType() != null && !object.contentType().isBlank()) {
            mediaType = MediaType.parseMediaType(object.contentType());
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(object.size())
                .body(new InputStreamResource(object.stream()));
    }
}
