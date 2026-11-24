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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Execution result containing steps, outputs, and downloaded files.
 * Matches ExecutionResult from ui_agent_messages.proto
 */
public class ExecutionResult {

    private List<ExecutionStep> steps = new ArrayList<>();
    private String finalOutput;
    private List<String> screenshots = new ArrayList<>();
    private List<DownloadedFile> downloads = new ArrayList<>();
    private Map<String, String> metadata = new HashMap<>();
    private String errorMessage;
    private List<ValidationResult> validations = new ArrayList<>();
    private boolean validationFailed;
    private String validationError;

    public ExecutionResult() {
    }

    @ProtoField(number = 1)
    public List<ExecutionStep> getSteps() {
        return steps;
    }

    public void setSteps(List<ExecutionStep> steps) {
        this.steps = steps;
    }

    @ProtoField(number = 2)
    public String getFinalOutput() {
        return finalOutput;
    }

    public void setFinalOutput(String finalOutput) {
        this.finalOutput = finalOutput;
    }

    @ProtoField(number = 3)
    public List<String> getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(List<String> screenshots) {
        this.screenshots = screenshots;
    }

    @ProtoField(number = 4)
    public List<DownloadedFile> getDownloads() {
        return downloads;
    }

    public void setDownloads(List<DownloadedFile> downloads) {
        this.downloads = downloads;
    }

    @ProtoField(number = 5)
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @ProtoField(number = 6)
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @ProtoField(number = 7)
    public List<ValidationResult> getValidations() {
        return validations;
    }

    public void setValidations(List<ValidationResult> validations) {
        this.validations = validations;
    }

    @ProtoField(number = 8)
    public boolean isValidationFailed() {
        return validationFailed;
    }

    public void setValidationFailed(boolean validationFailed) {
        this.validationFailed = validationFailed;
    }

    @ProtoField(number = 9)
    public String getValidationError() {
        return validationError;
    }

    public void setValidationError(String validationError) {
        this.validationError = validationError;
    }

    @Override
    public String toString() {
        return "ExecutionResult{" +
                "steps=" + steps.size() +
                ", finalOutput='" + finalOutput + '\'' +
                ", screenshots=" + screenshots.size() +
                ", downloads=" + downloads.size() +
                ", metadata=" + metadata +
                ", errorMessage='" + errorMessage + '\'' +
                ", validations=" + validations.size() +
                ", validationFailed=" + validationFailed +
                ", validationError='" + validationError + '\'' +
                '}';
    }
}

