package com.collaboration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class EngineApplication {

    public static void main(String[] args) {
        // Load .env variables into System properties for Spring to use
        try {
            Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });
        } catch (Exception e) {
            // Ignore if .env is missing (e.g. in production)
        }
        
        SpringApplication.run(EngineApplication.class, args);
    }
}
