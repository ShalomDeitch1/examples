/**
 * Why this exists in this repo:
 * - Wires RabbitMQ-specific beans (publisher, listener, backlog buffer) for the push-mode demo.
 *
 * Real system notes:
 * - Real apps tune ack/retry/DLQ behavior and monitor consumer lag/backlog growth.
 *
 * How it fits this example flow:
 * - Connects Rabbit delivery callbacks to the scheduled processor via the shared backlog buffer.
 */
package com.example.ticketmaster.waitingroom.rabbitmq;

import com.example.ticketmaster.waitingroom.core.push.GroupCollector;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitMqPushConfiguration {

  @Bean
  public GroupCollector joinBacklog() {
    return new GroupCollector();
  }

  @Bean
  public Declarables topology(RabbitProperties props) {
    DirectExchange exchange = new DirectExchange(props.exchange());
    Queue queue = new Queue(props.queue());
    Binding binding = BindingBuilder.bind(queue).to(exchange).with(props.routingKey());
    return new Declarables(exchange, queue, binding);
  }
}
