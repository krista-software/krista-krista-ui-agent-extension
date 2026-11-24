/*
 * Krista Ui Agent Extension Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extension.uiautomationagent.file;

import app.krista.extension.uiautomationagent.config.ExtensionAttributes;
import app.krista.extension.uiautomationagent.proto.DownloadedFile;
import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import app.krista.model.base.File;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.krista.ksdk.telemetry.TelemetryMetrics;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for handling file downloads from UI Agent and uploads to Krista FileRepository.
 *
 * This service:
 * 1. Downloads files from UI Agent service via HMAC-signed URLs
 * 2. Uploads files to Krista FileRepository (media server)
 * 3. Returns File objects for use in ExtensionResponse
 */
@Service
public class FileHandlingService {

    private static final Logger logger = LoggerFactory.getLogger(FileHandlingService.class);

    private static final int DOWNLOAD_TIMEOUT_SECONDS = 30;
    private static final int MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024; // 100 MB

    private final HttpClient httpClient;
    private final FileRepository fileRepository;
    private final ExtensionAttributes extensionAttributes;
    private final TelemetryMetrics telemetryMetrics;

    /**
     * Creates a new FileHandlingService with injected FileRepository and ExtensionAttributes.
     */
    @Inject
    public FileHandlingService(FileRepository fileRepository, ExtensionAttributes extensionAttributes, TelemetryMetrics telemetryMetrics) {
        this.fileRepository = fileRepository;
        this.extensionAttributes = extensionAttributes;
        this.telemetryMetrics = telemetryMetrics;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DOWNLOAD_TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        logger.info("FileHandlingService initialized with FileRepository and ExtensionAttributes");
    }
    
    /**
     * Processes a list of downloaded files from UI Agent.
     *
     * For each file:
     * 1. Downloads the file content from the HMAC-signed URL
     * 2. Uploads the content to Krista FileRepository
     * 3. Returns the File object
     *
     * @param downloadedFiles List of DownloadedFile objects from UI Agent (protobuf)
     * @return List of File objects ready for ExtensionResponse
     * @throws IOException If download or upload fails
     */
    public List<File> processFiles(List<DownloadedFile> downloadedFiles) throws IOException {

        if (downloadedFiles == null || downloadedFiles.isEmpty()) {
            logger.debug("No files to process");
            return new ArrayList<>();
        }

        logger.info("Processing {} files from UI Agent", downloadedFiles.size());

        List<File> result = new ArrayList<>();

        for (DownloadedFile downloadedFile : downloadedFiles) {
            long fileStartTime = System.currentTimeMillis();
            try {
                File file = processFile(downloadedFile);
                result.add(file);
                logger.info("Successfully processed file: {} (size: {} bytes)",
                        downloadedFile.getFilename(),
                        downloadedFile.getSizeBytes());
                try {
                    telemetryMetrics.incrementCounter("uiagent.files.processed", 1, app.krista.extension.uiautomationagent.service.InvokerContext.attrs("status", "success"));
                    telemetryMetrics.recordDuration("uiagent.files.download.duration_ms", System.currentTimeMillis() - fileStartTime, app.krista.extension.uiautomationagent.service.InvokerContext.attrs());
                    telemetryMetrics.incrementCounter("uiagent.files.bytes", downloadedFile.getSizeBytes(), app.krista.extension.uiautomationagent.service.InvokerContext.attrs());
                } catch (Exception ignored) {}
            } catch (Exception e) {
                logger.error("Failed to process file: {}", downloadedFile.getFilename(), e);
                try { telemetryMetrics.incrementCounter("uiagent.files.processed", 1, app.krista.extension.uiautomationagent.service.InvokerContext.attrs("status", "error")); } catch (Exception ignored) {}
                throw new IOException("Failed to process file: " + downloadedFile.getFilename(), e);
            }
        }

        logger.info("Successfully processed {} files", result.size());
        return result;
    }

    /**
     * Processes a single file: download from UI Agent, upload to FileRepository.
     *
     * @param downloadedFile File metadata from UI Agent
     * @return File object
     * @throws IOException If download or upload fails
     */
    private File processFile(DownloadedFile downloadedFile) throws IOException {
        // 1. Validate file metadata
        validateFile(downloadedFile);
        
        // 2. Download from UI Agent
        byte[] fileContent = downloadFromUrl(downloadedFile.getDownloadUrl());
        
        // 3. Validate downloaded content
        if (fileContent.length != downloadedFile.getSizeBytes()) {
            logger.warn("Downloaded file size ({} bytes) doesn't match expected size ({} bytes) for file: {}", 
                    fileContent.length, 
                    downloadedFile.getSizeBytes(), 
                    downloadedFile.getFilename());
        }
        
        // 4. Upload to FileRepository
        FileHandle handle = fileRepository.createNewFileByName(downloadedFile.getFilename());
        handle.setContent(new ByteArrayInputStream(fileContent));
        
        // 5. Get File object
        File file = handle.getFile();

        logger.debug("File uploaded to FileRepository: {}", downloadedFile.getFilename());

        return file;
    }
    
    /**
     * Downloads file content from a URL (HMAC-signed URL from UI Agent).
     * 
     * @param url HMAC-signed download URL
     * @return File content as byte array
     * @throws IOException If download fails
     */
    private byte[] downloadFromUrl(String url) throws IOException {
        logger.debug("Original URL from response: {}", url);

        // Construct full URL from base URL and path
        // Note: extensionAttributes.getUiAgentUrl() should include the scheme (http:// or https://)
        logger.info("🔍 DEBUG: downloadFromUrl - input url: {}", url);

        // Prepend /downloads if not present
        String path = url.startsWith("/downloads") ? url : "/downloads" + url;
        String baseUrl = extensionAttributes.getUiAgentUrl();
        String fullUrl = baseUrl + path;

        logger.info("🔍 DEBUG: Constructed full URL: {} (baseUrl: {}, path: {})", fullUrl, baseUrl, path);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .timeout(Duration.ofSeconds(DOWNLOAD_TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                throw new IOException("Failed to download file. HTTP status: " + response.statusCode());
            }

            byte[] content = response.body();
            logger.debug("Downloaded {} bytes", content.length);

            return content;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
    }
    
    /**
     * Validates file metadata before processing.
     * 
     * @param downloadedFile File metadata to validate
     * @throws IllegalArgumentException If validation fails
     */
    private void validateFile(DownloadedFile downloadedFile) {
        if (downloadedFile.getFilename() == null || downloadedFile.getFilename().isEmpty()) {
            throw new IllegalArgumentException("File name is required");
        }
        
        if (downloadedFile.getDownloadUrl() == null || downloadedFile.getDownloadUrl().isEmpty()) {
            throw new IllegalArgumentException("Download URL is required for file: " + downloadedFile.getFilename());
        }
        
        if (downloadedFile.getSizeBytes() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    String.format("File too large: %s (%d bytes, max: %d bytes)", 
                            downloadedFile.getFilename(), 
                            downloadedFile.getSizeBytes(), 
                            MAX_FILE_SIZE_BYTES));
        }
    }
}

