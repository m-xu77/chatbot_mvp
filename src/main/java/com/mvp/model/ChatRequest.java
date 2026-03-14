package com.mvp.model;

import java.util.List;

public record ChatRequest(List<Message> messages) {
    public record Message(String role, String content) {}
}
