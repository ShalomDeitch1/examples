package com.example.ticketmaster.longpolling.controller;

import com.example.ticketmaster.longpolling.model.NotificationStatus;
import com.example.ticketmaster.longpolling.service.LongPollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LongPollingController.class)
class LongPollingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LongPollingService longPollingService;

    @Test
    void testStartProcessCallsServiceAndReturnsOk() throws Exception {
        mockMvc.perform(post("/start/alice"))
            .andExpect(status().isOk())
            .andExpect(content().string("Process started for user alice"));

        verify(longPollingService).startProcess("alice");
    }

    @Test
    void testGetStatusWithoutLastStatusParameter() throws Exception {
        NotificationStatus mockStatus = NotificationStatus.notReady("bob");
        when(longPollingService.getStatus(eq("bob"), isNull())).thenReturn(mockStatus);

        mockMvc.perform(get("/status/bob"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("bob"))
            .andExpect(jsonPath("$.status").value("NOT_READY"))
            .andExpect(jsonPath("$.message").value("Your request is being processed"));

        verify(longPollingService).getStatus(eq("bob"), isNull());
    }

    @Test
    void testGetStatusWithLastStatusParameter() throws Exception {
        NotificationStatus mockStatus = NotificationStatus.waiting("charlie");
        when(longPollingService.getStatus("charlie", "NOT_READY")).thenReturn(mockStatus);

        mockMvc.perform(get("/status/charlie")
                .param("lastStatus", "NOT_READY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("charlie"))
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andExpect(jsonPath("$.message").value("You are in the waiting room"));

        verify(longPollingService).getStatus("charlie", "NOT_READY");
    }

    @Test
    void testGetStatusReturnsNotFound() throws Exception {
        NotificationStatus mockStatus = NotificationStatus.notFound("unknown");
        when(longPollingService.getStatus(eq("unknown"), isNull())).thenReturn(mockStatus);

        mockMvc.perform(get("/status/unknown"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("unknown"))
            .andExpect(jsonPath("$.status").value("NOT_FOUND"));

        verify(longPollingService).getStatus(eq("unknown"), isNull());
    }
}
