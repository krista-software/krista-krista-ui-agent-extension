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

package app.krista.extension.uiautomationagent.factory;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.RemediationAction;
import app.krista.extension.executor.RemediationActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating ExtensionResponse objects
 */
public class ExtensionResponseFactory {

    private static final Logger log = LoggerFactory.getLogger(ExtensionResponseFactory.class);

    private ExtensionResponseFactory() {}

    /**
     * Creates a successful response with values
     */
    public static ExtensionResponse create(Map<String, Object> values) {
        log.info("Creating ExtensionResponse with values: {}", values);
        
        // Create a new ExtensionResponse with the values
        ExtensionResponse response = new ExtensionResponse(
            ExtensionResponse.Result.SUCCESS, 
            values,  // This is the data that will be returned to the client
            null,    // No error
            null,    // No remediation actions
            null     // No state
        );
        
        log.info("Created ExtensionResponse: {}", response);
        return response;
    }

    /**
     * Creates an error response with exception details
     */
    public static ExtensionResponse create(Exception cause,
                                           String message,
                                           ExtensionResponse.Error.ExceptionType exceptionType) {
        ExtensionResponse.Error error = new ExtensionResponse.Error(message, System.currentTimeMillis(),
                exceptionType, Arrays.toString(cause.getStackTrace()));
        return new ExtensionResponse(ExtensionResponse.Result.FAILURE,
                null, error, null, null);
    }

    /**
     * Creates an error response with exception details and state
     */
    public static ExtensionResponse create(Exception cause,
                                           String message,
                                           ExtensionResponse.Error.ExceptionType exceptionType,
                                           Map<String, Object> state) {
        ExtensionResponse.Error error = new ExtensionResponse.Error(message, System.currentTimeMillis(),
                exceptionType, Arrays.toString(cause.getStackTrace()));
        return new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error,
                null, state);
    }

    /**
     * Creates an error response with exception details, remediation actions, and state
     */
    public static ExtensionResponse create(Exception cause,
                                           String message,
                                           ExtensionResponse.Error.ExceptionType exceptionType,
                                           List<RemediationAction> actions,
                                           String subCatalogRequestName,
                                           Map<String, Object> state) {
        RemediationActions remediationActions = new RemediationActions(actions, subCatalogRequestName);
        ExtensionResponse.Error error = new ExtensionResponse.Error(message, System.currentTimeMillis(),
                exceptionType, Arrays.toString(cause.getStackTrace()));
        return new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error,
                remediationActions, state);
    }

    /**
     * Creates a successful response with result string and files
     */
    public static ExtensionResponse create(String result, List<app.krista.model.base.File> files, String message) {
        log.info("Creating ExtensionResponse with result and {} files: {}", files.size(), message);

        Map<String, Object> values = new java.util.HashMap<>();
        values.put("Result", result);
        if (files != null && !files.isEmpty()) {
            values.put("Files", files);
        }

        return new ExtensionResponse(
            ExtensionResponse.Result.SUCCESS,
            values,
            null,
            null,
            null
        );
    }
}
