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

package app.krista.extension.uiautomationagent.api;

import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Collections;
import java.util.Set;

/**
 * JAX-RS Application configuration for the UI Agent extension.
 * Registers all REST API resources under the /api path.
 * 
 * NOTE: This class is required by the Krista platform even if there are no API endpoints.
 * Without it, the platform throws NoSuchElementException when trying to access custom tabs.
 */
@ApplicationPath("api")
@ContractsProvided(Application.class)
@Service
public class ApiApplication extends Application {
    
    private static final Logger log = LoggerFactory.getLogger(ApiApplication.class);

    /**
     * Register all JAX-RS resource classes.
     *
     * @return Set of resource classes to be registered
     */
    @Override
    public Set<Class<?>> getClasses() {
        log.debug("Registering JAX-RS resources: UiResource");
        return Set.of(UiResource.class);
    }
}

