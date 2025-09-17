package org.zemo.omninet.ai.controller;

import org.zemo.omninet.ai.service.SpeechSynthesisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/speech")
@RequiredArgsConstructor
@Slf4j
public class SpeechSynthesisController {

    private final SpeechSynthesisService speechSynthesisService;

    @PostMapping("/synthesize")
    public ResponseEntity<byte[]> synthesizeSpeech(@RequestParam("text") String text) {
        log.debug("Synthesizing speech for text: {}", text);

        byte[] audioData = speechSynthesisService.synthesizeSpeech(text);

        if (audioData.length == 0) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/wav"));
        headers.setContentLength(audioData.length);

        return new ResponseEntity<>(audioData, headers, HttpStatus.OK);
    }
}