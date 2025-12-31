package com.example.ticketmaster.sse.controller;

import com.example.ticketmaster.sse.service.SseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SseController.class)
class SseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SseService sseService;

    @Test
    void testStartProcessCallsServiceAndReturnsOk() throws Exception {
        mockMvc.perform(post("/start/alice"))
            .andExpect(status().isOk())
            .andExpect(content().string("Process started for user alice"));

        verify(sseService).startProcess("alice");
    }

    @Test
    void testSubscribeReturnsTextEventStream() throws Exception {
        SseEmitter mockEmitter = new SseEmitter();
        when(sseService.subscribe("bob")).thenReturn(mockEmitter);

        mockMvc.perform(get("/subscribe/bob"))
            .andExpect(status().isOk())
            .andExpect(request().asyncStarted());

        verify(sseService).subscribe("bob");
    }
}
