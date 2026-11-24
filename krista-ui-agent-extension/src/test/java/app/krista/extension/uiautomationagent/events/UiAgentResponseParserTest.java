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

package app.krista.extension.uiautomationagent.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UiAgentResponseParser.
 * Tests JSON parsing functionality for UI Agent responses.
 */
@DisplayName("UiAgentResponseParser Tests")
class UiAgentResponseParserTest {

    // ============================================================
    // SUCCESSFUL PARSING TESTS
    // ============================================================

    @Nested
    @DisplayName("Successful Parsing Tests")
    class SuccessfulParsingTests {

        @Test
        @DisplayName("Should parse simple JSON object")
        void parseJsonResponse_withSimpleJson_returnsMap() throws Exception {
            // Arrange
            String json = "{\"type\":\"success\",\"message\":\"Task completed\"}";

            // Act
            Map<String, Object> result = UiAgentResponseParser.parseJsonResponse(json);

            // Assert
            assertNotNull(result);
            assertEquals("success", result.get("type"));
            assertEquals("Task completed", result.get("message"));
        }

        @Test
        @DisplayName("Should parse JSON with nested objects")
        void parseJsonResponse_withNestedJson_returnsNestedMap() throws Exception {
            // Arrange
            String json = "{\"type\":\"success\",\"data\":{\"name\":\"John\",\"age\":30}}";

            // Act
            Map<String, Object> result = UiAgentResponseParser.parseJsonResponse(json);

            // Assert
            assertNotNull(result);
            assertEquals("success", result.get("type"));
            assertTrue(result.get("data") instanceof Map);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            assertEquals("John", data.get("name"));
            assertEquals(30.0, data.get("age")); // Jackson parses numbers as Double
        }

        @Test
        @DisplayName("Should parse JSON with different data types")
        void parseJsonResponse_withMixedTypes_returnsCorrectTypes() throws Exception {
            // Arrange
            String json = "{\"string\":\"text\",\"number\":42,\"boolean\":true,\"nullValue\":null}";

            // Act
            Map<String, Object> result = UiAgentResponseParser.parseJsonResponse(json);

            // Assert
            assertNotNull(result);
            assertEquals("text", result.get("string"));
            assertEquals(42.0, result.get("number")); // Jackson parses as Double
            assertEquals(true, result.get("boolean"));
            assertNull(result.get("nullValue"));
        }

        @Test
        @DisplayName("Should parse JSON with arrays")
        void parseJsonResponse_withArrays_returnsArrayAsString() throws Exception {
            // Arrange
            String json = "{\"type\":\"success\",\"items\":[\"item1\",\"item2\",\"item3\"]}";

            // Act
            Map<String, Object> result = UiAgentResponseParser.parseJsonResponse(json);

            // Assert
            assertNotNull(result);
            assertEquals("success", result.get("type"));
            // Arrays are converted to strings in the current implementation
            assertTrue(result.get("items") instanceof String);
        }

