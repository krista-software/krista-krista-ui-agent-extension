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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;
import java.util.UUID;

/**
 * AMQP Client for UI Agent using JMS/AMQP 1.0.
 * 
 * Uses Apache Qpid JMS to communicate with the Node.js UI Agent service via AMQP.
 * This ensures compatibility with the Node.js rhea library.
 * 
 * @author Krista Platform Team
 * @version 2.0 (AMQP 1.0)
 */
public class UiAgentAmqpClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(UiAgentAmqpClient.class);

    // ESB Configuration
    private static final String ESB_API_VERSION = "1";
    private static final String REQUEST_QUEUE = "krista-ui-agent-1-IN";
    
    // JMS connection components
    private final Connection connection;
    private final Session session;
    private final MessageProducer producer;
    private final MessageConsumer consumer;
    private final Queue responseQueue;
    
    // Extension identity
    private final String extensionId;
    
    // Timeout configuration
    private final long responseTimeoutMs;

    /**
     * Create a new AMQP client.
     * 
     * @param host Artemis broker host
     * @param port Artemis AMQP port (5672)
     * @param username Artemis username
     * @param password Artemis password
     * @param extensionId Extension ID (used for response queue)
     * @param responseTimeoutMs Response timeout in milliseconds
     * @throws Exception if connection fails
     */
    public UiAgentAmqpClient(String host, int port, String username, String password, 
                            String extensionId, long responseTimeoutMs) throws Exception {
        this.extensionId = extensionId;
        this.responseTimeoutMs = responseTimeoutMs;
        
        log.info("Initializing UI Agent AMQP Client for extension: {}", extensionId);
        log.info("Request queue: {}", REQUEST_QUEUE);
        
        // Create JNDI context for Qpid JMS
        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        props.setProperty("connectionfactory.myFactoryLookup", 
            String.format("amqp://%s:%d", host, port));
        props.setProperty("queue.requestQueue", REQUEST_QUEUE);
        
        InitialContext context = new InitialContext(props);
        ConnectionFactory factory = (ConnectionFactory) context.lookup("myFactoryLookup");
        
        // Create connection
        connection = factory.createConnection(username, password);
        connection.start();
        
        // Create session
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        
        // Create request queue
        Queue requestQueue = session.createQueue(REQUEST_QUEUE);
        producer = session.createProducer(requestQueue);
        
        // Create response queue (temporary)
        String responseQueueName = String.format("krista-ui-agent-responses-%s-1-OUT", extensionId);
        responseQueue = session.createQueue(responseQueueName);
        consumer = session.createConsumer(responseQueue);
        
        log.info("✅ UI Agent AMQP Client initialized successfully");
        log.info("  Extension ID: {}", extensionId);
        log.info("  Request queue: {}", REQUEST_QUEUE);
        log.info("  Response queue: {}", responseQueueName);
    }

    /**
     * Execute a prompt (one-off, no session).
     * 
     * @param prompt Natural language prompt
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
        
        // Send request and wait for response
        byte[] responseBytes = sendRequest("executePrompt", "PromptExecuteRequest", 
                                           request.toByteArray(), requestId);
        
        // Deserialize response
        return PromptExecuteResponse.parseFrom(responseBytes);
    }

    /**
     * Send a request and wait for response.
     * 
     * @param apiName ESB API name (e.g., "executePrompt")
     * @param messageType Protobuf message type (e.g., "PromptExecuteRequest")
     * @param requestBytes Serialized protobuf request
     * @param correlationId Correlation ID for request/response matching
     * @return Response bytes
     * @throws Exception if send/receive fails
     */
    private byte[] sendRequest(String apiName, String messageType, byte[] requestBytes,
                               String correlationId) throws Exception {
        // Create JMS bytes message
        BytesMessage message = session.createBytesMessage();
        message.writeBytes(requestBytes);
        
        // Set ESB properties
        message.setStringProperty("ESB_API_NAME", apiName);
        message.setStringProperty("ESB_API_VER", ESB_API_VERSION);
        message.setJMSCorrelationID(correlationId);
        message.setJMSReplyTo(responseQueue);
        message.setStringProperty("esb_contentType", "application/x-protobuf");
        message.setStringProperty("esb_protoMsg", "krista.ui.agent." + messageType);
        message.setStringProperty("message_type", messageType);

        log.debug("Sending request: apiName={}, messageType={}, correlationId={}",
                  apiName, messageType, correlationId);

        // Send message
        producer.send(message);

        // Wait for response
        Message responseMsg = consumer.receive(responseTimeoutMs);

        if (responseMsg == null) {
            throw new RuntimeException("Response timeout after " + responseTimeoutMs + "ms");
        }

        if (!(responseMsg instanceof BytesMessage)) {
            throw new RuntimeException("Expected BytesMessage, got: " + responseMsg.getClass().getName());
        }

        BytesMessage bytesResponse = (BytesMessage) responseMsg;
        byte[] responseBytes = new byte[(int) bytesResponse.getBodyLength()];
        bytesResponse.readBytes(responseBytes);

        log.debug("Received response: {} bytes", responseBytes.length);

        return responseBytes;
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
     */
    @Override
    public void close() throws Exception {
        log.info("Closing UI Agent AMQP Client");

        if (consumer != null) {
            consumer.close();
        }
        if (producer != null) {
            producer.close();
        }
        if (session != null) {
            session.close();
        }
        if (connection != null) {
            connection.close();
        }

        log.info("✅ UI Agent AMQP Client closed successfully");
    }
}


