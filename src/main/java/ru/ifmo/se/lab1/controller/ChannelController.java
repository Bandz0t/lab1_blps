package ru.ifmo.se.lab1.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.ifmo.se.lab1.dto.ChannelDto;
import ru.ifmo.se.lab1.dto.CreateChannelRequest;
import ru.ifmo.se.lab1.service.ChannelService;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {
    private final ChannelService channelService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChannelDto createChannel(@Valid @RequestBody CreateChannelRequest request) {
        return channelService.createChannel(request);
    }

    @GetMapping("/{id}")
    public ChannelDto getChannel(@PathVariable Long id) {
        return channelService.getChannel(id);
    }
}
