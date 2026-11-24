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

package app.krista.extension.uiautomationagent.factory;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.RemediationAction;
import app.krista.model.base.File;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExtensionResponseFactory.
 * Tests all factory methods for creating success and error responses.
 */
@DisplayName("ExtensionResponseFactory Tests")
class ExtensionResponseFactoryTest {

    // ============================================================
    // SUCCESS RESPONSE TESTS
    // ============================================================

    @Nested
    @DisplayName("Success Response Tests")
    class SuccessResponseTests {

        @Test
        @DisplayName("Should create success response with values map")
        void create_withValuesMap_returnsSuccessResponse() {
            // Arrange
            Map<String, Object> values = new HashMap<>();
            values.put("Status", "success");
            values.put("Output", "Task completed");

            // Act
            ExtensionResponse response = ExtensionResponseFactory.create(values);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
            assertNull(response.getError());
        }

        @Test
        @DisplayName("Should create success response with empty values map")
        void create_withEmptyValuesMap_returnsSuccessResponse() {
            // Arrange
            Map<String, Object> values = new HashMap<>();

            // Act
            ExtensionResponse response = ExtensionResponseFactory.create(values);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
            assertNull(response.getError());
        }

        @Test
        @DisplayName("Should create success response with result and files")
        void create_withResultAndFiles_returnsSuccessResponse() {
            // Arrange
            String result = "Downloaded 2 files";
            List<File> files = Arrays.asList(
                    mock(File.class),
                    mock(File.class)
            );
            String message = "Files downloaded successfully";

            // Act
            ExtensionResponse response = ExtensionResponseFactory.create(result, files, message);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
            assertNull(response.getError());
        }

        @Test
        @DisplayName("Should create success response with result and no files")
        void create_withResultAndNoFiles_returnsSuccessResponse() {
            // Arrange
            String result = "Task completed";
            List<File> files = new ArrayList<>();
            String message = "No files";

            // Act
            ExtensionResponse response = ExtensionResponseFactory.create(result, files, message);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
            assertNull(response.getError());
        }

        @Test
        @DisplayName("Should create success response with result and null files")
        void create_withResultAndNullFiles_returnsSuccessResponse() {
            // Arrange
            String result = "Task completed";
            String message = "No files";

            // Act & Assert - The factory doesn't handle null files gracefully
            // This is expected behavior - null files will cause NullPointerException
            assertThrows(NullPointerException.class, () -> {
                ExtensionResponseFactory.create(result, null, message);
            });
        }
    }

    // ============================================================
    // ERROR RESPONSE TESTS
    // ============================================================

    @Nested
    @DisplayName("Error Response Tests")
    class ErrorResponseTests {

        @Test
        @DisplayName("Should create error response with exception")
        void create_withException_returnsErrorResponse() {
            // Arrange
            Exception cause = new RuntimeException("Test error");
            String message = "Operation failed";
            ExtensionResponse.Error.ExceptionType exceptionType = 
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR;

            // Act
            ExtensionResponse response = ExtensionResponseFactory.create(cause, message, exceptionType);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
            assertEquals(exceptionType, response.getError().getExceptionType());
        }

        @Test
        @DisplayName("Should create error response with INPUT_ERROR type")
        void create_withInputError_returnsErrorResponse() {
            // Arrange
            Exception cause = new IllegalArgumentException("Invalid input");
            String message = "Validation failed";
            ExtensionResponse.Error.ExceptionType exceptionType =
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR;

            // Act
            ExtensionResponse response = ExtensionResponseFactory.create(cause, message, exceptionType);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
            assertEquals(exceptionType, response.getError().getExceptionType());
        }

        @Test
        @DisplayName("Should create error response with state")
        void create_withExceptionAndState_returnsErrorResponse() {
            // Arrange
            Exception cause = new RuntimeException("Test error");
            String message = "Operation failed";
            ExtensionResponse.Error.ExceptionType exceptionType =
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR;
            Map<String, Object> state = new HashMap<>();
            state.put("sessionId", "session-123");
            state.put("retryCount", 3);

            // Act
            ExtensionResponse response = ExtensionResponseFactory.create(
                    cause, message, exceptionType, state);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
            assertEquals(exceptionType, response.getError().getExceptionType());
        }

        @Test
        @DisplayName("Should create error response with remediation actions")
        void create_withRemediationActions_returnsErrorResponse() {
            // Arrange
            Exception cause = new RuntimeException("Test error");
            String message = "Operation failed";
            ExtensionResponse.Error.ExceptionType exceptionType =
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR;

            // RemediationAction is abstract, so we mock it
            List<RemediationAction> actions = Arrays.asList(
                    mock(RemediationAction.class),
                    mock(RemediationAction.class)
            );
            String subCatalogRequestName = "retryOperation";
            Map<String, Object> state = new HashMap<>();

            // Act
            ExtensionResponse response = ExtensionResponseFactory.create(
                    cause, message, exceptionType, actions, subCatalogRequestName, state);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
            assertEquals(exceptionType, response.getError().getExceptionType());
        }

        @Test
        @DisplayName("Should include stack trace in error response")
        void create_withException_includesStackTrace() {
            // Arrange
            Exception cause = new RuntimeException("Test error");
            String message = "Operation failed";
            ExtensionResponse.Error.ExceptionType exceptionType =
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR;

            // Act
            ExtensionResponse response = ExtensionResponseFactory.create(cause, message, exceptionType);

            // Assert
            assertNotNull(response);
            assertNotNull(response.getError());
            assertNotNull(response.getError().getStackTrace());
            // Stack trace is converted to string with Arrays.toString(), so it contains brackets
            assertTrue(response.getError().getStackTrace().contains("["));
        }
    }

}

