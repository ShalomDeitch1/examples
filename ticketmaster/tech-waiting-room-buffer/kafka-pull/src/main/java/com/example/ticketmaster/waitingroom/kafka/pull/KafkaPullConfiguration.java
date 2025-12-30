/**
 * Why this exists in this repo:
 * - Wires Kafka pull-mode beans: topic properties and publisher.
 *
 * Real system notes:
 * - In production, topics/partitions/retention/ACLs are usually managed by infrastructure (Terraform/Helm/operator)
 *   and enforced by security policy; apps should not implicitly create topics.
 * - This demo declares the topic via Spring Kafka ({@code NewTopic}) to keep tests deterministic:
 *   when Kafka starts (Testcontainers), there can be a small window where the topic/leader isn’t ready.
 *   Without explicit creation, early publishes can fail asynchronously while the HTTP API still returns 202,
 *   which looks like “dropped” requests.
 *
 * How it fits this example flow:
 * - Provides the topic name used by publisher and poller.
 */
package com.example.ticketmaster.waitingroom.kafka.pull;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaAdmin.NewTopics;

@Configuration
@EnableConfigurationProperties(KafkaPullProperties.class)
public class KafkaPullConfiguration {

	@Bean
	public NewTopics kafkaPullTopics(KafkaPullProperties properties) {
		return new KafkaAdmin.NewTopics(
				TopicBuilder.name(properties.topic())
						.partitions(1)
						.replicas(1)
						.build()
		);
	}
}
