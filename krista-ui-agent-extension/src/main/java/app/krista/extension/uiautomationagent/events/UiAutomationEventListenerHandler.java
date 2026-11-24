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

import app.krista.extension.event.WaitForEventListener;
import app.krista.extension.executor.Invoker;
import app.krista.extension.uiautomationagent.esb.UiAgentClient;
import app.krista.extension.uiautomationagent.service.TelemetryService;
import app.krista.extension.uiautomationagent.esb.UiAgentRawClient;
import app.krista.extension.uiautomationagent.proto.*;
import app.krista.extension.uiautomationagent.config.ExtensionAttributes;
import app.krista.extension.uiautomationagent.config.ExtensionAttributesProvider;
import app.krista.extension.uiautomationagent.proto.*;
import app.krista.extension.uiautomationagent.session.BrowserSessionManager;
import app.krista.extension.uiautomationagent.file.FileHandlingService;
import app.krista.ksdk.context.ConversationContext;
import app.krista.extensions.util.EventHandler;
import app.krista.ksdk.events.EventPublisher;
import app.krista.model.base.File;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.JsonObject;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import app.krista.model.base.FreeForm;
import app.krista.extension.uiautomationagent.session.InteractiveSessionManager;
import app.krista.extension.uiautomationagent.session.InteractiveSession;
import app.krista.ksdk.telemetry.TelemetryMetrics;
import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


@Service
public class UiAutomationEventListenerHandler {
    private static final Logger log = LoggerFactory.getLogger(UiAutomationEventListenerHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Invoker invoker;
    private final BrowserSessionManager sessionManager;
    private final EventHandler eventHandler;
    private final EventPublisher eventPublisher;
    private final ConversationContext conversationContext;
    private final FileHandlingService fileHandlingService;
    private final TelemetryMetrics telemetryMetrics;
    private static final long DEFAULT_TIMEOUT_MS = 60000;

    // Session mapping: (conversationId, sessionKey) → serviceSessionId
    private final Map<SessionKey, String> sessionMapping = new ConcurrentHashMap<>();

    // Store uploaded files from last session execution
    private List<app.krista.model.base.File> lastSessionUploadedFiles = new ArrayList<>();

    /**
     * Session key for multiplexing sessions across conversations.
     * Combines conversationId and user-provided sessionKey to uniquely identify a session.
     */
    static class SessionKey {
        final String conversationId;
        final String sessionKey;

        SessionKey(String conversationId, String sessionKey) {
            this.conversationId = conversationId;
            this.sessionKey = sessionKey != null && !sessionKey.isEmpty() ? sessionKey : "default";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SessionKey)) return false;
            SessionKey that = (SessionKey) o;
            return Objects.equals(conversationId, that.conversationId) &&
                   Objects.equals(sessionKey, that.sessionKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(conversationId, sessionKey);
        }

        @Override
        public String toString() {
            return String.format("SessionKey{conv=%s, key=%s}", conversationId, sessionKey);
        }
    }

    // ESB client reference (injected from extension) - legacy
    private UiAgentClient esbClient;

    // Raw ESB client using Google Protobuf (new approach)
    private UiAgentRawClient rawEsbClient;

    @Inject
    public UiAutomationEventListenerHandler(@Named("self") Invoker invoker,
                                           EventHandler eventHandler,
                                           ConversationContext conversationContext,
                                           EventPublisher eventPublisher,
                                           FileHandlingService fileHandlingService,
                                           TelemetryMetrics telemetryMetrics) {
        this.invoker = invoker;
        this.eventPublisher = eventPublisher;
        this.sessionManager = BrowserSessionManager.getInstance();
        this.eventHandler = eventHandler;
        this.conversationContext = conversationContext;
        this.fileHandlingService = fileHandlingService;
        this.telemetryMetrics = telemetryMetrics;
    }

    /**
     * Set the ESB client for sending requests to UI Agent service (legacy).
     * Called by KristaUiAgentExtension after ESB client is initialized.
     */
    public void setEsbClient(UiAgentClient esbClient) {
        this.esbClient = esbClient;
        log.info("ESB client set in UiAutomationEventListenerHandler");
    }

    /**
     * Set the raw ESB client for sending requests to UI Agent service (Google Protobuf).
     * Called by KristaUiAgentExtension after raw ESB client is initialized.
     */
    public void setRawEsbClient(UiAgentRawClient rawEsbClient) {
        this.rawEsbClient = rawEsbClient;
        log.info("Raw ESB client set in UiAutomationEventListenerHandler");
    }

    public void onEventListenerRegistered(WaitForEventListener listener) {
        if (listener == null) {
            return;
        }

        if (listener.getRequestId().equals(WaitForEventRequests.PROMPT_BASED_INTERACTION)) {
            initiatePromptBasedInteraction(listener);
        } else if (listener.getRequestId().equals(WaitForEventRequests.INTERACTIVE_AUTOMATION)) {
            initiateInteractiveAutomation(listener);
        }
    }



    private Map<String, Object> handlePromptCompletedEvent(FreeForm eventData) {
        // Get the data from the event - looking for "result" key
        Object data = eventData.get("result");

        log.info("Received prompt completed event with data: {}", data);

        // Prepare the inner response map
        Map<String, Object> innerResponse = new HashMap<>();
        innerResponse.put("Status", "success");

        if (data != null) {
            innerResponse.put("Output", data);
            log.info("Adding response data to Output field");
        } else {
            innerResponse.put("Output", "");
            log.warn("Response data is null, setting empty Output");
        }

        // Wrap in Response key to match expected structure
        return Map.of("Response", innerResponse);
    }

