package com.interview.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.interview")
@MapperScan("com.interview.infrastructure.persistence.mapper")
@EnableRabbit
@EnableScheduling
public class InterviewApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewApiApplication.class, args);
    }
}
