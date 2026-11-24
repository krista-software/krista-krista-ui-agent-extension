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

import app.krista.infra.esb.ServiceBusConfig;

import java.time.Duration;

/**
 * ESB configuration provider for UI Agent Extension.
 *
 * Selects broker configuration based on the UI Agent URL:
 *   - URLs containing ".eng.antbrains"  → engineering environment
 *   - All other URLs                    → production environment
 *
 * MIGRATION PATH:
 * ---------------
 * Phase 1 (Current): Returns values keyed off the UI Agent URL
 * Phase 2 (Future):  Calls platform-provided configuration function
 *
 * @author Krista Platform Team
 * @version 2.0 (Using Krista ESB Infrastructure)
 */
public class UiAgentEsbConfigProvider {

    private record BrokerConfig(String host, int port, String username, String password) {
        static BrokerConfig of(String host, int port, String username, String password) {
            return new BrokerConfig(host, port, username, password);
        }
    }

    private static final BrokerConfig ENG_CONFIG = BrokerConfig.of(
        /* host     */ "artemis-uiagent.antbrains.com",
        /* port     */ 50000,
        /* username */ "admin",
        /* password */ "admin123"
    );

    // TODO: update production broker details
    private static final BrokerConfig PROD_CONFIG = BrokerConfig.of(
        /* host     */ "artemis-uiagent.antbrains.com",
        /* port     */ 50000,
        /* username */ "admin",
        /* password */ "admin123"
    );

    private static BrokerConfig resolveConfig(String uiAgentUrl) {
        if (uiAgentUrl == null || uiAgentUrl.isBlank()) {
            return ENG_CONFIG;
        }
        if (uiAgentUrl.contains(".eng.antbrains")) {
            return ENG_CONFIG;
        }
        if (uiAgentUrl.contains(".krista.app")) {
            return PROD_CONFIG;
        }

        // URL is set but doesn't match any known environment — default to eng to avoid prod risk
        return ENG_CONFIG;
    }

    public static ServiceBusConfig getServiceBusConfig(String uiAgentUrl) {
        BrokerConfig broker = resolveConfig(uiAgentUrl);
        ServiceBusConfig config = new ServiceBusConfig();

        config.setPropertyValue("broker.primary.host", broker.host());
        config.setPropertyValue("broker.primary.port", String.valueOf(broker.port()));
        config.setPropertyValue("broker.user.name",    broker.username());
        config.setPropertyValue("broker.user.pwd",     broker.password());
        config.setPropertyValue("broker.ssl.on", "false");
        config.setPropertyValue("broker.service.support.resmon", "false");

        config.setDestinationBaseName("krista-ui-agent");
        config.setDestinationType(ServiceBusConfig.DestinationType.Queue);
        config.setVersion(1);
        config.setResponseTimeout(Duration.ofMinutes(60));
        config.setSessionCheckoutTimeout(Duration.ofMinutes(10));

        config.setPropertyValue("broker.destination.in.queue.name", "krista-ui-agent-1-IN");

        return config;
    }

    public static ServiceBusConfig getServiceBusConfig(String uiAgentUrl, String invokerId, String workspaceId) {
        BrokerConfig broker = resolveConfig(uiAgentUrl);
        ServiceBusConfig config = new ServiceBusConfig();

        config.setPropertyValue("broker.primary.host", broker.host());
        config.setPropertyValue("broker.primary.port", String.valueOf(broker.port()));
        config.setPropertyValue("broker.user.name",    broker.username());
        config.setPropertyValue("broker.user.pwd",     broker.password());
        config.setPropertyValue("broker.ssl.on", "false");
        config.setPropertyValue("broker.service.support.resmon", "false");

        String uniqueDestinationBase = String.format("krista-ui-agent-INV_%s-ws_%s", invokerId, workspaceId);
        config.setDestinationBaseName(uniqueDestinationBase);
        config.setDestinationType(ServiceBusConfig.DestinationType.Queue);
        config.setVersion(1);
        config.setResponseTimeout(Duration.ofMinutes(60));
        config.setSessionCheckoutTimeout(Duration.ofMinutes(10));

        config.setPropertyValue("broker.destination.in.queue.name", "krista-ui-agent-1-IN");

        return config;
    }

    public static String getHost(String uiAgentUrl) {
        return resolveConfig(uiAgentUrl).host();
    }

    public static int getPort(String uiAgentUrl) {
        return resolveConfig(uiAgentUrl).port();
    }

    public static String getUsername(String uiAgentUrl) {
        return resolveConfig(uiAgentUrl).username();
    }

    public static String getPassword(String uiAgentUrl) {
        return resolveConfig(uiAgentUrl).password();
    }

    public static int getAmqpPort() {
        return 5672;
    }
}

