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
 * Input for executeSession API.
 * Matches SessionExecuteRequest from ui_agent_messages.proto
 */
public class SessionExecuteInput implements Input {

    private String requestId;
    private ExtensionIdentity extension;
    private String sessionId;
    private String prompt;
    private long timestamp;
    private String workspaceId;
    private String accountId;

    public SessionExecuteInput() {
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

    @Override
    @ProtoField(number = 3)
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @ProtoField(number = 4)
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @ProtoField(number = 5)
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    @ProtoField(number = 6)
    public String getWorkspaceId() {
        return workspaceId != null ? workspaceId : "n/a";
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    @ProtoField(number = 7)
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}

