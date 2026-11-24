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

import java.util.HashMap;
import java.util.Map;

public final class InvokerContext {

    private static volatile String invokerId = "";
    private static volatile String workspaceId = "";

    private InvokerContext() {}

    public static void setContext(String invokerId, String workspaceId) {
        InvokerContext.invokerId = invokerId == null ? "" : invokerId;
        InvokerContext.workspaceId = workspaceId == null ? "" : workspaceId;
    }

    public static String invokerId() {
        return invokerId;
    }

    public static String workspaceId() {
        return workspaceId;
    }

    public static Map<String, String> attrs() {
        Map<String, String> m = new HashMap<>(4);
        m.put("invoker_id", invokerId);
        m.put("workspace_id", workspaceId);
        return m;
    }

    public static Map<String, String> attrs(String k, String v) {
        Map<String, String> m = attrs();
        if (k != null && v != null) m.put(k, v);
        return m;
    }
}
