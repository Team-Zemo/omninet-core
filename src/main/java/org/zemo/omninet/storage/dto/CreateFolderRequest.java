package org.zemo.omninet.storage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFolderRequest {
    
    @NotBlank(message = "Folder name is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+(?:/[a-zA-Z0-9_-]+)*/?$", 
             message = "Folder name can only contain alphanumeric characters, hyphens, underscores, and forward slashes")
    private String folderName;
}
