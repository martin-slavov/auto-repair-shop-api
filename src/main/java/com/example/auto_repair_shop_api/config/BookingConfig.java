package com.example.auto_repair_shop_api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(BookingProperties.class)
public class BookingConfig {

    // Server's local time zone, like the rest of the API. A bean so tests can use a fixed clock.
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
