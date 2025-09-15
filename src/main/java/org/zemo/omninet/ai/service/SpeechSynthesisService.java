package org.zemo.omninet.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class SpeechSynthesisService {

    private static final String TTS_API_URL = "http://127.0.0.1:5000/tts";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SpeechSynthesisService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public byte[] synthesizeSpeech(String text) {
        log.debug("Synthesizing speech for text: {}", text);

        try {
            // Create JSON payload
            String jsonPayload = objectMapper.writeValueAsString(Map.of("text", text));

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TTS_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            // Send request and get response
            HttpResponse<byte[]> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                log.debug("Speech synthesis successful");
                return response.body();
            } else {
                log.error("TTS API failed with status code: {}", response.statusCode());
                return new byte[0];
            }

        } catch (IOException | InterruptedException e) {
            log.error("Error during speech synthesis", e);
            return new byte[0];
        }
    }
}