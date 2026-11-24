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
 * Reply for startSession API.
 * Matches SessionStartResponse from ui_agent_messages.proto
 */
public class SessionStartReply extends ReplyBase {

    private String requestId;
    private String sessionId;
    private boolean ok;
    private String status;
    private String message;
    private long timestamp;
    private int maxConcurrentSessions;
    private int currentSessionCount;

    public SessionStartReply() {
    }

    @ProtoField(number = 1)
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @ProtoField(number = 2)
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @ProtoField(number = 3)
    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    @ProtoField(number = 4)
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @ProtoField(number = 5)
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @ProtoField(number = 6)
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @ProtoField(number = 7)
    public int getMaxConcurrentSessions() {
        return maxConcurrentSessions;
    }

    public void setMaxConcurrentSessions(int maxConcurrentSessions) {
        this.maxConcurrentSessions = maxConcurrentSessions;
    }

    @ProtoField(number = 8)
    public int getCurrentSessionCount() {
        return currentSessionCount;
    }

    public void setCurrentSessionCount(int currentSessionCount) {
        this.currentSessionCount = currentSessionCount;
    }

    @Override
    public String toString() {
        return "SessionStartReply{" +
                "requestId='" + requestId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", ok=" + ok +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", maxConcurrentSessions=" + maxConcurrentSessions +
                ", currentSessionCount=" + currentSessionCount +
                '}';
    }
}

