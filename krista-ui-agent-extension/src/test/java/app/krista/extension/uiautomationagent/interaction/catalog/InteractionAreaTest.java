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

package app.krista.extension.uiautomationagent.interaction.catalog;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.uiautomationagent.esb.UiAgentRawClient;
import app.krista.extension.uiautomationagent.events.UiAutomationEventListenerHandler;
import app.krista.extension.uiautomationagent.file.FileHandlingService;
import app.krista.extension.uiautomationagent.interaction.KristaUiAgentExtension;
import app.krista.extension.uiautomationagent.proto.*;
import app.krista.extensions.uiagent.service.UiAgentSessionManager;
import app.krista.extension.executor.Invoker;
import app.krista.ksdk.files.FileRepository;
import app.krista.model.base.FreeForm;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * Comprehensive unit tests for InteractionArea catalog requests.
 * Tests all 4 catalog request methods focusing on behavior and interactions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InteractionArea Catalog Request Tests")
class InteractionAreaTest {

    @Mock
    private KristaUiAgentExtension mockExtension;

    @Mock
    private UiAgentRawClient mockRawClient;

    @Mock
    private UiAgentSessionManager mockSessionManager;

    @Mock
    private FileHandlingService mockFileHandlingService;

    @Mock
    private FileRepository mockFileRepository;

    @Mock
    private UiAutomationEventListenerHandler mockEventHandler;

    @Mock
    private Invoker mockInvoker;

    @InjectMocks
    private InteractionArea interactionArea;

    // ============================================================
    // TEST DATA SETUP
    // ============================================================

    @BeforeEach
    void setUp() {
        // Setup only the essential mocks that are always used
        lenient().when(mockExtension.getRawEsbClient()).thenReturn(mockRawClient);
        lenient().when(mockExtension.getEventListenerHandler()).thenReturn(mockEventHandler);
        lenient().when(mockExtension.getInvoker()).thenReturn(mockInvoker);
        lenient().when(mockInvoker.getWorkspaceId()).thenReturn("test-workspace-123");
        lenient().when(mockInvoker.getInvokerId()).thenReturn("test-invoker-456");
    }

    private UserConfiguration createMockUserConfiguration() {
        return UserConfiguration.newBuilder()
                .setConfigVersion("1.0")
                .setLlm(LLMConfiguration.newBuilder()
                        .setPrimaryModel("gpt-4o")
                        .setFallbackModel("gpt-4o-mini")
                        .build())
                .build();
    }

    private SessionStartResponse createSuccessSessionStartResponse(String sessionId) {
        return SessionStartResponse.newBuilder()
                .setOk(true)
                .setSessionId(sessionId)
                .setMessage("Session started successfully")
                .build();
    }

    private SessionExecuteResponse createSuccessSessionExecuteResponse() {
        return SessionExecuteResponse.newBuilder()
                .setOk(true)
                .setResult(ExecutionResult.newBuilder()
                        .setFinalOutput("{\"data\": \"test result\"}")
                        .build())
                .build();
    }

    private SessionCloseResponse createSuccessSessionCloseResponse() {
        return SessionCloseResponse.newBuilder()
                .setOk(true)
                .setMessage("Session closed successfully")
                .build();
    }

    private FreeForm createFreeFormWithData(String key, String value) {
        FreeForm freeForm = new FreeForm();
        freeForm.put(key, "Text", value);
        return freeForm;
    }

    // ============================================================
    // SINGLE COMMAND MODE TESTS
    // ============================================================

    @Nested
    @DisplayName("Single Command Mode Tests")
    class SingleCommandModeTests {

        @Test
        @DisplayName("Should return pending status for valid prompt")
        void singleCommandMode_withValidPrompt_returnsPendingStatus() throws Exception {
            // Arrange
            String prompt = "Navigate to google.com and search for Krista";

            // Act
            ExtensionResponse response = interactionArea.singleCommandMode(
                    null, null, prompt, null);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        @DisplayName("Should reject blank or null prompt")
        void singleCommandMode_withBlankPrompt_returnsFailure(String prompt) throws Exception {
            // Act
            ExtensionResponse response = interactionArea.singleCommandMode(
                    null, null, prompt, null);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
            assertEquals(ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    response.getError().getExceptionType());
        }

        @Test
        @DisplayName("Should handle PromptCompleted event")
        void singleCommandMode_withPromptCompletedEvent_returnsSuccess() throws Exception {
            // Arrange
            String eventName = "PromptCompleted";
            FreeForm eventData = createFreeFormWithData("result", "Automation completed successfully");

            // Act
            ExtensionResponse response = interactionArea.singleCommandMode(
                    eventName, eventData, "dummy prompt", null);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());
        }

