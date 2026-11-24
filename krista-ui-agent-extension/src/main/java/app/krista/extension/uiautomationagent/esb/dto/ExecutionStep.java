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

import org.infinispan.protostream.annotations.ProtoField;

/**
 * Individual execution step from UI automation.
 * Matches ExecutionStep from ui_agent_messages.proto
 */
public class ExecutionStep {

    private String action;
    private String selector;
    private String value;
    private String result;
    private boolean success;
    private String error;
    private long timestamp;

    public ExecutionStep() {
    }

    @ProtoField(number = 1)
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @ProtoField(number = 2)
    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    @ProtoField(number = 3)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @ProtoField(number = 4)
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @ProtoField(number = 5)
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @ProtoField(number = 6)
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    @ProtoField(number = 7)
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ExecutionStep{" +
                "action='" + action + '\'' +
                ", selector='" + selector + '\'' +
                ", value='" + value + '\'' +
                ", result='" + result + '\'' +
                ", success=" + success +
                ", error='" + error + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

