// controller/DataLookupController.java
package com.myfr.llm.functions.tempurature_poc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.HashMap;
import java.time.LocalDateTime;

@RequestMapping("/api/data")
public class DataLookupController {
    private static final Logger log = LoggerFactory.getLogger(DataLookupController.class);
    private static final String RESPONSE_LOG_PATTERN = "LLM Data Response - Type: {}, Request ID: {}, Value: {}";

    @GetMapping("/lookup/{query}")
    public ResponseEntity<Map<String, Object>> lookupData(
            @PathVariable String query,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {

        // Generate request ID if not provided
        String trackingId = requestId != null ? requestId : UUID.randomUUID().toString().substring(0, 8);

        MDC.put("requestId", trackingId);
        MDC.put("queryType", query);

        try {
            // Log request details at debug level
            log.info("LLM Data Request - Type: {}, Request ID: {}", query, trackingId);

            // Generate data
            Object value = generateSampleData(query);

            // Create response
            Map<String, Object> result = new HashMap<>();
            result.put("timestamp", LocalDateTime.now());
            result.put("query", query);
            result.put("value", value);
            result.put("requestId", trackingId);

            log.info(RESPONSE_LOG_PATTERN, query, trackingId, value);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error processing request - Type: {}, Request ID: {}, Error: {}",
                    query, trackingId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    private Object generateSampleData(String query) {
        log.debug("Generating sample data for query type: {}", query);

        return switch (query.toLowerCase()) {
            case "temperature" -> {
                double temp = new Random().nextDouble() * 30 + 10; // 10-40°C
                log.debug("Generated temperature: {}", temp);
                yield temp;
            }
            case "stock" -> {
                double price = new Random().nextDouble() * 1000 + 100; // $100-1100
                log.debug("Generated stock price: {}", price);
                yield price;
            }
            default -> {
                log.warn("Unknown query type: {}", query);
                yield "No data available for: " + query;
            }
        };
    }
}