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

package app.krista.extension.uiautomationagent.esb.dto;

import app.krista.infra.esb.client.Input;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * Input for executePrompt API.
 * Matches PromptExecuteRequest from ui_agent_messages.proto
 */
public class PromptExecuteInput implements Input {

    private String requestId;
    private ExtensionIdentity extension;
    private String prompt;
    private long timestamp;
    private String workspaceId;
    private String accountId;

    // User configuration fields (flattened from UserConfiguration)
    private String primaryModel;
    private String fallbackModel;
    private String openaiApiKey;
    private String claudeApiKey;
    private String geminiApiKey;
    private String browserType;
    private boolean headless;
    private int timeout;
    private int viewportWidth;
    private int viewportHeight;
    private boolean debugMode;

    public PromptExecuteInput() {
    }

    @ProtoField(number = 1)
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @ProtoField(number = 2)
    public ExtensionIdentity getExtension() {
        return extension;
    }

    public void setExtension(ExtensionIdentity extension) {
        this.extension = extension;
    }

    @ProtoField(number = 3)
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @ProtoField(number = 4)
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    @ProtoField(number = 5)
    public String getWorkspaceId() {
        return workspaceId != null ? workspaceId : "n/a";
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    @ProtoField(number = 6)
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @ProtoField(number = 7)
    public String getPrimaryModel() {
        return primaryModel;
    }

    public void setPrimaryModel(String primaryModel) {
        this.primaryModel = primaryModel;
    }

    @ProtoField(number = 8)
    public String getFallbackModel() {
        return fallbackModel;
    }

    public void setFallbackModel(String fallbackModel) {
        this.fallbackModel = fallbackModel;
    }

    @ProtoField(number = 9)
    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    public void setOpenaiApiKey(String openaiApiKey) {
        this.openaiApiKey = openaiApiKey;
    }

    @ProtoField(number = 10)
    public String getClaudeApiKey() {
        return claudeApiKey;
    }

    public void setClaudeApiKey(String claudeApiKey) {
        this.claudeApiKey = claudeApiKey;
    }

    @ProtoField(number = 11)
    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    @ProtoField(number = 12)
    public String getBrowserType() {
        return browserType;
    }

    public void setBrowserType(String browserType) {
        this.browserType = browserType;
    }

    @ProtoField(number = 13)
    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @ProtoField(number = 14, defaultValue = "30000")
    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @ProtoField(number = 15, defaultValue = "1280")
    public int getViewportWidth() {
        return viewportWidth;
    }

    public void setViewportWidth(int viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    @ProtoField(number = 16, defaultValue = "720")
    public int getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportHeight(int viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    @ProtoField(number = 17)
    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
}

