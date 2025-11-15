package com.healthtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HealthTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthTrackerApplication.class, args);
    }
}
