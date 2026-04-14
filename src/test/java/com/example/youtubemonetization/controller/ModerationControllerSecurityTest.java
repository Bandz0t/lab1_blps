package com.example.youtubemonetization.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    @WithMockUser(roles = "AUTHOR")
    void shouldReturnForbiddenForAuthorRole() throws Exception {
        mockMvc.perform(post("/api/moderation/videos/1/decision")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isForbidden());
    }
}
