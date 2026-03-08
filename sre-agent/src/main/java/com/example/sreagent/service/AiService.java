package com.example.sreagent.service;

import org.springframework.stereotype.Service;

@Service
public class AiService {

    public String processInput(String input) {
        // Implement the logic to process the input and interact with AI models
        return "Processed input: " + input;
    }

    public String getResponse() {
        // Implement the logic to get a response from the AI model
        return "AI response";
    }
}