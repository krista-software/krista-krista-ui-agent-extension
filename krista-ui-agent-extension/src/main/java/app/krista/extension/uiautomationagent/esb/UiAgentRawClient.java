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

import app.krista.extension.uiautomationagent.proto.*;
import com.google.protobuf.Timestamp;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQExceptionType;
import org.apache.activemq.artemis.api.core.client.*;
import app.krista.ksdk.telemetry.TelemetryMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Raw Artemis Core API Client for UI Agent.
 * 
 * Uses Google Protobuf directly (NOT Infinispan ProtoStream) to communicate
 * with the Node.js UI Agent service.
 * 
 * This bypasses ServiceClientBase and uses raw Artemis Core API to send/receive
 * protobuf-encoded messages with proper ESB properties.
 * 
 * @author Krista Platform Team
 * @version 1.0 (Raw Artemis + Google Protobuf)
 */
public class UiAgentRawClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(UiAgentRawClient.class);

    // ESB Configuration
    private static final String ESB_API_VERSION = "1";
    private static final String REQUEST_QUEUE = "krista-ui-agent-1-IN";

    // Singleton instance and lock (prevents duplicate consumers when INVOKER_LOADED called 3 times)
    private static UiAgentRawClient instance = null;
    private static final Object lock = new Object();

    // Artemis connection components
    private final ServerLocator locator;
    private final ClientSessionFactory sessionFactory;
    private final ClientSession session;
    private final ClientProducer producer;
    // NOTE: No shared consumer - we create one per request for parallel processing

    // Extension identity
    private final String extensionId;
    private final String responseQueue;

    // Timeout configuration
    private final long responseTimeoutMs;

    // Track active requests to prevent session closure during in-flight requests
    private final java.util.concurrent.atomic.AtomicInteger activeRequests = new java.util.concurrent.atomic.AtomicInteger(0);
    private volatile boolean closed = false;

    // Telemetry (set after construction since this is a singleton, not HK2-managed)
    private static volatile TelemetryMetrics telemetryMetrics;

    public static void setTelemetryMetrics(TelemetryMetrics metrics) {
        telemetryMetrics = metrics;
    }

    /**
     * Get singleton instance of UiAgentRawClient.
     * This prevents duplicate consumers when INVOKER_LOADED is called multiple times.
     *
     * @param host Artemis broker host
     * @param port Artemis broker port
     * @param username Artemis username
     * @param password Artemis password
     * @param extensionId Extension ID (used for response queue)
     * @param responseTimeoutMs Response timeout in milliseconds
     * @return Singleton instance
     * @throws Exception if connection fails
     */
    public static UiAgentRawClient getInstance(String host, int port, String username, String password,
                                                String extensionId, long responseTimeoutMs) throws Exception {
        synchronized (lock) {
            if (instance == null) {
                log.info("Creating new UiAgentRawClient instance (first initialization)");
                instance = new UiAgentRawClient(host, port, username, password, extensionId, responseTimeoutMs);
            } else {
                log.info("Reusing existing UiAgentRawClient instance (preventing duplicate consumer)");
            }
        }
        return instance;
    }

    /**
     * Create a new raw Artemis client (private constructor for singleton pattern).
     *
     * @param host Artemis broker host
     * @param port Artemis broker port
     * @param username Artemis username
     * @param password Artemis password
     * @param extensionId Extension ID (used for response queue)
     * @param responseTimeoutMs Response timeout in milliseconds
     * @throws Exception if connection fails
     */
    private UiAgentRawClient(String host, int port, String username, String password,
                            String extensionId, long responseTimeoutMs) throws Exception {
        this.extensionId = extensionId;
        this.responseTimeoutMs = responseTimeoutMs;
        this.responseQueue = String.format("krista-ui-agent-responses-%s-1-OUT", extensionId);
        
        log.info("Initializing UI Agent Raw Client for extension: {}", extensionId);
        log.info("Request queue: {}", REQUEST_QUEUE);
        log.info("Response queue: {}", responseQueue);

        // Create Artemis connection
        String brokerUrl = String.format("tcp://%s:%d", host, port);
        log.info("🔗 Connecting to broker: {}", brokerUrl);
        log.info("   Username: {}", username);

        locator = ActiveMQClient.createServerLocator(brokerUrl);
        locator.setReconnectAttempts(3);
        locator.setRetryInterval(1000);

        log.info("🔗 Creating session factory...");
        sessionFactory = locator.createSessionFactory();

        log.info("🔗 Creating session...");
        session = sessionFactory.createSession(username, password, false, true, true, false, 0);

        log.info("✅ Successfully connected to Artemis broker: {}", brokerUrl);

        // Create producer for sending requests
        log.info("🔗 Creating producer for queue: {}", REQUEST_QUEUE);
        producer = session.createProducer(REQUEST_QUEUE);
        log.info("✅ Producer created successfully for queue: {}", REQUEST_QUEUE);

        // Create response queue (extension's responsibility - unique per extension)
        // CRITICAL: Use MULTICAST routing (pub/sub) so ALL consumers get the message
        // This allows multiple browser tabs/sessions to receive the same response
        // Pattern follows Krista's ServiceClientBase approach
        try {
            session.createQueue(org.apache.activemq.artemis.api.core.QueueConfiguration.of(responseQueue)
                .setAddress(responseQueue)
                .setRoutingType(org.apache.activemq.artemis.api.core.RoutingType.MULTICAST) // Pub/sub - all consumers get message
                .setDurable(false)
                .setMaxConsumers(-1)); // Allow unlimited consumers (multiple tabs/sessions)
            log.info("✅ Created response queue: {} (MULTICAST routing, unlimited consumers)", responseQueue);
        } catch (ActiveMQException e) {
            if (e.getType() == ActiveMQExceptionType.QUEUE_EXISTS) {
                // Queue already exists - check if it has correct configuration
                log.info("⚠️ Response queue already exists: {}", responseQueue);

                // Query queue to check configuration
                try {
                    org.apache.activemq.artemis.api.core.client.ClientSession.QueueQuery queueQuery =
                        session.queueQuery(org.apache.activemq.artemis.api.core.SimpleString.of(responseQueue));

                    log.info("📊 Existing queue configuration:");
                    log.info("   - Max consumers: {}", queueQuery.getMaxConsumers());
                    log.info("   - Consumer count: {}", queueQuery.getConsumerCount());
                    log.info("   - Routing type: {}", queueQuery.getRoutingType());

                    // If queue has wrong configuration (maxConsumers=1 from old version), try to delete and recreate
                    if (queueQuery.getMaxConsumers() == 1) {
                        log.warn("⚠️ Queue has maxConsumers=1 (old configuration), attempting to delete and recreate...");
                        try {
                            session.deleteQueue(org.apache.activemq.artemis.api.core.SimpleString.of(responseQueue));
                            log.info("✅ Deleted old queue with wrong configuration");

                            // Recreate with correct configuration
                            session.createQueue(org.apache.activemq.artemis.api.core.QueueConfiguration.of(responseQueue)
                                .setAddress(responseQueue)
                                .setRoutingType(org.apache.activemq.artemis.api.core.RoutingType.MULTICAST)
                                .setDurable(false)
                                .setMaxConsumers(-1));
                            log.info("✅ Recreated response queue with correct configuration (MULTICAST, unlimited consumers)");
                        } catch (ActiveMQException deleteEx) {
                            log.error("❌ Failed to delete/recreate queue: {}", deleteEx.getMessage());
                            log.error("   Please manually delete the queue from Artemis console and restart the extension");
                            throw deleteEx;
                        }
                    } else {
                        log.info("✅ Queue configuration is correct, will reuse it");
                    }
                } catch (ActiveMQException queryEx) {
                    log.warn("Failed to query queue configuration: {}", queryEx.getMessage());
                    log.info("✅ Will attempt to reuse existing queue");
                }
            } else {
                throw e;
            }
        }

        // Verify queue exists and get info
        try {
            org.apache.activemq.artemis.api.core.client.ClientSession.QueueQuery queueQuery =
                session.queueQuery(org.apache.activemq.artemis.api.core.SimpleString.of(responseQueue));
            log.info("📊 Response queue status:");
            log.info("   - Exists: {}", queueQuery.isExists());
            log.info("   - Consumer count: {}", queueQuery.getConsumerCount());
            log.info("   - Message count: {}", queueQuery.getMessageCount());
            log.info("   - Max consumers: {}", queueQuery.getMaxConsumers());
            log.info("   - Durable: {}", queueQuery.isDurable());
            log.info("   - Auto-delete: {}", queueQuery.isAutoDelete());
        } catch (Exception e) {
            log.warn("Could not query queue status: {}", e.getMessage());
        }

        log.info("✅ Queue configured for MULTICAST routing with unlimited consumers");
        log.info("   Each request will create its own temporary consumer for parallel processing");

        // Start the session
        session.start();
        log.info("✅ Session started, consumer is now active");

        log.info("UI Agent Raw Client initialized successfully");
    }

    /**
     * Execute a one-off prompt (no session).
     * 
     * @param prompt The prompt to execute
     * @param userConfig User configuration
     * @param workspaceId Workspace ID
     * @param accountId Account ID
     * @return PromptExecuteResponse
     * @throws Exception if execution fails
     */
    public PromptExecuteResponse executePrompt(String prompt, UserConfiguration userConfig,
                                                String workspaceId, String accountId) throws Exception {
        String requestId = UUID.randomUUID().toString();
        
        log.info("Executing prompt: {} (requestId: {})", prompt.substring(0, Math.min(50, prompt.length())), requestId);

        // Build extension identity
        ExtensionIdentity extIdentity = buildExtensionIdentity();
        log.info("Extension identity: extensionId={}, name={}, version={}",
                 extIdentity.getExtensionId(), extIdentity.getExtensionName(), extIdentity.getExtensionVersion());

        // Build request
        PromptExecuteRequest request = PromptExecuteRequest.newBuilder()
            .setRequestId(requestId)
            .setExtension(extIdentity)
            .setPrompt(prompt)
            .setUserConfig(userConfig)
            .setWorkspaceId(workspaceId)
            .setAccountId(accountId)
            .setTimestamp(Timestamp.newBuilder()
                .setSeconds(System.currentTimeMillis() / 1000)
                .build())
            .build();

        log.info("Built PromptExecuteRequest: hasExtension={}, hasUserConfig={}, workspaceId={}, accountId={}",
                 request.hasExtension(), request.hasUserConfig(), request.getWorkspaceId(), request.getAccountId());

        // Log the full protobuf message in readable format
        log.info("📦 PromptExecuteRequest (readable format):");
        log.info("  requestId: {}", request.getRequestId());
        log.info("  extension.extensionId: {}", request.getExtension().getExtensionId());
        log.info("  extension.extensionName: {}", request.getExtension().getExtensionName());
        log.info("  extension.extensionVersion: {}", request.getExtension().getExtensionVersion());
        log.info("  prompt: {}", request.getPrompt().substring(0, Math.min(100, request.getPrompt().length())));
        log.info("  workspaceId: {}", request.getWorkspaceId());
        log.info("  accountId: {}", request.getAccountId());
        if (request.hasUserConfig() && request.getUserConfig().hasLlm()) {
            log.info("  userConfig.llm.primaryModel: {}", request.getUserConfig().getLlm().getPrimaryModel());
            log.info("  userConfig.llm.fallbackModel: {}", request.getUserConfig().getLlm().getFallbackModel());
            log.info("  userConfig.llm.providers: {}", request.getUserConfig().getLlm().getProvidersMap().keySet());
        }

        // Send request and wait for response
        byte[] responseBytes = sendRequest("executePrompt", "PromptExecuteRequest",
                                           request.toByteArray(), requestId);
        
        // Deserialize response
        return PromptExecuteResponse.parseFrom(responseBytes);
    }

    /**
     * Start a new browser session.
     * 
     * @param sessionId Optional session ID (null for auto-generated)
     * @param userConfig User configuration
     * @param workspaceId Workspace ID
     * @param accountId Account ID
     * @return SessionStartResponse
     * @throws Exception if start fails
     */
    public SessionStartResponse startSession(String sessionId, UserConfiguration userConfig,
                                             String workspaceId, String accountId) throws Exception {
        String requestId = UUID.randomUUID().toString();
        
        log.info("Starting session (requestId: {})", requestId);
        
        // Build request
        SessionStartRequest.Builder builder = SessionStartRequest.newBuilder()
            .setRequestId(requestId)
            .setExtension(buildExtensionIdentity())
            .setUserConfig(userConfig)
            .setWorkspaceId(workspaceId)
            .setAccountId(accountId)
            .setTimestamp(Timestamp.newBuilder()
                .setSeconds(System.currentTimeMillis() / 1000)
                .build());
        
        if (sessionId != null) {
            builder.setSessionId(sessionId);
        }
        
        SessionStartRequest request = builder.build();

        // Send request and wait for response
        byte[] responseBytes = sendRequest("startSession", "SessionStartRequest",
                                           request.toByteArray(), requestId);

        // Deserialize response
        return SessionStartResponse.parseFrom(responseBytes);
    }

    /**
     * Execute a command in an existing session.
     *
     * @param sessionId Session ID
     * @param prompt Command to execute
     * @param workspaceId Workspace ID
     * @param accountId Account ID
     * @return SessionExecuteResponse
     * @throws Exception if execution fails
     */
    public SessionExecuteResponse executeSession(String sessionId, String prompt,
                                                  String workspaceId, String accountId) throws Exception {
        String requestId = UUID.randomUUID().toString();

        log.info("Executing in session: {} (requestId: {})", sessionId, requestId);

        // Build request
        SessionExecuteRequest request = SessionExecuteRequest.newBuilder()
            .setRequestId(requestId)
            .setExtension(buildExtensionIdentity())
            .setSessionId(sessionId)
            .setPrompt(prompt)
            .setWorkspaceId(workspaceId)
            .setAccountId(accountId)
            .setTimestamp(Timestamp.newBuilder()
                .setSeconds(System.currentTimeMillis() / 1000)
                .build())
            .build();

        // Send request and wait for response; use owner's extension ID for session affinity
        byte[] responseBytes = sendRequest("execute", "SessionExecuteRequest",
                                           request.toByteArray(), requestId, resolveGroupId(sessionId));

        // Deserialize response
        return SessionExecuteResponse.parseFrom(responseBytes);
    }

    /**
     * Close a browser session.
     *
     * @param sessionId Session ID to close
     * @param workspaceId Workspace ID
     * @param accountId Account ID
     * @return SessionCloseResponse
     * @throws Exception if close fails
     */
    public SessionCloseResponse closeSession(String sessionId, String workspaceId, String accountId) throws Exception {
        String requestId = UUID.randomUUID().toString();

        log.info("Closing session: {} (requestId: {})", sessionId, requestId);

        // Build request
        SessionCloseRequest request = SessionCloseRequest.newBuilder()
            .setRequestId(requestId)
            .setExtension(buildExtensionIdentity())
            .setSessionId(sessionId)
            .setWorkspaceId(workspaceId)
            .setAccountId(accountId)
            .setTimestamp(Timestamp.newBuilder()
                .setSeconds(System.currentTimeMillis() / 1000)
                .build())
            .build();

        // Send request and wait for response; use owner's extension ID for session affinity
        byte[] responseBytes = sendRequest("closeSession", "SessionCloseRequest",
                                           request.toByteArray(), requestId, resolveGroupId(sessionId));

        // Deserialize response
        return SessionCloseResponse.parseFrom(responseBytes);
    }

    /**
     * Send a file delete request (fire-and-forget, no response expected).
     *
     * This is a best-effort cleanup operation. If the delete fails, it won't affect
     * the main request flow. The service will handle cleanup of expired files automatically.
     *
     * @param sessionId Session ID for JMSXGroupId affinity
     * @param filename Filename to delete
     */
    public void sendFileDeleteRequest(String sessionId, String filename) {
        try {
            String requestId = UUID.randomUUID().toString();

            log.info("Sending file delete request: sessionId={}, filename={}, requestId={}",
                     sessionId, filename, requestId);

            // Build extension identity
            ExtensionIdentity extIdentity = buildExtensionIdentity();

            // Build request
            FileDeleteRequest request = FileDeleteRequest.newBuilder()
                .setRequestId(requestId)
                .setExtension(extIdentity)
                .setSessionId(sessionId)
                .setFilename(filename)
                .setTimestamp(Timestamp.newBuilder()
                    .setSeconds(System.currentTimeMillis() / 1000)
                    .build())
                .build();

            // Create Artemis message with BYTES_TYPE
            ClientMessage message = session.createMessage(
                org.apache.activemq.artemis.api.core.Message.BYTES_TYPE, true);

            // Set message body (raw protobuf bytes)
            message.getBodyBuffer().writeBytes(request.toByteArray());

            // Set ESB properties
            message.putStringProperty("ESB_API_NAME", "deleteFile");
            message.putStringProperty("ESB_API_VER", ESB_API_VERSION);
            message.putStringProperty("message_type", "FileDeleteRequest");
            message.putStringProperty("extension_id", extensionId);
            message.putStringProperty("invoker_id", app.krista.extension.uiautomationagent.service.InvokerContext.invokerId());
            message.putStringProperty("workspace_id", app.krista.extension.uiautomationagent.service.InvokerContext.workspaceId());

            // Set content type properties
            message.putStringProperty("content-type", "application/x-protobuf");
            message.putStringProperty("esb_contentType", "application/x-protobuf");
            message.putStringProperty("esb_protoMsg", "krista.ui.agent.FileDeleteRequest");

            // Set JMSXGroupId for session affinity (routes to same pod that owns the session)
            String groupId = resolveGroupId(sessionId);
            message.putStringProperty("JMSXGroupID", groupId);
            message.putStringProperty("_AMQ_GROUP_ID", groupId);

            log.info("📤 Sending file delete message (fire-and-forget):");
            log.info("  API Name: deleteFile");
            log.info("  Session ID: {}", sessionId);
            log.info("  Filename: {}", filename);
            log.info("  Request ID: {}", requestId);
            log.info("  JMSXGroupID: {}", groupId);

            // Send message (fire-and-forget - no response expected)
            producer.send(message);

            log.info("✅ File delete request sent successfully");

        } catch (Exception e) {
            // Log error but don't throw - this is best-effort cleanup
            log.warn("⚠️ Failed to send file delete request for file '{}' in session '{}': {}",
                     filename, sessionId, e.getMessage());
            log.debug("File delete error details:", e);
        }
    }

    /**
     * Send a request and wait for response.
     *
     * Creates a temporary consumer for this request to enable parallel processing.
     * Each request gets its own consumer, so multiple conversations can run concurrently.
     *
     * @param apiName ESB API name (e.g., "executePrompt")
     * @param messageType Protobuf message type (e.g., "PromptExecuteRequest")
     * @param requestBytes Serialized protobuf request
     * @param correlationId Correlation ID for request/response matching
     * @return Response bytes
     * @throws Exception if send/receive fails
     */
    /**
     * Extracts the owner extension ID from a session ID.
     *
     * Session IDs are composite keys: "ext-INV_<owner>:<uuid>".
     * When closeSession/executeSession runs under a different invocation (new extensionId),
     * we must use the original owner's ID as JMSXGroupID so Artemis routes to the same pod.
     */
    private String resolveGroupId(String sessionId) {
        if (sessionId != null) {
            int colonIdx = sessionId.lastIndexOf(':');
            if (colonIdx > 0) {
                String prefix = sessionId.substring(0, colonIdx);
                if (prefix.startsWith("ext-")) {
                    return prefix;
                }
            }
        }
        return extensionId;
    }

    private byte[] sendRequest(String apiName, String messageType, byte[] requestBytes,
                              String correlationId) throws Exception {
        return sendRequest(apiName, messageType, requestBytes, correlationId, extensionId);
    }

    private byte[] sendRequest(String apiName, String messageType, byte[] requestBytes,
                              String correlationId, String groupId) throws Exception {
        // Check if client is closed
        if (closed) {
            throw new IllegalStateException("UiAgentRawClient is closed");
        }

        // Track active request
        activeRequests.incrementAndGet();
        long esbStartTime = System.currentTimeMillis();
        boolean esbSuccess = false;
        boolean esbTimeout = false;
        try {
            byte[] result = sendRequestInternal(apiName, messageType, requestBytes, correlationId, groupId);
            esbSuccess = true;
            return result;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                esbTimeout = true;
            }
            throw e;
        } finally {
            activeRequests.decrementAndGet();
            try {
                if (telemetryMetrics != null) {
                    long esbDuration = System.currentTimeMillis() - esbStartTime;
                    java.util.Map<String, String> attrs = app.krista.extension.uiautomationagent.service.InvokerContext.attrs("api_name", apiName);
                    telemetryMetrics.incrementCounter("uiagent.esb.messages.sent", 1, attrs);
                    telemetryMetrics.recordDuration("uiagent.esb.response.duration_ms", esbDuration, attrs);
                    if (!esbSuccess) {
                        telemetryMetrics.incrementCounter("uiagent.esb.errors", 1, attrs);
                    }
                    if (esbTimeout) {
                        telemetryMetrics.incrementCounter("uiagent.esb.timeouts", 1, attrs);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Internal implementation of sendRequest with consumer management.
     */
    private byte[] sendRequestInternal(String apiName, String messageType, byte[] requestBytes,
                                       String correlationId, String groupId) throws Exception {
        // Create Artemis message with BYTES_TYPE (CRITICAL for binary data)
        ClientMessage message = session.createMessage(org.apache.activemq.artemis.api.core.Message.BYTES_TYPE, true);

        // Set message body (raw protobuf bytes)
        message.getBodyBuffer().writeBytes(requestBytes);

        // Set ESB properties (CRITICAL for Node.js service to understand the message)
        message.putStringProperty("ESB_API_NAME", apiName);
        message.putStringProperty("ESB_API_VER", ESB_API_VERSION);

        // Set AMQP standard fields (not application properties)
        message.setCorrelationID(correlationId);
        message.setReplyTo(org.apache.activemq.artemis.api.core.SimpleString.of(responseQueue));

        // Force Artemis to treat as binary (both properties for compatibility)
        message.putStringProperty("content-type", "application/x-protobuf");
        message.putStringProperty("esb_contentType", "application/x-protobuf");
        message.putStringProperty("esb_protoMsg", "krista.ui.agent." + messageType);
        message.putStringProperty("message_type", messageType);

        // Identity properties — read by service to tag metrics with invoker_id / workspace_id labels.
        message.putStringProperty("invoker_id", app.krista.extension.uiautomationagent.service.InvokerContext.invokerId());
        message.putStringProperty("workspace_id", app.krista.extension.uiautomationagent.service.InvokerContext.workspaceId());

        // Set JMSXGroupID for session affinity (routes to same pod that owns the session)
        message.putStringProperty("JMSXGroupID", groupId);
        message.putStringProperty("_AMQ_GROUP_ID", groupId);

        log.info("📤 Sending ESB message:");
        log.info("  API Name: {}", apiName);
        log.info("  Message Type: {}", messageType);
        log.info("  Correlation ID: {}", correlationId);
        log.info("  Reply To: {}", responseQueue);
        log.info("  Content Type: application/x-protobuf");
        log.info("  Proto Message: krista.ui.agent.{}", messageType);
        log.info("  JMSXGroupID: {}", groupId);
        log.info("  Body Size: {} bytes", requestBytes.length);
        log.info("  Body (first 100 bytes hex): {}", bytesToHex(requestBytes, 100));

        // Create a temporary consumer for this request (enables parallel processing)
        // CRITICAL: Use message selector to filter by correlation ID
        // This ensures each consumer only receives responses for its specific request
        ClientConsumer tempConsumer = null;
        try {
            // Create filter expression for correlation ID
            // Note: Artemis Core API uses SimpleString for correlation ID, so we filter on the string representation
            String filter = "JMSCorrelationID = '" + correlationId + "'";
            tempConsumer = session.createConsumer(responseQueue, filter);
            log.info("✅ Created temporary consumer with filter: {} for correlation_id: {}", filter, correlationId);

            // Send message
            log.info("📤 About to send message to queue: {}", REQUEST_QUEUE);
            log.info("   Producer address: {}", producer.getAddress());
            log.info("   Session closed: {}", session.isClosed());
            log.info("   Message size: {} bytes", message.getBodySize());

            producer.send(message);

            log.info("✅ Message sent successfully to queue: {}", REQUEST_QUEUE);
            log.info("   Sent at: {}", new java.util.Date());

            // Wait for response with correlation ID matching
            log.info("⏳ Waiting for response with correlation_id: {} (timeout: {}ms)", correlationId, responseTimeoutMs);

            long startTime = System.currentTimeMillis();
            ClientMessage responseMsg = null;

            while (responseMsg == null && (System.currentTimeMillis() - startTime) < responseTimeoutMs) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.info("⏳ Polling for message (elapsed: {}ms / {}ms)...", elapsed, responseTimeoutMs);

                ClientMessage msg = tempConsumer.receive(5000); // Poll every 5 seconds

            if (msg == null) {
                log.info("⚠️ No message received in last 5 seconds, continuing to wait... (elapsed: {}ms)", elapsed);
                continue;
            }

            // Log all message properties for debugging
            log.info("📨 Received message from queue: {}", responseQueue);
            log.info("  Message ID: {}", msg.getMessageID());
            log.info("  Timestamp: {}", msg.getTimestamp());
            log.info("  Body Size: {} bytes", msg.getBodySize());

            // Log all properties
            for (org.apache.activemq.artemis.api.core.SimpleString propName : msg.getPropertyNames()) {
                Object propValue = msg.getObjectProperty(propName);
                log.info("  Property: {} = {}", propName.toString(), propValue);
            }

            // Check correlation ID (use AMQP standard field, not application property)
            String msgCorrelationId = msg.getCorrelationID() != null ?
                msg.getCorrelationID().toString() : null;
            log.info("  Expected correlation_id: {}", correlationId);
            log.info("  Received correlation_id: {}", msgCorrelationId);

                if (msgCorrelationId != null && msgCorrelationId.equals(correlationId)) {
                    log.info("✅ Correlation ID matches! Processing response.");
                    responseMsg = msg;
                } else {
                    log.warn("⚠️ Correlation ID mismatch! Expected: {}, Got: {}. Discarding message.",
                             correlationId, msgCorrelationId);
                    msg.acknowledge(); // Acknowledge and discard
                }
            }

            if (responseMsg == null) {
                throw new RuntimeException("Response timeout after " + responseTimeoutMs + "ms - no message with matching correlation_id: " + correlationId);
            }

            // Check message type to see if it's an error response
            String responseMessageType = responseMsg.getStringProperty("message_type");

            // Extract response bytes
            int bodySize = responseMsg.getBodySize();
            byte[] responseBytes = new byte[bodySize];
            responseMsg.getBodyBuffer().readBytes(responseBytes);

            // Acknowledge message
            responseMsg.acknowledge();

            log.info("✅ Received and processed response: {} bytes", bodySize);

            // If it's an ErrorResponse, parse it and throw an exception
            if ("ErrorResponse".equals(responseMessageType)) {
                try {
                    ErrorResponse errorResponse = ErrorResponse.parseFrom(responseBytes);
                    String errorMsg = errorResponse.getErrorMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "Unknown error (code: " + errorResponse.getErrorCode() + ")";
                    }
                    throw new RuntimeException(errorMsg);
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw new RuntimeException("Failed to parse error response: " + e.getMessage());
                }
            }

            return responseBytes;

        } finally {
            // Always close the temporary consumer to prevent resource leaks
            if (tempConsumer != null) {
                try {
                    tempConsumer.close();
                    log.info("✅ Closed temporary consumer for correlation_id: {}", correlationId);
                } catch (Exception e) {
                    log.warn("⚠️ Failed to close temporary consumer: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Build ExtensionIdentity for requests.
     *
     * @return ExtensionIdentity
     */
    private ExtensionIdentity buildExtensionIdentity() {
        return ExtensionIdentity.newBuilder()
            .setExtensionId(extensionId)
            .setExtensionName("UI Automation Agent")
            .setExtensionVersion("1.0.0")
            .build();
    }

    /**
     * Close the client and release resources.
     * Queue cleanup is handled manually via Artemis console when extension is removed.
     * Note: No shared consumer to close - each request creates/closes its own temporary consumer.
     */
    @Override
    public void close() throws Exception {
        log.info("Closing UI Agent Raw Client");

        // Mark as closed to prevent new requests
        closed = true;

        // Wait for active requests to complete (max 30 seconds)
        int waitCount = 0;
        while (activeRequests.get() > 0 && waitCount < 60) {
            log.info("⏳ Waiting for {} active request(s) to complete...", activeRequests.get());
            try {
                Thread.sleep(500);
                waitCount++;
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for active requests");
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (activeRequests.get() > 0) {
            log.warn("⚠️ Closing with {} active request(s) still in progress", activeRequests.get());
        } else {
            log.info("✅ All active requests completed");
        }

        if (producer != null) {
            try {
                producer.close();
                log.info("✅ Producer closed");
            } catch (Exception e) {
                log.warn("Failed to close producer: {}", e.getMessage());
            }
        }

        if (session != null) {
            try {
                session.close();
                log.info("✅ Session closed");
            } catch (Exception e) {
                log.warn("Failed to close session: {}", e.getMessage());
            }
        }

        if (sessionFactory != null) {
            try {
                sessionFactory.close();
                log.info("✅ Session factory closed");
            } catch (Exception e) {
                log.warn("Failed to close session factory: {}", e.getMessage());
            }
        }

        if (locator != null) {
            try {
                locator.close();
                log.info("✅ Locator closed");
            } catch (Exception e) {
                log.warn("Failed to close locator: {}", e.getMessage());
            }
        }

        // Clear singleton instance to allow reinitialization
        synchronized (lock) {
            instance = null;
            log.info("✅ Singleton instance cleared");
        }

        log.info("✅ UI Agent Raw Client closed successfully");
    }

    /**
     * Convert bytes to hex string for logging.
     */
    private static String bytesToHex(byte[] bytes, int maxBytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        int length = Math.min(bytes.length, maxBytes);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x ", bytes[i]));
        }
        if (bytes.length > maxBytes) {
            sb.append("...");
        }
        return sb.toString();
    }
}

