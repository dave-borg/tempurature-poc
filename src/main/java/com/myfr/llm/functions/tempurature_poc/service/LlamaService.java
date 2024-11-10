// service/LlamaService.java
package com.myfr.llm.functions.tempurature_poc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LlamaService {
    private static final Logger log = LoggerFactory.getLogger(LlamaService.class);

    private final WebClient llamaWebClient;
    private final String systemPrompt;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlamaService(WebClient llamaWebClient, @Value("${app.system.prompt:}") String systemPrompt) {
        this.llamaWebClient = llamaWebClient;
        this.systemPrompt = systemPrompt != null ? systemPrompt : DEFAULT_SYSTEM_PROMPT;
    }

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are an AI assistant with access to real-time data through an API endpoint.
            To look up data, you can make HTTP GET requests to: http://java-service:8081/api/data/lookup/{query}

            Available queries:
            - temperature: Gets current temperature
            - stock: Gets current stock price

            When you need real-time data:
            1. Make the appropriate HTTP GET request
            2. Include the data in your response
            3. Always specify the timestamp of the data
            4. Just give me a concise response, I don't need an explaination of how you're retreiving the data.

            Example:
            To get temperature: GET http://java-service:8081/api/data/lookup/temperature
            Response format: {"timestamp": "2024-11-09T10:30:00", "query": "temperature", "value": 22.5}

            To get temperature: GET http://java-service:8081/api/data/lookup/stock

            Remember to interpret and explain the data in your responses.
            """;

    public Mono<String> generateResponse(String userInput) {
        log.info("Generating response for user input: {}", userInput);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", formatPrompt(userInput));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 500);
        requestBody.put("tools", Arrays.asList(
                Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", "getData",
                                "description", "Get real-time data from the system",
                                "parameters", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "query", Map.of(
                                                        "type", "string",
                                                        "enum", Arrays.asList("temperature", "stock"),
                                                        "description", "Type of data to retrieve")),
                                        "required", Arrays.asList("query"))))));

        log.debug("Sending request to Llama service with body: {}", requestBody);

        return llamaWebClient.post()
                .uri("/completion")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .flatMap(this::processLlamaResponse)
                .doOnError(e -> log.error("Error calling Llama service", e))
                .doOnSuccess(response -> log.debug("Received response from Llama: {}", response));
    }

    private String formatPrompt(String userInput) {
        return String.format("""
                System: %s
                User: %s
                Assistant:""",
                systemPrompt, userInput);
    }

    private Mono<String> processLlamaResponse(Map<String, Object> response) {
        try {
            log.debug("Processing Llama response: {}", response);

            // Handle function calls if present
            if (response.containsKey("tool_calls")) {
                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) response.get("tool_calls");
                return executeToolCalls(toolCalls)
                        .map(results -> formatFinalResponse(
                                response.get("content").toString(),
                                results));
            }

            return Mono.just(response.get("content").toString());
        } catch (Exception e) {
            log.error("Error processing Llama response", e);
            return Mono.error(e);
        }
    }

    private Mono<Map<String, Object>> executeToolCalls(List<Map<String, Object>> toolCalls) {
        log.debug("Executing tool calls: {}", toolCalls);

        return Mono.just(toolCalls)
                .flatMap(calls -> {
                    try {
                        return Mono.just(calls.stream()
                                .map(toolCall -> {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                                    @SuppressWarnings("unchecked")
                                    String query = ((Map<String, String>) function.get("arguments")).get("query");

                                    return llamaWebClient.get()
                                            .uri("http://java-service:8081/api/data/lookup/" + query)
                                            .retrieve()
                                            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                                            });
                                })
                                .reduce((acc, result) -> {
                                    Map<String, Object> combined = new HashMap<>();
                                    combined.putAll(acc.block());
                                    combined.putAll(result.block());
                                    return Mono.just(combined);
                                })
                                .get()
                                .block());
                    } catch (Exception e) {
                        log.error("Error executing tool calls", e);
                        return Mono.error(e);
                    }
                });
    }

    private String formatFinalResponse(String llamaResponse, Map<String, Object> toolResults) {
        try {
            return String.format("%s%n%nReal-time data:%n%s",
                    llamaResponse,
                    objectMapper.writeValueAsString(toolResults));
        } catch (Exception e) {
            log.error("Error formatting final response", e);
            return llamaResponse + "\n\nError processing real-time data.";
        }
    }
}