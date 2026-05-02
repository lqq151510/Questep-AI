package com.interview.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.interview")
@MapperScan("com.interview.infrastructure.persistence.mapper")
public class InterviewApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewApiApplication.class, args);
    }
}
