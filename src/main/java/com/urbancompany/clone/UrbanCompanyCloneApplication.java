package com.urbancompany.clone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class UrbanCompanyCloneApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrbanCompanyCloneApplication.class, args);
    }
}
