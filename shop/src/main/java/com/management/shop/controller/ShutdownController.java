package com.management.shop.controller;


import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShutdownController {
    @CrossOrigin(origins = {"*"})
    @PostMapping({"/shutdown"})
    public void shutdownApp() {
        (new Thread(() -> {
            try {
                Thread.sleep(500L);
                System.out.println("Shutting down application....");
                System.exit(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        })).start();
    }
}
