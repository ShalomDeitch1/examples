package com.example.ticketmaster.notification.controller;

import com.example.ticketmaster.notification.model.NotificationStatus;
import com.example.ticketmaster.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        reset(notificationService);
    }

    @Test
    void startProcess_shouldCallServiceAndReturnSuccess() throws Exception {
        String userId = "user123";

        mockMvc.perform(post("/start/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("Process started for user user123"));

        verify(notificationService, times(1)).startProcess(userId);
    }

    @Test
    void getStatus_shouldReturnNotReadyStatus() throws Exception {
        String userId = "user456";
        NotificationStatus notReadyStatus = NotificationStatus.notReady(userId);
        when(notificationService.getStatus(userId)).thenReturn(notReadyStatus);

        mockMvc.perform(get("/status/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("NOT_READY"))
                .andExpect(jsonPath("$.message").value("Your request is being processed"));

        verify(notificationService, times(1)).getStatus(userId);
    }

    @Test
    void getStatus_shouldReturnWaitingStatus() throws Exception {
        String userId = "user789";
        NotificationStatus waitingStatus = NotificationStatus.waiting(userId);
        when(notificationService.getStatus(userId)).thenReturn(waitingStatus);

        mockMvc.perform(get("/status/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.message").value("You are in the waiting room"));

        verify(notificationService, times(1)).getStatus(userId);
    }

    @Test
    void getStatus_shouldReturnReadyStatus() throws Exception {
        String userId = "user999";
        NotificationStatus readyStatus = NotificationStatus.ready(userId);
        when(notificationService.getStatus(userId)).thenReturn(readyStatus);

        mockMvc.perform(get("/status/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.message").value("You can now proceed to ticket selection"));

        verify(notificationService, times(1)).getStatus(userId);
    }

    @Test
    void getStatus_shouldReturnNotFoundStatus() throws Exception {
        String userId = "unknown";
        NotificationStatus notFoundStatus = NotificationStatus.notFound(userId);
        when(notificationService.getStatus(userId)).thenReturn(notFoundStatus);

        mockMvc.perform(get("/status/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));

        verify(notificationService, times(1)).getStatus(userId);
    }
}
