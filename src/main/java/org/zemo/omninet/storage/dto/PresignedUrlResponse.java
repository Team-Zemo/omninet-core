package org.zemo.omninet.storage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {
    
    private String url;
    private String fileName;
    private String expiresIn;
    
    public PresignedUrlResponse(String url, String fileName) {
        this.url = url;
        this.fileName = fileName;
        this.expiresIn = "1 hour";
    }
}
