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

import app.krista.extension.event.WaitForEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BrowserSessionManager {
    private static final Logger log = LoggerFactory.getLogger(BrowserSessionManager.class);
    private static BrowserSessionManager instance;
    
    private final Map<String, BrowserSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, WaitForEventListener> listeners = new ConcurrentHashMap<>();
    
    private BrowserSessionManager() {}
    
    public static synchronized BrowserSessionManager getInstance() {
        if (instance == null) {
            instance = new BrowserSessionManager();
        }
        return instance;
    }
    
    public BrowserSession createOrGetSession(String sessionId) {
        return sessions.computeIfAbsent(sessionId, id -> {
            log.info("Creating new browser session with ID: {}", id);
            return new BrowserSession(id);
        });
    }
    
    public void registerListener(String sessionId, WaitForEventListener listener) {
        listeners.put(sessionId, listener);
    }
    
    public WaitForEventListener getListener(String sessionId) {
        return listeners.get(sessionId);
    }
    
    public void completeSession(String sessionId) {
        BrowserSession session = sessions.remove(sessionId);
        if (session != null) {
            session.close();
        }
        listeners.remove(sessionId);
    }
    
    public boolean hasActiveSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }
}