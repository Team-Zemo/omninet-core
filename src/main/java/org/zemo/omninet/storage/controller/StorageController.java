package org.zemo.omninet.storage.controller;

import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninet.storage.dto.*;
import org.zemo.omninet.storage.service.StorageService;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/storage")
@Validated
public class StorageController {

    private final StorageService storageService;

    /**
     * Extract user ID from authentication object
     */
    private String getUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.zemo.omninet.security.model.User) {
            return ((org.zemo.omninet.security.model.User) principal).getId();
        }
        // Fallback to authentication name if it's already a string ID
        return authentication.getName();
    }

    /**
     * Create a folder for the authenticated user
     */
    @PostMapping("/folders")
    public ResponseEntity<StorageResponse> createFolder(
            @Valid @RequestBody CreateFolderRequest request,
            Authentication authentication) {
        try {
            String userId = getUserId(authentication);
            boolean created = storageService.createUserFolder(userId, request.getFolderName());

            if (created) {
                return ResponseEntity.ok(StorageResponse.success("Folder created successfully"));
            } else {
                return ResponseEntity.ok(StorageResponse.error("Folder already exists"));
            }
        } catch (Exception e) {
            log.error("Error creating folder for user {}: {}", getUserId(authentication), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to create folder: " + e.getMessage()));
        }
    }

    /**
     * Delete a folder for the authenticated user
     */
    @DeleteMapping("/folders")
    public ResponseEntity<StorageResponse> deleteFolder(
            @Valid @RequestBody DeleteFolderRequest request,
            Authentication authentication) {
        try {
            log.info("Deleting folder {}", request.getFolderName());
            String userId = getUserId(authentication);
            storageService.deleteUserFolder(userId, request.getFolderName());
            return ResponseEntity.ok(StorageResponse.success("Folder deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting folder for user {}: {}", getUserId(authentication), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to delete folder: " + e.getMessage()));
        }
    }

    /**
     * Generate a presigned URL for file upload
     */
    @PostMapping("/files/upload-url")
    public ResponseEntity<StorageResponse> generateUploadUrl(
            @Valid @RequestBody FileUploadRequest request,
            Authentication authentication) {
        try {
            String userId = getUserId(authentication);
            String presignedUrl = storageService.generatePresignedUploadUrl(userId, request.getFileName());

            PresignedUrlResponse response = new PresignedUrlResponse(presignedUrl, request.getFileName());
            return ResponseEntity.ok(StorageResponse.success("Upload URL generated successfully", response));
        } catch (Exception e) {
            log.error("Error generating upload URL for user {}: {}", getUserId(authentication), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to generate upload URL: " + e.getMessage()));
        }
    }

    /**
     * Generate a presigned URL for file download
     */
    @PostMapping("/files/download-url")
    public ResponseEntity<StorageResponse> generateDownloadUrl(
            @Valid @RequestBody FileDownloadRequest request,
            Authentication authentication) {
        try {
            String userId = getUserId(authentication);
            String presignedUrl = storageService.generatePresignedDownloadUrl(userId, request.getFileName());

            PresignedUrlResponse response = new PresignedUrlResponse(presignedUrl, request.getFileName());
            return ResponseEntity.ok(StorageResponse.success("Download URL generated successfully", response));
        } catch (Exception e) {
            log.error("Error generating download URL for user {}: {}", getUserId(authentication), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to generate download URL: " + e.getMessage()));
        }
    }

    /**
     * Delete a file for the authenticated user
     */
    @DeleteMapping("/files/{fileName}")
    public ResponseEntity<StorageResponse> deleteFile(
            @PathVariable String fileName,
            Authentication authentication) {
        try {
            String userId = getUserId(authentication);
            storageService.deleteFile(userId, fileName);
            return ResponseEntity.ok(StorageResponse.success("File deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting file for user {}: {}", getUserId(authentication), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to delete file: " + e.getMessage()));
        }
    }

    /**
     * List all files and folders in a specific folder
     */
    @GetMapping("/folders/{folderName}/contents")
    public ResponseEntity<StorageResponse> listFolderContents(
            @PathVariable String folderName,
            Authentication authentication) {
        try {
            String userId = getUserId(authentication);
            String fullFolderPath = "users/" + userId + "/" + folderName + "/";

            Iterable<Result<Item>> objects = storageService.listObjectsInFolder(fullFolderPath);
            List<FileInfoResponse> fileInfoList = new ArrayList<>();

            for (Result<Item> result : objects) {
                Item item = result.get();
                String objectName = item.objectName();

                // Skip the folder itself
                if (objectName.equals(fullFolderPath)) {
                    continue;
                }

                // Extract relative path from the folder
                String relativePath = objectName.substring(fullFolderPath.length());

                // Skip empty paths
                if (relativePath.isEmpty()) {
                    continue;
                }

                // Check if this is a direct child (not nested deeper)
                String[] pathParts = relativePath.split("/");
                boolean isDirectChild = pathParts.length <= 2; // file or folder/

                if (!isDirectChild) {
                    continue; // Skip nested items for this listing
                }

                // Determine if it's a folder or file
                boolean isFolder = objectName.endsWith("/");
                String name;
                String displayPath;

                name = pathParts[0];
                if (isFolder) {
                    // For folders, remove the trailing slash for display
                    displayPath = name + "/";
                } else {
                    // For files, use the full name
                    displayPath = relativePath;
                }

                // Get size and last modified date
                long size = isFolder ? 0 : item.size();

                // Convert MinIO's ZonedDateTime to LocalDateTime
                LocalDateTime lastModified = item.lastModified() != null ?
                    item.lastModified().toLocalDateTime() :
                    LocalDateTime.now();

                fileInfoList.add(new FileInfoResponse(name, displayPath, size, lastModified, isFolder));
            }

            return ResponseEntity.ok(StorageResponse.success("Folder contents retrieved successfully", fileInfoList));
        } catch (Exception e) {
            log.error("Error listing folder contents for user {}: {}", getUserId(authentication), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to list folder contents: " + e.getMessage()));
        }
    }

    /**
     * Check if a file exists
     */
    @GetMapping("/files/{fileName}/exists")
    public ResponseEntity<StorageResponse> checkFileExists(
            @PathVariable String fileName,
            Authentication authentication) {
        try {
            String userId = getUserId(authentication);
            String fullFilePath = "users/" + userId + "/" + fileName;
            boolean exists = storageService.fileExists(fullFilePath);

            return ResponseEntity.ok(StorageResponse.success("File existence checked", exists));
        } catch (Exception e) {
            log.error("Error checking file existence for user {}: {}", getUserId(authentication), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to check file existence: " + e.getMessage()));
        }
    }

    /**
     * Check if a folder exists
     */
    @GetMapping("/folders/{folderName}/exists")
    public ResponseEntity<StorageResponse> checkFolderExists(
            @PathVariable String folderName,
            Authentication authentication) {
        try {
            String userId = getUserId(authentication);
            String fullFolderPath = "users/" + userId + "/" + folderName + "/";
            boolean exists = storageService.folderExists(fullFolderPath);

            return ResponseEntity.ok(StorageResponse.success("Folder existence checked", exists));
        } catch (Exception e) {
            log.error("Error checking folder existence for user {}: {}", getUserId(authentication), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to check folder existence: " + e.getMessage()));
        }
    }

    /**
     * List all folders in the user's root directory
     */
    @GetMapping("/folders")
    public ResponseEntity<StorageResponse> listUserFolders(Authentication authentication) {
        try {
            String userId = getUserId(authentication);
            String userRootPath = "users/" + userId + "/";

            Iterable<Result<Item>> objects = storageService.listObjectsInFolder(userRootPath);
            List<FileInfoResponse> folderList = new ArrayList<>();

            for (Result<Item> result : objects) {
                Item item = result.get();
                String objectName = item.objectName();

                // Skip the user root folder itself
                if (objectName.equals(userRootPath)) {
                    continue;
                }

                // Only include folders (objects ending with '/')
                if (objectName.endsWith("/")) {
                    String relativePath = objectName.substring(userRootPath.length());
                    String folderName = relativePath.substring(0, relativePath.length() - 1); // Remove trailing '/'

                    // Only include direct subfolders (no nested paths)
                    if (!folderName.contains("/")) {
                        folderList.add(new FileInfoResponse(folderName, relativePath, 0, true));
                    }
                }
            }

            return ResponseEntity.ok(StorageResponse.success("User folders retrieved successfully", folderList));
        } catch (Exception e) {
            log.error("Error listing user folders for user {}: {}", getUserId(authentication), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to list user folders: " + e.getMessage()));
        }
    }
}
