package com.example.youtubemonetization.controller;

import com.example.youtubemonetization.dto.response.ProcessStateResponse;
import com.example.youtubemonetization.service.ProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/processes")
public class ProcessController {

    private final ProcessService processService;

    @GetMapping("/{videoId}")
    @PreAuthorize("hasAnyAuthority('PROCESS_READ_OWN','PROCESS_READ_ALL')")
    public ProcessStateResponse getProcessState(@PathVariable Long videoId) {
        return processService.getProcessState(videoId);
    }

    @PostMapping("/{videoId}/continue")
    @PreAuthorize("hasAnyAuthority('PROCESS_READ_OWN','PROCESS_READ_ALL')")
    public ProcessStateResponse continueProcess(@PathVariable Long videoId) {
        return processService.continueProcess(videoId);
    }
}
