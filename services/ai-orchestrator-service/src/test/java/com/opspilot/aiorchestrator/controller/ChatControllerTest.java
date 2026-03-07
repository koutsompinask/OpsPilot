package com.opspilot.aiorchestrator.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.opspilot.aiorchestrator.domain.entity.Role;
import com.opspilot.aiorchestrator.dto.ChatAskResponse;
import com.opspilot.aiorchestrator.dto.ChatSourceResponse;
import com.opspilot.aiorchestrator.exception.GlobalExceptionHandler;
import com.opspilot.aiorchestrator.security.CurrentUser;
import com.opspilot.aiorchestrator.security.CurrentUserResolver;
import com.opspilot.aiorchestrator.service.ChatService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ChatController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrentUserResolver currentUserResolver;

    @MockBean
    private ChatService chatService;

    @Test
    void askShouldReturnValidationErrorForBlankQuestion() throws Exception {
        mockMvc.perform(post("/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void askShouldReturnResponseBody() throws Exception {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), UUID.randomUUID(), "u@example.com", Role.TENANT_MEMBER);
        when(currentUserResolver.fromJwt(any())).thenReturn(user);
        when(chatService.ask(eq(user), eq("When is check-in?"), eq(4))).thenReturn(new ChatAskResponse(
                "Check in starts at 15:00.",
                0.82,
                List.of(new ChatSourceResponse("hotel-policy.txt", "chunk-14")),
                false
        ));

        mockMvc.perform(post("/chat/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"When is check-in?\",\"topK\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Check in starts at 15:00."))
                .andExpect(jsonPath("$.confidence").value(0.82))
                .andExpect(jsonPath("$.sources[0].document").value("hotel-policy.txt"))
                .andExpect(jsonPath("$.sources[0].chunkId").value("chunk-14"))
                .andExpect(jsonPath("$.ticketCreated").value(false));
    }
}
