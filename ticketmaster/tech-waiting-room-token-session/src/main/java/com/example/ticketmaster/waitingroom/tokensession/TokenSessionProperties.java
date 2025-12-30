package com.example.ticketmaster.waitingroom.tokensession;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "waitingroom")
public record TokenSessionProperties(Redis redis, Processing processing) {
  public record Redis(@NotBlank String stream) {
  }

  public record Processing(
      @Min(1) int capacity,
      @Min(1) int batchSize,
      @Min(1) long rateMs,
      @Min(0) long initialDelayMs
  ) {
  }
}
