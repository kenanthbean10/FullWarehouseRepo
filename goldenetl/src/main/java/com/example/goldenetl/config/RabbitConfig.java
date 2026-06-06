package com.example.goldenetl.config;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RabbitConfig {
    @Bean
    public SimpleMessageConverter converter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        // This explicitly tells Spring to allow LinkedHashMap and other util classes
        converter.setAllowedListPatterns(List.of("java.util.*", "java.lang.*", "com.example.*"));
        return converter;
    }
}
