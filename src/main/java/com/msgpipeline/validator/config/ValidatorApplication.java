package com.msgpipeline.validator.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.msgpipeline.validator")
public class ValidatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ValidatorApplication.class, args);
    }
}
