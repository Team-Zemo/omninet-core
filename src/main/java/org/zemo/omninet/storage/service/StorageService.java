package org.zemo.omninet.storage.service;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zemo.omninet.security.service.UserService;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class StorageService {

    private final MinioClient minioClient;
    private final UserService userService;

    @Value("${minio.bucket.name}")
    private String bucketName;

    public StorageService(MinioClient minioClient, UserService userService) {
        this.minioClient = minioClient;
        this.userService = userService;
    }

    @PostConstruct
    private void initialize() {
        try {
            log.info("Initializing StorageService with bucket: {}", bucketName);

            ensureBucketExists();

            if (!folderExists("system/")) {
                createFolder("system/");
            }

            createFolderForEveryUser();

            log.info("StorageService initialization completed successfully");
        } catch (Exception e) {
            log.error("Error during StorageService initialization", e);
        }
    }

    /**
     * Ensures that the MinIO bucket exists, creates it if it doesn't
     */
    private void ensureBucketExists() {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!bucketExists) {
                log.info("Bucket '{}' does not exist, creating it...", bucketName);
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("Successfully created bucket: {}", bucketName);
            } else {
                log.info("Bucket '{}' already exists", bucketName);
            }
        } catch (Exception e) {
            log.error("Error ensuring bucket exists: {}", e.getMessage());
            throw new RuntimeException("Failed to ensure bucket exists", e);
        }
    }

    private void createFolderForEveryUser() {
        try {
            String root = "users/";
            if (!folderExists(root)) {
                createFolder(root);
            }

            List<String> users = userService.getAllUserIds();

            for (String userId : users) {
                String userFolder = root + userId + "/";
                if (!folderExists(userFolder)) {
                    createFolder(userFolder);
                    log.info("Created folder for user: {}", userId);
                } else {
                    log.info("Folder already exists for user: {}", userId);
                }
            }
        } catch (Exception e) {
            log.error("Error creating folders for users", e);
        }
    }

    /**
     * Creates folder for a specific user in the MinIO bucket.
     *
     * @param userId     The ID of the user for whom to create folders.
     * @param folderName The name of the folder to create (e.g., "profile/", "documents/", "videos/new/").
     *                   Create a folder like "users/{userId}/{folderName}/".
     * @return true if the folder was created successfully, false if it already exists.
     */
    public boolean createUserFolder(String userId, String folderName) {
        try {
            if (userId == null || userId.isEmpty()) {
                throw new IllegalArgumentException("User ID must not be null or empty");
            }
            if (folderName == null || folderName.isEmpty()) {
                throw new IllegalArgumentException("Folder name must not be null or empty");
            }

            String fullFolderName = "users/" + userId + "/" + folderName;
            return createFolder(fullFolderName);

        } catch (Exception e) {
            throw new RuntimeException("Error creating user folder in MinIO", e);
        }
    }

    /**
     * Deletes a user folder in the MinIO bucket.
     *
     * @param userId     The ID of the user whose folder to delete.
     * @param folderName The name of the folder to delete (e.g., "profile/", "documents/", "videos/new/").
     */
    public void deleteUserFolder(String userId, String folderName) {
        try {
            if (userId == null || userId.isEmpty()) {
                throw new IllegalArgumentException("User ID must not be null or empty");
            }
            if (folderName == null || folderName.isEmpty()) {
                throw new IllegalArgumentException("Folder name must not be null or empty");
            }
            if (!folderName.endsWith("/")) {
                folderName += "/";
            }
            String fullFolderName = "users/" + userId + "/" + folderName;
            deleteFolder(fullFolderName);

        } catch (Exception e) {
            throw new RuntimeException("Error deleting user folder in MinIO", e);
        }
    }

    /**
     * Creates a "folder" in a MinIO bucket.
     *
     * @param folderName The name of the folder to create (e.g., "users/new-user/").
     */
    public boolean createFolder(String folderName) {
        try {
            if (!folderName.endsWith("/")) {
                folderName += "/";
            }
            if (folderExists(folderName)) {
                System.out.println("Folder already exists: " + folderName);
                return false;
            }
            // Create a zero-byte object to represent the folder
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(folderName)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1) // Empty stream
                            .build());

            System.out.println("Successfully created folder: " + folderName + " in bucket: " + bucketName);
            return true;

        } catch (MinioException e) {
            System.err.println("Error occurred: " + e);
            System.err.println("HTTP trace: " + e.httpTrace());
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error creating folder in MinIO", e);
        }
    }


    /**
     * Deletes a "folder" in a MinIO bucket.
     *
     * @param folderName The name of the folder to delete (e.g., "users/userID/").
     */
    public void deleteFolder(String folderName) {
        try {
            if (!folderName.endsWith("/")) {
                folderName += "/";
            }
            if (!folderExists(folderName)) {
                System.out.println("Folder does not exist: " + folderName);
                return;
            }
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(folderName)
                            .build());

            System.out.println("Successfully deleted folder: " + folderName + " in bucket: " + bucketName);

        } catch (MinioException e) {
            System.err.println("Error occurred: " + e);
            System.err.println("HTTP trace: " + e.httpTrace());
        } catch (Exception e) {
            throw new RuntimeException("Error deleting folder in MinIO", e);
        }
    }

    /**
     * Checks if a folder exists in the MinIO bucket.
     *
     * @param folderName The name of the folder to check (e.g., "users/userID/").
     * @return true if the folder exists, false otherwise.
     */
    public boolean folderExists(String folderName) {
        try {
            if (!folderName.endsWith("/")) {
                folderName += "/";
            }

            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(folderName)
                            .build());
            return true;

        } catch (MinioException e) {
            // Handle specific MinIO exceptions
            if (e.getMessage().contains("NoSuchKey") || e.getMessage().contains("does not exist")) {
                log.debug("Folder does not exist: {}", folderName);
                return false;
            }
            log.error("Error checking folder existence for {}: {}", folderName, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error checking folder existence for {}: {}", folderName, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a file exists in the MinIO bucket.
     *
     * @param objectName The name of the object to check (e.g., "users/userID/Images/profile.jpg").
     * @return true if the file exists, false otherwise.
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            return true;
        } catch (MinioException e) {
            if (e.getMessage().contains("NoSuchKey") || e.getMessage().contains("does not exist")) {
                log.debug("File does not exist: {}", objectName);
                return false;
            }
            log.error("Error checking file existence for {}: {}", objectName, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error checking file existence for {}: {}", objectName, e.getMessage());
            return false;
        }
    }

    /**
     * Generates a presigned URL for uploading a file to MinIO.
     *
     * @param userId   The ID of the user who is uploading the file.
     * @param fileName The name of the file to upload. (e.g., "documents/report.pdf")
     * @return A presigned URL that can be used to upload the file.
     */
    public String generatePresignedUploadUrl(String userId, String fileName) {
        try {
            if (fileName == null || fileName.isEmpty()) {
                throw new IllegalArgumentException("File name must not be null or empty");
            }

            // Build the full file path
            String fullFilePath = "users/" + userId + "/" + fileName;

            if (fileExists(fullFilePath)) {
                log.warn("File already exists, will be overwritten: {}", fullFilePath);
            }

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(fullFilePath)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error generating MinIO upload URL", e);
        }
    }

    /**
     * Generates a presigned URL for downloading a file from MinIO.
     *
     * @param userId   The ID of the user who owns the file.
     * @param fileName The name of the file to download. (e.g., "documents/report.pdf")
     * @return A presigned URL that can be used to download the file.
     */
    public String generatePresignedDownloadUrl(String userId, String fileName) {
        try {
            if (fileName == null || fileName.isEmpty()) {
                throw new IllegalArgumentException("File name must not be null or empty");
            }

            String fullFilePath = "users/" + userId + "/" + fileName;

            if (!fileExists(fullFilePath)) {
                throw new IllegalArgumentException("File does not exist: " + fileName);
            }

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(fullFilePath)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error generating MinIO download URL", e);
        }
    }

    /**
     * Deletes a file from the MinIO bucket.
     *
     * @param userId   The ID of the user who owns the file.
     * @param fileName The name of the file to delete. (e.g., "documents/report.pdf")
     */
    public void deleteFile(String userId, String fileName) {
        try {
            if (fileName == null || fileName.isEmpty()) {
                throw new IllegalArgumentException("File name must not be null or empty");
            }

            // Build the full file path
            String fullFilePath = "users/" + userId + "/" + fileName;

            // Check if file exists before deleting
            if (!fileExists(fullFilePath)) {
                throw new IllegalArgumentException("File does not exist: " + fileName);
            }

            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullFilePath)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Error deleting file from MinIO", e);
        }
    }

    /**
     * Lists all objects in a specific folder with size in the MinIO bucket.
     *
     * @param folderName The name of the folder to list objects from (e.g., "users/userID/Images/").
     * @return A list of object names in the specified folder.
     */
    public Iterable<Result<Item>> listObjectsInFolder(String folderName) {
        try {
            if (folderName == null || folderName.isEmpty()) {
                throw new IllegalArgumentException("Folder name must not be null or empty");
            }
            if (!folderName.endsWith("/")) {
                folderName += "/";
            }
            if (!folderExists(folderName)) {
                log.warn("Folder does not exist: {}, returning empty list", folderName);
                return java.util.Collections.emptyList();
            }

            return minioClient.listObjects(
                    io.minio.ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(folderName)
                            .recursive(true)
                            .build()
            );

        } catch (Exception e) {
            throw new RuntimeException("Error listing objects in folder in MinIO", e);
        }
    }


    /**
     * Method to directly upload a file to a specific folder in the MinIO bucket.
     * This method is useful for uploading files without generating a presigned URL.
     * Primarily used for internal operations or administrative tasks.
     *
     * @param fileName The name of the file to upload (e.g., "system/logo.jpg")
     * @return true if the file was uploaded successfully, false if it already exists.
     */
    public boolean uploadFile(String fileName, byte[] fileData) {
        try {
            if (fileName == null || fileName.isEmpty()) {
                throw new IllegalArgumentException("File name must not be null or empty");
            }

            if (fileData == null || fileData.length == 0) {
                throw new IllegalArgumentException("File data must not be null or empty");
            }

            if (fileExists(fileName)) {
                log.warn("File already exists, will be overwritten: {}", fileName);
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(new ByteArrayInputStream(fileData), fileData.length, -1)
                            .build());

            log.info("Successfully uploaded file: {} to bucket: {}", fileName, bucketName);
            return true;

        } catch (Exception e) {
            throw new RuntimeException("Error uploading file to MinIO", e);
        }
    }

    /**
     * Method to directly download a file from a specific folder in the MinIO bucket.
     * This method is useful for downloading files without generating a presigned URL.
     * Primarily used for internal operations or administrative tasks.
     *
     * @param fileName The name of the file to download (e.g., "system/logo.jpg")
     * @return byte array of the file data if the file exists, null otherwise.
     */
    public byte[] downloadFile(String fileName) {
        try {
            if (fileName == null || fileName.isEmpty()) {
                throw new IllegalArgumentException("File name must not be null or empty");
            }

            if (!fileExists(fileName)) {
                log.warn("File does not exist: {}", fileName);
                return null;
            }

            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build());

            byte[] fileData = response.readAllBytes();
            response.close();
            return fileData;

        } catch (Exception e) {
            throw new RuntimeException("Error downloading file from MinIO", e);
        }
    }
}
