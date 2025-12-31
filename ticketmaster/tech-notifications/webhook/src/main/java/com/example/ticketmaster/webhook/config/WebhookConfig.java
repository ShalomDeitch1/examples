package com.example.ticketmaster.webhook.config;

import com.example.ticketmaster.webhook.sender.RestClientWebhookSender;
import com.example.ticketmaster.webhook.sender.WebhookSender;
import com.example.ticketmaster.webhook.signature.WebhookSigner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
/**
 * Spring wiring for the webhook demo.
 * <p>
 * Why these beans exist:
 * <ul>
 *     <li>{@link org.springframework.web.client.RestClient} is used by the sender to POST callbacks.</li>
 *     <li>{@link com.example.ticketmaster.webhook.signature.WebhookSigner} reads the shared secret from config.</li>
 *     <li>{@link java.time.Clock} is injected so tests can control "now".</li>
 * </ul>
 */
public class WebhookConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }

    @Bean
    public WebhookSender webhookSender(RestClient restClient) {
        return new RestClientWebhookSender(restClient);
    }

    @Bean
    public WebhookSigner webhookSigner(@Value("${webhook.sharedSecret}") String sharedSecret) {
        return new WebhookSigner(sharedSecret);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
