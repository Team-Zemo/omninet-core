package org.zemo.omninetsecurity.storage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageResponse {
    
    private boolean success;
    private String message;
    private Object data;
    
    public static StorageResponse success(String message) {
        return new StorageResponse(true, message, null);
    }
    
    public static StorageResponse success(String message, Object data) {
        return new StorageResponse(true, message, data);
    }
    
    public static StorageResponse error(String message) {
        return new StorageResponse(false, message, null);
    }
}
