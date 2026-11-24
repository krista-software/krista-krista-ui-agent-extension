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

package app.krista.extension.uiautomationagent.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BrowserSession {
    private static final Logger log = LoggerFactory.getLogger(BrowserSession.class);
    
    private final String sessionId;
    private final long creationTime;
    private long lastAccessTime;
    private final Map<String, Object> sessionData = new HashMap<>();
    
    public BrowserSession(String sessionId) {
        this.sessionId = sessionId;
        this.creationTime = System.currentTimeMillis();
        this.lastAccessTime = this.creationTime;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void updateLastAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    public void setData(String key, Object value) {
        sessionData.put(key, value);
    }
    
    public Object getData(String key) {
        return sessionData.get(key);
    }
    
    public void close() {
        log.info("Closing browser session: {}", sessionId);
        // Add code to close browser resources if needed
    }
}