package org.zemo.omninetsecurity.storage.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoResponse {

    private String name;
    private String path;
    private long size;
    private LocalDateTime lastModified;
    private boolean isFolder;
    private String type;

    public FileInfoResponse(String name, String path, long size, boolean isFolder) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.isFolder = isFolder;
        this.lastModified = LocalDateTime.now();
        this.type = isFolder ? "folder" : getFileExtension(name);
    }

    public FileInfoResponse(String name, String path, long size, LocalDateTime lastModified, boolean isFolder) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.lastModified = lastModified != null ? lastModified : LocalDateTime.now();
        this.isFolder = isFolder;
        this.type = isFolder ? "folder" : getFileExtension(name);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "file";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
