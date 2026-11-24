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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InteractiveSessionManager {

    private static final Map<String, InteractiveSession> sessions = new ConcurrentHashMap<>();

    public static synchronized InteractiveSession createOrReuseSession(String conversationId, Path workingDir, String nodePath, File configFile) throws IOException {
        if (sessions.containsKey(conversationId)) {
            return sessions.get(conversationId);
        }

        InteractiveSession session = new InteractiveSession(conversationId, workingDir, nodePath, configFile);
        sessions.put(conversationId, session);
        return session;
    }

    public static InteractiveSession getSession(String conversationId) {
        return sessions.get(conversationId);
    }

    public static void endSession(String conversationId) {
        InteractiveSession session = sessions.remove(conversationId);
        if (session != null) {
            session.shutdown();
        }
    }

    public static boolean sessionExists(String conversationId) {
        return sessions.containsKey(conversationId);
    }
}