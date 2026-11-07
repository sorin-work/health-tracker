package com.healthtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Users authenticate with JWTs only, so skip the default in-memory user (and its logged password).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class HealthTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthTrackerApplication.class, args);
    }
}
