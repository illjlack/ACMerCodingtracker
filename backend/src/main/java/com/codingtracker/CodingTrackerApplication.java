package com.codingtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * CodingTracker 主应用程序入口
 * 
 * @author CodingTracker Team
 * @version 2.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableConfigurationProperties
public class CodingTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodingTrackerApplication.class, args);
    }
}