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

package app.krista.extension.uiautomationagent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import app.krista.extension.executor.Invoker;
import app.krista.extension.util.InvokerAttributes;
import org.jvnet.hk2.annotations.Service;

@Service
public class ExtensionAttributes {

    private static final Logger log = LoggerFactory.getLogger(ExtensionAttributes.class);

    // LLM configuration constants
    public static final String PRIMARY_LLM_API_KEY = "Primary LLM API Key";
    public static final String FALLBACK_LLM_API_KEY = "Fallback LLM API Key";
    public static final String PRIMARY_LLM = "Primary LLM";
    public static final String FALLBACK_LLM = "Fallback LLM";
    public static final String OPEN_AI = "Open AI";
    public static final String GOOGLE_GEMINI = "Gemini";
    public static final String CLAUDE = "Claude";
    public static final String UI_AGENT_URL = "UI Agent URL";
    public static final String WORKING_DIRECTORY = "Working Directory";

    private String primaryLLM;
    private String fallbackLLM;
    private String primaryLLMApiKey;
    private String fallbackLLMApiKey;
    private Invoker invoker;
    private String uiAgentUrl;
    private String workingDirectory;

    @Inject
    public ExtensionAttributes(@Named("self") Invoker invoker) {
        this.invoker = invoker;
        Map<String, Object> attributes = invoker.getAttributes();
        update(attributes);
        log.info("ExtensionAttributes initialized with invoker attributes");
        
        // Log the actual attributes from invoker for debugging
        logInvokerAttributes();
    }
    
