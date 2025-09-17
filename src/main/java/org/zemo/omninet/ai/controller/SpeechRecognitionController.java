package org.zemo.omninet.ai.controller;

import org.zemo.omninet.ai.service.SpeechRecognitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/speech")
@RequiredArgsConstructor
@Slf4j
public class SpeechRecognitionController {

    private final SpeechRecognitionService speechRecognitionService;

    @PostMapping("/recognize")
    public ResponseEntity<Map<String, String>> recognizeSpeech(@RequestParam("audio") MultipartFile audioFile) {
        log.debug("Received audio file for recognition: {}, size: {}", audioFile.getOriginalFilename(), audioFile.getSize());

        String recognizedText = speechRecognitionService.recognizeSpeech(audioFile);

        Map<String, String> response = new HashMap<>();
        response.put("text", recognizedText);

        return ResponseEntity.ok(response);
    }
}