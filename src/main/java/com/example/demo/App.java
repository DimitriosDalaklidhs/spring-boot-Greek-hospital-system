package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {  // keep name App to avoid renaming references
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}