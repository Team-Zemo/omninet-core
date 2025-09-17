package org.zemo.omninet.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Map;

@Service
@Slf4j
public class SpeechRecognitionService {

    private static final String STT_API_URL = "http://127.0.0.1:5000/stt";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SpeechRecognitionService(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    public String recognizeSpeech(MultipartFile audioFile) {
        try {
            // Save MultipartFile to a temp file
            var tempFile = Files.createTempFile("audio", ".wav");
            audioFile.transferTo(tempFile);

            // Build multipart/form-data request
            String boundary = "----JavaMultipartBoundary" + System.currentTimeMillis();
            var byteArray = Files.readAllBytes(tempFile);
            String fileName = audioFile.getOriginalFilename();

            String partHeader = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"audio\"; filename=\"" + fileName + "\"\r\n"
                    + "Content-Type: audio/wav\r\n\r\n";
            String partFooter = "\r\n--" + boundary + "--\r\n";

            byte[] body = concat(
                    partHeader.getBytes(),
                    byteArray,
                    partFooter.getBytes()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(STT_API_URL))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Files.deleteIfExists(tempFile);

            if (response.statusCode() == 200) {
                Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                return (String) result.getOrDefault("text", "");
            } else {
                log.error("STT API failed: {}", response.body());
                return "";
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error during speech recognition", e);
            return "";
        }
    }

    private static byte[] concat(byte[]... arrays) throws IOException {
        int total = 0;
        for (byte[] arr : arrays) total += arr.length;
        byte[] result = new byte[total];
        int pos = 0;
        for (byte[] arr : arrays) {
            System.arraycopy(arr, 0, result, pos, arr.length);
            pos += arr.length;
        }
        return result;
    }
}