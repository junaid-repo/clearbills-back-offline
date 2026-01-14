package com.management.shop.service;
import com.management.shop.entity.UserSettingsEntity;
import com.management.shop.repository.UserSettingsRepository;
import com.management.shop.service.BackupService;
import com.management.shop.service.DriveService;
import java.io.File;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@Configuration
@EnableScheduling
public class BackupSchedulerService implements SchedulingConfigurer {
    private static final Logger LOGGER = Logger.getLogger(com.management.shop.service.BackupSchedulerService.class.getName());

    @Autowired
    private BackupService localBackupService;

    @Autowired
    private DriveService driveService;

    @Autowired
    UserSettingsRepository settingsRepo;

    @Value("${app.username}")
    private String appUsername;

    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(() -> performScheduledBackup(), triggerContext -> {
            String cronExpression;
            UserSettingsEntity settings = this.settingsRepo.findByUsername(this.appUsername);
            if (settings == null || "OFF".equalsIgnoreCase(settings.getAutoBackupFrequency())) {
                cronExpression = "0 0 * * * ?";
            } else {
                String dbTime = settings.getAutoBackupTiming();
                cronExpression = generateCronFromTime(dbTime);
            }
            CronTrigger trigger = new CronTrigger(cronExpression);
            Date nextExecDate = trigger.nextExecutionTime(triggerContext);
            return nextExecDate.toInstant();
        });
    }

    private String generateCronFromTime(String timeStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
            LocalTime time = LocalTime.parse(timeStr, formatter);
            return String.format("0 %d %d * * ?", new Object[] { Integer.valueOf(time.getMinute()), Integer.valueOf(time.getHour()) });
        } catch (Exception e) {
            LOGGER.warning("Could not parse time from DB: '" + timeStr + "'. Defaulting to 11:00 AM.");
            return "0 0 11 * * ?";
        }
    }

    public void performScheduledBackup() {
        UserSettingsEntity settings = this.settingsRepo.findByUsername(this.appUsername);
        if (settings == null)
            return;
        String backupFrequency = settings.getAutoBackupFrequency();
        if ("OFF".equalsIgnoreCase(backupFrequency))
            return;
        if (!shouldRunBackup(backupFrequency))
            return;
        try {
            LOGGER.info("Starting Scheduled Auto-Backup...");
            String localPath = this.localBackupService.createBackup();
            File fileToUpload = new File(localPath);
            try {
                this.driveService.uploadFile(fileToUpload);
                LOGGER.info("Scheduled Backup uploaded to Drive successfully.");
            } catch (Exception e) {
                LOGGER.warning("Upload failed (Drive not linked?): " + e.getMessage());
            }
            if (fileToUpload.exists())
                fileToUpload.delete();
        } catch (Exception e) {
            LOGGER.severe("Scheduled Backup Failed: " + e.getMessage());
        }
    }

    private boolean shouldRunBackup(String backupFrequency) {
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        int dayOfMonth = LocalDate.now().getDayOfMonth();
        switch (backupFrequency) {
            case "DAILY":
                return true;
            case "WEEKLY":
                return (day == DayOfWeek.SUNDAY);
            case "MONTHLY":
                return (dayOfMonth == 1);
        }
        return false;
    }

    public void setFrequency(String frequency, String timing) {
        this.settingsRepo.updateAutoBackUpSettings(frequency, extractUsername(), LocalDateTime.now(), timing);
    }

    public Map<String, Object> getFrequency() {
        UserSettingsEntity userSets = this.settingsRepo.findByUsername(extractUsername());
        Map<String, Object> retMap = new HashMap<>();
        if (userSets != null) {
            retMap.put("frequency", userSets.getAutoBackupFrequency());
            retMap.put("time", userSets.getAutoBackupTiming());
        }
        return retMap;
    }

    public String extractUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}