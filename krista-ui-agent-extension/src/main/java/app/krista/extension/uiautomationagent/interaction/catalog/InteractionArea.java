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

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;

import app.krista.extension.uiautomationagent.events.WaitForEventRequests;
import app.krista.extension.uiautomationagent.config.ExtensionAttributes;
import app.krista.extension.uiautomationagent.config.ExtensionAttributesProvider;
import app.krista.extension.uiautomationagent.session.BrowserSessionManager;
import app.krista.extensions.uiagent.service.UiAgentSessionManager;
import app.krista.extension.uiautomationagent.file.FileHandlingService;
import app.krista.extension.uiautomationagent.interaction.KristaUiAgentExtension;
import app.krista.ksdk.files.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import app.krista.extension.executor.*;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.*;
import app.krista.model.base.*;
import app.krista.extension.uiautomationagent.factory.ExtensionResponseFactory;
import app.krista.extension.uiautomationagent.proto.*;

import app.krista.ksdk.telemetry.TelemetryMetrics;

import javax.inject.Inject;
import javax.inject.Named;

@Domain(id = "catEntryDomain_56e80896-0143-40dc-8f20-364815774684",
        name = "UI Automation Agent",
        ecosystemId = "catEntryEcosystem_d3b05047-07b0-4b06-95a3-9fb8f7f608d9",
        ecosystemName = "Krista",
        ecosystemVersion = "a23649e2-79f6-479b-bd34-67d69908dcc8")
public class InteractionArea {
    private static final Logger log = LoggerFactory.getLogger(InteractionArea.class);

    private final UiAgentSessionManager sessionManager;
    private final FileHandlingService fileHandlingService;
    private final FileRepository fileRepository;
    private final KristaUiAgentExtension extension;
    private final TelemetryMetrics telemetryMetrics;

    @Inject
    public InteractionArea(
            UiAgentSessionManager sessionManager,
            FileHandlingService fileHandlingService,
            FileRepository fileRepository,
            KristaUiAgentExtension extension,
            TelemetryMetrics telemetryMetrics) {
        this.sessionManager = sessionManager;
        this.fileHandlingService = fileHandlingService;
        this.fileRepository = fileRepository;
        this.extension = extension;
        this.telemetryMetrics = telemetryMetrics;
        log.info("InteractionArea initialized with SessionManager, FileHandlingService, and Extension");
    }


