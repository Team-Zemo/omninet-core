package org.zemo.omninet.ai.controller;

import lombok.Data;

@Data
public class AiVoiceResponse {
    byte[] audioData;
    String textResponse;
}
