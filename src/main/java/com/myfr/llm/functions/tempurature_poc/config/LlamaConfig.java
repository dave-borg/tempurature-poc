// config/LlamaConfig.java
package com.myfr.llm.functions.tempurature_poc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Configuration
public class LlamaConfig {
    @Value("${llama.server.url}")
    private String llamaServerUrl;

    @Bean
    WebClient llamaWebClient() {
        return WebClient.builder()
                .baseUrl(llamaServerUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}