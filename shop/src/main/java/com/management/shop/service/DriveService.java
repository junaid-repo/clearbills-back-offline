package com.management.shop.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Service
public class DriveService {

    private static final String APPLICATION_NAME = "Billing App Backup";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final java.io.File TOKENS_DIR = new java.io.File(System.getProperty("user.home"), ".billing_app_tokens");
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private Drive driveService;

    /**
     * @param allowBrowser If true, it will open the browser window for login.
     * If false, it will return NULL if user is not logged in.
     */
    private synchronized Drive getDriveService(boolean allowBrowser) throws IOException, GeneralSecurityException {
        // 1. If we already have an active service in memory, return it.
        if (driveService != null) {
            return driveService;
        }

        // 2. Load Client Secrets
        InputStream in = DriveService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // 3. Build the Flow (This connects to the token storage folder)
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(TOKENS_DIR))
                .setAccessType("offline")
                .build();

        // 4. CRITICAL FIX: Check if we have credentials BEFORE trying to authorize
        Credential credential = flow.loadCredential("user");

        if (credential == null && !allowBrowser) {
            // User is not logged in, and we are NOT allowed to open the browser.
            // Return null so listBackups knows to stay silent.
            return null;
        }

        // 5. If we are here, we either have a credential OR we are allowed to open the browser.

        // Define Browser (Linux Fix)
        AuthorizationCodeInstalledApp.Browser customBrowser = new AuthorizationCodeInstalledApp.Browser() {
            @Override
            public void browse(String url) throws IOException {
                String os = System.getProperty("os.name").toLowerCase();
                Runtime rt = Runtime.getRuntime();
                try {
                    if (os.contains("win")) rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
                    else if (os.contains("mac")) rt.exec("open " + url);
                    else if (os.contains("nix") || os.contains("nux")) rt.exec(new String[] {"xdg-open", url});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        // This line opens the browser IF credential was null
        credential = new AuthorizationCodeInstalledApp(flow, receiver, customBrowser).authorize("user");

        this.driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        return this.driveService;
    }

    // --- METHODS ---

    public String uploadFile(java.io.File localFile) throws Exception {
        // allowBrowser = true.
        // If user clicks "Backup", we WANT the browser to open if they aren't logged in.
        Drive service = getDriveService(true);

        File fileMetadata = new File();
        fileMetadata.setName(localFile.getName());
        FileContent mediaContent = new FileContent("application/octet-stream", localFile);

        File file = service.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();

        return file.getId();
    }

    public List<File> listBackups() throws Exception {
        // allowBrowser = false.
        // When page loads, we just want to see files. If not logged in, return empty list. DO NOT open browser.
        Drive service = getDriveService(false);

        if (service == null) {
            return Collections.emptyList();
        }

        try {
            String query = "trashed = false and name contains '.sql'";
            FileList result = service.files().list()
                    .setQ(query)
                    .setOrderBy("createdTime desc")
                    .setFields("nextPageToken, files(id, name, createdTime, size)")
                    .execute();
            return result.getFiles();
        } catch (Exception e) {
            // If the token is corrupt or expired, reset service and return empty
            this.driveService = null;
            return Collections.emptyList();
        }
    }

    public void downloadFile(String fileId, java.io.File destinationFile) throws Exception {
        // allowBrowser = true. If they try to restore, they need to be logged in.
        Drive service = getDriveService(true);
        try (OutputStream outputStream = new FileOutputStream(destinationFile)) {
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
        }
    }

    public void deleteFile(String fileId) throws Exception {
        Drive service = getDriveService(true);
        service.files().delete(fileId).execute();
    }

    public void unlink() {
        try {
            this.driveService = null;
            if (TOKENS_DIR.exists() && TOKENS_DIR.isDirectory()) {
                java.io.File[] files = TOKENS_DIR.listFiles();
                if (files != null) {
                    for (java.io.File file : files) {
                        file.delete();
                    }
                }
                TOKENS_DIR.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to unlink Drive account");
        }
    }
}