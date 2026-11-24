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

import app.krista.infra.esb.service.ReplyBase;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * Reply for executePrompt API.
 * Matches PromptExecuteResponse from ui_agent_messages.proto
 */
public class PromptExecuteReply extends ReplyBase {

    private String requestId;
    private boolean ok;
    private String status;
    private ExecutionResult result;
    private long timestamp;

    public PromptExecuteReply() {
    }

    @ProtoField(number = 1)
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @ProtoField(number = 2)
    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    @ProtoField(number = 3)
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @ProtoField(number = 4)
    public ExecutionResult getResult() {
        return result;
    }

    public void setResult(ExecutionResult result) {
        this.result = result;
    }

    @ProtoField(number = 5)
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PromptExecuteReply{" +
                "requestId='" + requestId + '\'' +
                ", ok=" + ok +
                ", status='" + status + '\'' +
                ", result=" + result +
                ", timestamp=" + timestamp +
                '}';
    }
}

