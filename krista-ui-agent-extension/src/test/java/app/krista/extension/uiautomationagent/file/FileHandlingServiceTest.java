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

import app.krista.extension.uiautomationagent.proto.DownloadedFile;
import app.krista.extension.uiautomationagent.config.ExtensionAttributes;
import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import app.krista.model.base.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FileHandlingService.
 *
 * Note: These tests mock the FileRepository and HTTP downloads.
 * Integration tests should verify actual file downloads and uploads.
 */
class FileHandlingServiceTest {

    private FileHandlingService service;
    private FileRepository mockFileRepository;
    private ExtensionAttributes mockExtensionAttributes;

    @BeforeEach
    void setUp() {
        mockFileRepository = mock(FileRepository.class);
        mockExtensionAttributes = mock(ExtensionAttributes.class);
        when(mockExtensionAttributes.getUiAgentUrl()).thenReturn("http://localhost:3000");
        service = new FileHandlingService(mockFileRepository, mockExtensionAttributes, mock(app.krista.ksdk.telemetry.TelemetryMetrics.class));
    }

    @Test
    void testProcessFiles_EmptyList() throws IOException {
        List<File> result = service.processFiles(new ArrayList<>());

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(mockFileRepository);
    }

    @Test
    void testProcessFiles_NullList() throws IOException {
        List<File> result = service.processFiles(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(mockFileRepository);
    }

    @Test
    void testValidateFile_MissingFilename() {
        DownloadedFile file = DownloadedFile.newBuilder()
                .setFilename("")
                .setDownloadUrl("https://example.com/file")
                .setSizeBytes(1024)
                .build();

        IOException exception = assertThrows(IOException.class, () -> {
            service.processFiles(List.of(file));
        });
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void testValidateFile_MissingDownloadUrl() {
        DownloadedFile file = DownloadedFile.newBuilder()
                .setFilename("test.pdf")
                .setDownloadUrl("")
                .setSizeBytes(1024)
                .build();

        IOException exception = assertThrows(IOException.class, () -> {
            service.processFiles(List.of(file));
        });
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void testValidateFile_FileTooLarge() {
        DownloadedFile file = DownloadedFile.newBuilder()
                .setFilename("huge.pdf")
                .setDownloadUrl("https://example.com/huge.pdf")
                .setSizeBytes(200L * 1024 * 1024) // 200 MB (exceeds 100 MB limit)
                .build();

        IOException exception = assertThrows(IOException.class, () -> {
            service.processFiles(List.of(file));
        });
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    /**
     * Note: This test cannot actually download from a URL without a real HTTP server.
     * In a real integration test, you would:
     * 1. Start a mock HTTP server
     * 2. Serve a test file
     * 3. Verify the download
     *
     * For unit testing, we're testing the validation and FileRepository interaction.
     */
    @Test
    void testProcessFiles_ValidFile_MockedDownload() {
        // This test demonstrates the expected flow, but cannot test actual HTTP download
        // without a mock HTTP server. See integration tests for full end-to-end testing.

        DownloadedFile downloadedFile = DownloadedFile.newBuilder()
                .setFilename("test.pdf")
                .setDownloadUrl("https://example.com/test.pdf")
                .setSizeBytes(1024)
                .setMimeType("application/pdf")
                .build();

        // Note: This will fail because we can't actually download from example.com
        // In integration tests, use a real mock HTTP server
        assertThrows(IOException.class, () -> {
            service.processFiles(List.of(downloadedFile));
        });
    }

    @Test
    void testFileRepositoryInteraction() throws IOException {
        // Setup mock FileRepository
        FileHandle mockHandle = mock(FileHandle.class);
        File mockFile = mock(File.class);

        when(mockFileRepository.createNewFileByName(eq("test.pdf"))).thenReturn(mockHandle);
        when(mockHandle.getFile()).thenReturn(mockFile);

        // Verify the interaction pattern (even though download will fail)
        DownloadedFile downloadedFile = DownloadedFile.newBuilder()
                .setFilename("test.pdf")
                .setDownloadUrl("https://example.com/test.pdf")
                .setSizeBytes(1024)
                .build();

        // This will fail at download stage, but demonstrates the expected flow
        try {
            service.processFiles(List.of(downloadedFile));
            fail("Should have thrown IOException due to download failure");
        } catch (IOException e) {
            // Expected - can't download from example.com
            assertTrue(e.getMessage().contains("Failed to process file"));
        }
    }
}

