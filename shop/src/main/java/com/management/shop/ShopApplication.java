package com.management.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EntityScan
public class ShopApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopApplication.class, args);
		//openBrowser("http://localhost:6062");
	}
	private static void openBrowser(String url) {
		try {
			String os = System.getProperty("os.name").toLowerCase();
			Runtime rt = Runtime.getRuntime();
			if (os.contains("win")) {
				rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
			} else if (os.contains("mac")) {
				rt.exec("open " + url);
			} else if (os.contains("nix") || os.contains("nux")) {
				String[] browsers = {"xdg-open", "google-chrome", "firefox"};
				for (String browser : browsers) {
					if (rt.exec(new String[]{"which", browser}).waitFor() == 0) {
						rt.exec(new String[]{browser, url});
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
