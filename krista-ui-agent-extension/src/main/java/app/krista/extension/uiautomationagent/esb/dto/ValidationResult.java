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
 * Validation result for a single validation step.
 * Matches ValidationResult from ui_agent_messages.proto
 */
public class ValidationResult {

    private int step;
    private String validationType;
    private String validationPrompt;
    private boolean passed;
    private String expected;
    private String actual;
    private String errorMessage;
    private Screenshot screenshot;
    private long timestamp;

    public ValidationResult() {
    }

    @ProtoField(number = 1)
    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    @ProtoField(number = 2)
    public String getValidationType() {
        return validationType;
    }

    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }

    @ProtoField(number = 3)
    public String getValidationPrompt() {
        return validationPrompt;
    }

    public void setValidationPrompt(String validationPrompt) {
        this.validationPrompt = validationPrompt;
    }

    @ProtoField(number = 4)
    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    @ProtoField(number = 5)
    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    @ProtoField(number = 6)
    public String getActual() {
        return actual;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }

    @ProtoField(number = 7)
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @ProtoField(number = 8)
    public Screenshot getScreenshot() {
        return screenshot;
    }

    public void setScreenshot(Screenshot screenshot) {
        this.screenshot = screenshot;
    }

    @ProtoField(number = 9)
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "step=" + step +
                ", validationType='" + validationType + '\'' +
                ", validationPrompt='" + validationPrompt + '\'' +
                ", passed=" + passed +
                ", expected='" + expected + '\'' +
                ", actual='" + actual + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", screenshot=" + screenshot +
                ", timestamp=" + timestamp +
                '}';
    }
}

