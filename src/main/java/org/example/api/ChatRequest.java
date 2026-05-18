package org.example.api;

public record ChatRequest(String personality, String message, String sessionId) {
    }
