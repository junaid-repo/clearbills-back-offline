package com.management.shop.controller;

import com.google.api.services.drive.model.File;
import com.management.shop.service.BackupService;
import com.management.shop.service.DriveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cloud")
public class CloudBackupController {

    @Autowired
    private DriveService driveService;

    @Autowired
    private BackupService localBackupService; // Reusing your existing local backup service

    // 1. Upload to Drive
    @PostMapping("/backup")
    public ResponseEntity<String> uploadBackupToDrive() {
        try {
            // Step 1: Create a fresh local SQL dump
            String localFilePath = localBackupService.createBackup();
            java.io.File fileToUpload = new java.io.File(localFilePath);

            // Step 2: Upload to Drive
            String fileId = driveService.uploadFile(fileToUpload);

            // Step 3: Cleanup local file to save space
            if(fileToUpload.exists()) fileToUpload.delete();

            return ResponseEntity.ok("Backup uploaded successfully. ID: " + fileId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadBackup() {
        try {
            // Generates the SQL file
            String localFilePath = localBackupService.createBackup();
            java.io.File fileToUpload = new java.io.File(localFilePath);

            if (!fileToUpload.exists()) {
                throw new RuntimeException("Backup file was not created.");
            }

            Resource resource = new FileSystemResource(fileToUpload);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileToUpload.getName())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 2. Restore Local Backup
    @PostMapping("/restore")
    public ResponseEntity<String> restoreBackup(@RequestParam("file") MultipartFile file) {
        try {
            localBackupService.restoreBackup(file);
            return ResponseEntity.ok("Restore successful. Please refresh the app.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Restore failed: " + e.getMessage());
        }
    }

    // 2. List Backups for the UI
    // 2. List Backups for the UI (Updated to include Email)
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listDriveBackups() {
        try {
            // 1. Fetch the list of files
            List<File> files = driveService.listBackups();

            // 2. Fetch the connected email
            String email = driveService.getConnectedEmail();

            // 3. Wrap them in a map
            Map<String, Object> response = new HashMap<>();
            response.put("files", files);
            response.put("email", email);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // 3. Restore from Drive
    @PostMapping("/restore/{fileId}")
    public ResponseEntity<String> restoreFromDrive(@PathVariable String fileId) {
        try {
            // Step 1: Create a temp file to hold the download
            java.io.File tempFile = java.io.File.createTempFile("drive_restore_", ".sql");

            // Step 2: Download the file from Drive
            driveService.downloadFile(fileId, tempFile);

            // Step 3: Trigger the H2 Restore Logic
            // Note: You need to ensure your BackupService has a method that accepts a File object
            // If it currently only takes MultipartFile, add a helper method there.
            localBackupService.restoreFromLocalFile(tempFile);

            return ResponseEntity.ok("Restore successful");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Restore failed: " + e.getMessage());
        }
    }
    @DeleteMapping("/delete/{fileId}")
    public ResponseEntity<String> deleteDriveBackup(@PathVariable String fileId) {
        try {
            driveService.deleteFile(fileId);
            return ResponseEntity.ok("File deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Delete failed: " + e.getMessage());
        }
    }

    // --- NEW: Unlink Drive Account ---
    @PostMapping("/unlink")
    public ResponseEntity<String> unlinkDrive() {
        try {
            driveService.unlink();
            return ResponseEntity.ok("Drive account unlinked successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Unlink failed: " + e.getMessage());
        }
    }
}
