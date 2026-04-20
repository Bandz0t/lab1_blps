package ru.ifmo.se.lab1.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.ifmo.se.lab1.dto.VideoDto;
import ru.ifmo.se.lab1.service.VideoService;

@RestController
@RequestMapping("/api/system/videos")
@RequiredArgsConstructor
public class SystemController {
    private final VideoService videoService;

    @PostMapping("/{id}/process")
    public VideoDto processVideo(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean passCopyright) {
        return videoService.processVideoSystem(id, passCopyright);
    }
}
