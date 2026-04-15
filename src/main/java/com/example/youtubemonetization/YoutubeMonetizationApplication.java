package com.example.youtubemonetization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YoutubeMonetizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(YoutubeMonetizationApplication.class, args);
    }
}
