package com.management.shop.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BackupService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public String createBackup() throws IOException {
        String backupDir = System.getProperty("user.home") + System.getProperty("user.home") + "MyBillingApp_Backups";
        Path path = Paths.get(backupDir, new String[0]);
        if (!Files.exists(path, new java.nio.file.LinkOption[0]))
            Files.createDirectories(path, (FileAttribute<?>[])new FileAttribute[0]);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "backup_" + timestamp + ".sql";
        Path fullPath = path.resolve(fileName);
        String safeSqlPath = fullPath.toAbsolutePath().toString().replace("\\", "/");
        String sql = String.format("SCRIPT TO '%s'", new Object[] { safeSqlPath });
        System.out.println("Executing Full Backup: " + sql);
        this.jdbcTemplate.execute(sql);
        return fullPath.toAbsolutePath().toString();
    }

    public void restoreFromLocalFile(File mainBackupFile) {
        String tempUserCsvPath = System.getProperty("java.io.tmpdir") + System.getProperty("java.io.tmpdir") + "current_user_data_" + File.separator + ".csv";
        tempUserCsvPath = tempUserCsvPath.replace("\\", "/");
        File tempUserFile = new File(tempUserCsvPath);
        this.jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            boolean userTableExists = checkTableExists("user_info");
            if (userTableExists) {
                System.out.println("Exporting user_info to CSV: " + tempUserCsvPath);
                this.jdbcTemplate.execute(String.format("CALL CSVWRITE('%s', 'SELECT * FROM user_info')", new Object[] { tempUserCsvPath }));
                if (!tempUserFile.exists() || tempUserFile.length() == 0L)
                    throw new RuntimeException("CSV Export failed. Aborting to protect data.");
            }
            System.out.println("Wiping database...");
            this.jdbcTemplate.execute("DROP ALL OBJECTS");
            System.out.println("Restoring main backup...");
            String mainRestoreSql = String.format("RUNSCRIPT FROM '%s'", new Object[] { mainBackupFile.getAbsolutePath().replace("\\", "/") });
            this.jdbcTemplate.execute(mainRestoreSql);
            if (userTableExists && tempUserFile.exists()) {
                System.out.println("Re-importing user_info from CSV...");
                this.jdbcTemplate.execute("DELETE FROM user_info");
                String importSql = String.format("INSERT INTO user_info SELECT * FROM CSVREAD('%s', null, null)", new Object[] { tempUserCsvPath });
                this.jdbcTemplate.execute(importSql);
            }
            this.jdbcTemplate.execute("CHECKPOINT");
        } catch (Exception e) {
            e.printStackTrace();
            if (tempUserFile.exists())
                try {
                    System.out.println("Attempting emergency CSV restore...");
                    if (checkTableExists("user_info")) {
                        this.jdbcTemplate.execute("DELETE FROM user_info");
                        this.jdbcTemplate.execute(String.format("INSERT INTO user_info SELECT * FROM CSVREAD('%s', null, null)", new Object[] { tempUserCsvPath }));
                        this.jdbcTemplate.execute("CHECKPOINT");
                    }
                } catch (Exception ex) {
                    System.err.println("Critical Failure: Could not recover user_info.");
                    ex.printStackTrace();
                }
            throw new RuntimeException("Restore failed: " + e.getMessage());
        } finally {
            this.jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
            if (tempUserFile.exists())
                tempUserFile.delete();
        }
    }

    public void restoreBackup(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("restore_upload_", ".sql");
        file.transferTo(tempFile);
        try {
            restoreFromLocalFile(tempFile);
        } finally {
            if (tempFile.exists())
                tempFile.delete();
        }
    }

    private boolean checkTableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_NAME = ?";
            Integer count = (Integer)this.jdbcTemplate.queryForObject(sql, Integer.class, new Object[] { tableName.toUpperCase() });
            return (count != null && count.intValue() > 0);
        } catch (Exception e) {
            return false;
        }
    }
}