        @Test
        @DisplayName("Should handle PromptError event")
        void singleCommandMode_withPromptErrorEvent_returnsFailure() throws Exception {
            // Arrange
            String eventName = "PromptError";
            FreeForm eventData = createFreeFormWithData("error", "Browser automation failed");

            // Act
            ExtensionResponse response = interactionArea.singleCommandMode(
                    eventName, eventData, "dummy prompt", null);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
        }

        @Test
        @DisplayName("Should reject unknown event")
        void singleCommandMode_withUnknownEvent_returnsFailure() throws Exception {
            // Arrange
            String eventName = "UnknownEvent";
            FreeForm eventData = new FreeForm();

            // Act
            ExtensionResponse response = interactionArea.singleCommandMode(
                    eventName, eventData, "dummy prompt", null);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
        }
    }

    // ============================================================
    // INTERACTIVE SESSION START TESTS
    // ============================================================

    @Nested
    @DisplayName("Interactive Session Start Tests")
    class InteractiveSessionStartTests {

        @Test
        @DisplayName("Should start session successfully")
        void interactiveSessionStart_withValidConfig_startsSession() throws Exception {
            // Arrange
            String expectedSessionId = "test-session-123";
            UserConfiguration userConfig = createMockUserConfiguration();
            SessionStartResponse mockResponse = createSuccessSessionStartResponse(expectedSessionId);

            when(mockEventHandler.buildUserConfiguration()).thenReturn(userConfig);
            when(mockRawClient.startSession(anyString(), any(), anyString(), anyString()))
                    .thenReturn(mockResponse);

            // Act
            ExtensionResponse response = interactionArea.interactiveSessionStart();

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());

            // Verify session was tracked
            verify(mockSessionManager).createSession(
                    eq(expectedSessionId),
                    eq("test-invoker-456"),
                    anyMap());

            // Verify ESB client was called
            verify(mockRawClient).startSession(
                    anyString(),  // sessionId
                    eq(userConfig),
                    eq("test-workspace-123"),
                    anyString()); // accountId
        }

        @Test
        @DisplayName("Should handle session start failure from service")
        void interactiveSessionStart_withServiceFailure_returnsFailure() throws Exception {
            // Arrange
            UserConfiguration userConfig = createMockUserConfiguration();
            SessionStartResponse mockResponse = SessionStartResponse.newBuilder()
                    .setOk(false)
                    .setMessage("Max concurrent sessions reached")
                    .build();

            when(mockEventHandler.buildUserConfiguration()).thenReturn(userConfig);
            when(mockRawClient.startSession(anyString(), any(), anyString(), anyString()))
                    .thenReturn(mockResponse);

            // Act
            ExtensionResponse response = interactionArea.interactiveSessionStart();

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
        }

