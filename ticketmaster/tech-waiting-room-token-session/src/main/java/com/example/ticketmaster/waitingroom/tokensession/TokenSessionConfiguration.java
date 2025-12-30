package com.example.ticketmaster.waitingroom.tokensession;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TokenSessionProperties.class)
public class TokenSessionConfiguration {
}
