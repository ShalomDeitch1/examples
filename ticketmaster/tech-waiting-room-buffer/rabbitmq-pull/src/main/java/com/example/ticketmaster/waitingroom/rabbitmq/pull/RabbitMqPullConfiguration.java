/**
 * Why this exists in this repo:
 * - Declares the RabbitMQ topology (exchange/queue/binding) and enables config properties for the pull-mode demo.
 *
 * Real system notes:
 * - Broker topology is usually provisioned outside the app (IaC) and evolves with explicit versioning.
 *
 * How it fits this example flow:
 * - Ensures the queue exists for publisher + poller.
 */
package com.example.ticketmaster.waitingroom.rabbitmq.pull;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitPullProperties.class)
public class RabbitMqPullConfiguration {

  @Bean
  public Declarables topology(RabbitPullProperties props) {
    DirectExchange exchange = new DirectExchange(props.exchange());
    Queue queue = new Queue(props.queue());
    Binding binding = BindingBuilder.bind(queue).to(exchange).with(props.routingKey());
    return new Declarables(exchange, queue, binding);
  }
}