    private void initiatePromptBasedInteraction(WaitForEventListener listener) {
        String prompt = extractTextField(listener.getRequestInputs(), "Prompt");
        String validationSteps = extractTextField(listener.getRequestInputs(), "Validation Steps");

        // Strip HTML tags from prompt and validation steps
        prompt = stripHtmlTags(prompt);
        validationSteps = stripHtmlTags(validationSteps);

        // Combine prompt and validation steps
        String combinedPrompt = prompt;
        if (validationSteps != null && !validationSteps.trim().isEmpty()) {
            // Auto-number validation steps if they're not already numbered
            String numberedValidationSteps = autoNumberValidationSteps(prompt, validationSteps);
            combinedPrompt = prompt + "\n" + numberedValidationSteps;
            log.info("Added validation steps to prompt: {}", numberedValidationSteps);
        }

        String conversationId = conversationContext.getExecution().getConversation().getConversationId();

        log.info("=== PROMPT-BASED INTERACTION ===");
        log.info("🚀 Executing one-off prompt (no session): conversationId={}, prompt={}",
                 conversationId, combinedPrompt.substring(0, Math.min(50, combinedPrompt.length())));

        long promptStartTime = System.currentTimeMillis();
        String promptStatus = "success";
        String requestId = UUID.randomUUID().toString();
        try {
            // Check if raw ESB client is available
            if (rawEsbClient == null) {
                log.error("❌ Raw ESB client not initialized");
                publishErrorEvent(listener, "Raw ESB client not initialized. Extension may not be loaded properly.");
                return;
            }

            // Build UserConfiguration for the prompt execution
            UserConfiguration userConfig = buildUserConfiguration();

            // Execute prompt using executePrompt API (NO SESSION CREATION)
            log.info("📝 Executing prompt via executePrompt API");
            PromptExecuteResponse response = rawEsbClient.executePrompt(
                combinedPrompt,
                userConfig,
                invoker.getWorkspaceId(),
                "account-" + requestId
            );

            // Check for validation errors
            if ("VALIDATION_ERROR".equals(response.getStatus())) {
                String validationError = response.hasResult() && response.getResult().getValidationError() != null
                        ? response.getResult().getValidationError()
                        : (response.hasResult() && !response.getResult().getErrorMessage().isEmpty()
                            ? response.getResult().getErrorMessage()
                            : "Validation failed");

                log.error("❌ Validation failed: {}", validationError);
                promptStatus = "validation_error";
                try { telemetryMetrics.incrementCounter("uiagent.prompt.validation.failures", 1, app.krista.extension.uiautomationagent.service.InvokerContext.attrs()); } catch (Exception ignored) {}
                throw new RuntimeException("Validation failed: " + validationError);
            }

            // Capture execution error without throwing — screenshots must be processed first
            // so they appear in the UI even when the automation fails.
            String executionError = null;
            if (!response.getOk()) {
                if (response.hasResult() && !response.getResult().getErrorMessage().isEmpty()) {
                    executionError = response.getResult().getErrorMessage();
                } else {
                    executionError = "Execution failed with status: " + response.getStatus();
                }
                log.error("❌ Execution failed (screenshots will still be processed): {}", executionError);
                promptStatus = "error";
            } else {
                log.info("✅ Prompt executed successfully");
            }

            // Parse the result (same logic as executeInSession)
            Object resultData = null;
            if (executionError == null && response.hasResult()) {
                log.info("🔍 DEBUG - Response has result. Validations count: {}", response.getResult().getValidationsCount());
                log.info("🔍 DEBUG - ValidationFailed flag: {}", response.getResult().getValidationFailed());
                log.info("🔍 DEBUG - Full ExecutionResult: {}", response.getResult());

                // Try to access validations list directly
                if (response.getResult().getValidationsCount() > 0) {
                    log.info("🔍 DEBUG - First validation: {}", response.getResult().getValidations(0));
                }

                // Check if there are validations in the response
                if (response.getResult().getValidationsCount() > 0) {
                    // For validation responses, convert protobuf ValidationResult to plain Maps for JSON serialization
                    List<Map<String, Object>> validationsList = new ArrayList<>();
                    for (var v : response.getResult().getValidationsList()) {
                        Map<String, Object> vm = new LinkedHashMap<>();
                        vm.put("Step", v.getStep());
                        vm.put("Validation Type", v.getValidationType());
                        // Strip leading "null " prefix from validation prompt (service-side JS null concatenation artifact)
                        String validationPrompt = v.getValidationPrompt();
                        if (validationPrompt != null && validationPrompt.startsWith("null ")) {
                            validationPrompt = validationPrompt.substring(5);
                        }
                        vm.put("Prompt", validationPrompt);
                        vm.put("Passed", v.getPassed());
                        if (!v.getExpected().isEmpty()) vm.put("Expected", v.getExpected());
                        if (!v.getActual().isEmpty()) vm.put("Actual", v.getActual());
                        if (!v.getErrorMessage().isEmpty()) vm.put("Error", v.getErrorMessage());
                        if (v.hasTimestamp()) {
                            vm.put("Timestamp", java.time.Instant.ofEpochSecond(v.getTimestamp().getSeconds())
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        }
                        validationsList.add(vm);
                    }
                    Map<String, Object> validationResult = new HashMap<>();
                    validationResult.put("validations", validationsList);
                    validationResult.put("validationFailed", response.getResult().getValidationFailed());

                    log.info("📋 Extracted {} validation results, validationFailed={}",
                        response.getResult().getValidationsCount(),
                        response.getResult().getValidationFailed());

                    resultData = validationResult;
                } else {
                    // No validations - check finalOutput for data extraction
                    String finalOutput = response.getResult().getFinalOutput();

                    // Parse finalOutput as JSON
                        try {
                            JsonNode outputNode = MAPPER.readTree(finalOutput);

                            // Check if the response has a "result" wrapper
                            JsonNode resultNode = outputNode.has("result") ? outputNode.get("result") : outputNode;

                            // Check if the execution had an error (type=error)
                            if (resultNode.has("type") && "error".equals(resultNode.get("type").asText())) {
                                // Extract error details from steps
                                StringBuilder errorMsg = new StringBuilder("Execution failed");
                                if (resultNode.has("steps")) {
                                    JsonNode steps = resultNode.get("steps");
                                    for (JsonNode step : steps) {
                                        if (step.has("status") && "error".equals(step.get("status").asText())) {
                                            if (step.has("error")) {
                                                String stepError = step.get("error").asText();
                                                int stepNum = step.get("step").asInt();
                                                String stepPrompt = step.has("prompt") ? step.get("prompt").asText() : "";

                                                errorMsg = new StringBuilder("Step ").append(stepNum).append(" failed: ").append(stepError);
                                                if (!stepPrompt.isEmpty()) {
                                                    errorMsg.append("\nStep prompt: ").append(stepPrompt);
                                                }

                                                log.error("❌ UI Automation Error - Step {}: {}", stepNum, stepError);
                                                log.error("   Step prompt: {}", stepPrompt);
                                                break;
                                            }
                                        }
                                    }
                                }
                                // Throw exception to propagate the error
                                throw new RuntimeException(errorMsg.toString());
                            }

                            // Check if this is a validation error (type=validation_error)
                            if (resultNode.has("type") && "validation_error".equals(resultNode.get("type").asText())) {
                                // Extract validation error details
                                String validationError = resultNode.has("validationError")
                                    ? resultNode.get("validationError").asText()
                                    : "Validation failed";

                                log.error("❌ Validation Error: {}", validationError);

                                // Extract and filter validations array (exclude screenshot field)
                                List<Map<String, Object>> validationsList = new java.util.ArrayList<>();
                                if (resultNode.has("validations")) {
                                    JsonNode validationsNode = resultNode.get("validations");
                                    if (validationsNode.isArray()) {
                                        for (JsonNode validation : validationsNode) {
                                            Map<String, Object> filteredValidation = new HashMap<>();
                                            // Include only the required fields, excluding screenshot
                                            if (validation.has("step")) {
                                                filteredValidation.put("step", validation.get("step").asInt());
                                            }
                                            if (validation.has("validation_type")) {
                                                filteredValidation.put("validation_type", validation.get("validation_type").asText());
                                            }
                                            if (validation.has("validation_prompt")) {
                                                filteredValidation.put("validation_prompt", validation.get("validation_prompt").asText());
                                            }
                                            if (validation.has("passed")) {
                                                filteredValidation.put("passed", validation.get("passed").asBoolean());
                                            }
                                            if (validation.has("expected")) {
                                                filteredValidation.put("expected", validation.get("expected").asText());
                                            }
                                            if (validation.has("actual")) {
                                                filteredValidation.put("actual", validation.get("actual").asText());
                                            }
                                            if (validation.has("error_message")) {
                                                filteredValidation.put("error_message", validation.get("error_message").asText());
                                            }
                                            if (validation.has("timestamp")) {
                                                filteredValidation.put("timestamp", validation.get("timestamp").asText());
                                            }
                                            validationsList.add(filteredValidation);
                                        }
                                    }
                                }

                                // Publish error event with validation details instead of throwing exception
                                log.error("❌ Publishing validation error event to client with validation details");

                                // Create Response field as FreeForm with Status, Output, and Validations
                                FreeForm responseFreeForm = new FreeForm();
                                responseFreeForm.put("Status", "Text", "error");
                                responseFreeForm.put("Output", "Text", validationError);
                                responseFreeForm.put("Validations", "[ Text ]", validationsList);

                                // Publish error event with Response containing all fields
                                Map<String, Object> eventData = new HashMap<>();
                                eventData.put("Response", responseFreeForm);
                                eventPublisher.publishEvent(eventData, listener.getTarget(), listener.getListenerId());

                                log.info("✅ Validation error event published to client with validation details");
                                return; // Exit early after publishing error event
                            }

                            // Check if this is a data extraction response (has "response" field)
                            if (resultNode.has("response")) {
                                // Extract just the response data for data extraction operations
                                resultData = MAPPER.convertValue(resultNode.get("response"),
                                    new TypeReference<Map<String, Object>>() {});
                                log.info("Extracted 'response' field from finalOutput");
                            } else {
                                // Check if there are any extracted values (fields that are not metadata)
                                // Metadata fields to exclude: status, error, message, request_id, session_id, etc.
                                Set<String> metadataFields = Set.of("status", "error", "message", "request_id", "session_id",
                                    "validations", "validation_error", "validation_passed");

                                Map<String, Object> extractedData = new LinkedHashMap<>();
                                resultNode.fields().forEachRemaining(entry -> {
                                    String fieldName = entry.getKey();
                                    if (!metadataFields.contains(fieldName)) {
                                        // This is an extracted value, add it to the result
                                        extractedData.put(fieldName, MAPPER.convertValue(entry.getValue(), Object.class));
                                    }
                                });

                                if (!extractedData.isEmpty()) {
                                    // We have extracted data, return it
                                    resultData = extractedData;
                                    log.info("Found {} extracted values: {}", extractedData.size(), extractedData.keySet());
                                } else {
                                    // For action operations (navigate, click, etc.), don't return metadata
                                    resultData = null;
                                    log.info("Action operation - no data to return");
                                }
                            }
                    } catch (RuntimeException e) {
                        // Re-throw RuntimeException (our error handling)
                        throw e;
                    } catch (Exception e) {
                        // Only catch JSON parsing errors
                        log.error("Failed to parse final output JSON: {}", e.getMessage());
                        resultData = finalOutput; // Fallback to raw string
                    }
                }
            }

            // Process downloaded files (screenshots and downloads)
            List<DownloadedFile> allFiles = new ArrayList<>();

            // Add screenshots (convert Screenshot to DownloadedFile)
            List<Screenshot> screenshots = response.getResult().getScreenshotsList();
            if (screenshots != null && !screenshots.isEmpty()) {
                log.info("📸 Found {} screenshots to process", screenshots.size());
                for (Screenshot screenshot : screenshots) {
                    if (screenshot.getDownloadUrl() != null && !screenshot.getDownloadUrl().isEmpty()) {
                        DownloadedFile file = DownloadedFile.newBuilder()
                            .setFilename(screenshot.getFilename())
                            .setDownloadUrl(screenshot.getDownloadUrl())
                            .setSizeBytes(screenshot.getSizeBytes())
                            .setMimeType("image/png")
                            .build();
                        allFiles.add(file);
                    }
                }
            }

            // Add downloads
            List<DownloadedFile> downloads = response.getResult().getDownloadsList();
            if (downloads != null && !downloads.isEmpty()) {
                log.info("📁 Found {} downloads to process", downloads.size());
                allFiles.addAll(downloads);
            }

            // Process all files
            List<app.krista.model.base.File> uploadedFiles = new ArrayList<>();
            if (!allFiles.isEmpty()) {
                log.info("📤 Processing {} total files (screenshots + downloads) from prompt-based request", allFiles.size());
                // Use request_id as session_id for prompt-based requests (per design document)
                uploadedFiles = processDownloadedFiles(allFiles, response.getRequestId());
                log.info("✅ Successfully processed {} files", uploadedFiles.size());
            }

            // Create Response field as FreeForm with Status and Output
            FreeForm responseFreeForm = new FreeForm();

            if (executionError != null) {
                // Error case: show status=error with user-friendly message + screenshots still included below
                responseFreeForm.put("Status", "Text", "error");

                // Build detailed error message with which step/prompt failed
                String detailedError = buildDetailedErrorMessage(executionError, response);
                responseFreeForm.put("Output", "Text", detailedError);
                log.error("❌ Publishing error event with screenshots: {}", detailedError);
            } else {
                responseFreeForm.put("Status", "Text", "success");

                // Output can be FreeForm or Text depending on resultData
                if (resultData != null) {
                    log.info("📊 Result data type: {}, value: {}", resultData.getClass().getName(), resultData);

                    if (resultData instanceof Map || resultData instanceof FreeForm) {
                        log.info("📋 Adding result data as FreeForm (direct Map)");
                        responseFreeForm.put("Output", "FreeForm", resultData);
                    } else {
                        log.info("📝 Adding result data as text: {}", resultData.toString());
                        responseFreeForm.put("Output", "Text", resultData.toString());
                    }
                } else {
                    // For action operations with no data, don't include Output field at all
                    log.info("📝 No result data, not including Output field");
                }
            }

            // Add file metadata to Response FreeForm if files were uploaded
            // Create event payload with FreeForm Response (NO Session Id for prompt-based mode)
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("Response", responseFreeForm);

            // Add file fields separately to eventPayload (NOT inside Response FreeForm)
            if (!uploadedFiles.isEmpty()) {
                // Separate screenshots and downloads
                List<app.krista.model.base.File> screenshotFiles = new ArrayList<>();
                List<app.krista.model.base.File> downloadFiles = new ArrayList<>();

                int screenshotCount = screenshots != null ? screenshots.size() : 0;
                for (int i = 0; i < uploadedFiles.size(); i++) {
                    if (i < screenshotCount) {
                        screenshotFiles.add(uploadedFiles.get(i));
                    } else {
                        downloadFiles.add(uploadedFiles.get(i));
                    }
                }

                // Add screenshots using architect's file format - directly to eventPayload
                if (!screenshotFiles.isEmpty()) {
                    List<Map<String, Object>> screenshotFileMaps = new ArrayList<>();
                    for (app.krista.model.base.File file : screenshotFiles) {
                        Map<String, Object> fileMeta = new LinkedHashMap<>();
                        fileMeta.put("type", "com.krista.net.model.fieldschemas.File");
                        fileMeta.put("fileName", file.getFileName());
                        fileMeta.put("mediaId", file.getMediaId());
                        fileMeta.put("length", file.getLength());
                        fileMeta.put("extension", file.getExtension());
                        screenshotFileMaps.add(fileMeta);
                    }
                    eventPayload.put("Screenshots", screenshotFileMaps);
                    log.info("📸 Added {} screenshots to eventPayload", screenshotFileMaps.size());
                }

                // Add downloads using architect's file format - directly to eventPayload
                if (!downloadFiles.isEmpty()) {
                    List<Map<String, Object>> downloadFileMaps = new ArrayList<>();
                    for (app.krista.model.base.File file : downloadFiles) {
                        Map<String, Object> fileMeta = new LinkedHashMap<>();
                        fileMeta.put("type", "com.krista.net.model.fieldschemas.File");
                        fileMeta.put("fileName", file.getFileName());
                        fileMeta.put("mediaId", file.getMediaId());
                        fileMeta.put("length", file.getLength());
                        fileMeta.put("extension", file.getExtension());
                        downloadFileMaps.add(fileMeta);
                    }
                    eventPayload.put("Downloaded Files", downloadFileMaps);
                    log.info("📁 Added {} downloaded files to eventPayload", downloadFileMaps.size());
                }
            }

            log.info("📤 Event payload keys: {} (prompt-based mode)", eventPayload.keySet());

            // Publish event with error handling
            try {
                eventPublisher.publishEvent(eventPayload, listener.getTarget(), listener.getListenerId());
                log.info("✅ Event published successfully");
            } catch (Exception publishEx) {
                log.error("❌ Error publishing event: {}", publishEx.getMessage(), publishEx);
                throw publishEx;
            }

        } catch (Exception e) {
            promptStatus = "error";
            log.error("Error executing prompt via ESB: {}", e.getMessage(), e);
            String errorCategory = categorizeError(e);
            try {
                telemetryMetrics.incrementCounter("uiagent.prompt.errors", 1, app.krista.extension.uiautomationagent.service.InvokerContext.attrs("error_category", errorCategory));
                var attrs = TelemetryService.attrs();
                TelemetryService.putIfPresent(attrs, "error_type", "prompt_execution");
                TelemetryService.putIfPresent(attrs, "error_category", errorCategory);
                TelemetryService.putIfPresent(attrs, "error_message", e.getMessage());
                TelemetryService.putIfPresent(attrs, "conversation_id", conversationId);
                TelemetryService.putIfPresent(attrs, "request_id", requestId);
                TelemetryService.getInstance().recordLog("uiagent", io.opentelemetry.api.logs.Severity.ERROR,
                    "Prompt execution failed", attrs.build());
            } catch (Exception ignored) {}
            publishErrorEvent(listener, "Failed to execute UI automation: " + e.getMessage());
        } finally {
            try {
                long duration = System.currentTimeMillis() - promptStartTime;
                telemetryMetrics.incrementCounter("uiagent.prompt.executions", 1, app.krista.extension.uiautomationagent.service.InvokerContext.attrs("status", promptStatus));
                telemetryMetrics.recordDuration("uiagent.prompt.duration_ms", duration, app.krista.extension.uiautomationagent.service.InvokerContext.attrs("status", promptStatus));
                var attrs = TelemetryService.attrs();
                TelemetryService.putIfPresent(attrs, "status", promptStatus);
                TelemetryService.putIfPresent(attrs, "conversation_id", conversationId);
                TelemetryService.putIfPresent(attrs, "request_id", requestId);
                attrs.put("duration_ms", duration);
                TelemetryService.getInstance().recordLog("uiagent", io.opentelemetry.api.logs.Severity.INFO,
                    "Prompt execution completed", attrs.build());
            } catch (Exception ignored) {}
        }
    }

    private void initiateInteractiveAutomation(WaitForEventListener listener) {
        // Debug: Log all request inputs
        log.info("📋 DEBUG - Request inputs keys: {}", listener.getRequestInputs().keySet());

        // Extract inputs - using "Steps" field name from current catalog request
        String prompt = extractTextField(listener.getRequestInputs(), "Steps");
        if (prompt == null || prompt.isEmpty()) {
            // Fallback to "Prompt" for backward compatibility
            prompt = extractTextField(listener.getRequestInputs(), "Prompt");
        }

        // Strip HTML tags from prompt
        prompt = stripHtmlTags(prompt);

        // Extract optional session key - if not provided, a new session will be created
        String sessionKey = extractTextField(listener.getRequestInputs(), "Session Id");
        log.info("📋 DEBUG - Extracted sessionKey from 'Session Id' field: '{}'", sessionKey);

        // Extract optional end session flag
        Boolean endSession = extractBooleanField(listener.getRequestInputs(), "End Of Session After This");

        String conversationId = conversationContext.getExecution().getConversation().getConversationId();
        String executionId = conversationContext.getExecution().getExecutionId();

        log.info("🚀 Interactive automation: conversationId={}, sessionKey={}, prompt={}, endSession={}",
            conversationId, sessionKey, prompt.substring(0, Math.min(50, prompt.length())), endSession);

        long sessionExecStartTime = System.currentTimeMillis();
        String sessionExecStatus = "success";
        String finalSessionId = null;
        try {
            // Check if raw ESB client is available
            if (rawEsbClient == null) {
                log.error("❌ Raw ESB client not initialized");
                publishErrorEvent(listener, "Raw ESB client not initialized");
                return;
            }

            Object resultData = null;
            String status = "success";
            String message = null;
            String serviceSessionId = null;

            // If no session key provided, always create a new session
            if (sessionKey == null || sessionKey.trim().isEmpty()) {
                log.info("📝 No session key provided, creating new session");
                SessionStartResult startResult = startNewSession(prompt, conversationId, sessionKey);
                serviceSessionId = startResult.sessionId;
                finalSessionId = serviceSessionId;
                resultData = startResult.resultData;
                message = "Session started and command executed";

            } else {
                // Session key provided - check if it's a service session ID (with ext- prefix)
                // or a simple session key
                if (sessionKey.startsWith("ext-")) {
                    // User provided the full service session ID - search for it in the mapping values
                    log.info("🔍 Searching for service session ID: {}", sessionKey);
                    serviceSessionId = sessionKey; // Use it directly

                    // Verify it exists in our mapping
                    boolean found = sessionMapping.containsValue(serviceSessionId);
                    if (!found) {
                        log.warn("⚠️ Service session ID not found in mapping, will try to use it anyway");
                    }
                } else {
                    // User provided a simple session key - look it up in the mapping
                    SessionKey key = new SessionKey(conversationId, sessionKey);
                    serviceSessionId = sessionMapping.get(key);
                }

                // Track session id outside try so catch can include it in the error event payload
                if (serviceSessionId != null) {
                    finalSessionId = serviceSessionId;
                }

                if (serviceSessionId == null) {
                    // No existing session found, create new one
                    log.info("📝 No existing session found for key {}, starting new session", sessionKey);
                    SessionStartResult startResult = startNewSession(prompt, conversationId, sessionKey);
                    serviceSessionId = startResult.sessionId;
                    finalSessionId = serviceSessionId;
                    resultData = startResult.resultData;
                    message = "Session started and command executed";

                } else {
                    // Execute in existing session
                    log.info("♻️ Reusing existing session: {}", serviceSessionId);
                    try {
                        resultData = executeInSession(serviceSessionId, prompt);
                        message = "Command executed";

                    } catch (Exception e) {
                        if (e.getMessage() != null && e.getMessage().contains("Session not found")) {
                            // Service session expired, remove mapping and retry
                            log.warn("⏰ Session expired, creating new session: {}", serviceSessionId);
                            // Remove from mapping by value
                            final String expiredSessionId = serviceSessionId;
                            sessionMapping.entrySet().removeIf(entry -> entry.getValue().equals(expiredSessionId));
                            SessionStartResult startResult = startNewSession(prompt, conversationId, sessionKey);
                            serviceSessionId = startResult.sessionId;
                            finalSessionId = serviceSessionId;
                            resultData = startResult.resultData;
                            message = "Session recreated and command executed (previous session expired)";
                        } else {

                            throw e;
                        }
                    }
                }
            }

            // Close session if requested
            if (endSession != null && endSession) {
                log.info("🔚 Closing session as requested: {}", serviceSessionId);
                closeSession(serviceSessionId, conversationId, sessionKey);
                message = "Session closed";
            }

            // Create Response field as FreeForm with Status and Output
            // Catalog expects: { Status: Text, Output: FreeForm }
            // NOTE: Cannot nest FreeForm objects - must serialize complex data to JSON string
            FreeForm responseFreeForm = new FreeForm();
            responseFreeForm.put("Status", "Text", status);

            // Output can be FreeForm or Text depending on resultData
            if (resultData != null) {
                log.info("📊 Result data type: {}, value: {}", resultData.getClass().getName(), resultData);

                if (resultData instanceof Map) {
                    log.info("📋 Converting result data Map to FreeForm with recursive conversion");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = (Map<String, Object>) resultData;
                    FreeForm outputFreeForm = mapToFreeForm(resultMap);
                    responseFreeForm.put("Output", "FreeForm", outputFreeForm);
                } else if (resultData instanceof FreeForm) {
                    log.info("📋 Adding result data as FreeForm (already FreeForm)");
                    responseFreeForm.put("Output", "FreeForm", resultData);
                } else {
                    log.info("📝 Adding result data as text: {}", resultData.toString());
                    responseFreeForm.put("Output", "Text", resultData.toString());
                }
            } else {
                // For action operations with no data, don't include Output field at all
                log.info("📝 No result data, not including Output field");
            }

            // Create event payload with FreeForm Response and Session Id
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("Response", responseFreeForm);
            eventPayload.put("Session Id", serviceSessionId);

            // Add file fields separately to eventPayload (NOT inside Response FreeForm)
            if (!lastSessionUploadedFiles.isEmpty()) {
                // Separate screenshots and downloads
                List<app.krista.model.base.File> screenshotFiles = new ArrayList<>();
                List<app.krista.model.base.File> downloadFiles = new ArrayList<>();

                // For session-based, we need to determine which files are screenshots vs downloads
                // Screenshots have .png extension, downloads have other extensions
                for (app.krista.model.base.File file : lastSessionUploadedFiles) {
                    if (file.getExtension() != null && file.getExtension().equalsIgnoreCase("png")) {
                        screenshotFiles.add(file);
                    } else {
                        downloadFiles.add(file);
                    }
                }

                // Add screenshots using architect's file format - directly to eventPayload
                if (!screenshotFiles.isEmpty()) {
                    List<Map<String, Object>> screenshotFileMaps = new ArrayList<>();
                    for (app.krista.model.base.File file : screenshotFiles) {
                        Map<String, Object> fileMeta = new LinkedHashMap<>();
                        fileMeta.put("type", "com.krista.net.model.fieldschemas.File");
                        fileMeta.put("fileName", file.getFileName());
                        fileMeta.put("mediaId", file.getMediaId());
                        fileMeta.put("length", file.getLength());
                        fileMeta.put("extension", file.getExtension());
                        screenshotFileMaps.add(fileMeta);
                    }
                    eventPayload.put("Screenshots", screenshotFileMaps);
                    log.info("📸 Added {} screenshots to eventPayload", screenshotFileMaps.size());
                }

                // Add downloads using architect's file format - directly to eventPayload
                if (!downloadFiles.isEmpty()) {
                    List<Map<String, Object>> downloadFileMaps = new ArrayList<>();
                    for (app.krista.model.base.File file : downloadFiles) {
                        Map<String, Object> fileMeta = new LinkedHashMap<>();
                        fileMeta.put("type", "com.krista.net.model.fieldschemas.File");
                        fileMeta.put("fileName", file.getFileName());
                        fileMeta.put("mediaId", file.getMediaId());
                        fileMeta.put("length", file.getLength());
                        fileMeta.put("extension", file.getExtension());
                        downloadFileMaps.add(fileMeta);
                    }
                    eventPayload.put("Downloaded Files", downloadFileMaps);
                    log.info("📁 Added {} downloaded files to eventPayload", downloadFileMaps.size());
                }
            }

            log.info("📤 Event payload: Response={}, Session Id={}", responseFreeForm, serviceSessionId);

            log.info("✅ Publishing interactive automation result: sessionId={}, message={}", serviceSessionId, message);
            finalSessionId = serviceSessionId;

            eventPublisher.publishEvent(eventPayload, listener.getTarget(), listener.getListenerId());

        } catch (Exception e) {
            sessionExecStatus = "error";
            String errorCategory = categorizeError(e);
            log.error("❌ Error in interactive automation: {}", e.getMessage(), e);
            try {
                telemetryMetrics.incrementCounter("uiagent.session.execution.errors", 1, app.krista.extension.uiautomationagent.service.InvokerContext.attrs("error_category", errorCategory));
                var attrs = TelemetryService.attrs();
                TelemetryService.putIfPresent(attrs, "error_type", "session_execute");
                TelemetryService.putIfPresent(attrs, "error_category", errorCategory);
                TelemetryService.putIfPresent(attrs, "error_message", e.getMessage());
                TelemetryService.putIfPresent(attrs, "conversation_id", conversationId);
                TelemetryService.putIfPresent(attrs, "execution_id", executionId);
                TelemetryService.putIfPresent(attrs, "session_id", finalSessionId != null ? finalSessionId : sessionKey);
                TelemetryService.getInstance().recordLog("uiagent", io.opentelemetry.api.logs.Severity.ERROR,
                    "Interactive session execution failed", attrs.build());
            } catch (Exception ignored) {}
            publishErrorEvent(listener, "Failed to execute interactive automation: " + e.getMessage(), finalSessionId);
        } finally {
            try {
                long duration = System.currentTimeMillis() - sessionExecStartTime;
                telemetryMetrics.incrementCounter("uiagent.session.executions", 1, app.krista.extension.uiautomationagent.service.InvokerContext.attrs("status", sessionExecStatus));
                telemetryMetrics.recordDuration("uiagent.session.execution.duration_ms", duration, app.krista.extension.uiautomationagent.service.InvokerContext.attrs("status", sessionExecStatus));
                var attrs = TelemetryService.attrs();
                TelemetryService.putIfPresent(attrs, "status", sessionExecStatus);
                TelemetryService.putIfPresent(attrs, "conversation_id", conversationId);
                TelemetryService.putIfPresent(attrs, "execution_id", executionId);
                TelemetryService.putIfPresent(attrs, "session_id", finalSessionId);
                attrs.put("duration_ms", duration);
                TelemetryService.getInstance().recordLog("uiagent", io.opentelemetry.api.logs.Severity.INFO,
                    "Interactive session execution completed", attrs.build());
            } catch (Exception ignored) {}
        }
    }

    /**
     * Build UserConfiguration from extension attributes.
     * Used by both prompt-based and interactive automation.
     */
    public UserConfiguration buildUserConfiguration() {
        UserConfiguration.Builder userConfigBuilder = UserConfiguration.newBuilder();

        // Get extension attributes
        Map<String, Object> attributes = invoker.getAttributes();

        // Get LLM configuration
        String primaryLLM = getStringAttribute(attributes, ExtensionAttributes.PRIMARY_LLM);
        String primaryApiKey = getStringAttribute(attributes, ExtensionAttributes.PRIMARY_LLM_API_KEY);
        String fallbackLLM = getStringAttribute(attributes, ExtensionAttributes.FALLBACK_LLM);
        String fallbackApiKey = getStringAttribute(attributes, ExtensionAttributes.FALLBACK_LLM_API_KEY);

        // Build LLM configuration
        LLMConfiguration.Builder llmConfig = LLMConfiguration.newBuilder()
            .setPrimaryModel(normalizeLLMName(primaryLLM))
            .setFallbackModel(normalizeLLMName(fallbackLLM));

        // Add primary LLM provider with API key
        if (primaryApiKey != null && !primaryApiKey.isEmpty()) {
            String primaryProvider = getProviderName(primaryLLM);
            llmConfig.putProviders(primaryProvider, ProviderConfig.newBuilder()
                .setApiKey(primaryApiKey)
                .build());
        }

        // Add fallback LLM provider with API key
        if (fallbackApiKey != null && !fallbackApiKey.isEmpty()) {
            String fallbackProvider = getProviderName(fallbackLLM);
            llmConfig.putProviders(fallbackProvider, ProviderConfig.newBuilder()
                .setApiKey(fallbackApiKey)
                .build());
        }

        userConfigBuilder.setLlm(llmConfig.build());

        // Build browser configuration
        userConfigBuilder.setBrowser(BrowserConfiguration.newBuilder()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .setLocale("en-US")
            .setTimezone("America/Los_Angeles")
            .setScreenshotOnError(true)
            .setScreenshotFormat("png")
            .build());

        // Build session defaults
        userConfigBuilder.setSession(SessionDefaults.newBuilder()
            .setTimeoutMs(300000)
            .setMaxRetries(3)
            .setKeepAlive(true)  // Keep alive for interactive sessions
            .build());

        return userConfigBuilder.build();
    }

    /**
     * Start a new browser session via ESB and execute the initial prompt.
     * Returns both the sessionId and the result data from executing the prompt.
     */
    private SessionStartResult startNewSession(String prompt, String conversationId, String sessionKey) throws Exception {
        log.info("Starting new session: conversationId={}, sessionKey={}", conversationId, sessionKey);

        // Build UserConfiguration
        UserConfiguration userConfig = buildUserConfiguration();

        // Generate unique session ID
        String serviceSessionId = UUID.randomUUID().toString();

        log.info("Sending SessionStartRequest: sessionId={}, prompt={}",
            serviceSessionId, prompt.substring(0, Math.min(50, prompt.length())));

        // Start session via ESB
        SessionStartResponse response = rawEsbClient.startSession(
            serviceSessionId,
            userConfig,
            invoker.getWorkspaceId(),
            "account-" + UUID.randomUUID().toString() // TODO: Get from context
        );

        if (!response.getOk()) {
            throw new RuntimeException("Failed to start session: " + response.getMessage());
        }

        // Get the actual session ID from the service response (includes extension prefix)
        String actualSessionId = response.getSessionId();

        // Store mapping with the actual session ID from service
        SessionKey key = new SessionKey(conversationId, sessionKey);
        sessionMapping.put(key, actualSessionId);

        log.info("✅ Session started successfully: requestedId={}, actualSessionId={}", serviceSessionId, actualSessionId);

        // Execute the initial prompt in the new session using the actual session ID
        log.info("Executing initial prompt in new session");
        Object resultData;
        try {
            resultData = executeInSession(actualSessionId, prompt);
        } catch (Exception e) {
            log.warn("⚠️ Initial prompt failed in new session, closing to prevent leak: {}", actualSessionId);
            try {
                closeSession(actualSessionId, conversationId, sessionKey);
            } catch (Exception cleanupEx) {
                log.warn("⚠️ Cleanup close failed for leaked session {}: {}", actualSessionId, cleanupEx.getMessage());
            }
            sessionMapping.entrySet().removeIf(entry -> entry.getValue().equals(actualSessionId));
            throw e;
        }

        return new SessionStartResult(actualSessionId, resultData);
    }

    /**
     * Helper class to return both sessionId and result data from startNewSession
     */
    private static class SessionStartResult {
        final String sessionId;
        final Object resultData;

        SessionStartResult(String sessionId, Object resultData) {
            this.sessionId = sessionId;
            this.resultData = resultData;
        }
    }

    /**
     * Execute a prompt in an existing session via ESB.
     */
    private Object executeInSession(String serviceSessionId, String prompt) throws Exception {
        log.info("Executing in session: sessionId={}, prompt={}",
            serviceSessionId, prompt.substring(0, Math.min(50, prompt.length())));

        // Execute via ESB
        SessionExecuteResponse response = rawEsbClient.executeSession(
            serviceSessionId,
            prompt,
            invoker.getWorkspaceId(),
            "account-" + UUID.randomUUID().toString() // TODO: Get from context
        );

        // Capture error state without throwing yet — match single-prompt pattern so screenshots
        // and downloads still get processed and surfaced in the error event payload.
        String validationError = null;
        if ("VALIDATION_ERROR".equals(response.getStatus())) {
            validationError = response.hasResult() && response.getResult().getValidationError() != null
                    ? response.getResult().getValidationError()
                    : (response.hasResult() && !response.getResult().getErrorMessage().isEmpty()
                        ? response.getResult().getErrorMessage()
                        : "Validation failed");
            log.error("❌ Validation failed: {}", validationError);
        }

        String executionError = null;
        if (validationError == null && !response.getOk()) {
            String rawError = response.hasResult() && !response.getResult().getErrorMessage().isEmpty()
                ? response.getResult().getErrorMessage()
                : "Unknown error";
            executionError = response.hasResult()
                ? buildDetailedErrorMessage(rawError, response.getResult())
                : rawError;
            log.error("❌ Execution failed (screenshots will still be processed): {}", executionError);
        }

        if (validationError == null && executionError == null) {
            log.info("✅ Session execute successful: sessionId={}", serviceSessionId);
        }

        // DEBUG: Print entire response message
        log.info("🔍 DEBUG: Full SessionExecuteResponse:");
        log.info("🔍 DEBUG:   request_id: {}", response.getRequestId());
        log.info("🔍 DEBUG:   session_id: {}", response.getSessionId());
        log.info("🔍 DEBUG:   ok: {}", response.getOk());
        log.info("🔍 DEBUG:   status: {}", response.getStatus());
        if (response.hasResult()) {
            log.info("🔍 DEBUG:   result.final_output length: {}", response.getResult().getFinalOutput().length());
            log.info("🔍 DEBUG:   result.screenshots count: {}", response.getResult().getScreenshotsCount());
            log.info("🔍 DEBUG:   result.downloads count: {}", response.getResult().getDownloadsCount());
            log.info("🔍 DEBUG:   result.steps count: {}", response.getResult().getStepsCount());
            log.info("🔍 DEBUG:   Full protobuf message: {}", response.toString());
        }

        // Process downloaded files (screenshots and downloads)
        List<DownloadedFile> allFiles = new ArrayList<>();

        // Add screenshots (convert Screenshot to DownloadedFile)
        List<Screenshot> screenshots = response.getResult().getScreenshotsList();
        log.info("🔍 DEBUG: Screenshots list size: {}", screenshots != null ? screenshots.size() : "null");
        if (screenshots != null && !screenshots.isEmpty()) {
            log.info("📸 Found {} screenshots to process", screenshots.size());
            for (Screenshot screenshot : screenshots) {
                log.info("🔍 DEBUG: Screenshot - filename: {}, downloadUrl: {}, size: {}",
                    screenshot.getFilename(), screenshot.getDownloadUrl(), screenshot.getSizeBytes());
                if (screenshot.getDownloadUrl() != null && !screenshot.getDownloadUrl().isEmpty()) {
                    DownloadedFile file = DownloadedFile.newBuilder()
                        .setFilename(screenshot.getFilename())
                        .setDownloadUrl(screenshot.getDownloadUrl())
                        .setSizeBytes(screenshot.getSizeBytes())
                        .setMimeType("image/png")
                        .build();
                    allFiles.add(file);
                }
            }
        }

        // Add downloads
        List<DownloadedFile> downloads = response.getResult().getDownloadsList();
        log.info("🔍 DEBUG: Downloads list size: {}", downloads != null ? downloads.size() : "null");
        if (downloads != null && !downloads.isEmpty()) {
            log.info("📁 Found {} downloads to process", downloads.size());
            for (DownloadedFile download : downloads) {
                log.info("🔍 DEBUG: Download - filename: {}, downloadUrl: {}, size: {}",
                    download.getFilename(), download.getDownloadUrl(), download.getSizeBytes());
            }
            allFiles.addAll(downloads);
        }

        // Process all files
        List<app.krista.model.base.File> uploadedFiles = new ArrayList<>();
        if (!allFiles.isEmpty()) {
            log.info("📤 Processing {} total files (screenshots + downloads) from session-based request", allFiles.size());
            // Use session_id from response for session-based requests
            uploadedFiles = processDownloadedFiles(allFiles, response.getSessionId());
            log.info("✅ Successfully processed {} files", uploadedFiles.size());
        }

        // Store uploaded files for event payload (screenshots flow into the error event below)
        lastSessionUploadedFiles = uploadedFiles;

        // Now that files are preserved, surface validation or execution errors as exceptions —
        // the outer catch in initiateInteractiveAutomation publishes the error event and the
        // already-uploaded screenshots are picked up by publishErrorEvent.
        if (validationError != null) {
            throw new RuntimeException("Validation failed: " + validationError);
        }
        if (executionError != null) {
            throw new RuntimeException(executionError);
        }

        // Extract result data from finalOutput
        Object resultData = null;
        if (response.hasResult()) {
            String finalOutput = response.getResult().getFinalOutput();

            // Parse finalOutput as JSON and check for errors
            // For operations like navigate/click, finalOutput contains: {commandId, timestamp, type, steps}
            // For data extraction, finalOutput may contain: {response: {...}}
            try {
                JsonNode outputNode = MAPPER.readTree(finalOutput);

                // Check if the execution had an error (type=error)
                if (outputNode.has("type") && "error".equals(outputNode.get("type").asText())) {
                    // Extract error details from steps
                    StringBuilder errorMsg = new StringBuilder("Execution failed");
                    if (outputNode.has("steps")) {
                        JsonNode steps = outputNode.get("steps");
                        for (JsonNode step : steps) {
                            if (step.has("status") && "error".equals(step.get("status").asText())) {
                                if (step.has("error")) {
                                    String stepError = step.get("error").asText();
                                    int stepNum = step.get("step").asInt();
                                    String stepPrompt = step.has("prompt") ? step.get("prompt").asText() : "";

                                    errorMsg = new StringBuilder("Step ").append(stepNum).append(" failed: ").append(stepError);
                                    if (!stepPrompt.isEmpty()) {
                                        errorMsg.append("\nStep prompt: ").append(stepPrompt);
                                    }

                                    log.error("❌ UI Automation Error - Step {}: {}", stepNum, stepError);
                                    log.error("   Step prompt: {}", stepPrompt);
                                    break;
                                }
                            }
                        }
                    }
                    // Throw exception to propagate the error
                    throw new RuntimeException(errorMsg.toString());
                }

                // Check if this is a data extraction response (has "response" field)
                if (outputNode.has("response")) {
                    // Extract just the response data for data extraction operations
                    resultData = MAPPER.convertValue(outputNode.get("response"),
                        new TypeReference<Map<String, Object>>() {});
                    log.info("Extracted 'response' field from finalOutput");
                } else {
                    // Check if there are any extracted values (fields that are not metadata)
                    // Metadata fields to exclude: status, error, message, request_id, session_id, etc.
                    Set<String> metadataFields = Set.of("status", "error", "message", "request_id", "session_id",
                        "validations", "validation_error", "validation_passed", "type", "commandId", "timestamp", "steps");

                    Map<String, Object> extractedData = new LinkedHashMap<>();
                    outputNode.fields().forEachRemaining(entry -> {
                        String fieldName = entry.getKey();
                        if (!metadataFields.contains(fieldName)) {
                            // This is an extracted value, add it to the result
                            extractedData.put(fieldName, MAPPER.convertValue(entry.getValue(), Object.class));
                        }
                    });

                    if (!extractedData.isEmpty()) {
                        // We have extracted data, return it
                        resultData = extractedData;
                        log.info("Found {} extracted values: {}", extractedData.size(), extractedData.keySet());
                    } else {
                        // For action operations (navigate, click, etc.), don't return metadata
                        resultData = null;
                        log.info("Action operation - no data to return");
                    }
                }
            } catch (RuntimeException e) {
                // Re-throw RuntimeException (our error handling)
                throw e;
            } catch (Exception e) {
                // Only catch JSON parsing errors
                log.error("Failed to parse final output JSON: {}", e.getMessage());
                resultData = finalOutput; // Fallback to raw string
            }
        }

        return resultData;
    }

    /**
     * Close a browser session via ESB.
     */
    private void closeSession(String serviceSessionId, String conversationId, String sessionKey) {
        log.info("Closing session: sessionId={}", serviceSessionId);

        try {
            // Close via ESB
            SessionCloseResponse response = rawEsbClient.closeSession(
                serviceSessionId,
                invoker.getWorkspaceId(),
                "account-" + UUID.randomUUID().toString() // TODO: Get from context
            );

            if (response.getOk()) {
                log.info("✅ Session closed successfully: sessionId={}", serviceSessionId);

                // Handle profiling ZIP if present
                if (response.hasProfilingZipUrl()) {
                    log.info("📊 Processing profiling ZIP from session close");

                    try {
                        // Create DownloadedFile for profiling ZIP
                        DownloadedFile profilingFile = DownloadedFile.newBuilder()
                            .setFilename("profiling.zip")
                            .setDownloadUrl(response.getProfilingZipUrl())
                            .build();

                        List<DownloadedFile> profilingFiles = Arrays.asList(profilingFile);
                        List<app.krista.model.base.File> uploadedFiles = processDownloadedFiles(profilingFiles, serviceSessionId);

                        log.info("✅ Successfully processed profiling ZIP: {} files uploaded", uploadedFiles.size());
                    } catch (Exception e) {
                        log.error("❌ Error processing profiling ZIP", e);
                    }
                }
            } else {
                log.warn("⚠️ Failed to close session: {}", response.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ Error closing session: {}", serviceSessionId, e);
        } finally {
            // Always remove mapping
            SessionKey key = new SessionKey(conversationId, sessionKey);
            sessionMapping.remove(key);
            log.info("🗑️ Removed session mapping: {}", key);
        }
    }

    /* OLD PROCESS-BASED CODE - REMOVED
    // All the old process-based code has been replaced with ESB-based implementation above
    */

    private void publishErrorEvent(WaitForEventListener listener, String errorMessage) {
        publishErrorEvent(listener, errorMessage, null);
    }

    private void publishErrorEvent(WaitForEventListener listener, String errorMessage, String sessionId) {
        log.error("❌ Publishing error event to client: {}", errorMessage);

        // Create Response field as FreeForm matching catalog structure: { Status: Text, Output: Text }
        FreeForm responseFreeForm = new FreeForm();
        responseFreeForm.put("Status", "Text", "error");
        responseFreeForm.put("Output", "Text", errorMessage);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("Response", responseFreeForm);

        // Include Session Id so the flow's "Interactive Session End" step still has a value to close
        if (sessionId != null && !sessionId.isEmpty()) {
            eventPayload.put("Session Id", sessionId);
            log.info("📌 Added Session Id to error event: {}", sessionId);
        }

        // Include any screenshots/downloads captured before the failure — matches single-prompt behavior
        if (lastSessionUploadedFiles != null && !lastSessionUploadedFiles.isEmpty()) {
            List<Map<String, Object>> screenshotFileMaps = new ArrayList<>();
            List<Map<String, Object>> downloadFileMaps = new ArrayList<>();
            for (app.krista.model.base.File file : lastSessionUploadedFiles) {
                Map<String, Object> fileMeta = new LinkedHashMap<>();
                fileMeta.put("type", "com.krista.net.model.fieldschemas.File");
                fileMeta.put("fileName", file.getFileName());
                fileMeta.put("mediaId", file.getMediaId());
                fileMeta.put("length", file.getLength());
                fileMeta.put("extension", file.getExtension());
                if (file.getExtension() != null && file.getExtension().equalsIgnoreCase("png")) {
                    screenshotFileMaps.add(fileMeta);
                } else {
                    downloadFileMaps.add(fileMeta);
                }
            }
            if (!screenshotFileMaps.isEmpty()) {
                eventPayload.put("Screenshots", screenshotFileMaps);
                log.info("📸 Added {} screenshots to error event", screenshotFileMaps.size());
            }
            if (!downloadFileMaps.isEmpty()) {
                eventPayload.put("Downloaded Files", downloadFileMaps);
                log.info("📁 Added {} downloaded files to error event", downloadFileMaps.size());
            }
        }

        eventPublisher.publishEvent(eventPayload, listener.getTarget(), listener.getListenerId());

        log.info("✅ Error event published to client");
    }

    /**
     * Determine the Krista field type based on the Java object type.
     * This is used when adding fields to FreeForm objects.
     */
    private String determineFieldType(Object value) {
        if (value == null) {
            return "Text";
        }
        if (value instanceof Boolean) {
            return "Switch";
        }
        if (value instanceof Number) {
            return "Number";
        }
        if (value instanceof Map || value instanceof FreeForm) {
            return "FreeForm";
        }
        if (value instanceof List) {
            return "[ Text ]";  // Default to list of text
        }
        // Default to Text for strings and other types
        return "Text";
    }

    /**
     * Convert a Map to a FreeForm object using the 3-parameter put() method.
     * This ensures proper type mapping and avoids the UnsupportedOperationException
     * from FreeForm.putAll().
     */
    private FreeForm mapToFreeForm(Map<String, Object> map) {
        FreeForm freeForm = new FreeForm();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Recursively convert nested Maps to FreeForm
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                FreeForm nestedFreeForm = mapToFreeForm(nestedMap);
                freeForm.put(key, "FreeForm", nestedFreeForm);
                log.info("  Converted nested map for key '{}' to FreeForm", key);
            } else {
                String fieldType = determineFieldType(value);
                freeForm.put(key, fieldType, value);
                log.info("  Added field: key='{}', type='{}', value='{}'", key, fieldType, value);
            }
        }

        return freeForm;
    }

    private Boolean extractBooleanField(Map<String, Object> inputs, String fieldName) {
        if (inputs.containsKey(fieldName)) {
            Object value = inputs.get(fieldName);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        }
        return null;
    }

    private String extractTextField(Map<String, Object> inputs, String fieldName) {
        if (inputs.containsKey(fieldName)) {
            return (String) inputs.get(fieldName);
        }
        return "";
    }

    /**
     * Strip HTML tags from text (e.g., <code>, </code>, <br>, etc.)
     * This is needed because Krista UI may format text fields with HTML tags
     */
    private String categorizeError(Exception e) {
        if (e == null || e.getMessage() == null) return "unknown";
        String msg = e.getMessage().toLowerCase();
        if (msg.contains("timeout")) return "timeout";
        if (msg.contains("connection") || msg.contains("refused") || msg.contains("unreachable")) return "connection";
        if (msg.contains("validation")) return "validation";
        if (msg.contains("rate limit") || msg.contains("429")) return "rate_limit";
        if (msg.contains("not found") || msg.contains("session not found")) return "not_found";
        if (msg.contains("closed") || msg.contains("disconnected")) return "disconnected";
        return "unknown";
    }

    private String stripHtmlTags(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // Remove all HTML tags
        return text.replaceAll("<[^>]+>", "").trim();
    }

    /**
     * Auto-number validation steps if they're not already numbered.
     * This ensures the LLM parser treats them as separate steps.
     *
     * @param prompt The main prompt (to determine the next step number)
     * @param validationSteps The validation steps to number
     * @return Numbered validation steps
     */
    private String autoNumberValidationSteps(String prompt, String validationSteps) {
        if (validationSteps == null || validationSteps.trim().isEmpty()) {
            return validationSteps;
        }

        // Find the highest step number in the prompt
        int maxStepNumber = 0;
        String[] promptLines = prompt.split("\n");
        for (String line : promptLines) {
            // Match patterns like "1.", "2.", "3.", etc. at the start of the line
            if (line.trim().matches("^\\d+\\..*")) {
                try {
                    int stepNum = Integer.parseInt(line.trim().split("\\.")[0]);
                    maxStepNumber = Math.max(maxStepNumber, stepNum);
                } catch (NumberFormatException e) {
                    // Ignore parsing errors
                }
            }
        }

        // Split validation steps by newlines
        String[] validationLines = validationSteps.split("\n");
        StringBuilder numberedValidations = new StringBuilder();
        int nextStepNumber = maxStepNumber + 1;

        for (String line : validationLines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue; // Skip empty lines
            }

            // Check if line is already numbered
            if (trimmedLine.matches("^\\d+\\..*")) {
                // Already numbered, keep as-is
                numberedValidations.append(trimmedLine).append("\n");
            } else {
                // Not numbered, add number
                numberedValidations.append(nextStepNumber).append(". ").append(trimmedLine).append("\n");
                nextStepNumber++;
            }
        }

        return numberedValidations.toString().trim();
    }

