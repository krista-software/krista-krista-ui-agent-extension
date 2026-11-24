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

import app.krista.extension.uiautomationagent.events.UiAgentResponseParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InteractiveSession {
    private static final Logger log = LoggerFactory.getLogger(InteractiveSession.class);
    private final String conversationId;
    private final Process process;
    private final BufferedWriter stdin;
    private final Map<String, CompletableFuture<JsonNode>> responseFutures = new ConcurrentHashMap<>();

    public InteractiveSession(String conversationId, Path workingDir, String nodePath, File configFile) throws IOException {
        this.conversationId = conversationId;

        List<String> command = List.of(
            nodePath,
            workingDir.resolve("src/cli/index.js").toString(),
            "--interactive",
            "--config", configFile.getAbsolutePath(),
            "--stdout"
        );

        log.info("Starting interactive session for conversation: {}", conversationId);
        log.info("Command: {}", String.join(" ", command));
        log.info("Working directory: {}", workingDir);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toFile());
        pb.redirectErrorStream(true);
        pb.environment().put("NODE_PATH", workingDir.resolve("node_modules").toString());
        pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", workingDir.resolve(".cache/ms-playwright").toString());
        pb.environment().put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "true");

        this.process = pb.start();

        // Check if process started successfully
        if (!process.isAlive()) {
            throw new IOException("Failed to start Node.js process");
        }

        log.info("Node.js process started successfully with PID: {}", process.pid());

        this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        startReaderThread();
    }

    public void sendPrompt(String commandId, String prompt) throws IOException {
        log.info("Sending prompt with commandId: {} for conversation: {}", commandId, conversationId);
        log.debug("Prompt content: {}", prompt);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();
        json.put("commandId", commandId);
        json.put("conversationId", conversationId);
        json.put("prompt", prompt);

        String jsonString = json.toString();
        log.debug("Sending JSON: {}", jsonString);

        stdin.write(jsonString);
        stdin.write("\n");
        stdin.flush();

        responseFutures.put(commandId, new CompletableFuture<>());
        log.info("Future registered for commandId: {}", commandId);
    }

     public JsonNode waitForResponse(String commandId, long timeoutMs) throws Exception {
        CompletableFuture<JsonNode> future = responseFutures.get(commandId);
        if (future == null) throw new IllegalStateException("No pending request for: " + commandId);
        return future.get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    private void startReaderThread() {
        new Thread(() -> {
            ObjectMapper mapper = new ObjectMapper();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean jsonResponseStarted = false;
                boolean jsonResponseEnded = false;
                StringBuilder jsonBuffer = new StringBuilder();
                
                log.info("Reader thread started for conversation: {}", conversationId);
                while ((line = reader.readLine()) != null) {
                    log.debug("Received line from Node.js: {}", line);
                    
                    // Look for JSON response delimiters
                    if (line.equals("---BEGIN_JSON_RESPONSE---")) {
                        jsonResponseStarted = true;
                        jsonResponseEnded = false;
                        jsonBuffer.setLength(0); // Clear buffer
                        continue;
                    }
                    
                    if (line.equals("---END_JSON_RESPONSE---")) {
                        jsonResponseEnded = true;
                        
                        // Process the complete JSON response
                        String jsonResponse = jsonBuffer.toString().trim();
                        if (!jsonResponse.isEmpty()) {
                            try {
                                JsonNode json = mapper.readTree(jsonResponse);
                                String commandId = json.has("commandId") ? json.get("commandId").asText() : null;
                                if (commandId != null && responseFutures.containsKey(commandId)) {
                                    log.info("Completing future for commandId: {}", commandId);
                                    responseFutures.get(commandId).complete(json);
                                    responseFutures.remove(commandId);
                                } else {
                                    log.warn("Received response without matching commandId: {}", jsonResponse);
                                }
                            } catch (Exception parseEx) {
                                log.error("Failed to parse JSON response: {}", jsonResponse, parseEx);
                            }
                        }
                        
                        // Reset flags
                        jsonResponseStarted = false;
                        jsonResponseEnded = false;
                        continue;
                    }
                    
                    // Only capture lines between the delimiters
                    if (jsonResponseStarted && !jsonResponseEnded) {
                        jsonBuffer.append(line).append("\n");
                    }
                    
                    // Log non-JSON lines for debugging
                    if (!jsonResponseStarted) {
                        log.debug("Node.js output: {}", line);
                    }
                }
                log.info("Reader thread ended for conversation: {}", conversationId);
            } catch (Exception e) {
                log.error("Reader thread error for conversation: {}", conversationId, e);
                responseFutures.values().forEach(future -> future.completeExceptionally(e));
                responseFutures.clear();
            }
        }, "InteractiveSession-" + conversationId).start();
    }

    public void shutdown() {
        try {
            stdin.write("{\"prompt\": \"exit\"}\n");
            stdin.flush();
            stdin.close();
        } catch (IOException ignored) {}

        process.destroy();
        
        // Complete any pending futures
        responseFutures.values().forEach(future -> future.cancel(true));
        responseFutures.clear();
    }
}