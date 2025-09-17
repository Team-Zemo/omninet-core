package org.zemo.omninet.ai.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaConfig {

    @Value("${spring.ai.ollama.model}")
    private String modelName;

    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    @Bean
    public OllamaChatModel ollamaChatModel() {
        return OllamaChatModel.builder()
                .ollamaApi(new OllamaApi.Builder().baseUrl(baseUrl).build())
                .defaultOptions(OllamaOptions.builder().model(modelName).build())
                .build();
    }
}
