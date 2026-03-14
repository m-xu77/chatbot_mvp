package com.mvp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvp.model.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
public class OpenRouterService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final WebClient webClient;

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.model:minimax/minimax-m2.5}")
    private String model;

    public OpenRouterService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://openrouter.ai/api/v1")
                .build();
    }

    public Flux<String> streamChat(List<ChatRequest.Message> messages) {
        var body = Map.of(
                "model", model,
                "stream", true,
                "messages", messages.stream()
                        .map(m -> Map.of("role", m.role(), "content", m.content()))
                        .toList()
        );

        log.info("Calling OpenRouter: model={}, messages={}", model, messages.size());

        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", "http://localhost:8080")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(chunk -> log.debug("SSE chunk: {}", chunk))
                .filter(chunk -> !chunk.equals("[DONE]"))
                .mapNotNull(this::extractContent)
                .filter(s -> !s.isEmpty())
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("OpenRouter error: {} {}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
                    String msg = switch (ex.getStatusCode().value()) {
                        case 429 -> "[Error] Rate limit exceeded (429). Wait a moment and try again.";
                        case 401 -> "[Error] Invalid API key (401). Check your OPENROUTER_API_KEY.";
                        case 402 -> "[Error] Insufficient credits (402). Top up your OpenRouter account.";
                        default  -> "[Error] OpenRouter returned " + ex.getStatusCode().value() + ": " + ex.getResponseBodyAsString();
                    };
                    return Flux.just(msg);
                });
    }

    private String extractContent(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode content = root.path("choices").path(0).path("delta").path("content");
            if (content.isMissingNode() || content.isNull()) return null;
            return content.asText();
        } catch (Exception e) {
            log.warn("Failed to parse SSE chunk: {}", json);
            return null;
        }
    }
}
