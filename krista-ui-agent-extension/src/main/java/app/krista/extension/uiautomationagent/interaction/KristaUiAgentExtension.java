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

package app.krista.extension.uiautomationagent.interaction;

import app.krista.extension.authorization.Authenticated;
import app.krista.extension.event.WaitForEventListener;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.*;
import app.krista.extension.request.ExecutionMode;
import app.krista.extension.uiautomationagent.esb.UiAgentClient;
import app.krista.extension.uiautomationagent.esb.UiAgentRawClient;
import app.krista.extension.uiautomationagent.esb.UiAgentEsbConfigProvider;
import app.krista.infra.esb.ServiceBusConfig;
import app.krista.extension.uiautomationagent.config.ExtensionAttributes;
import app.krista.extension.uiautomationagent.config.ExtensionAttributesProvider;
import app.krista.extension.uiautomationagent.events.UiAutomationEventListenerHandler;
import app.krista.ksdk.context.AuthorizationContext;
import okhttp3.*;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.JMSException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Main extension class for the Agentic Browser.
 * This class is automatically discovered and instantiated by the Krista extension framework
 * through the @Service annotation and dependency injection.
 */
@Service
@Authenticated
@Java(version = Java.Version.JAVA_21)
@Field.Text(value = ExtensionAttributes.UI_AGENT_URL, required = true, order = 1)
@Field.PickOne(value = ExtensionAttributes.PRIMARY_LLM, values = {ExtensionAttributes.OPEN_AI, ExtensionAttributes.GOOGLE_GEMINI, ExtensionAttributes.CLAUDE}, required = true, order = 2)
@Field.Text(value = ExtensionAttributes.PRIMARY_LLM_API_KEY, isSecured = true, required = true, order = 3)
@Field.PickOne(value = ExtensionAttributes.FALLBACK_LLM, values = {ExtensionAttributes.OPEN_AI, ExtensionAttributes.GOOGLE_GEMINI, ExtensionAttributes.CLAUDE}, required = true, order = 4)
@Field.Text(value = ExtensionAttributes.FALLBACK_LLM_API_KEY, isSecured = true, required = true, order = 5)

@Extension(implementationModel = Extension.ImplementationModel.Krista_4_O, jaxrsId = "uiagent", version = "1.0.0", supportingModes = {ExecutionMode.LIVE})
@Containerize(baseImageVersion = "3.6.2")
@StaticResource(path = "ui", file = "ui")
@StaticResource(path = "docs", file = "docs")
@Domain(id = "catEntryDomain_56e80896-0143-40dc-8f20-364815774684",
        name = "UI Automation Agent",
        ecosystemId = "catEntryEcosystem_d3b05047-07b0-4b06-95a3-9fb8f7f608d9",
        ecosystemName = "Krista",
        ecosystemVersion = "a23649e2-79f6-479b-bd34-67d69908dcc8")
public class KristaUiAgentExtension {

    private static final Logger log = LoggerFactory.getLogger(KristaUiAgentExtension.class);

    private final Invoker invoker;
    private final ExtensionAttributes extensionAttributes;
    private final AuthorizationContext authorizationContext;
    private final UiAutomationEventListenerHandler eventListenerHandler;
    private final app.krista.ksdk.telemetry.TelemetryMetrics telemetryMetrics;

    // ESB client for communicating with UI Agent service
    private UiAgentClient esbClient;

    // Raw Artemis client using Google Protobuf (Artemis Core protocol - port 61616)
    private UiAgentRawClient rawEsbClient;

    @Inject
    public KristaUiAgentExtension(@Named("self") Invoker invoker,
                                 ExtensionAttributes extensionAttributes,
                                 AuthorizationContext authorizationContext,
                                 UiAutomationEventListenerHandler eventListenerHandler,
                                 app.krista.ksdk.telemetry.TelemetryMetrics telemetryMetrics) {
        this.invoker = invoker;
        this.extensionAttributes = extensionAttributes;
        this.authorizationContext = authorizationContext;
        this.eventListenerHandler = eventListenerHandler;
        this.telemetryMetrics = telemetryMetrics;

        // Initialize ExtensionAttributes immediately
        initializeExtensionAttributes();

        log.info("KristaUiAgentExtension constructor called - instance: {}", System.identityHashCode(this));
    }

