package com.management.shop.controller;


import com.management.shop.dto.SchedulerSettings;
import com.management.shop.dto.ShopSettings;
import com.management.shop.dto.UiSettings;
import com.management.shop.service.BackupSchedulerService;
import com.management.shop.service.SettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class SettingsController {

    @Autowired
    SettingsService serv;

    @Autowired
    private BackupSchedulerService schedulerService;

    @PutMapping("api/shop/settings/user/save/ui")
    ResponseEntity<Map<String, String>> saveUserUISettings(@RequestBody UiSettings request){

       String response=serv.saveUserUISettings(request);

       Map<String,String> responseMap=new HashMap<>();
        responseMap.put("status","success");
        responseMap.put("message","UI settings updated");
       return  ResponseEntity.status(HttpStatus.OK).body(responseMap);

    }
    @PutMapping("api/shop/settings/user/save/scheduler")
    ResponseEntity<Map<String, String>> saveUserSchedulerSettings(@RequestBody SchedulerSettings request){

        String response=serv.saveUserSchedulerSettings(request);
        Map<String,String> responseMap=new HashMap<>();
        responseMap.put("status","success");
        responseMap.put("message","UI settings updated");


        return  ResponseEntity.status(HttpStatus.OK).body(responseMap);

    }
    @PutMapping("api/shop/settings/user/save/billing")
    ResponseEntity<Map<String, String>> saveBillingSettings(@RequestBody Map<String, Object> request){

        String response=serv.saveBillingSettings(request);
        Map<String,String> responseMap=new HashMap<>();
        responseMap.put("status","success");
        responseMap.put("message","UI settings updated");


        return  ResponseEntity.status(HttpStatus.OK).body(responseMap);

    }
    @PutMapping("api/shop/settings/user/save/invoice")
    ResponseEntity<Map<String, String>> saveInvoice(@RequestBody Map<String, Object> request){

        String response=serv.saveInvoiceSetting(request);
        Map<String,String> responseMap=new HashMap<>();
        responseMap.put("status","success");
        responseMap.put("message","UI settings updated");


        return  ResponseEntity.status(HttpStatus.OK).body(responseMap);

    }
    @GetMapping("api/shop/get/user/settings")
    ResponseEntity<ShopSettings> getFullUserSettings(){

        ShopSettings response=serv.getFullUserSettings();


        return  ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @GetMapping("api/shop/settings/user/backup-schedule")
    public ResponseEntity<?> getBackupSchedule() {
        return ResponseEntity.ok(Map.of("frequency", schedulerService.getFrequency()));
    }

    @PostMapping("api/shop/settings/user/save/backup-schedule")
    public ResponseEntity<?> saveBackupSchedule(@RequestBody Map<String, String> payload) {
        String frequency = payload.get("frequency");
        if (frequency != null) {
            schedulerService.setFrequency(frequency);
            // TODO: Ideally, save 'frequency' to your DB here so it persists after restart
        }
        return ResponseEntity.ok("Saved");
    }
}