        @Test
        @DisplayName("Should parse complex nested JSON")
        void parseJsonResponse_withComplexNesting_returnsNestedStructure() throws Exception {
            // Arrange
            String json = "{\"result\":{\"user\":{\"name\":\"Alice\",\"profile\":{\"age\":25,\"city\":\"NYC\"}}}}";

            // Act
            Map<String, Object> result = UiAgentResponseParser.parseJsonResponse(json);

            // Assert
            assertNotNull(result);
            assertTrue(result.get("result") instanceof Map);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> resultData = (Map<String, Object>) result.get("result");
            assertTrue(resultData.get("user") instanceof Map);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) resultData.get("user");
            assertEquals("Alice", user.get("name"));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> profile = (Map<String, Object>) user.get("profile");
            assertEquals(25.0, profile.get("age"));
            assertEquals("NYC", profile.get("city"));
        }
    }

    // ============================================================
    // ERROR HANDLING TESTS
    // ============================================================

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        @DisplayName("Should return error for null or empty JSON")
        void parseJsonResponse_withNullOrEmpty_returnsErrorMap(String json) throws Exception {
            // Act
            Map<String, Object> result = UiAgentResponseParser.parseJsonResponse(json);

            // Assert
            assertNotNull(result);
            assertEquals("error", result.get("type"));
            assertEquals("Empty response", result.get("error"));
        }

        @Test
        @DisplayName("Should return error for invalid JSON")
        void parseJsonResponse_withInvalidJson_returnsErrorMap() throws Exception {
            // Arrange
            String invalidJson = "{invalid json}";

            // Act
            Map<String, Object> result = UiAgentResponseParser.parseJsonResponse(invalidJson);

            // Assert
            assertNotNull(result);
            assertEquals("error", result.get("type"));
            assertTrue(result.get("error").toString().contains("Invalid JSON"));
        }

        @Test
        @DisplayName("Should return error for malformed JSON")
        void parseJsonResponse_withMalformedJson_returnsErrorMap() throws Exception {
            // Arrange
            String malformedJson = "{\"key\":\"value\"";  // Missing closing brace

            // Act
            Map<String, Object> result = UiAgentResponseParser.parseJsonResponse(malformedJson);

            // Assert
            assertNotNull(result);
            assertEquals("error", result.get("type"));
            assertTrue(result.get("error").toString().contains("Invalid JSON"));
        }

        @Test
        @DisplayName("Should handle JSON array gracefully")
        void parseJsonResponse_withJsonArray_handlesGracefully() throws Exception {
            // Arrange
            String jsonArray = "[\"item1\",\"item2\"]";

            // Act
            Map<String, Object> result = UiAgentResponseParser.parseJsonResponse(jsonArray);

            // Assert - The parser will return an empty map for arrays
            // since nodeToMap() only processes object fields
            assertNotNull(result);
        }
    }

    // ============================================================
    // EDGE CASE TESTS
    // ============================================================

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should parse empty JSON object")
        void parseJsonResponse_withEmptyObject_returnsEmptyMap() throws Exception {
            // Arrange
            String json = "{}";

            // Act
            Map<String, Object> result = UiAgentResponseParser.parseJsonResponse(json);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should parse JSON with special characters")
        void parseJsonResponse_withSpecialCharacters_parsesCorrectly() throws Exception {
            // Arrange
            String json = "{\"message\":\"Hello\\nWorld\\t!\",\"path\":\"C:\\\\Users\\\\test\"}";

            // Act
            Map<String, Object> result = UiAgentResponseParser.parseJsonResponse(json);

            // Assert
            assertNotNull(result);
            assertTrue(result.get("message").toString().contains("Hello"));
            assertTrue(result.get("path").toString().contains("Users"));
        }

        @Test
        @DisplayName("Should parse JSON with Unicode characters")
        void parseJsonResponse_withUnicode_parsesCorrectly() throws Exception {
            // Arrange
            String json = "{\"emoji\":\"😀\",\"chinese\":\"你好\"}";

            // Act
            Map<String, Object> result = UiAgentResponseParser.parseJsonResponse(json);

            // Assert
            assertNotNull(result);
            assertEquals("😀", result.get("emoji"));
            assertEquals("你好", result.get("chinese"));
        }

        @Test
        @DisplayName("Should parse JSON with very long strings")
        void parseJsonResponse_withLongString_parsesCorrectly() throws Exception {
            // Arrange
            String longString = "a".repeat(10000);
            String json = "{\"longValue\":\"" + longString + "\"}";

            // Act
            Map<String, Object> result = UiAgentResponseParser.parseJsonResponse(json);

            // Assert
            assertNotNull(result);
            assertEquals(longString, result.get("longValue"));
        }

        @Test
        @DisplayName("Should parse JSON with numeric edge cases")
        void parseJsonResponse_withNumericEdgeCases_parsesCorrectly() throws Exception {
            // Arrange
            String json = "{\"zero\":0,\"negative\":-42,\"decimal\":3.14159,\"large\":999999999}";

            // Act
            Map<String, Object> result = UiAgentResponseParser.parseJsonResponse(json);

            // Assert
            assertNotNull(result);
            assertEquals(0.0, result.get("zero"));
            assertEquals(-42.0, result.get("negative"));
            assertEquals(3.14159, result.get("decimal"));
            assertEquals(999999999.0, result.get("large"));
        }
    }
}

