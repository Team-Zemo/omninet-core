package org.zemo.omninet.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
