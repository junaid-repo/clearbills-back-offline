package com.management.shop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class BackupService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public String createBackup() throws IOException {
        String backupDir = System.getProperty("user.home") + File.separator + "MyBillingApp_Backups";
        Path path = Paths.get(backupDir);

        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "backup_" + timestamp + ".sql";
        Path fullPath = path.resolve(fileName);

        String safeSqlPath = fullPath.toAbsolutePath().toString().replace("\\", "/");

        String sql = String.format("SCRIPT TO '%s'", safeSqlPath);
        System.out.println("Executing Full Backup: " + sql);
        jdbcTemplate.execute(sql);

        return fullPath.toAbsolutePath().toString();
    }

    public void restoreFromLocalFile(File mainBackupFile) {
        // Use a CSV file instead of SQL script for robustness
        String tempUserCsvPath = System.getProperty("java.io.tmpdir") + File.separator + "current_user_data_" + System.currentTimeMillis() + ".csv";
        // Fix path slashes for H2 commands
        tempUserCsvPath = tempUserCsvPath.replace("\\", "/");
        File tempUserFile = new File(tempUserCsvPath);

        // 1. DISABLE INTEGRITY CHECKS
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

        try {
            // ================================================================
            // STEP 1: EXPORT USER DATA TO CSV
            // ================================================================
            boolean userTableExists = checkTableExists("user_info");

            if (userTableExists) {
                System.out.println("Exporting user_info to CSV: " + tempUserCsvPath);
                // CALL CSVWRITE('path', 'SQL Query')
                jdbcTemplate.execute(String.format("CALL CSVWRITE('%s', 'SELECT * FROM user_info')", tempUserCsvPath));

                // Fail-safe check
                if (!tempUserFile.exists() || tempUserFile.length() == 0) {
                    throw new RuntimeException("CSV Export failed. Aborting to protect data.");
                }
            }

            // ================================================================
            // STEP 2: NUCLEAR WIPE
            // ================================================================
            System.out.println("Wiping database...");
            jdbcTemplate.execute("DROP ALL OBJECTS");

            // ================================================================
            // STEP 3: RESTORE MAIN BACKUP
            // ================================================================
            System.out.println("Restoring main backup...");
            String mainRestoreSql = String.format("RUNSCRIPT FROM '%s'", mainBackupFile.getAbsolutePath().replace("\\", "/"));
            jdbcTemplate.execute(mainRestoreSql);

            // ================================================================
            // STEP 4: IMPORT USER DATA FROM CSV
            // ================================================================
            if (userTableExists && tempUserFile.exists()) {
                System.out.println("Re-importing user_info from CSV...");

                // 1. Clear the old user data that came from the main backup
                jdbcTemplate.execute("DELETE FROM user_info");

                // 2. Insert the preserved data from CSV
                // 'null' means use default import options
                String importSql = String.format("INSERT INTO user_info SELECT * FROM CSVREAD('%s', null, null)", tempUserCsvPath);
                jdbcTemplate.execute(importSql);
            }

            jdbcTemplate.execute("CHECKPOINT");

        } catch (Exception e) {
            e.printStackTrace();

            // EMERGENCY RECOVERY (Try to push CSV back if main restore failed)
            if (tempUserFile.exists()) {
                try {
                    System.out.println("Attempting emergency CSV restore...");
                    // Only try if table exists (it might not if DROP ALL happened and RESTORE failed)
                    if (checkTableExists("user_info")) {
                        jdbcTemplate.execute("DELETE FROM user_info");
                        jdbcTemplate.execute(String.format("INSERT INTO user_info SELECT * FROM CSVREAD('%s', null, null)", tempUserCsvPath));
                        jdbcTemplate.execute("CHECKPOINT");
                    }
                } catch (Exception ex) {
                    System.err.println("Critical Failure: Could not recover user_info.");
                    ex.printStackTrace();
                }
            }
            throw new RuntimeException("Restore failed: " + e.getMessage());
        } finally {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");


            if (tempUserFile.exists()) {
                tempUserFile.delete();
            }
        }
    }

    public void restoreBackup(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("restore_upload_", ".sql");
        file.transferTo(tempFile);
        try {
            restoreFromLocalFile(tempFile);
        } finally {
            if (tempFile.exists()) tempFile.delete();
        }
    }

    private boolean checkTableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName.toUpperCase());
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}