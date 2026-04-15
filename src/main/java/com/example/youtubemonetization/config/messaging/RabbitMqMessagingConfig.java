package com.example.youtubemonetization.config.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ModerationMessagingProperties.class)
@ConditionalOnProperty(name = "app.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqMessagingConfig {

    @Bean
    public TopicExchange moderationExchange(ModerationMessagingProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean
    public Queue moderationQueue(ModerationMessagingProperties properties) {
        return new Queue(properties.getQueue(), true, false, false);
    }

    @Bean
    public Binding moderationBinding(
            Queue moderationQueue,
            TopicExchange moderationExchange,
            ModerationMessagingProperties properties
    ) {
        return BindingBuilder.bind(moderationQueue)
                .to(moderationExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