    /**
     * Initializes ExtensionAttributes
     */
    private void initializeExtensionAttributes() {
        try {
            // Use the injected ExtensionAttributes (already initialized by Guice)
            ExtensionAttributesProvider.setExtensionAttributes(this.extensionAttributes);

            // Verify attributes are set correctly
            boolean valid = this.extensionAttributes.verifyAttributes();
            if (!valid) {
                log.warn("Some extension attributes are missing or invalid");
            }

            log.info("ExtensionAttributes initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize ExtensionAttributes", e);
            throw new RuntimeException("Failed to initialize ExtensionAttributes", e);
        }
    }

    /**
     * Tests the connection to the UI Agent service.
     * This method is called by the Krista platform when testing the extension connection.
     */
    @InvokerRequest(InvokerRequest.Type.TEST_CONNECTION)
    public void testConnection() throws IOException {
        // Validate attributes first
        this.validateAttributes(this.invoker.getAttributes());

        // Call UI Agent service health endpoint to verify connectivity
        String uiAgentUrl = getStringAttribute(this.invoker.getAttributes(), ExtensionAttributes.UI_AGENT_URL);
        testUiAgentHealth(uiAgentUrl);

        log.info("Test connection successful");
    }

    /**
     * Validates the extension attributes.
     * This method is called by the Krista platform when validating extension attributes.
     */
    @InvokerRequest(InvokerRequest.Type.VALIDATE_ATTRIBUTES)
    public void validateAttributes(Map<String, Object> attributes) throws IOException {
        // Validate UI Agent URL format and connectivity
        String uiAgentUrl = getStringAttribute(attributes, ExtensionAttributes.UI_AGENT_URL);
        validateUiAgentUrl(uiAgentUrl);
        testUiAgentHealth(uiAgentUrl);

        // Validate LLM API keys if LLM is selected
        String primaryLLM = getStringAttribute(attributes, ExtensionAttributes.PRIMARY_LLM);
        if (primaryLLM != null && !primaryLLM.isBlank()) {
            String primaryApiKey = getStringAttribute(attributes, ExtensionAttributes.PRIMARY_LLM_API_KEY);
            if (primaryApiKey == null || primaryApiKey.isBlank()) {
                throw new RuntimeException("Primary LLM API Key is required when Primary LLM is selected");
            }
            // Validate API key format
            validateApiKeyFormat(primaryLLM, primaryApiKey, "Primary");
        }

        String fallbackLLM = getStringAttribute(attributes, ExtensionAttributes.FALLBACK_LLM);
        if (fallbackLLM != null && !fallbackLLM.isBlank()) {
            String fallbackApiKey = getStringAttribute(attributes, ExtensionAttributes.FALLBACK_LLM_API_KEY);
            if (fallbackApiKey == null || fallbackApiKey.isBlank()) {
                throw new RuntimeException("Fallback LLM API Key is required when Fallback LLM is selected");
            }
            // Validate API key format
            validateApiKeyFormat(fallbackLLM, fallbackApiKey, "Fallback");
        }

        log.info("Successfully validated attributes");
    }

    /**
     * Validates the format and authenticity of an API key by making a test API call.
     */
    private void validateApiKeyFormat(String llmProvider, String apiKey, String keyType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException(keyType + " LLM API Key cannot be empty");
        }

