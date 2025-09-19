package org.zemo.omninet.storage.controller;

import io.minio.Result;
import io.minio.messages.Item;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
     * Normalize folder name for proper path handling
     */
    private String normalizeFolderName(String folderName) {
        if (folderName == null || folderName.isEmpty() || "root".equals(folderName)) {
            return "";
        }

        if (folderName.contains("..")) {
            throw new IllegalArgumentException("Folder name cannot contain '..' (directory traversal attempt)");
        }

        if (folderName.startsWith("/")) {
            folderName = folderName.substring(1);
        }

        if (folderName.endsWith("/")) {
            folderName = folderName.substring(0, folderName.length() - 1);
        }

        if (folderName.contains("\\") || folderName.contains("\0")) {
            throw new IllegalArgumentException("Folder name contains invalid characters");
        }

        return folderName.isEmpty() ? "" : folderName + "/";
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
     * Delete a file for the authenticated user
     */
    @Operation(description = "Api to delete a file")
    @DeleteMapping("/files")
    public ResponseEntity<StorageResponse> deleteFile(
            @RequestBody DeleteFileRequest request) {
        try {
            String userEmail = getUserEmail();
            String fileName = request.getFileName();
            storageService.deleteFile(userEmail, fileName);
            return ResponseEntity.ok(StorageResponse.success("File deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting file for user {}: {}", getUserEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to delete file: " + e.getMessage()));
        }
    }

    /**
     * Check if a file exists
     */
    @Operation(description = "Api to check if a file exists.")
    @GetMapping("/files/exists")
    public ResponseEntity<StorageResponse> checkFileExists(
            @RequestParam(name = "fileName") String fileName) {
        try {
            String userEmail = getUserEmail();
            String fullFilePath = "users/" + userEmail + "/" + fileName;
            boolean exists = storageService.fileExists(fullFilePath);

            return ResponseEntity.ok(StorageResponse.success("File existence checked", exists));
        } catch (Exception e) {
            log.error("Error checking file existence '{}' for user {}: {}", fileName, getUserEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to check file existence: " + e.getMessage()));
        }
    }

    /**
     * Check if a folder exists
     */
    @Operation(description = "Api to check if a folder exists.")
    @GetMapping("/folders/exists")
    public ResponseEntity<StorageResponse> checkFolderExists(
            @RequestParam(name = "folderName", required = false, defaultValue = "root") String folderName) {
        try {
            String userEmail = getUserEmail();
            String normalizedFolder = normalizeFolderName(folderName);
            String fullFolderPath = "users/" + userEmail + "/" + normalizedFolder;
            boolean exists = storageService.folderExists(fullFolderPath);

            return ResponseEntity.ok(StorageResponse.success("Folder existence checked", exists));
        } catch (Exception e) {
            log.error("Error checking folder existence '{}' for user {}: {}", folderName, getUserEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to check folder existence: " + e.getMessage()));
        }
    }

    /**
     * List all files and folders in a specific folder
     */
    @Operation(description = "Api to get all files and folders in a specific folder")
    @GetMapping("/contents")
    public ResponseEntity<StorageResponse> listFolderContents(
            @RequestParam(name = "folderName", required = false,defaultValue = "") String folderName) {
        try {
            System.out.println("Listing contents of folder: " + folderName);

            String userEmail = getUserEmail();

            if ("".equals(folderName)) {
                folderName = "";
            }

            String normalizedFolder = normalizeFolderName(folderName);
            String fullFolderPath = "users/" + userEmail + "/" + normalizedFolder;

            Iterable<Result<Item>> objects = storageService.listDirectChildren(fullFolderPath);
            List<FileInfoResponse> fileInfoList = new ArrayList<>();

            for (Result<Item> result : objects) {
                Item item = result.get();
                String objectName = item.objectName();

                if (objectName.equals(fullFolderPath)) {
                    continue;
                }

                String relativePath = objectName.substring(fullFolderPath.length());
                if (relativePath.isEmpty()) {
                    continue;
                }

                boolean isFolder = objectName.endsWith("/");
                String name;

                if (isFolder) {
                    name = relativePath.substring(0, relativePath.length() - 1);
                    if (name.contains("/")) {
                        continue;
                    }
                } else {
                    name = relativePath;
                    if (name.contains("/")) {
                        continue;
                    }
                }

                long size = isFolder ? 0 : item.size();
                LocalDateTime lastModified = item.lastModified() != null ?
                        item.lastModified().toLocalDateTime() :
                        LocalDateTime.now();

                String fullPathFromUserRoot = normalizedFolder + name + (isFolder ? "/" : "");
                if (fullPathFromUserRoot.startsWith("/")) {
                    fullPathFromUserRoot = fullPathFromUserRoot.substring(1);
                }

                fileInfoList.add(new FileInfoResponse(name, fullPathFromUserRoot, size, lastModified, isFolder));
            }

            return ResponseEntity.ok(StorageResponse.success("Folder contents retrieved successfully", fileInfoList));
        } catch (Exception e) {
            log.error("Error listing folder contents '{}' for user {}: {}", folderName, getUserEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(StorageResponse.error("Failed to list folder contents: " + e.getMessage()));
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
}