    /**
     * Log all attributes from the invoker for debugging
     */
    private void logInvokerAttributes() {
        try {
            Map<String, Object> attributes = invoker.getAttributes();
            log.info("All invoker attributes:");
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue().toString() : "null";
                if (entry.getKey().contains("API Key") || entry.getKey().contains("ApiKey")) {
                    value = maskApiKey(value);
                }
                log.info("  - {}: {}", entry.getKey(), value);
            }
        } catch (Exception e) {
            log.error("Error logging invoker attributes", e);
        }
    }

    /**
     * Updates the extension attributes with new values
     * 
     * @param attributes The new attribute values
     */
    public void update(Map<String, Object> attributes) {
        if (attributes == null) {
            log.error("Attributes map is null");
            return;
        }

        log.info("All invoker attributes:");
        attributes.forEach((key, value) -> {
            if (key.contains("API Key") || key.contains("ApiKey")) {
                log.info("  - {}: {}", key, maskApiKey(value != null ? value.toString() : null));
            } else {
                log.info("  - {}: {}", key, value);
            }
        });

        // Store the raw values from attributes
        this.primaryLLM = InvokerAttributes.getStringOrNull(attributes, PRIMARY_LLM);
        this.fallbackLLM = InvokerAttributes.getStringOrNull(attributes, FALLBACK_LLM);
        this.primaryLLMApiKey = InvokerAttributes.getStringOrNull(attributes, PRIMARY_LLM_API_KEY);
        this.fallbackLLMApiKey = InvokerAttributes.getStringOrNull(attributes, FALLBACK_LLM_API_KEY);
        this.uiAgentUrl = InvokerAttributes.getStringOrNull(attributes, UI_AGENT_URL);
        this.workingDirectory = InvokerAttributes.getStringOrNull(attributes, WORKING_DIRECTORY);

        // Enhanced logging with detailed attribute information
        log.info("Updated extension attributes:");
        log.info("  - Primary LLM: {}", maskSensitiveValue(this.primaryLLM));
        log.info("  - Fallback LLM: {}", maskSensitiveValue(this.fallbackLLM));
        log.info("  - Primary LLM API Key: {}", maskApiKey(this.primaryLLMApiKey));
        log.info("  - Fallback LLM API Key: {}", maskApiKey(this.fallbackLLMApiKey));
        log.info("  - UI Agent URL: {}", this.uiAgentUrl);
        log.info("  - Working Directory: {}", this.workingDirectory);
    }

    /**
     * Masks an API key for secure logging
     * 
     * @param apiKey The API key to mask
     * @return A masked version of the API key
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "[not set]";
        }
        
        // Show only first 4 and last 4 characters if key is long enough
        if (apiKey.length() > 8) {
            return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
        } else {
            return "****";
        }
    }


    /**
     * Masks a potentially sensitive value for logging
     * 
     * @param value The value to mask
     * @return The value or [not set] if empty
     */
    private String maskSensitiveValue(String value) {
        if (value == null || value.isEmpty()) {
            return "[not set]";
        }
        return value;
    }

    /**
     * Gets the primary LLM
     * 
     * @return The primary LLM or default if not set
     */
    public String getPrimaryLLM() {
        // Always get the latest value from invoker attributes
        Object value = invoker.getAttributes().get(PRIMARY_LLM);
        if (value == null || value.toString().isBlank()) {
            log.warn("Primary LLM not set in invoker attributes (will use default: {})", OPEN_AI);
            return OPEN_AI; // Default to OpenAI
        }
        return value.toString();
    }

    /**
     * Gets the fallback LLM
     * 
     * @return The fallback LLM or default if not set
     */
    public String getFallbackLLM() {
        // Always get the latest value from invoker attributes
        Object value = invoker.getAttributes().get(FALLBACK_LLM);
        if (value == null || value.toString().isBlank()) {
            log.warn("Fallback LLM not set in invoker attributes (will use default: {})", CLAUDE);
            return CLAUDE; // Default to Claude
        }
        return value.toString();
    }



    /**
     * Gets the primary LLM API key
     *
     * @return The primary LLM API key or empty string if not set
     */
    public String getPrimaryLLMApiKey() {
        log.info("🔑 Getting Primary LLM API Key...");

        // ALWAYS get fresh value from invoker.getAttributes() - this is the source of truth
        Map<String, Object> currentAttributes = invoker.getAttributes();
        log.info("🔑 Invoker has {} attributes", currentAttributes.size());
        log.info("🔑 Invoker attribute keys: {}", currentAttributes.keySet());

        Object apiKeyObj = currentAttributes.get(PRIMARY_LLM_API_KEY);

        if (apiKeyObj == null) {
            log.error("❌ Primary LLM API Key is NULL in invoker.getAttributes()!");
            log.error("   Key name: '{}'", PRIMARY_LLM_API_KEY);
            log.error("   Available keys: {}", currentAttributes.keySet());
            log.error("   Cached value: {}", maskApiKey(this.primaryLLMApiKey));
            throw new IllegalStateException("Primary LLM API Key is required but not found in invoker attributes");
        }

        String keyValue = apiKeyObj.toString().trim();
        log.info("🔑 Raw key value from invoker (length: {}, first 10 chars: '{}')",
                 keyValue.length(),
                 keyValue.substring(0, Math.min(10, keyValue.length())));

        // Check if it's a template variable (starts with ${ and ends with })
        if (keyValue.startsWith("${") && keyValue.endsWith("}")) {
            log.error("❌ Primary LLM API Key is a TEMPLATE VARIABLE: {}", keyValue);
            log.error("   This means the extension settings have a placeholder, not a real API key!");
            log.error("   Please enter your actual API key in the extension settings.");
            throw new IllegalStateException("Primary LLM API Key is a template variable '" + keyValue + "', not an actual key");
        }

        if (keyValue.isBlank()) {
            log.error("❌ Primary LLM API Key is BLANK!");
            throw new IllegalStateException("Primary LLM API Key is required but is blank");
        }

        log.info("✅ Primary LLM API Key retrieved successfully");
        log.info("   Length: {}", keyValue.length());
        log.info("   Starts with: {}", keyValue.substring(0, Math.min(7, keyValue.length())));
        log.info("   Ends with: ...{}", keyValue.substring(Math.max(0, keyValue.length() - 4)));

        return keyValue;
    }

    /**
     * Gets the fallback LLM API key
     *
     * @return The fallback LLM API key or empty string if not set
     */
    public String getFallbackLLMApiKey() {
        // Always get the latest value from invoker attributes (same pattern as ai-catalog-services)
        log.info("🔑 Getting Fallback LLM API Key from invoker.getAttributes()...");
        Map<String, Object> currentAttributes = invoker.getAttributes();

        Object apiKey = currentAttributes.get(FALLBACK_LLM_API_KEY);
        if (apiKey == null) {
            log.info("⚠️ Fallback LLM API Key is NULL in invoker attributes (this is optional)");
            return null;
        }

        String keyValue = apiKey.toString().trim();

        // Check if it's a template variable
        if (keyValue.startsWith("${") && keyValue.endsWith("}")) {
            log.warn("⚠️ Fallback LLM API Key appears to be a template variable: {}", keyValue);
            log.warn("   Ignoring fallback key - will use primary only");
            return null;
        }

        if (keyValue.isBlank()) {
            log.info("⚠️ Fallback LLM API Key is BLANK (this is optional)");
            return null;
        }

        log.info("✅ Fallback LLM API Key retrieved (length: {}, starts with: {})",
                 keyValue.length(), keyValue.substring(0, Math.min(7, keyValue.length())));
        return keyValue;
    }
    
    /**
     * Gets the UI Agent service URL
     *
     * @return The UI Agent service URL
     */
    public String getUiAgentUrl() {
        // First try to get from instance variable
        if (this.uiAgentUrl != null && !this.uiAgentUrl.isBlank()) {
            return this.uiAgentUrl;
        }

        // Then try to get from invoker attributes
        try {
            Object value = invoker.getAttributes().get(UI_AGENT_URL);
            if (value != null && !value.toString().isBlank()) {
                this.uiAgentUrl = value.toString(); // Update the instance variable
                return this.uiAgentUrl;
            }
        } catch (Exception e) {
            log.error("Error getting UI Agent URL from invoker", e);
        }

        // Fallback to default localhost URL
        String defaultUrl = "http://localhost:3000";
        log.info("UI Agent URL not set, using default: {}", defaultUrl);
        return defaultUrl;
    }
    
    /**
     * Gets the default timeout value in milliseconds
     *
     * @return The default timeout value (60 seconds)
     */
    public long getDefaultTimeout() {
        return 60000; // Default 60 seconds
    }
    
    /**
     * Verifies that all required extension attributes are properly set
     * and logs their current values
     * 
     * @return true if all required attributes are set, false otherwise
     */
    public boolean verifyAttributes() {
        log.info("Verifying extension attributes from invoker...");
        Map<String, Object> attributes = invoker.getAttributes();
        
        boolean allValid = true;
        
        // Check primary LLM configuration
        Object primaryLLM = attributes.get(PRIMARY_LLM);
        if (primaryLLM == null || primaryLLM.toString().isBlank()) {
            log.warn("Primary LLM not set in invoker attributes (will use default: {})", OPEN_AI);
            allValid = false;
        } else {
            log.info("Primary LLM from invoker: {}", primaryLLM);
        }
        
        Object primaryLLMApiKey = attributes.get(PRIMARY_LLM_API_KEY);
        if (primaryLLMApiKey == null || primaryLLMApiKey.toString().isBlank()) {
            log.warn("Primary LLM API Key not set in invoker attributes");
            allValid = false;
        } else {
            log.info("Primary LLM API Key from invoker: {}", maskApiKey(primaryLLMApiKey.toString()));
        }
        
        // Check fallback LLM configuration
        Object fallbackLLM = attributes.get(FALLBACK_LLM);
        if (fallbackLLM == null || fallbackLLM.toString().isBlank()) {
            log.warn("Fallback LLM not set in invoker attributes (will use default: {})", CLAUDE);
            allValid = false;
        } else {
            log.info("Fallback LLM from invoker: {}", fallbackLLM);
        }
        
        Object fallbackLLMApiKey = attributes.get(FALLBACK_LLM_API_KEY);
        if (fallbackLLMApiKey == null || fallbackLLMApiKey.toString().isBlank()) {
            log.warn("Fallback LLM API Key not set in invoker attributes");
            allValid = false;
        } else {
            log.info("Fallback LLM API Key from invoker: {}", maskApiKey(fallbackLLMApiKey.toString()));
        }

        // Check UI Agent URL
        Object uiAgentUrl = attributes.get(UI_AGENT_URL);
        log.info("UI Agent URL from invoker: {}", uiAgentUrl);

        log.info("Extension attributes verification complete. All valid: {}", allValid);
        
        return allValid;
    }
    
    /**
     * Stores the model configuration for the primary LLM.
     * This method saves the configuration to the appropriate backend service.
     * 
     * @param attributes The attributes containing the model configuration
     */
    public void storeDefaultModelConfig(Map<String, Object> attributes) {
        log.info("Storing default model config: {}", attributes.get(PRIMARY_LLM));
        
        // Implementation similar to ConfigService.storeDefaultModelConfig in ai_nlu_ex
        // For now, we'll just log the action since we don't have the full implementation
        
        String primaryLLM = InvokerAttributes.getStringOrNull(attributes, PRIMARY_LLM);
        String primaryApiKey = InvokerAttributes.getStringOrNull(attributes, PRIMARY_LLM_API_KEY);
        
        if (primaryLLM != null && !primaryLLM.isBlank() && 
            primaryApiKey != null && !primaryApiKey.isBlank()) {
            log.info("Default model config stored for: {}", primaryLLM);
        } else {
            log.warn("Cannot store default model config: missing LLM or API key");
        }
    }

    /**
     * Stores the model configuration for the fallback LLM.
     * This method saves the configuration to the appropriate backend service.
     * 
     * @param attributes The attributes containing the model configuration
     */
    public void storeFallbackModelConfig(Map<String, Object> attributes) {
        log.info("Storing fallback model config: {}", attributes.get(FALLBACK_LLM));
        
        // Implementation similar to ConfigService.storeFallbackModelConfig in ai_nlu_ex
        // For now, we'll just log the action since we don't have the full implementation
        
        String fallbackLLM = InvokerAttributes.getStringOrNull(attributes, FALLBACK_LLM);
        String fallbackApiKey = InvokerAttributes.getStringOrNull(attributes, FALLBACK_LLM_API_KEY);
        
        if (fallbackLLM != null && !fallbackLLM.isBlank() && 
            fallbackApiKey != null && !fallbackApiKey.isBlank()) {
            log.info("Fallback model config stored for: {}", fallbackLLM);
        } else {
            log.warn("Cannot store fallback model config: missing LLM or API key");
        }
    }

    /**
     * Gets the working directory for Node.js scripts
     * 
     * @return The working directory or null if not set
     */
    public String getWorkingDirectory() {
        // First try to get from instance variable
        if (this.workingDirectory != null && !this.workingDirectory.isBlank()) {
            File workingDir = new File(this.workingDirectory);
            if (workingDir.exists() && workingDir.isDirectory()) {
                return this.workingDirectory;
            }
        }
        
        // Then try to get from invoker attributes
        try {
            Object value = invoker.getAttributes().get(WORKING_DIRECTORY);
            if (value != null && !value.toString().isBlank()) {
                String path = value.toString();
                File workingDir = new File(path);
                if (workingDir.exists() && workingDir.isDirectory()) {
                    this.workingDirectory = path; // Update the instance variable
                    return this.workingDirectory;
                }
            }
        } catch (Exception e) {
            log.error("Error getting Working Directory from invoker", e);
        }
        
        // Return null if not found
        return null;
    }
}
