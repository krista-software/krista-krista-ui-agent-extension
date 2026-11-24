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
 * Screenshot information.
 * Matches Screenshot from ui_agent_messages.proto
 */
public class Screenshot {

    private int step;
    private String stepText;
    private String action;
    private String filename;
    private String downloadUrl;
    private long timestamp;

    public Screenshot() {
    }

    @ProtoField(number = 1)
    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    @ProtoField(number = 2)
    public String getStepText() {
        return stepText;
    }

    public void setStepText(String stepText) {
        this.stepText = stepText;
    }

    @ProtoField(number = 3)
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @ProtoField(number = 4)
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @ProtoField(number = 5)
    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    @ProtoField(number = 6)
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Screenshot{" +
                "step=" + step +
                ", stepText='" + stepText + '\'' +
                ", action='" + action + '\'' +
                ", filename='" + filename + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