    @CatalogRequest(
            id = WaitForEventRequests.PROMPT_BASED_INTERACTION,
            name = "Single Command Mode",
            description = "Execute browser automation tasks using natural language commands. Each request is independent and completes immediately without maintaining state or session context. Ideal for quick, one-off automation tasks.",
            area = "Interaction",
            type = CatalogRequest.Type.WAIT_FOR_EVENT)
    @Field.Desc(name = "Response", type = "FreeForm", required = false)
    @Field.File(name = "Downloaded Files", multipleFileUpload = true, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.File(name = "Screenshots", multipleFileUpload = true, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public ExtensionResponse singleCommandMode(
            @Field(name = "eventName", type = "Text") String eventName,
            @Field(name = "eventData", type = "FreeForm") FreeForm eventData,
            @Field(name = "Prompt", type = "Paragraph", required = true, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {}) String prompt,
            @Field(name = "Validation Steps", type = "Paragraph", required = false, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {}) String validationSteps) throws Exception {

        // Check if this is an event response
        if (eventName != null && !eventName.isEmpty()) {
            log.info("Received event: {} for prompt-based interaction", eventName);
            return handlePromptBasedEvent(eventName, eventData);
        }

        // Initial request - For WAIT_FOR_EVENT type, just validate and return
        // The actual work is done in UiAutomationEventListenerHandler.initiatePromptBasedInteraction()

        // Validate prompt FIRST before trying to log it
        if (prompt == null || prompt.trim().isEmpty()) {
            return ExtensionResponseFactory.create(
                    new IllegalArgumentException("Prompt cannot be empty"),
                    "Prompt-based interaction requires a non-empty prompt",
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR
            );
        }

        log.info("Prompt-based interaction request: prompt={}",
                prompt.substring(0, Math.min(50, prompt.length())));

        // Return pending status - the event listener will handle the actual execution
        Map<String, Object> response = new HashMap<>();
        response.put("Status", "pending");
        response.put("Output", "Prompt-based interaction initiated. Waiting for UI Agent response...");

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("Response", response);

        return new ExtensionResponse(ExtensionResponse.Result.SUCCESS, responseData, null, null, null);
    }

    private ExtensionResponse handlePromptBasedEvent(String eventName, FreeForm eventData) {
        // Handle events for prompt-based interaction
        switch (eventName) {
            case "PromptCompleted":
                return handlePromptCompletedEvent(eventData);
            case "PromptError":
                return handlePromptErrorEvent(eventData);
            default:
                return ExtensionResponseFactory.create(
                        new IllegalArgumentException("Unknown event: " + eventName),
                        "Received unknown event type for prompt-based interaction",
                        ExtensionResponse.Error.ExceptionType.INPUT_ERROR
                );
        }
    }

    private ExtensionResponse handlePromptCompletedEvent(FreeForm eventData) {
        // Process the completion event
        Object result = eventData.containsKey("result") ?
                eventData.get("result") : "Prompt completed";

        Map<String, Object> response = new HashMap<>();
        response.put("Status", "success");
        response.put("Output", result);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("Response", response);

        return ExtensionResponseFactory.create(responseData);
    }

    private ExtensionResponse handlePromptErrorEvent(FreeForm eventData) {
        // Process the error event
        String errorMessage = eventData.containsKey("error") ?
                (String) eventData.get("error") : "Unknown error";

        return ExtensionResponseFactory.create(
                new RuntimeException(errorMessage),
                "Prompt execution error: " + errorMessage,
                ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR
        );
    }

    @CatalogRequest(
            id = "localDomainRequest_4d1d1df7-83f1-487a-98bb-66e31681ca1c",
            name = "Interactive Session Start",
            description = "Start a new browser automation session. Returns a Session ID that can be used to execute multiple steps while maintaining browser state and context.",
            area = "Interaction",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Text(name = "Session Id", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public ExtensionResponse interactiveSessionStart() {

        log.info("Starting new interactive automation session");
        long startTime = System.currentTimeMillis();
        String status = "success";

        try {
            // Build UserConfiguration
            UserConfiguration userConfig = extension.getEventListenerHandler().buildUserConfiguration();

            // Generate unique session ID
            String sessionId = UUID.randomUUID().toString();

            log.info("Sending SessionStartRequest: sessionId={}", sessionId);

            // Start session via ESB
            SessionStartResponse response = extension.getRawEsbClient().startSession(
                    sessionId,
                    userConfig,
                    extension.getInvoker().getWorkspaceId(),
                    "account-" + UUID.randomUUID().toString()
            );

            if (!response.getOk()) {
                log.error("Session start failed: {}", response.getMessage());
                return ExtensionResponseFactory.create(
                        new RuntimeException("Failed to start session: " + response.getMessage()),
                        "Session start failed",
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR
                );
            }

            String actualSessionId = response.getSessionId();
            log.info("✅ Session started successfully: actualSessionId={}", actualSessionId);

            // Track session
            sessionManager.createSession(
                    actualSessionId,
                    extension.getInvoker().getInvokerId(),
                    Map.of("createdBy", "interactiveSessionStart")
            );

            // Return session ID
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("Session Id", actualSessionId);

            return new ExtensionResponse(
                    ExtensionResponse.Result.SUCCESS,
                    responseData,
                    null, null, null
            );

        } catch (Exception e) {
            status = "error";
            log.error("Error starting interactive session", e);
            try {
                var attrs = app.krista.extension.uiautomationagent.service.TelemetryService.attrs();
                app.krista.extension.uiautomationagent.service.TelemetryService.putIfPresent(attrs, "error_type", "session_start");
                app.krista.extension.uiautomationagent.service.TelemetryService.putIfPresent(attrs, "error_message", e.getMessage());
                app.krista.extension.uiautomationagent.service.TelemetryService.getInstance().recordLog("uiagent",
                        io.opentelemetry.api.logs.Severity.ERROR, "Session start failed", attrs.build());
            } catch (Exception ignored) {
            }
            return ExtensionResponseFactory.create(
                    e,
                    "Failed to start interactive session",
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR
            );
        } finally {
            try {
                telemetryMetrics.incrementCounter("uiagent.session.starts", 1, app.krista.extension.uiautomationagent.service.InvokerContext.attrs("status", status));
                telemetryMetrics.recordDuration("uiagent.session.start.duration_ms", System.currentTimeMillis() - startTime, app.krista.extension.uiautomationagent.service.InvokerContext.attrs("status", status));
            } catch (Exception ignored) {
            }
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_3b7651c3-e71e-4bca-ae06-5ccc65b79ed0",
            name = "Execute Interactive Session Steps",
            description = "Start an interactive browser automation session that maintains state across multiple commands. The session keeps the browser open and remembers context between actions, allowing you to build complex workflows step-by-step. Perfect for multi-step automation tasks that require continuity.",
            area = "Interaction",
            type = CatalogRequest.Type.WAIT_FOR_EVENT)
    @Field.Desc(name = "Response", type = "FreeForm", required = false)
    @Field.Desc(name = "Session Id", type = "Text", required = false)
    @Field.File(name = "Downloaded Files", multipleFileUpload = true, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    @Field.File(name = "Screenshots", multipleFileUpload = true, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public ExtensionResponse executeInteractiveSessionSteps(
            @Field(name = "eventName", type = "Text") String eventName,
            @Field(name = "eventData", type = "FreeForm") FreeForm eventData,
            @Field.Text(name = "Session Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String sessionId,
            @Field(name = "Steps", type = "Paragraph", required = true, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {}) String steps,
            @Field(name = "Validation Steps", type = "Paragraph", required = false, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {}) String validationSteps) {

        if (eventName != null && !eventName.isEmpty()) {
            log.info("Received event: {} for session execution", eventName);

            // Status is nested inside Response FreeForm (see publishErrorEvent / happy-path payload).
            String status = null;
            String output = null;
            if (eventData != null) {
                Object responseField = eventData.get("Response");
                if (responseField instanceof FreeForm) {
                    FreeForm responseForm = (FreeForm) responseField;
                    Object statusValue = responseForm.get("Status");
                    if (statusValue != null) {
                        status = statusValue.toString();
                    }
                    Object outputValue = responseForm.get("Output");
                    if (outputValue != null) {
                        output = outputValue.toString();
                    }
                }
            }

            if (status != null && !"success".equalsIgnoreCase(status)) {
                String errorMessage = output != null ? output : "Session execution failed";
                log.error("❌ Interactive session execution failed: {}", errorMessage);
                return ExtensionResponseFactory.create(
                        new RuntimeException(errorMessage),
                        "Session execution failed: " + errorMessage,
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR
                );
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("status", "event_received");
            return new ExtensionResponse(ExtensionResponse.Result.SUCCESS, responseData, null, null, null);
        }

        log.info("Executing steps in session: sessionId={}", sessionId);

        if (sessionId == null || sessionId.trim().isEmpty()) {
            return ExtensionResponseFactory.create(
                    new IllegalArgumentException("Session ID is required"),
                    "Session ID cannot be empty. Please start a session first.",
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR
            );
        }

        if (steps == null || steps.trim().isEmpty()) {
            return ExtensionResponseFactory.create(
                    new IllegalArgumentException("Steps cannot be empty"),
                    "Automation steps cannot be empty",
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR
            );
        }

        try {
            // Combine steps and validation steps
            String combinedPrompt = steps;
            if (validationSteps != null && !validationSteps.trim().isEmpty()) {
                combinedPrompt = steps + "\n" + validationSteps;
                log.info("Added validation steps to prompt");
            }

            // Execute via ESB
            SessionExecuteResponse response = extension.getRawEsbClient().executeSession(
                    sessionId,
                    combinedPrompt,
                    extension.getInvoker().getWorkspaceId(),
                    "account-" + UUID.randomUUID().toString()
            );

            // Check for validation errors
            if ("VALIDATION_ERROR".equals(response.getStatus())) {
                String validationError = response.hasResult() && response.getResult().getValidationError() != null
                        ? response.getResult().getValidationError()
                        : (response.hasResult() && !response.getResult().getErrorMessage().isEmpty()
                        ? response.getResult().getErrorMessage()
                        : "Validation failed");

                log.error("❌ Validation failed: {}", validationError);

                return ExtensionResponseFactory.create(
                        new RuntimeException("Validation failed: " + validationError),
                        "Execution completed but validation failed",
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR
                );
            }

            if (!response.getOk()) {
                String errorMsg = response.hasResult() && !response.getResult().getErrorMessage().isEmpty()
                        ? response.getResult().getErrorMessage()
                        : "Unknown error";
                log.error("Session execution failed: {}", errorMsg);
                return ExtensionResponseFactory.create(
                        new RuntimeException("Failed to execute steps: " + errorMsg),
                        "Session execution failed",
                        ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR
                );
            }

            log.info("✅ Session execute successful: sessionId={}", sessionId);

            // Extract result data
            Object resultData = null;
            if (response.hasResult()) {
                String finalOutput = response.getResult().getFinalOutput();

                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode outputNode = mapper.readTree(finalOutput);

                    if (outputNode.has("data")) {
                        resultData = mapper.convertValue(outputNode.get("data"), new TypeReference<Map<String, Object>>() {
                        });
                    } else {
                        resultData = mapper.convertValue(outputNode, new TypeReference<Map<String, Object>>() {
                        });
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse finalOutput as JSON, returning as text");
                    resultData = finalOutput;
                }
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("Response", resultData != null ? resultData : "Execution completed");

            return new ExtensionResponse(
                    ExtensionResponse.Result.SUCCESS,
                    responseData,
                    null, null, null
            );

        } catch (Exception e) {
            log.error("Error executing session steps", e);
            return ExtensionResponseFactory.create(
                    e,
                    "Failed to execute session steps",
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR
            );
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_d705685e-b9e3-422b-9716-7ae91916e5f9",
            name = "Interactive Session End",
            description = "Close an active browser automation session. This will close the browser and clean up resources. Returns a summary of the session.",
            area = "Interaction",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Summary", type = "{ Session Id: Text, Message: Text, Status: Text }", required = false)
    @Field.File(name = "Profiling Data", multipleFileUpload = false, required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public ExtensionResponse interactiveSessionEnd(
            @Field.Text(name = "Session Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String sessionId) {

        log.info("Ending interactive automation session: sessionId={}", sessionId);
        long closeStartTime = System.currentTimeMillis();
        String closeStatus = "success";

        if (sessionId == null || sessionId.trim().isEmpty()) {
            return ExtensionResponseFactory.create(
                    new IllegalArgumentException("Session ID is required"),
                    "Session ID cannot be empty",
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR
            );
        }

        try {
            // Close via ESB
            SessionCloseResponse response = extension.getRawEsbClient().closeSession(
                    sessionId,
                    extension.getInvoker().getWorkspaceId(),
                    "account-" + UUID.randomUUID().toString()
            );

            if (response.getOk()) {
                log.info("✅ Session closed successfully: sessionId={}", sessionId);
            } else {
                log.warn("⚠️ Failed to close session: {}", response.getMessage());
            }

            // Handle profiling ZIP if present
            app.krista.model.base.File profilingFile = null;
            if (response.hasProfilingZipUrl()) {
                log.info("📊 Processing profiling ZIP from session close");

                try {
                    // Create DownloadedFile for profiling ZIP
                    app.krista.extension.uiautomationagent.proto.DownloadedFile profilingDownload =
                            app.krista.extension.uiautomationagent.proto.DownloadedFile.newBuilder()
                                    .setFilename("profiling.zip")
                                    .setDownloadUrl(response.getProfilingZipUrl())
                                    .build();

                    List<app.krista.extension.uiautomationagent.proto.DownloadedFile> profilingFiles =
                            java.util.Arrays.asList(profilingDownload);
                    List<app.krista.model.base.File> uploadedFiles =
                            extension.getEventListenerHandler().processDownloadedFiles(profilingFiles, sessionId);

                    if (!uploadedFiles.isEmpty()) {
                        profilingFile = uploadedFiles.get(0);
                        log.info("✅ Successfully processed profiling ZIP: {}", profilingFile.getFileName());
                    }
                } catch (Exception e) {
                    log.error("❌ Error processing profiling ZIP", e);
                }
            }

            // Clean up session tracking
            sessionManager.closeSession(sessionId);

            // Build summary - matching field descriptor: { Session Id: Text, Message: Text, Status: Text }
            Map<String, Object> responseData = new HashMap<>();
            Map<String, Object> summary = new HashMap<>();

            summary.put("Session Id", sessionId);
            summary.put("Message", response.getMessage());
            summary.put("Status", response.getOk() ? "closed" : "failed");

            responseData.put("Summary", summary);

            // Add profiling data file if available
            if (profilingFile != null) {
                Map<String, Object> fileMeta = new java.util.LinkedHashMap<>();
                fileMeta.put("type", "com.krista.net.model.fieldschemas.File");
                fileMeta.put("fileName", profilingFile.getFileName());
                fileMeta.put("mediaId", profilingFile.getMediaId());
                fileMeta.put("length", profilingFile.getLength());
                fileMeta.put("extension", profilingFile.getExtension());

                responseData.put("Profiling Data", fileMeta);
                log.info("📊 Added profiling data to response");
            }

            return new ExtensionResponse(
                    ExtensionResponse.Result.SUCCESS,
                    responseData,
                    null, null, null
            );

        } catch (Exception e) {
            closeStatus = "error";
            log.error("Error ending interactive session", e);
            try {
                var attrs = app.krista.extension.uiautomationagent.service.TelemetryService.attrs();
                app.krista.extension.uiautomationagent.service.TelemetryService.putIfPresent(attrs, "error_type", "session_close");
                app.krista.extension.uiautomationagent.service.TelemetryService.putIfPresent(attrs, "error_message", e.getMessage());
                app.krista.extension.uiautomationagent.service.TelemetryService.getInstance().recordLog("uiagent",
                        io.opentelemetry.api.logs.Severity.ERROR, "Session close failed", attrs.build());
            } catch (Exception ignored) {
            }

            try {
                sessionManager.closeSession(sessionId);
            } catch (Exception cleanupEx) {
                log.warn("Failed to clean up session tracking", cleanupEx);
            }

            return ExtensionResponseFactory.create(
                    e,
                    "Failed to end interactive session",
                    ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR
            );
        } finally {
            try {
                telemetryMetrics.incrementCounter("uiagent.session.closes", 1, app.krista.extension.uiautomationagent.service.InvokerContext.attrs("status", closeStatus));
                telemetryMetrics.recordDuration("uiagent.session.close.duration_ms", System.currentTimeMillis() - closeStartTime, app.krista.extension.uiautomationagent.service.InvokerContext.attrs("status", closeStatus));
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Normalize LLM name to lowercase format expected by service.
     */
    private String normalizeLLMName(String llmName) {
        if (llmName == null || llmName.isEmpty()) {
            return null;
        }
        return llmName.toLowerCase().replace(" ", "");
    }


      /* @SubCatalogRequest(name = "returnOrder", description = "Request Description", type = CatalogRequest.Type.CHANGE_SYSTEM)
    // input parameter will always be a map. User should define provide its name and keys and values which will
    // represent keyName inside the map and field type of the value.
    public ExtensionResponse returnOrder(
            @Field.Desc(name = "ExampleMap", type = "{yesOrNo : Boolean, orderId : Text}", required = true)
            Map<String, Object> ExampleMap) {
    }*/
}