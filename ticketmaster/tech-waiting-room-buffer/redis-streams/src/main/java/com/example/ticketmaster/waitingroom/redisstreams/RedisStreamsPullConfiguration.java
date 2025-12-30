/**
 * Why this exists in this repo:
 * - Wires Redis Streams clients, initializer, and the pull-mode components.
 *
 * Real system notes:
 * - Stream creation/group setup is typically done by IaC or migrations, not ad-hoc on startup.
 *
 * How it fits this example flow:
 * - Connects the HTTP publisher and scheduled poller to the Redis stream.
 */
package com.example.ticketmaster.waitingroom.redisstreams;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RedisStreamsProperties.class)
public class RedisStreamsPullConfiguration {
}
