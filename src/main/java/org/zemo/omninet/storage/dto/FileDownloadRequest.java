package org.zemo.omninet.storage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDownloadRequest {
    
    @NotBlank(message = "File name is required")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+(?:/[a-zA-Z0-9._-]+)*$", 
             message = "File name can only contain alphanumeric characters, dots, hyphens, underscores, and forward slashes")
    private String fileName;
}
