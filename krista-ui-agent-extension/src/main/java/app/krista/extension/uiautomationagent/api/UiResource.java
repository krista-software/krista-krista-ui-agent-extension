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

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

/**
 * JAX-RS resource for serving the UI Agent custom tab files.
 * This follows the Krista extension pattern where custom tabs are served via JAX-RS endpoints.
 *
 * NOTE: No @Service annotation needed - only the Application class needs it.
 * This follows the pattern from Outlook extension.
 */
@Path("/")
public class UiResource {

    private static final Logger log = LoggerFactory.getLogger(UiResource.class);

    /**
     * Serves static files for the custom tab UI.
     * Follows the pattern from Outlook extension where files are served via JAX-RS.
     *
     * Path pattern: /ui/{subPath} where subPath can be empty or a file path
     * Full URL: /{jaxrsId}/uiagent/ui/{subPath}
     *
     * @param subPath The sub-path of the file to serve (e.g., "index.html", "css/style.css")
     * @return InputStream of the requested file
     */
    @GET
    @Path("/ui/{subPath:.*}")
    public InputStream serveUiFiles(@PathParam("subPath") String subPath) {
        // Default to index.html if no subPath provided
        String filePath = "ui/" + (subPath == null || subPath.isEmpty() ? "index.html" : subPath);

        log.debug("Serving UI file: {}", filePath);

        InputStream stream = getClass().getClassLoader().getResourceAsStream(filePath);

        if (stream == null) {
            log.warn("File not found: {}", filePath);
        }

        return stream;
    }
}

