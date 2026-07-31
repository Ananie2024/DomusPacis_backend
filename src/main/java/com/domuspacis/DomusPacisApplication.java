package com.domuspacis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DomusPacisApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(DomusPacisApplication.class, args);
    }

    private static void loadDotEnv() {
        File envFile = new File(".env.local");
        if (!envFile.exists()) {
            envFile = new File(".env");
        }
        System.out.println("DEBUG: Looking for .env file at: " + envFile.getAbsolutePath());
        System.out.println("DEBUG: .env file exists: " + envFile.exists());
        if (envFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eqIndex = line.indexOf('=');
                    if (eqIndex > 0) {
                        String key = line.substring(0, eqIndex).trim();
                        String val = line.substring(eqIndex + 1).trim();
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, val);
                            System.out.println("DEBUG: Loaded env var: " + key + "=" + val);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("DEBUG: Error loading .env file: " + e.getMessage());
            }
        }
    }
}
