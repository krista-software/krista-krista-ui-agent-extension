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

package app.krista.extension.uiautomationagent.service;

import app.krista.infra.esb.ServiceBusConfig;
import app.krista.infra.esb.service.ApiDefinition;
import app.krista.infra.esb.service.ServiceDefinition;
import app.krista.extension.uiautomationagent.esb.dto.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Service definition for UI Agent ESB service.
 * 
 * Defines the API endpoints that the Node.js service will implement.
 * This follows the same pattern as VoiceAgentServiceDefinition.
 * 
 * @author Krista Platform Team
 * @version 1.0
 */
public class UiAgentServiceDefinition implements ServiceDefinition {
    
    public static final int VERSION = 1;
    public static final String SERVICE_NAME = "krista-ui-agent";
    
    private static final Map<String, ApiDefinition> apis = new HashMap<>();
    
    static {
        // Define all API endpoints using Infinispan ProtoStream classes
        apis.put("executePrompt", new ApiDefinition(
            "executePrompt",
            SERVICE_NAME,
            PromptExecuteInput.class,
            PromptExecuteReply.class
        ));

        apis.put("startSession", new ApiDefinition(
            "startSession",
            SERVICE_NAME,
            SessionStartInput.class,
            SessionStartReply.class
        ));

        apis.put("executeSession", new ApiDefinition(
            "executeSession",
            SERVICE_NAME,
            SessionExecuteInput.class,
            SessionExecuteReply.class
        ));

        apis.put("closeSession", new ApiDefinition(
            "closeSession",
            SERVICE_NAME,
            SessionCloseInput.class,
            SessionCloseReply.class
        ));
    }
    
    /**
     * Get service configuration.
     * 
     * @return ServiceBusConfig for the UI Agent service
     */
    public static ServiceBusConfig getConfig() {
        return getConfig(null);
    }
    
    /**
     * Get service configuration with optional config file.
     * 
     * @param configFile Optional ESB config file path
     * @return ServiceBusConfig for the UI Agent service
     */
    public static ServiceBusConfig getConfig(String configFile) {
        ServiceBusConfig config = configFile != null 
            ? new ServiceBusConfig(configFile) 
            : new ServiceBusConfig();
            
        return config
            .setDestinationBaseName(SERVICE_NAME)
            .setDestinationType(ServiceBusConfig.DestinationType.Queue)
            .setVersion(VERSION)
            .setResponseTimeout(Duration.ofMinutes(60)) // 60 minutes for long-running UI automation tasks
            .setSessionCheckoutTimeout(Duration.ofMinutes(10));
    }
    
    @Override
    public ApiDefinition getApiDefinition(String apiName) {
        return apis.get(apiName);
    }
}

