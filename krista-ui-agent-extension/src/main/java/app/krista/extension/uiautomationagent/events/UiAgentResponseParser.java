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

package app.krista.extension.uiautomationagent.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility class for parsing JSON responses from the UI Agent.
 */
public class UiAgentResponseParser {
    private static final Logger log = LoggerFactory.getLogger(UiAgentResponseParser.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Parses a JSON response string into a Map.
     */
    public static Map<String, Object> parseJsonResponse(String jsonResponse) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            result.put("type", "error");
            result.put("error", "Empty response");
            return result;
        }
        
        try {
            JsonNode rootNode = mapper.readTree(jsonResponse);
            
            // Convert JsonNode to Map
            return nodeToMap(rootNode);
        } catch (Exception e) {
            log.error("Error parsing JSON response: {}", e.getMessage(), e);
            result.put("type", "error");
            result.put("error", "Invalid JSON: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Converts a JsonNode to a Map recursively.
     */
    private static Map<String, Object> nodeToMap(JsonNode node) {
        Map<String, Object> result = new HashMap<>();
        
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            
            if (value.isObject()) {
                result.put(key, nodeToMap(value));
            } else if (value.isArray()) {
                // For simplicity, we're not handling arrays in this example
                result.put(key, value.toString());
            } else if (value.isNull()) {
                result.put(key, null);
            } else if (value.isBoolean()) {
                result.put(key, value.asBoolean());
            } else if (value.isNumber()) {
                result.put(key, value.asDouble());
            } else {
                result.put(key, value.asText());
            }
        }
        
        return result;
    }
}