    private String generateSessionId(WaitForEventListener listener) {
        // Generate a unique session ID based on the execution context
        return "ui-session-" + System.currentTimeMillis();
    }



    /**
     * Get string attribute from map, handling null and type conversion.
     */
    private String getStringAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value == null) {
            return null;
        }
        return value.toString().trim();
    }

    /**
     * Get provider name from LLM name.
     * Maps extension attribute LLM names to provider names for the providers map.
     */
    private String getProviderName(String llmName) {
        if (llmName == null || llmName.isEmpty()) {
            return "openai"; // Default
        }

        String normalized = llmName.toLowerCase().replaceAll("\\s+", "");

        if (normalized.contains("openai") || normalized.contains("gpt")) {
            return "openai";
        } else if (normalized.contains("claude") || normalized.contains("anthropic")) {
            return "anthropic";  // Provider name is "anthropic", not "claude"
        } else if (normalized.contains("gemini") || normalized.contains("google")) {
            return "gemini";
        } else {
            log.warn("Unknown LLM name: {}, defaulting to openai", llmName);
            return "openai";
        }
    }

    /**
     * Normalize LLM name from extension attribute format to actual model name.
     * "Open AI" -> "gpt-5-mini", "Claude" -> "claude-3-5-sonnet-20241022", "Gemini" -> "gemini-1.5-flash"
     */
    private String normalizeLLMName(String llmName) {
        if (llmName == null || llmName.isEmpty()) {
            return "gpt-5-mini"; // Default to gpt-5-mini (faster, more cost-efficient version of GPT-5)
        }

        String normalized = llmName.toLowerCase().replaceAll("\\s+", "");

        if (normalized.contains("openai") || normalized.contains("gpt")) {
            return "gpt-5-mini"; // Use gpt-5-mini for OpenAI (faster, more cost-efficient)
        } else if (normalized.contains("claude") || normalized.contains("anthropic")) {
            return "claude-sonnet-4-6"; // Latest Claude model
        } else if (normalized.contains("gemini") || normalized.contains("google")) {
            return "gemini-2.5-pro"; // Fast Gemini model
        }

        return normalized;
    }

    /**
     * Process downloaded files from UI Agent response.
     *
     * Downloads files from UI Agent, uploads to Krista media server, and sends delete requests.
     *
     * @param downloadedFiles List of DownloadedFile from protobuf response
     * @param sessionId Session ID for file delete requests
     * @return List of Krista File objects for ExtensionResponse
     */
    public List<app.krista.model.base.File> processDownloadedFiles(List<DownloadedFile> downloadedFiles, String sessionId) {
        if (downloadedFiles == null || downloadedFiles.isEmpty()) {
            log.debug("No files to process");
            return new ArrayList<>();
        }

        log.info("Processing {} downloaded files for session {}", downloadedFiles.size(), sessionId);

        try {
            // Download from UI Agent and upload to Krista media server
            List<app.krista.model.base.File> uploadedFiles = fileHandlingService.processFiles(downloadedFiles);

            log.info("Successfully uploaded {} files to media server", uploadedFiles.size());

            // Send delete requests for each file (best-effort cleanup)
            for (DownloadedFile downloadedFile : downloadedFiles) {
                String filename = downloadedFile.getFilename();
                if (filename != null && !filename.isEmpty()) {
                    log.debug("Sending delete request for file: {}", filename);
                    rawEsbClient.sendFileDeleteRequest(sessionId, filename);
                }
            }

            return uploadedFiles;

        } catch (Exception e) {
            log.error("Failed to process downloaded files for session {}: {}", sessionId, e.getMessage(), e);
            // Return empty list on error - don't fail the entire request
            return new ArrayList<>();
        }
    }

    /**
     * Builds a detailed error message including which step/prompt failed and why.
     */
    private String buildDetailedErrorMessage(String topLevelError, PromptExecuteResponse response) {
        if (!response.hasResult()) return topLevelError;
        return buildDetailedErrorMessage(topLevelError, response.getResult());
    }

    private String buildDetailedErrorMessage(String topLevelError, ExecutionResult result) {
        List<ExecutionStep> steps = result.getStepsList();
        if (steps == null || steps.isEmpty()) return topLevelError;

        // Use " | " as separator — Krista FreeForm Text fields strip \n characters
        StringBuilder sb = new StringBuilder(topLevelError);

        for (int i = 0; i < steps.size(); i++) {
            ExecutionStep step = steps.get(i);
            if (step.getSuccess()) continue;
            int num = step.getStepNumber() > 0 ? step.getStepNumber() : (i + 1);
            sb.append(" | Step ").append(num);
            if (!step.getPrompt().isEmpty()) {
                sb.append(": \"").append(step.getPrompt()).append("\"");
            } else if (!step.getAction().isEmpty()) {
                sb.append(" (").append(step.getAction()).append(")");
            }
            if (!step.getError().isEmpty()) {
                sb.append(" — ").append(step.getError());
            }
        }
        return sb.toString();
    }

}