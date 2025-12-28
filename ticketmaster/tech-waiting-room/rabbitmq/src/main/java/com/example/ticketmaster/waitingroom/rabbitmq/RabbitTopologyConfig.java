package com.example.ticketmaster.waitingroom.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

  @Bean
  public Declarables waitingRoomTopology(RabbitWaitingRoomProperties props) {
    DirectExchange exchange = new DirectExchange(props.exchange());
    Queue queue = new Queue(props.queue());
    Binding binding = BindingBuilder.bind(queue).to(exchange).with(props.routingKey());
    return new Declarables(exchange, queue, binding);
  }
}
