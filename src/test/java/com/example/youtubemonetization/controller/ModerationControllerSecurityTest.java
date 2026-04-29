package com.example.youtubemonetization.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.youtubemonetization.dto.response.ModerationDecisionResponse;
import com.example.youtubemonetization.service.ModerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ModerationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModerationService moderationService;

    @Test
    @WithMockUser(authorities = "MODERATION_REVIEW")
    void shouldAllowModeratorWithoutCsrfForApiEndpoint() throws Exception {
        when(moderationService.processModerationDecision(eq(1L), any()))
                .thenReturn(ModerationDecisionResponse.builder()
                        .videoId(1L)
                        .decision("APPROVE")
                        .videoStatus("APPROVED")
                        .moderationStatus("APPROVED")
                        .copyrightStatus("CLEARED")
                        .monetizationStatus("ENABLED")
                        .notificationCreated(true)
                        .auditCreated(true)
                        .moderatorId(10L)
                        .moderatorUsername("moderator")
                        .message("ok")
                        .build());

        mockMvc.perform(post("/api/moderation/videos/1/decision")
                        .contentType("application/json")
                        .content("{\"decision\":\"APPROVE\",\"reason\":\"ok\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "AUTHOR")
    void shouldReturnForbiddenForAuthorRole() throws Exception {
        mockMvc.perform(post("/api/moderation/videos/1/decision")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());
    }
}