        // First validate format
        switch (llmProvider) {
            case ExtensionAttributes.OPEN_AI:
                if (!apiKey.startsWith("sk-")) {
                    throw new RuntimeException(keyType + " LLM API Key for OpenAI must start with 'sk-'");
                }
                if (apiKey.length() < 20) {
                    throw new RuntimeException(keyType + " LLM API Key for OpenAI appears to be invalid (too short)");
                }
                // Test the key with a real API call
                testOpenAIKey(apiKey, keyType);
                break;

            case ExtensionAttributes.CLAUDE:
                if (!apiKey.startsWith("sk-ant-")) {
                    throw new RuntimeException(keyType + " LLM API Key for Claude must start with 'sk-ant-'");
                }
                if (apiKey.length() < 20) {
                    throw new RuntimeException(keyType + " LLM API Key for Claude appears to be invalid (too short)");
                }
                // Test the key with a real API call
                testClaudeKey(apiKey, keyType);
                break;

            case ExtensionAttributes.GOOGLE_GEMINI:
                if (apiKey.length() < 20) {
                    throw new RuntimeException(keyType + " LLM API Key for Gemini appears to be invalid (too short)");
                }
                // Test the key with a real API call
                testGeminiKey(apiKey, keyType);
                break;

            default:
                log.warn("Unknown LLM provider '{}', skipping API key validation", llmProvider);
        }

