package org.zemo.omninet.storage.controller;

import io.minio.Result;
import io.minio.messages.Item;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.zemo.omninet.notes.util.CommonUtil;
import org.zemo.omninet.storage.dto.*;
import org.zemo.omninet.storage.service.StorageService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/storage")
@Validated
@Tag(name = "Storage Related")
public class StorageController {

    private final StorageService storageService;

    /**
     * Extract user Email from authentication object
     */
    private String getUserEmail() {
        return Objects.requireNonNull(CommonUtil.getLoggedInUser()).getEmail();
    }

    /**
     * Create a folder for the authenticated user
     */
    @Operation(description = "Api to create a folder.\n Should provide full path eg- \"pop\", \"pop/opo\"")
    @PostMapping("/folders")
    public ResponseEntity<StorageResponse> createFolder(
            @Valid @RequestBody CreateFolderRequest request) {
        try {
            String userEmail = getUserEmail();
            boolean created = storageService.createUserFolder(userEmail, request.getFolderName());

            if (created) {
                return ResponseEntity.ok(StorageResponse.success("Folder created successfully"));
            } else {
                return ResponseEntity.ok(StorageResponse.error("Folder already exists"));
            }
        } catch (Exception e) {
            log.error("Error creating folder for user {}: {}", getUserEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to create folder: " + e.getMessage()));
        }
    }

    /**
     * Delete a folder for the authenticated user
     */
    @Operation(description = "Api to delete a folder.\n Should provide full path eg- \"pop\", \"pop/opo\"")
    @DeleteMapping("/folders")
    public ResponseEntity<StorageResponse> deleteFolder(
            @Valid @RequestBody DeleteFolderRequest request) {
        try {
            log.info("Deleting folder {}", request.getFolderName());
            String userEmail = getUserEmail();
            storageService.deleteUserFolder(userEmail, request.getFolderName());
            return ResponseEntity.ok(StorageResponse.success("Folder deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting folder for user {}: {}", getUserEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to delete folder: " + e.getMessage()));
        }
    }

    /**
     * Generate a presigned URL for file upload
     */
    @Operation(description = "Api to get a PreSignedUrl to upload a file.")
    @PostMapping("/files/upload-url")
    public ResponseEntity<StorageResponse> generateUploadUrl(@Valid @RequestBody FileUploadRequest request) {
        try {
            String userEmail = getUserEmail();
            String presignedUrl = storageService.generatePresignedUploadUrl(userEmail, request.getFileName());

            PresignedUrlResponse response = new PresignedUrlResponse(presignedUrl, request.getFileName());
            return ResponseEntity.ok(StorageResponse.success("Upload URL generated successfully", response));
        } catch (Exception e) {
            log.error("Error generating upload URL for user {}: {}", getUserEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to generate upload URL: " + e.getMessage()));
        }
    }

    /**
     * Generate a presigned URL for file download
     */
    @Operation(description = "Api to get a PreSignedUrl to download a file.")
    @PostMapping("/files/download-url")
    public ResponseEntity<StorageResponse> generateDownloadUrl(
            @Valid @RequestBody FileDownloadRequest request) {
        try {
            String userEmail = getUserEmail();
            String presignedUrl = storageService.generatePresignedDownloadUrl(userEmail, request.getFileName());

            PresignedUrlResponse response = new PresignedUrlResponse(presignedUrl, request.getFileName());
            return ResponseEntity.ok(StorageResponse.success("Download URL generated successfully", response));
        } catch (Exception e) {
            log.error("Error generating download URL for user {}: {}", getUserEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to generate download URL: " + e.getMessage()));
        }
    }

    /**
     * Delete a file for the authenticated user
     */
    @Operation(description = "Api to delete a file")
    @DeleteMapping("/files/{fileName}")
    public ResponseEntity<StorageResponse> deleteFile(
            @PathVariable String fileName) {
        try {
            String userEmail = getUserEmail();
            storageService.deleteFile(userEmail, fileName);
            return ResponseEntity.ok(StorageResponse.success("File deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting file for user {}: {}", getUserEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to delete file: " + e.getMessage()));
        }
    }

    /**
     * List all files and folders in a specific folder
     */
    @Operation(description = "Api to get all files and folders in a specific foder")
    @GetMapping("/folders/{folderName}/contents")
    public ResponseEntity<StorageResponse> listFolderContents(
            @PathVariable String folderName) {
        try {
            String userEmail = getUserEmail();
            String fullFolderPath = "users/" + userEmail + "/" + folderName + "/";

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
            log.error("Error listing folder contents for user {}: {}", getUserEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to list folder contents: " + e.getMessage()));
        }
    }

    /**
     * Check if a file exists
     */
    @Operation(description = "Api to check if a file exists.")
    @GetMapping("/files/{fileName}/exists")
    public ResponseEntity<StorageResponse> checkFileExists(
            @PathVariable String fileName) {
        try {
            String userEmail = getUserEmail();
            String fullFilePath = "users/" + userEmail + "/" + fileName;
            boolean exists = storageService.fileExists(fullFilePath);

            return ResponseEntity.ok(StorageResponse.success("File existence checked", exists));
        } catch (Exception e) {
            log.error("Error checking file existence for user {}: {}", getUserEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to check file existence: " + e.getMessage()));
        }
    }

    /**
     * Check if a folder exists
     */
    @Operation(description = "Api to check if a folder exists.")
    @GetMapping("/folders/{folderName}/exists")
    public ResponseEntity<StorageResponse> checkFolderExists(
            @PathVariable String folderName) {
        try {
            String userEmail = getUserEmail();
            String fullFolderPath = "users/" + userEmail + "/" + folderName + "/";
            boolean exists = storageService.folderExists(fullFolderPath);

            return ResponseEntity.ok(StorageResponse.success("Folder existence checked", exists));
        } catch (Exception e) {
            log.error("Error checking folder existence for user {}: {}", getUserEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to check folder existence: " + e.getMessage()));
        }
    }

    /**
     * List all folders in the user's root directory
     */
    @Operation(description = "Api to list all folders in the user's root directory")
    @GetMapping("/folders")
    public ResponseEntity<StorageResponse> listUserFolders() {
        try {
            String userEmail = getUserEmail();
            String userRootPath = "users/" + userEmail + "/";

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
            log.error("Error listing user folders for user {}: {}", getUserEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to list user folders: " + e.getMessage()));
        }
    }
}
