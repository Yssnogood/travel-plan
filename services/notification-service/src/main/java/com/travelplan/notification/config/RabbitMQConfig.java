package com.travelplan.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    
    public static final String WELCOME_QUEUE = "notification.email.welcome";
    public static final String PASSWORD_RESET_QUEUE = "notification.email.password-reset";
    public static final String BOOKING_QUEUE = "notification.email.booking";
    public static final String PAYMENT_QUEUE = "notification.email.payment";

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue welcomeQueue() {
        return new Queue(WELCOME_QUEUE, true);
    }

    @Bean
    public Queue passwordResetQueue() {
        return new Queue(PASSWORD_RESET_QUEUE, true);
    }

    @Bean
    public Queue bookingQueue() {
        return new Queue(BOOKING_QUEUE, true);
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    public Binding welcomeBinding(Queue welcomeQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(welcomeQueue).to(notificationExchange).with("email.welcome");
    }

    @Bean
    public Binding passwordResetBinding(Queue passwordResetQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(passwordResetQueue).to(notificationExchange).with("email.password-reset");
    }

    @Bean
    public Binding bookingBinding(Queue bookingQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(bookingQueue).to(notificationExchange).with("email.booking");
    }

    @Bean
    public Binding paymentBinding(Queue paymentQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(paymentQueue).to(notificationExchange).with("email.payment");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
