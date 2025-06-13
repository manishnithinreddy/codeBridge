package com.codebridge.server.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${codebridge.rabbitmq.activity-log.exchange-name}")
    private String activityLogExchangeName;

    @Value("${codebridge.rabbitmq.activity-log.queue-name}")
    private String activityLogQueueName;

    @Value("${codebridge.rabbitmq.activity-log.routing-key}")
    private String activityLogRoutingKey;

    @Bean
    public Queue activityLogQueue() {
        // Durable queue
        return new Queue(activityLogQueueName, true);
    }

    @Bean
    public TopicExchange activityLogExchange() {
        // Durable exchange
        return new TopicExchange(activityLogExchangeName, true, false);
    }

    @Bean
    public Binding activityLogBinding(Queue activityLogQueue, TopicExchange activityLogExchange) {
        return BindingBuilder.bind(activityLogQueue).to(activityLogExchange).with(activityLogRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Optional: Configure RabbitTemplate to use the jsonMessageConverter by default
    // If not defined, you might need to set it on RabbitTemplate instance before sending objects
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
