package com.management.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EntityScan // Ensure your entities are in this package or sub-packages, otherwise add (basePackages = "...")
public class ShopApplication {

    // 1. FIX: Declare the field here
    private final ServletWebServerApplicationContext webServerAppCtxt;

    // Constructor injection
    public ShopApplication(ServletWebServerApplicationContext webServerAppCtxt) {
        this.webServerAppCtxt = webServerAppCtxt;
    }

    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }

    @EventListener({ApplicationReadyEvent.class})
    public void launchBrowser() {
        // Now this works because the field is declared
        int port = this.webServerAppCtxt.getWebServer().getPort();
        String url = "http://localhost:" + port;

        System.out.println("App started. Opening browser: " + url);

        try {
            String os = System.getProperty("os.name").toLowerCase();
            Runtime rt = Runtime.getRuntime();

            if (os.contains("win")) {
                // Windows
                rt.exec(new String[] { "cmd", "/c", "start", "chrome", "--app=" + url });
            } else if (os.contains("mac")) {
                // Mac
                rt.exec(new String[] { "open", "-a", "Google Chrome", "--args", "--app=" + url });
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux (assuming google-chrome is in path)
                rt.exec(new String[] { "google-chrome", "--app=" + url });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}