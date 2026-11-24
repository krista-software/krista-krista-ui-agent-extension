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

package app.krista.extension.uiautomationagent.esb;

import app.krista.extension.uiautomationagent.esb.dto.*;
import app.krista.infra.esb.ServiceBusConfig;
import app.krista.infra.esb.client.ServiceClientBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UI Agent ESB Client.
 *
 * Extends Krista's ServiceClientBase to communicate with UI Agent Service.
 * Uses Artemis Core protocol on port 61616 (same as voice extensions).
 *
 * Uses Infinispan ProtoStream for serialization (no Google Protobuf wrappers).
 *
 * @author Krista Platform Team
 * @version 3.0 (Using Infinispan ProtoStream)
 */
public class UiAgentClient extends ServiceClientBase {

    private static final Logger log = LoggerFactory.getLogger(UiAgentClient.class);

    private final String invokerId;
    private final String workspaceId;

    /**
     * Create UI Agent client.
     *
     * Architecture:
     * - Request queue: krista-ui-agent-1-IN (shared, created by service)
     * - Response queue: UI-Agent-{invokerId}-{workspaceId}-1-OUT (unique, auto-created by ServiceClientBase)
     *
     * @param config ServiceBusConfig for ESB connection
     * @param invokerId Invoker ID
     * @param workspaceId Workspace ID
     */
    public UiAgentClient(ServiceBusConfig config, String invokerId, String workspaceId) {
        super(config);
        this.invokerId = invokerId;
        this.workspaceId = workspaceId;

        log.info("UI Agent Client initialized for invoker: {}, workspace: {}", invokerId, workspaceId);
    }
    
    /**
     * Execute a one-off prompt (no session).
     *
     * @param input PromptExecuteInput
     * @return PromptExecuteReply
     */
    public PromptExecuteReply executePrompt(PromptExecuteInput input) {
        try {
            log.debug("Executing prompt for workspace: {}", workspaceId);

            // Set workspace (invoker is now part of ExtensionIdentity)
            input.setWorkspaceId(workspaceId);

            // Execute via ESB
            return execAPI("executePrompt", input, PromptExecuteReply.class);

        } catch (Exception e) {
            log.error("Execute prompt failed with exception", e);
            throw new RuntimeException("Execute prompt failed", e);
        }
    }
    
    /**
     * Start a new automation session.
     *
     * @param input SessionStartInput
     * @return SessionStartReply
     */
    public SessionStartReply startSession(SessionStartInput input) {
        try {
            log.debug("Starting session: {}", input.getSessionId());

            // Set workspace (invoker is now part of ExtensionIdentity)
            input.setWorkspaceId(workspaceId);

            return execAPI("startSession", input, SessionStartReply.class);

        } catch (Exception e) {
            log.error("Start session failed with exception", e);
            throw new RuntimeException("Start session failed", e);
        }
    }

    /**
     * Execute a command in an existing session.
     *
     * @param input SessionExecuteInput
     * @return SessionExecuteReply
     */
    public SessionExecuteReply executeSession(SessionExecuteInput input) {
        try {
            log.debug("Executing in session: {}", input.getSessionId());

            // Set workspace (invoker is now part of ExtensionIdentity)
            input.setWorkspaceId(workspaceId);

            return execAPI("executeSession", input, SessionExecuteReply.class);

        } catch (Exception e) {
            log.error("Execute session failed with exception", e);
            throw new RuntimeException("Execute session failed", e);
        }
    }

    /**
     * Close an automation session.
     *
     * @param input SessionCloseInput
     * @return SessionCloseReply
     */
    public SessionCloseReply closeSession(SessionCloseInput input) {
        try {
            log.debug("Closing session: {}", input.getSessionId());

            return execAPI("closeSession", input, SessionCloseReply.class);

        } catch (Exception e) {
            log.error("Close session failed with exception", e);
            throw new RuntimeException("Close session failed", e);
        }
    }
}

