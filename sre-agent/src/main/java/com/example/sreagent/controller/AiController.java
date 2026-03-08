package com.example.sreagent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @GetMapping("/status")
    public String getStatus() {
        return "AI Service is running";
    }

    // Additional methods for processing AI-related requests can be added here
}