        log.info("{} LLM API Key validation passed for provider: {}", keyType, llmProvider);
    }

    /**
     * Validates that the UI Agent URL is a well-formed HTTP or HTTPS URL.
     */
    private void validateUiAgentUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new RuntimeException("UI Agent URL is required");
        }

        try {
            URL parsed = new URL(url.trim());
            String protocol = parsed.getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                throw new RuntimeException("UI Agent URL must use http or https protocol, got: " + protocol);
            }
            if (parsed.getHost() == null || parsed.getHost().isBlank()) {
                throw new RuntimeException("UI Agent URL must contain a valid host");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("UI Agent URL is not a valid URL: " + url);
        }

        log.info("UI Agent URL validation passed: {}", url);
    }

    /**
     * Tests connectivity to the UI Agent service by calling its /health endpoint.
     */
    private void testUiAgentHealth(String uiAgentUrl) {
        if (uiAgentUrl == null || uiAgentUrl.isBlank()) {
            throw new RuntimeException("UI Agent URL is not configured");
        }

        String healthUrl = uiAgentUrl.trim();
        if (healthUrl.endsWith("/")) {
            healthUrl = healthUrl + "health";
        } else {
            healthUrl = healthUrl + "/health";
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(healthUrl)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("UI Agent service health check failed with status " + response.code() + ": " + response.message());
            }
            log.info("UI Agent service health check passed at {}", healthUrl);
        } catch (IOException e) {
            throw new RuntimeException("Unable to connect to UI Agent service at " + healthUrl + " - " + e.getMessage());
        }
    }

    /**
     * Tests OpenAI API key by making a models list request
     */
    private void testOpenAIKey(String apiKey, String keyType) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/models")
                .header("Authorization", "Bearer " + apiKey)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 401) {
                throw new RuntimeException(keyType + " LLM API Key for OpenAI is invalid or expired");
            } else if (response.code() == 429) {
                throw new RuntimeException(keyType + " LLM API Key for OpenAI has exceeded rate limits");
            } else if (!response.isSuccessful()) {
                throw new RuntimeException(keyType + " LLM API Key for OpenAI validation failed: " + response.message());
            }
            log.info("{} OpenAI API key validated successfully", keyType);
        } catch (IOException e) {
            throw new RuntimeException(keyType + " LLM API Key validation failed: Unable to connect to OpenAI API - " + e.getMessage());
        }
    }

    /**
     * Tests Claude/Anthropic API key by making a messages request
     */
    private void testClaudeKey(String apiKey, String keyType) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        // Use a minimal test request to validate the key
        String jsonBody = "{\"model\":\"claude-3-5-sonnet-20241022\",\"max_tokens\":1,\"messages\":[{\"role\":\"user\",\"content\":\"test\"}]}";
        RequestBody body = RequestBody.create(jsonBody, okhttp3.MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 401) {
                throw new RuntimeException(keyType + " LLM API Key for Claude is invalid or expired");
            } else if (response.code() == 429) {
                throw new RuntimeException(keyType + " LLM API Key for Claude has exceeded rate limits");
            } else if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : response.message();
                throw new RuntimeException(keyType + " LLM API Key for Claude validation failed: " + errorBody);
            }
            log.info("{} Claude API key validated successfully", keyType);
        } catch (IOException e) {
            throw new RuntimeException(keyType + " LLM API Key validation failed: Unable to connect to Claude API - " + e.getMessage());
        }
    }

    /**
     * Tests Google Gemini API key by making a models list request
     */
    private void testGeminiKey(String apiKey, String keyType) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 400 || response.code() == 403) {
                throw new RuntimeException(keyType + " LLM API Key for Gemini is invalid or expired");
            } else if (response.code() == 429) {
                throw new RuntimeException(keyType + " LLM API Key for Gemini has exceeded rate limits");
            } else if (!response.isSuccessful()) {
                throw new RuntimeException(keyType + " LLM API Key for Gemini validation failed: " + response.message());
            }
            log.info("{} Gemini API key validated successfully", keyType);
        } catch (IOException e) {
            throw new RuntimeException(keyType + " LLM API Key validation failed: Unable to connect to Gemini API - " + e.getMessage());
        }
    }

    private String getStringAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value != null ? value.toString() : "";
    }

    /**
     * Handles updates to the extension attributes.
     * This method is called by the Krista platform when extension attributes are updated.
     */
    @InvokerRequest(InvokerRequest.Type.INVOKER_UPDATED)
    public void invokerUpdated(Map<String, Object> oldAttributes, Map<String, Object> newAttributes) throws IOException {
        try {
            log.info("Updating UI Agent configuration from extension attributes");
            extensionAttributes.update(newAttributes);
            extensionAttributes.storeDefaultModelConfig(newAttributes);
            extensionAttributes.storeFallbackModelConfig(newAttributes);

            // CRITICAL: Update the static provider instance so event handlers get the updated attributes
            ExtensionAttributesProvider.setExtensionAttributes(this.extensionAttributes);
            log.info("✅ Updated ExtensionAttributesProvider with new attributes");
        } catch (Exception ex) {
            log.error("Error while updating extension attributes", ex);
            extensionAttributes.update(oldAttributes);
            throw ex;
        }
    }

    /**
     * Handles the loading of the extension.
     * This method is called by the Krista platform when the extension is loaded.
     * NOTE: This is called 3 times - once for each mode (DRAFT, TEST, LIVE).
     * Each mode gets its own consumer instance.
     */
    @InvokerRequest(InvokerRequest.Type.INVOKER_LOADED)
    public void invokerLoaded() throws IOException {
        log.info("KristaUiAgentExtension.invokerLoaded() called - instance: {}", System.identityHashCode(this));

        // CRITICAL: Update the static provider with the current attributes
        // This ensures event handlers get the correct attributes after restart
        // The extensionAttributes field is already injected by Krista framework
        ExtensionAttributesProvider.setExtensionAttributes(this.extensionAttributes);
        log.info("✅ Updated ExtensionAttributesProvider on invoker load");

        try {
            // Get invoker and workspace IDs
            String invokerId = invoker.getInvokerId();
            String workspaceId = invoker.getWorkspaceId();
            app.krista.extension.uiautomationagent.service.InvokerContext.setContext(invokerId, workspaceId);

            // Resolve broker config from the configured UI Agent URL
            String uiAgentUrl = this.extensionAttributes.getUiAgentUrl();

            // Get ESB configuration (shared request queue, auto-generated response queue)
            ServiceBusConfig esbConfig = UiAgentEsbConfigProvider.getServiceBusConfig(uiAgentUrl);

            log.info("Initializing ESB client (Krista Infrastructure):");
            log.info("  Invoker ID: {}", invokerId);
            log.info("  Workspace ID: {}", workspaceId);
            log.info("  Broker: {}:{}", UiAgentEsbConfigProvider.getHost(uiAgentUrl), UiAgentEsbConfigProvider.getPort(uiAgentUrl));
            log.info("  Protocol: Artemis Core (port 61616)");

            // Create ESB client using Krista's ServiceClientBase (legacy)
            this.esbClient = new UiAgentClient(esbConfig, invokerId, workspaceId);

            log.info("✅ ESB client initialized successfully (legacy)");
            log.info("  Using Krista's ESB infrastructure (same as voice extensions)");
            log.info("  Request queue: krista-ui-agent-1-IN (shared, managed by service)");
            log.info("  Response queue: krista-ui-agent-1-OUT (auto-generated by ServiceClientBase)");

            // Create raw Artemis client using Google Protobuf (singleton pattern)
            // This prevents duplicate consumers when INVOKER_LOADED is called multiple times
            String extensionId = "ext-" + invokerId + "-" + workspaceId;
            this.rawEsbClient = UiAgentRawClient.getInstance(
                UiAgentEsbConfigProvider.getHost(uiAgentUrl),
                UiAgentEsbConfigProvider.getPort(uiAgentUrl),
                UiAgentEsbConfigProvider.getUsername(uiAgentUrl),
                UiAgentEsbConfigProvider.getPassword(uiAgentUrl),
                extensionId,
                1200000 // 20 minute timeout (UI automation can take time)
            );

            // Pass telemetry to the singleton ESB client
            UiAgentRawClient.setTelemetryMetrics(this.telemetryMetrics);

            log.info("✅ Raw ESB client initialized successfully (Google Protobuf)");
            log.info("  Extension ID: {}", extensionId);
            log.info("  Request queue: krista-ui-agent-1-IN");
            log.info("  Response queue: krista-ui-agent-responses-{}-1-OUT", extensionId);

            // Inject ESB clients into event listener handler
            // Using legacy ESB client (Krista infrastructure) for protocol compatibility
            eventListenerHandler.setEsbClient(esbClient);
            eventListenerHandler.setRawEsbClient(rawEsbClient);
            log.info("✅ ESB clients injected into event listener handler");
            log.info("  Using legacy ESB client (Krista infrastructure) for protocol conversion");

        } catch (Exception e) {
            log.error("❌ Failed to initialize ESB client", e);
            throw new IOException("Failed to initialize ESB client: " + e.getMessage(), e);
        }

        log.info("KristaUiAgentExtension loaded successfully");
    }

    /**
     * Handles the unloading of the extension.
     * This method is called by the Krista platform when the extension is unloaded/reloaded.
     * Note: ESB connection is managed by Krista infrastructure.
     * The response queue persists for reconnection on reload.
     */
    @InvokerRequest(InvokerRequest.Type.INVOKER_UNLOADED)
    public void invokerUnloaded() {
        log.info("KristaUiAgentExtension is being unloaded (reload/upgrade)");

        // ESB client cleanup (managed by Krista infrastructure)
        if (esbClient != null) {
            try {
                // ServiceClientBase handles connection cleanup automatically
                esbClient = null;
                log.info("✅ ESB client released successfully");
            } catch (Exception e) {
                log.error("Error releasing ESB client", e);
            }
        }

        // Raw ESB client cleanup - close consumer, queue persists for reload
        if (rawEsbClient != null) {
            try {
                rawEsbClient.close();
                rawEsbClient = null;
                log.info("✅ Raw ESB client closed (queue preserved for reload)");
            } catch (Exception e) {
                log.error("❌ Error closing raw ESB client", e);
            }
        }

        log.info("KristaUiAgentExtension unloaded successfully");
    }

    /**
     * Handles the removal of the extension.
     * This method is called by the Krista platform when the extension is permanently removed.
     * Note: ESB queues should be manually cleaned up from Artemis console.
     */
    @InvokerRequest(InvokerRequest.Type.INVOKER_REMOVED)
    public void invokerRemoved() {
        log.info("KristaUiAgentExtension is being removed (permanent deletion)");

        // ESB client cleanup (managed by Krista infrastructure)
        if (esbClient != null) {
            try {
                // ServiceClientBase handles queue cleanup automatically
                esbClient = null;
                log.info("✅ ESB client released successfully");
            } catch (Exception e) {
                log.error("❌ Error releasing ESB client", e);
            }
        }

        // Raw ESB client cleanup - close connections
        if (rawEsbClient != null) {
            try {
                rawEsbClient.close();
                rawEsbClient = null;
                log.info("✅ Raw ESB client closed");
                log.info("⚠️ Note: Response queue should be manually deleted from Artemis console if needed");
            } catch (Exception e) {
                log.error("❌ Error closing raw ESB client", e);
            }
        }

        log.info("KristaUiAgentExtension removed successfully");
    }

    /**
     * Handles event listener registration.
     * This method is called by the Krista platform when a wait-for-event request is registered.
     */
    @InvokerRequest(InvokerRequest.Type.REGISTER_EVENT_LISTENER)
    public void onEventListenerRegistered(WaitForEventListener listener) {
        log.info("Event listener registration received: {}", listener.getRequestId());
        eventListenerHandler.onEventListenerRegistered(listener);
    }

    /**
     * Get the ESB client instance (legacy).
     * Used by UiAgentFunctions to send requests to the UI Agent service.
     *
     * @return The ESB client instance, or null if not initialized
     */
    public UiAgentClient getEsbClient() {
        return esbClient;
    }

    /**
     * Get the raw ESB client instance (Google Protobuf).
     * Used by InteractionArea to send requests to the UI Agent service.
     *
     * @return The raw ESB client instance, or null if not initialized
     */
    public UiAgentRawClient getRawEsbClient() {
        return rawEsbClient;
    }

    /**
     * Get the invoker instance.
     * Used by InteractionArea to access workspace and invoker information.
     *
     * @return The invoker instance
     */
    public Invoker getInvoker() {
        return invoker;
    }

    /**
     * Get the event listener handler.
     * Used by InteractionArea to access shared configuration methods.
     *
     * @return The event listener handler
     */
    public UiAutomationEventListenerHandler getEventListenerHandler() {
        return eventListenerHandler;
    }

    /**
     * Define custom tabs for the extension UI.
     * This method is called by the Krista platform to register custom tabs.
     *
     * Two tabs are provided:
     * 1. "Try Prompt" - Interactive UI served via JAX-RS endpoint
     *    - Path: "uiagent/api/ui/" maps to:
     *      - "uiagent" = jaxrsId (defined in @Extension annotation line 42)
     *      - "api" = @ApplicationPath in ApiApplication
     *      - "ui/" = @Path("/ui/{subPath:.*}") in UiResource
     *
     * 2. "Documentation" - Static markdown files served via @StaticResource
     *    - Path: "static/docs" maps to @StaticResource(path = "docs", file = "docs")
     *    - Serves files from src/main/resources/docs/
     *
     * @return Map of tab titles to their corresponding paths
     */
    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> customTabs() {
        // Get UI Agent URL directly from invoker attributes (not the getter which has a localhost fallback)
        String uiAgentUrl = null;
        try {
            Object value = this.invoker.getAttributes().get(ExtensionAttributes.UI_AGENT_URL);
            if (value != null && !value.toString().isBlank()) {
                uiAgentUrl = value.toString();
            }
        } catch (Exception e) {
            log.warn("Failed to get UI Agent URL from invoker: {}", e.getMessage());
        }

        // URL encode the UI Agent URL for passing as query parameter
        String encodedUrl = "";
        if (uiAgentUrl != null && !uiAgentUrl.isBlank()) {
            try {
                encodedUrl = java.net.URLEncoder.encode(uiAgentUrl, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warn("Failed to encode UI Agent URL: {}", e.getMessage());
            }
        }

        return Map.of(
            "Playground", "uiagent/api/ui/?uiAgentUrl=" + encodedUrl,
            "Documentation", "static/docs"
        );
    }
}
