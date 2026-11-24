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

package app.krista.extension.uiautomationagent.config;

import app.krista.extension.executor.Invoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Provider for accessing ExtensionAttributes throughout the application
 */
@Singleton
public class ExtensionAttributesProvider {
    private static final Logger log = LoggerFactory.getLogger(ExtensionAttributesProvider.class);
    
    private static ExtensionAttributes extensionAttributes;
    private static boolean initialized = false;
    
    @Inject
    public ExtensionAttributesProvider(@Named("self") Invoker invoker) {
        // Don't create instance here - let KristaUiAgentExtension set it explicitly
        // This ensures we use the same instance that gets updated via invokerUpdated()
        log.info("ExtensionAttributesProvider constructor called (instance will be set by KristaUiAgentExtension)");
    }
    
    /**
     * Get the ExtensionAttributes instance
     * 
     * @return ExtensionAttributes instance
     */
    public static ExtensionAttributes getExtensionAttributes() {
        if (extensionAttributes == null) {
            log.error("ExtensionAttributes not initialized. This should not happen in production.");
            throw new IllegalStateException("ExtensionAttributes not initialized");
        }
        return extensionAttributes;
    }
    
    /**
     * Set the ExtensionAttributes instance
     * 
     * @param attrs ExtensionAttributes instance
     */
    public static void setExtensionAttributes(ExtensionAttributes attrs) {
        extensionAttributes = attrs;
        initialized = true;
        log.info("ExtensionAttributes set explicitly");
    }
    
    /**
     * Check if ExtensionAttributes is initialized
     * 
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