        @Test
        @DisplayName("Should handle exception during session start")
        void interactiveSessionStart_withException_returnsFailure() throws Exception {
            // Arrange
            when(mockEventHandler.buildUserConfiguration())
                    .thenThrow(new RuntimeException("Configuration error"));

            // Act
            ExtensionResponse response = interactionArea.interactiveSessionStart();

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
        }
    }

    // ============================================================
    // EXECUTE INTERACTIVE SESSION STEPS TESTS
    // ============================================================

    @Nested
    @DisplayName("Execute Interactive Session Steps Tests")
    class ExecuteInteractiveSessionStepsTests {

        @Test
        @DisplayName("Should execute steps successfully in existing session")
        void executeInteractiveSessionSteps_withValidParams_executesSuccessfully() throws Exception {
            // Arrange
            String sessionId = "test-session-123";
            String steps = "Click the login button";
            SessionExecuteResponse mockResponse = createSuccessSessionExecuteResponse();

            when(mockRawClient.executeSession(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(mockResponse);

            // Act
            ExtensionResponse response = interactionArea.executeInteractiveSessionSteps(
                    null, null, sessionId, steps, null);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());

            // Verify ESB client was called
            verify(mockRawClient).executeSession(
                    eq(sessionId),
                    eq(steps),
                    eq("test-workspace-123"),
                    anyString()); // accountId
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t"})
        @DisplayName("Should reject blank session ID")
        void executeInteractiveSessionSteps_withBlankSessionId_returnsFailure(String sessionId) throws Exception {
            // Act
            ExtensionResponse response = interactionArea.executeInteractiveSessionSteps(
                    null, null, sessionId, "some steps", null);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\n"})
        @DisplayName("Should reject blank steps")
        void executeInteractiveSessionSteps_withBlankSteps_returnsFailure(String steps) throws Exception {
            // Act
            ExtensionResponse response = interactionArea.executeInteractiveSessionSteps(
                    null, null, "session-123", steps, null);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
        }

        @Test
        @DisplayName("Should handle execution failure from service")
        void executeInteractiveSessionSteps_withServiceFailure_returnsFailure() throws Exception {
            // Arrange
            SessionExecuteResponse mockResponse = SessionExecuteResponse.newBuilder()
                    .setOk(false)
                    .setResult(ExecutionResult.newBuilder()
                            .setErrorMessage("Session not found")
                            .build())
                    .build();

            when(mockRawClient.executeSession(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(mockResponse);

            // Act
            ExtensionResponse response = interactionArea.executeInteractiveSessionSteps(
                    null, null, "session-123", "some steps", null);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
        }
    }

    // ============================================================
    // INTERACTIVE SESSION END TESTS
    // ============================================================

    @Nested
    @DisplayName("Interactive Session End Tests")
    class InteractiveSessionEndTests {

        @Test
        @DisplayName("Should end session successfully")
        void interactiveSessionEnd_withValidSessionId_closesSession() throws Exception {
            // Arrange
            String sessionId = "test-session-123";
            SessionCloseResponse mockResponse = createSuccessSessionCloseResponse();

            when(mockRawClient.closeSession(anyString(), anyString(), anyString()))
                    .thenReturn(mockResponse);

            // Act
            ExtensionResponse response = interactionArea.interactiveSessionEnd(sessionId);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());

            // Verify session was cleaned up
            verify(mockSessionManager).closeSession(sessionId);

            // Verify ESB client was called
            verify(mockRawClient).closeSession(
                    eq(sessionId),
                    eq("test-workspace-123"),
                    anyString()); // accountId
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t"})
        @DisplayName("Should reject blank session ID")
        void interactiveSessionEnd_withBlankSessionId_returnsFailure(String sessionId) throws Exception {
            // Act
            ExtensionResponse response = interactionArea.interactiveSessionEnd(sessionId);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());
        }

        @Test
        @DisplayName("Should handle session close failure gracefully")
        void interactiveSessionEnd_withServiceFailure_stillCleansUp() throws Exception {
            // Arrange
            String sessionId = "test-session-123";
            SessionCloseResponse mockResponse = SessionCloseResponse.newBuilder()
                    .setOk(false)
                    .setMessage("Session not found")
                    .build();

            when(mockRawClient.closeSession(anyString(), anyString(), anyString()))
                    .thenReturn(mockResponse);

            // Act
            ExtensionResponse response = interactionArea.interactiveSessionEnd(sessionId);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.SUCCESS, response.getResult());

            // Verify cleanup still happened
            verify(mockSessionManager).closeSession(sessionId);
        }

        @Test
        @DisplayName("Should handle exception during session end")
        void interactiveSessionEnd_withException_returnsFailure() throws Exception {
            // Arrange
            String sessionId = "test-session-123";
            when(mockRawClient.closeSession(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Network error"));

            // Act
            ExtensionResponse response = interactionArea.interactiveSessionEnd(sessionId);

            // Assert
            assertNotNull(response);
            assertEquals(ExtensionResponse.Result.FAILURE, response.getResult());
            assertNotNull(response.getError());

            // Verify cleanup was attempted
            verify(mockSessionManager).closeSession(sessionId);
        }
    }
}

