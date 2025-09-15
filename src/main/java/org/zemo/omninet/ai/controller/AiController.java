package org.zemo.omninet.ai.controller;

import org.zemo.omninet.ai.model.ChatMessage;
import org.zemo.omninet.ai.model.ChatSession;
import org.zemo.omninet.ai.service.AiService;
import org.zemo.omninet.ai.service.ChatService;
import org.zemo.omninet.ai.service.SpeechRecognitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final AiService aiService;
    private final SpeechRecognitionService speechRecognitionService;
    private final ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestParam("prompt") String prompt,
            @RequestParam("sessionId") Long sessionId) {

        log.debug("Received chat request: {} for session: {}", prompt, sessionId);

        try {
            ChatSession session = chatService.getChatSessionById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            // Save user message
            ChatMessage userMessage = chatService.saveMessage(session, ChatMessage.MessageType.USER_TEXT, prompt);

            // Get AI response with conversation history
            String aiResponse = aiService.getTextResponseWithHistory(prompt, session);

            // Save AI response
            ChatMessage aiMessage = chatService.saveMessage(session, ChatMessage.MessageType.AI_TEXT, aiResponse);

            Map<String, Object> result = new HashMap<>();
            result.put("userMessage", Map.of(
                    "id", userMessage.getId(),
                    "content", userMessage.getContent(),
                    "type", userMessage.getType(),
                    "createdAt", userMessage.getCreatedAt()
            ));
            result.put("aiMessage", Map.of(
                    "id", aiMessage.getId(),
                    "content", aiMessage.getContent(),
                    "type", aiMessage.getType(),
                    "createdAt", aiMessage.getCreatedAt()
            ));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Failed to process chat request")
            );
        }
    }

    @PostMapping("/chat/reactive")
    public Mono<ResponseEntity<Map<String, Object>>> reactiveChat(
            @RequestParam("prompt") String prompt,
            @RequestParam("sessionId") Long sessionId) {

        log.debug("Received reactive chat request: {} for session: {}", prompt, sessionId);

        return Mono.just(sessionId)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(id -> {
                    try {
                        ChatSession session = chatService.getChatSessionById(id)
                                .orElseThrow(() -> new RuntimeException("Session not found"));

                        // Save user message
                        ChatMessage userMessage = chatService.saveMessage(session, ChatMessage.MessageType.USER_TEXT, prompt);

                        // Get AI response with conversation history
                        String aiResponse = aiService.getTextResponseWithHistory(prompt, session);

                        // Save AI response
                        ChatMessage aiMessage = chatService.saveMessage(session, ChatMessage.MessageType.AI_TEXT, aiResponse);

                        Map<String, Object> result = new HashMap<>();
                        result.put("userMessage", Map.of(
                                "id", userMessage.getId(),
                                "content", userMessage.getContent(),
                                "type", userMessage.getType(),
                                "createdAt", userMessage.getCreatedAt()
                        ));
                        result.put("aiMessage", Map.of(
                                "id", aiMessage.getId(),
                                "content", aiMessage.getContent(),
                                "type", aiMessage.getType(),
                                "createdAt", aiMessage.getCreatedAt()
                        ));

                        return Mono.just(ResponseEntity.ok(result));
                    } catch (Exception e) {
                        log.error("Error processing reactive chat request", e);
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                Map.of("error", "Failed to process chat request")
                        ));
                    }
                });
    }

    @PostMapping("/chat/speech")
    public ResponseEntity<byte[]> chatWithSpeech(
            @RequestParam("prompt") String prompt,
            @RequestParam("sessionId") Long sessionId) {

        log.debug("Received chat with speech request: {} for session: {}", prompt, sessionId);

        try {
            ChatSession session = chatService.getChatSessionById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            // Save user message
            chatService.saveMessage(session, ChatMessage.MessageType.USER_TEXT, prompt);

            // Get AI response with speech and conversation history
            byte[] audioData = aiService.getSpeechResponseWithHistory(prompt, session);
            String aiResponse = aiService.getTextResponseWithHistory(prompt, session);

            // Save AI response
            chatService.saveMessage(session, ChatMessage.MessageType.AI_AUDIO, aiResponse);

            if (audioData.length == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            headers.setContentLength(audioData.length);
            headers.set("Content-Disposition", "attachment; filename=\"ai_response.wav\"");

            return new ResponseEntity<>(audioData, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error processing speech chat request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/chat/voice")
    public ResponseEntity<byte[]> voiceToVoice(
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam("sessionId") Long sessionId) {

        log.debug("Received voice-to-voice request for session: {}, file: {}, size: {}",
                sessionId, audioFile.getOriginalFilename(), audioFile.getSize());

        try {
            ChatSession session = chatService.getChatSessionById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            // Recognize speech from audio
            String recognizedText = speechRecognitionService.recognizeSpeech(audioFile);
            log.debug("Recognized text: {}", recognizedText);

            if (recognizedText == null || recognizedText.trim().isEmpty()) {
                log.warn("No text recognized from audio");
                return ResponseEntity.badRequest().body(null);
            }

            // Save user audio message
            chatService.saveMessage(session, ChatMessage.MessageType.USER_AUDIO, recognizedText);

            // Get AI response with conversation history and synthesize speech
            byte[] audioResponse = aiService.getSpeechResponseWithHistory(recognizedText, session);
            String aiResponse = aiService.getTextResponseWithHistory(recognizedText, session);

            // Save AI audio response
            chatService.saveMessage(session, ChatMessage.MessageType.AI_AUDIO, aiResponse);

            if (audioResponse.length == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            headers.setContentLength(audioResponse.length);
            headers.set("Content-Disposition", "attachment; filename=\"voice_response.wav\"");

            return new ResponseEntity<>(audioResponse, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error processing voice-to-voice request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}