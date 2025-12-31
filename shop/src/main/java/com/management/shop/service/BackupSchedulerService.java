package com.management.shop.service;

import com.management.shop.entity.BillingEntity;
import com.management.shop.entity.UserSettingsEntity;
import com.management.shop.repository.UserSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.io.File;
import java.time.LocalDateTime;
import java.util.logging.Logger;

@Service
public class BackupSchedulerService {

    private static final Logger LOGGER = Logger.getLogger(BackupSchedulerService.class.getName());

    @Autowired
    private BackupService localBackupService;

    @Autowired
    private DriveService driveService;

    @Autowired
    UserSettingsRepository settingsRepo;

    @Value("${app.username}")
    private String appUsername;

    // Default setting
    private String backupFrequency = "WEEKLY";

    public void setFrequency(String frequency) {
        settingsRepo.updateAutoBackUpSettings(frequency, extractUsername(), LocalDateTime.now());
    }

    public String getFrequency() {
        UserSettingsEntity userSets= settingsRepo.findByUsername(extractUsername());

        return userSets.getAutoBackupFrequency();
    }

    public String extractUsername() {
        String username = "";

            username= SecurityContextHolder.getContext().getAuthentication().getName();


        // For testing purposes, you might uncomment the line below
        // username="junaid1";
        return username;
    }

    // Runs every day at 11 AM (Server Time)
    @Scheduled(cron = "0 0 11 * * ?")
    public void performScheduledBackup() {

        String backupFrequency=settingsRepo.findByUsername(appUsername).getAutoBackupFrequency();

        if ("OFF".equalsIgnoreCase(backupFrequency)) return;

        // Check if today matches the frequency requirement
        if (!shouldRunBackup()) return;

        try {
            LOGGER.info("Starting Scheduled Auto-Backup...");

            // 1. Create Local Dump
            String localPath = localBackupService.createBackup();
            File fileToUpload = new File(localPath);

            // 2. Upload to Drive (if linked)
            try {
                // This might fail if user is not linked, so we catch specifically
                driveService.uploadFile(fileToUpload);
                LOGGER.info("Scheduled Backup uploaded to Drive successfully.");
            } catch (Exception e) {
                LOGGER.warning("Auto-backup created locally, but upload failed (Drive not linked?): " + e.getMessage());
            }

            // 3. Cleanup local file (since we are cloud-focused)
            if (fileToUpload.exists()) fileToUpload.delete();

        } catch (Exception e) {
            LOGGER.severe("Scheduled Backup Failed: " + e.getMessage());
        }
    }

    private boolean shouldRunBackup() {
        java.time.DayOfWeek day = java.time.LocalDate.now().getDayOfWeek();
        int dayOfMonth = java.time.LocalDate.now().getDayOfMonth();

        switch (backupFrequency) {
            case "DAILY":
                return true;
            case "WEEKLY":
                // Run only on Sundays
                return day == java.time.DayOfWeek.SUNDAY;
            case "MONTHLY":
                // Run only on 1st of the month
                return dayOfMonth == 1;
            default:
                return false;
        }
    }
}