package com.example.ticketmaster.waitingroom.tokensession.api;

import jakarta.validation.constraints.NotBlank;

public record CreateSessionRequest(@NotBlank String eventId, @NotBlank String userId) {}
