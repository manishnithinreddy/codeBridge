package com.codebridge.server.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
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
        // durable: true
        return new Queue(activityLogQueueName, true);
    }

    @Bean
    public TopicExchange activityLogExchange() {
        return new TopicExchange(activityLogExchangeName);
    }

    @Bean
    public Binding activityLogBinding(Queue activityLogQueue, TopicExchange activityLogExchange) {
        return BindingBuilder.bind(activityLogQueue).to(activityLogExchange).with(activityLogRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // This converter will be used by RabbitTemplate to serialize/deserialize messages as JSON
        // It uses Jackson ObjectMapper internally.
        return new Jackson2JsonMessageConverter();
    }

    // Optional: Spring Boot auto-configures RabbitTemplate to use any MessageConverter bean.
    // Explicitly defining it here allows for customization if needed, but is often not required
    // just to set the message converter.
    /*
    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory, final MessageConverter messageConverter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
    */
}
