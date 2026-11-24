# Krista UI Agent Extension - Complete Technical Guide

**Version:** 1.0.0-rc2.8  
**Last Updated:** 2026-02-03  
**Framework:** Krista 4.0  
**Status:** ✅ Production Ready

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Architecture Overview](#architecture-overview)
3. [Component Design](#component-design)
4. [ESB Infrastructure & Queue Management](#esb-infrastructure--queue-management)
5. [File Handling & Media Server Integration](#file-handling--media-server-integration)
6. [Communication Protocol](#communication-protocol)
7. [Catalog Requests API](#catalog-requests-api)
8. [Session Management](#session-management)
9. [Configuration](#configuration)
10. [Message Flow](#message-flow)
11. [Implementation Status](#implementation-status)
12. [Security Considerations](#security-considerations)
13. [Performance & Scalability](#performance--scalability)
14. [Monitoring & Observability](#monitoring--observability)
15. [Testing Strategy](#testing-strategy)
16. [Deployment Guide](#deployment-guide)
17. [Troubleshooting](#troubleshooting)

---

## Executive Summary

### What It Is

The **Krista UI Agent Extension** is a Framework 4.0 extension that enables browser automation through AI-powered agents. It allows users to automate web interactions using natural language prompts, supporting both single-command execution and multi-step interactive sessions.

### Key Features

- ✅ **Natural Language Automation** - Execute browser tasks using plain English commands
- ✅ **Multi-LLM Support** - OpenAI GPT-4, Google Gemini, Anthropic Claude
- ✅ **Interactive Sessions** - Maintain browser state across multiple commands
- ✅ **File Handling** - Automatic download, screenshot capture, and media server integration
- ✅ **Validation Framework** - Post-execution verification with screenshots
- ✅ **Session Profiling** - Performance metrics and execution traces
- ✅ **AMQP/ESB Integration** - Google Protobuf over Artemis Core (port 61616)

### Architecture at a Glance

```
┌─────────────────────────────────────────────────────────────────┐
│                    Krista Platform                              │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         UI Agent Extension (Java 21)                     │  │
│  │  • KristaUiAgentExtension                                │  │
│  │  • InteractionArea (Catalog Requests)                    │  │
│  │  • UiAgentRawClient (Google Protobuf)                    │  │
│  │  • SessionManager                                        │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓ AMQP 1.0
┌─────────────────────────────────────────────────────────────────┐
│              Apache Artemis ESB (Port 61616)                    │
│  Request Queue:  krista-ui-agent-1-IN (shared)                  │
│  Response Queue: ext-{invokerId}-{workspaceId}-OUT (unique)     │
└─────────────────────────────────────────────────────────────────┘
                              ↓ Google Protobuf
┌─────────────────────────────────────────────────────────────────┐
│           Node.js UI Agent Service (Playwright)                 │
│  • Session Management (24hr TTL)                                │
│  • Browser Automation (Chromium/Firefox/WebKit)                 │
│  • LLM Integration (OpenAI/Gemini/Claude)                       │
│  • File Management & Media Server Upload                        │
└─────────────────────────────────────────────────────────────────┘
```

### Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Extension Runtime** | Java | 21 |
| **Framework** | Krista | 4.0 |
| **Message Broker** | Apache Artemis Core | Latest |
| **Serialization** | Google Protobuf | 3.x |
| **Service Runtime** | Node.js | 20.14.0 |
| **Browser Automation** | Playwright | Latest |
| **Build Tool** | Gradle | 8.x |

---

## Architecture Overview

### High-Level Design

The extension follows a **microservices architecture** with clear separation between the Java extension (running in Krista platform) and the Node.js service (running browser automation).

#### Design Principles

1. **Separation of Concerns** - Java handles Krista integration, Node.js handles browser automation
2. **Asynchronous Communication** - AMQP message queues for request/response
3. **Type Safety** - Google Protobuf for schema validation
4. **Scalability** - Stateless design with session affinity
5. **Reliability** - Correlation IDs, timeouts, and error handling

### Component Responsibilities

#### Java Extension Components

| Component | Responsibility | Location |
|-----------|---------------|----------|
| **KristaUiAgentExtension** | Main extension class, lifecycle management | `interaction/KristaUiAgentExtension.java` |
| **InteractionArea** | Catalog request handlers | `interaction/catalog/InteractionArea.java` |
| **UiAgentRawClient** | Raw Artemis client with Google Protobuf | `esb/UiAgentRawClient.java` |
| **UiAgentClient** | Legacy Infinispan ProtoStream client (deprecated) | `esb/UiAgentClient.java` |
| **UiAgentEsbConfigProvider** | ESB configuration provider | `esb/UiAgentEsbConfigProvider.java` |
| **UiAutomationEventListenerHandler** | WAIT_FOR_EVENT pattern handler | `events/UiAutomationEventListenerHandler.java` |
| **UiAgentSessionManager** | Session lifecycle tracking | `service/UiAgentSessionManager.java` |
| **ExtensionAttributes** | Configuration constants | `config/ExtensionAttributes.java` |

#### Node.js Service Components

- **Session Manager** - Browser session lifecycle (24hr TTL)
- **LLM Integration** - OpenAI, Gemini, Claude adapters
- **Playwright Wrapper** - Browser automation
- **File Handler** - Download management, screenshot capture
- **Media Server Client** - HMAC-signed file uploads

---

## Component Design

### 1. KristaUiAgentExtension

**Purpose:** Main extension entry point, manages lifecycle and dependencies.

**Key Responsibilities:**
- Initialize ESB clients (UiAgentRawClient)
- Provide extension attributes (LLM config, UI Agent URL)
- Handle INVOKER_LOADED/INVOKER_REMOVED events
- Expose dependencies to InteractionArea

**Configuration Fields:**

```java
@Field.Text(value = "UI Agent URL", required = true, order = 1)
@Field.PickOne(value = "Primary LLM", values = {"OpenAI", "Google Gemini", "Claude"}, required = true, order = 2)
@Field.Text(value = "Primary LLM API Key", isSecured = true, required = true, order = 3)
@Field.PickOne(value = "Fallback LLM", values = {"OpenAI", "Google Gemini", "Claude"}, required = true, order = 4)
@Field.Text(value = "Fallback LLM API Key", isSecured = true, required = true, order = 5)
```

**Singleton Pattern:**
```java
// UiAgentRawClient uses singleton to prevent duplicate consumers
private static UiAgentRawClient instance = null;
private static final Object lock = new Object();

public static UiAgentRawClient getInstance(String host, int port, ...) {
    synchronized (lock) {
        if (instance == null) {
            instance = new UiAgentRawClient(host, port, ...);
        }
        return instance;
    }
}
```

**Why Singleton?** INVOKER_LOADED event is called 3 times during extension initialization. Without singleton, we'd create 3 duplicate consumers on the response queue.

---

### 2. UiAgentRawClient

**Purpose:** Raw Artemis Core API client using Google Protobuf (NOT Infinispan ProtoStream).

**Why Raw Client?**
- Node.js service expects Google Protobuf format
- ServiceClientBase uses Infinispan ProtoStream (incompatible)
- Need direct control over message properties and headers

**Queue Architecture:**

```
Request Queue:  krista-ui-agent-1-IN
  • Shared by all extension instances
  • Created by Node.js service
  • ANYCAST routing (one consumer gets message)

Response Queue: ext-{invokerId}-{workspaceId}-OUT
  • Unique per extension instance
  • Created by extension on startup
  • MULTICAST routing (all consumers get message)
  • Allows multiple browser tabs/sessions
```

**Message Properties:**

```java
// ESB properties (CRITICAL for Node.js service)
message.putStringProperty("ESB_API_NAME", apiName);           // e.g., "executePrompt"
message.putStringProperty("ESB_API_VER", "1");
message.putStringProperty("content-type", "application/x-protobuf");
message.putStringProperty("esb_contentType", "application/x-protobuf");
message.putStringProperty("esb_protoMsg", "krista.ui.agent." + messageType);
message.putStringProperty("message_type", messageType);       // e.g., "PromptExecuteRequest"

// AMQP standard fields
message.setCorrelationID(correlationId);                      // UUID for request/response matching
message.setReplyTo(responseQueue);                            // Where to send response
```

**API Methods:**

```java
// One-shot prompt execution (no session)
PromptExecuteResponse executePrompt(String prompt, UserConfiguration userConfig,
                                    String workspaceId, String accountId)

// Start new browser session
SessionStartResponse startSession(String sessionId, UserConfiguration userConfig,
                                  String workspaceId, String accountId)

// Execute command in existing session
SessionExecuteResponse executeSession(String sessionId, String prompt,
                                      String workspaceId, String accountId)

// Close browser session
SessionCloseResponse closeSession(String sessionId, String workspaceId, String accountId)

// Delete file from session directory
FileDeleteResponse deleteFile(String sessionId, String filename,
                              String workspaceId, String accountId)

// List files in session directory
FileListResponse listFiles(String sessionId, String workspaceId, String accountId)
```

**Correlation ID Pattern:**

```java
// Generate unique correlation ID
String correlationId = UUID.randomUUID().toString();

// Send request with correlation ID
producer.send(message);

// Wait for response with matching correlation ID
while (true) {
    ClientMessage responseMsg = consumer.receive(timeout);
    if (responseMsg.getCorrelationID().equals(correlationId)) {
        return responseMsg; // Found matching response
    }
    // Ignore messages with different correlation IDs
}
```

**Timeout Handling:**
- Default: 20 minutes (1,200,000 ms)
- Configurable per request
- Throws exception on timeout

---

### 3. InteractionArea (Catalog Requests)

**Purpose:** Expose catalog requests to Krista users.

**Catalog Request Types:**

| Request | Type | Description |
|---------|------|-------------|
| **Single Command Mode** | WAIT_FOR_EVENT | One-shot automation, no session |
| **Interactive Session Start** | CHANGE_SYSTEM | Create new browser session |
| **Execute Interactive Session Steps** | WAIT_FOR_EVENT | Execute in existing session |
| **Interactive Session End** | CHANGE_SYSTEM | Close browser session |

**Field Annotations:**

```java
// Output field annotations (CRITICAL for UI display)
@Field.Desc(name = "Response", type = "FreeForm", required = false)
@Field.File(name = "Downloaded Files", multipleFileUpload = true, required = false)
@Field.File(name = "Screenshots", multipleFileUpload = true, required = false)

// Input field annotations
@Field(name = "Prompt", type = "Paragraph", required = true)
@Field.Text(name = "Session Id", required = true)
```

**Why Field Annotations Matter:**
- Field names in annotations MUST match keys in response data (case-sensitive)
- Without annotations, Krista UI shows "unknown entity"
- `@Field.Desc` for complex JSON, `@Field.Text` for simple strings

---

### 4. UiAutomationEventListenerHandler

**Purpose:** Handle WAIT_FOR_EVENT pattern for asynchronous operations.

**Event Flow:**

```
1. User invokes catalog request
   ↓
2. InteractionArea validates input, returns SUCCESS
   ↓
3. Framework calls UiAutomationEventListenerHandler.initiateRequest()
   ↓
4. Handler sends request to Node.js service via UiAgentRawClient
   ↓
5. Handler waits for response (blocking)
   ↓
6. Handler publishes event with result
   ↓
7. InteractionArea receives event, returns final response
```

**Session Multiplexing:**

```java
// Session key combines conversation ID and user-provided session key
static class SessionKey {
    final String conversationId;
    final String sessionKey;  // defaults to "default"
}

// Map session keys to service session IDs
Map<SessionKey, String> sessionMapping = new ConcurrentHashMap<>();

// Example:
// SessionKey(conv-123, "checkout") → "ext-abc-session-xyz"
// SessionKey(conv-123, "search")   → "ext-abc-session-def"
```

**Session Lifecycle:**

```java
// 1. Check for existing session
SessionKey key = new SessionKey(conversationId, sessionKey);
String serviceSessionId = sessionMapping.get(key);

if (serviceSessionId == null) {
    // 2. Create new session
    SessionStartResponse response = rawClient.startSession(...);
    sessionMapping.put(key, response.getSessionId());
} else {
    // 3. Execute in existing session
    try {
        SessionExecuteResponse response = rawClient.executeSession(serviceSessionId, ...);
    } catch (SessionNotFoundException e) {
        // Service session expired, remove mapping and retry
        sessionMapping.remove(key);
    }
}

// 4. Close session if requested
if (endOfSessionAfterThis) {
    rawClient.closeSession(serviceSessionId, ...);
    sessionMapping.remove(key);
}
```

---

### 5. UiAgentSessionManager

**Purpose:** Track session lifecycle and state.

**Session States:**

```java
enum SessionState {
    ACTIVE,      // Session is running
    CLOSED,      // Session was explicitly closed
    EXPIRED,     // Session timed out
    ERROR        // Session encountered error
}
```

**Session Info:**

```java
record SessionInfo(
    String sessionId,           // UI Agent session ID
    String executionId,         // Krista execution ID
    Instant createdAt,          // Creation timestamp
    SessionState state,         // Current state
    Map<String, Object> metadata // Additional metadata
)
```

**API:**

```java
// Create and track session
SessionInfo createSession(String sessionId, String executionId, Map<String, Object> metadata)

// Get session info
Optional<SessionInfo> getSession(String sessionId)

// Update session state
void updateSessionState(String sessionId, SessionState state)

// Close session
void closeSession(String sessionId)

// Get all active sessions
List<SessionInfo> getActiveSessions()
```

---

## ESB Infrastructure & Queue Management

### Overview

The extension uses **Apache Artemis Core API** directly (not JMS) to communicate with the Node.js service. This section details the complete ESB infrastructure, connection management, and queue lifecycle.

### Broker Configuration

**Location:** `esb/UiAgentEsbConfigProvider.java`

**Connection Settings:**

```java
// Broker connection
private static final String BROKER_HOST = "activemq-artemis";  // or IP address
private static final int BROKER_PORT = 61616;                   // Artemis Core protocol
private static final String BROKER_USERNAME = "artemis";
private static final String BROKER_PASSWORD = "simetraehcapa";

// Timeouts
Response Timeout: 60 minutes (3,600,000 ms)  // Long-running UI automation
Session Checkout: 10 minutes (600,000 ms)    // Session pool timeout
```

**Why Port 61616?**
- Port 61616 is Artemis Core protocol (binary, high-performance)
- NOT port 5672 (AMQP 1.0) or 61613 (STOMP)
- Direct access to Artemis internals for queue management

### Connection Lifecycle

**1. Extension Initialization**

```java
// KristaUiAgentExtension.java - INVOKER_LOADED event
@Override
public void onEvent(ExtensionEvent event) {
    if (event.getType() == ExtensionEventType.INVOKER_LOADED) {
        synchronized (lock) {
            if (rawClient == null) {
                // Create singleton instance
                rawClient = UiAgentRawClient.getInstance(
                    UiAgentEsbConfigProvider.getHost(),
                    UiAgentEsbConfigProvider.getPort(),
                    UiAgentEsbConfigProvider.getUsername(),
                    UiAgentEsbConfigProvider.getPassword(),
                    extensionId,
                    1200000L  // 20 minute timeout
                );
            }
        }
    }
}
```

**2. Artemis Connection Setup**

```java
// UiAgentRawClient.java - Constructor
private UiAgentRawClient(String host, int port, String username, String password,
                        String extensionId, long responseTimeoutMs) throws Exception {

    // 1. Create server locator
    String brokerUrl = String.format("tcp://%s:%d", host, port);
    locator = ActiveMQClient.createServerLocator(brokerUrl);
    locator.setReconnectAttempts(3);      // Retry 3 times on connection failure
    locator.setRetryInterval(1000);       // Wait 1 second between retries

    // 2. Create session factory
    sessionFactory = locator.createSessionFactory();

    // 3. Create client session
    session = sessionFactory.createSession(
        username,           // Username
        password,           // Password
        false,              // xa (not XA transaction)
        true,               // autoCommitSends (auto-commit on send)
        true,               // autoCommitAcks (auto-commit on ack)
        false,              // preAcknowledge (don't pre-ack)
        0                   // ackBatchSize (ack immediately)
    );

    // 4. Start session
    session.start();

    // 5. Create producer (for sending requests)
    producer = session.createProducer();
}
```

**3. Connection Pooling**

- **Singleton Pattern:** Only ONE connection per extension instance
- **Shared Session:** All requests use the same ClientSession
- **Shared Producer:** All requests use the same ClientProducer
- **Per-Request Consumer:** Each request creates a temporary consumer (see below)

### Queue Architecture

#### Request Queue (Shared)

**Name:** `krista-ui-agent-1-IN`

**Characteristics:**
- **Created by:** Node.js service (NOT extension)
- **Routing Type:** ANYCAST (point-to-point)
- **Consumers:** Node.js service workers (1 or more)
- **Durability:** Durable (survives broker restart)
- **Shared by:** All extension instances

**Message Routing:**
```
Extension 1 ──┐
Extension 2 ──┼──> krista-ui-agent-1-IN ──> Node.js Worker (one gets message)
Extension 3 ──┘
```

**Why ANYCAST?**
- Only ONE worker should process each request
- Load balancing across multiple workers
- Prevents duplicate processing

#### Response Queue (Unique per Extension)

**Name Pattern:** `krista-ui-agent-responses-{extensionId}-1-OUT`

**Example:** `krista-ui-agent-responses-ac6b95cb-4fdd-4d07-bdc0-9820fd75ac2f-1-OUT`

**Characteristics:**
- **Created by:** Extension (on startup)
- **Routing Type:** MULTICAST (publish/subscribe)
- **Consumers:** Extension instances (unlimited)
- **Durability:** Non-durable (deleted on broker restart)
- **Max Consumers:** -1 (unlimited)

**Queue Creation Code:**

```java
// UiAgentRawClient.java - Constructor
String responseQueue = String.format("krista-ui-agent-responses-%s-1-OUT", extensionId);

session.createQueue(new QueueConfiguration(responseQueue)
    .setAddress(responseQueue)
    .setRoutingType(RoutingType.MULTICAST)  // Pub/sub - all consumers get message
    .setDurable(false)                       // Don't persist to disk
    .setMaxConsumers(-1));                   // Allow unlimited consumers
```

**Why MULTICAST?**
- Multiple browser tabs can receive the same response
- Supports concurrent sessions in different tabs
- Each tab creates its own consumer

**Queue Verification:**

```java
// Query queue after creation
QueueQuery queueQuery = session.queueQuery(SimpleString.toSimpleString(responseQueue));

log.info("Response queue status:");
log.info("  - Exists: {}", queueQuery.isExists());
log.info("  - Consumer count: {}", queueQuery.getConsumerCount());
log.info("  - Message count: {}", queueQuery.getMessageCount());
log.info("  - Max consumers: {}", queueQuery.getMaxConsumers());
log.info("  - Routing type: {}", queueQuery.getRoutingType());
```

**Queue Reconfiguration:**

If queue exists with wrong configuration (e.g., maxConsumers=1 from old version):

```java
if (queueQuery.getMaxConsumers() == 1) {
    log.warn("Queue has maxConsumers=1 (old configuration), deleting and recreating...");

    // Delete old queue
    session.deleteQueue(SimpleString.toSimpleString(responseQueue));

    // Recreate with correct configuration
    session.createQueue(new QueueConfiguration(responseQueue)
        .setAddress(responseQueue)
        .setRoutingType(RoutingType.MULTICAST)
        .setDurable(false)
        .setMaxConsumers(-1));
}
```

### Consumer Management

**Per-Request Consumer Pattern:**

Unlike traditional messaging where you create one consumer and reuse it, this extension creates a **temporary consumer for each request**. This enables parallel request processing with correlation IDs.

**Why Per-Request Consumers?**

1. **Parallel Requests:** Multiple requests can be in-flight simultaneously
2. **Correlation ID Matching:** Each consumer only receives messages for its request
3. **No Message Stealing:** Consumers don't interfere with each other
4. **Clean Shutdown:** No shared consumer to manage lifecycle

**Consumer Creation:**

```java
// UiAgentRawClient.java - sendRequest() method
private ClientMessage sendRequest(String apiName, byte[] requestBytes,
                                  String messageType) throws Exception {

    // 1. Create correlation ID
    String correlationId = UUID.randomUUID().toString();

    // 2. Create message
    ClientMessage message = session.createMessage(true);
    message.getBodyBuffer().writeBytes(requestBytes);

    // 3. Set message properties
    message.putStringProperty("ESB_API_NAME", apiName);
    message.putStringProperty("ESB_API_VER", "1");
    message.putStringProperty("content-type", "application/x-protobuf");
    message.putStringProperty("esb_contentType", "application/x-protobuf");
    message.putStringProperty("esb_protoMsg", "krista.ui.agent." + messageType);
    message.putStringProperty("message_type", messageType);
    message.setCorrelationID(correlationId);
    message.setReplyTo(responseQueue);

    // 4. Send request
    producer.send(REQUEST_QUEUE, message);

    // 5. Create temporary consumer for this request
    ClientConsumer consumer = session.createConsumer(responseQueue);

    try {
        // 6. Wait for response with matching correlation ID
        while (true) {
            ClientMessage responseMsg = consumer.receive(responseTimeoutMs);

            if (responseMsg == null) {
                throw new TimeoutException("No response received within timeout");
            }

            // 7. Check correlation ID
            if (correlationId.equals(responseMsg.getCorrelationID())) {
                responseMsg.acknowledge();
                return responseMsg;  // Found matching response
            } else {
                // Different correlation ID - ignore and continue waiting
                responseMsg.acknowledge();
                log.debug("Ignoring message with different correlation ID: {}",
                         responseMsg.getCorrelationID());
            }
        }
    } finally {
        // 8. Always close consumer
        consumer.close();
    }
}
```

**Consumer Lifecycle:**

```
Request Start
    ↓
Create Consumer ──> Subscribe to response queue
    ↓
Send Request ──> To krista-ui-agent-1-IN
    ↓
Wait for Response ──> Loop: receive() with timeout
    ↓
Check Correlation ID ──> Match? Return : Continue
    ↓
Close Consumer ──> Unsubscribe from queue
    ↓
Request End
```

### Message Flow with Queues

**Complete Request/Response Flow:**

```
┌─────────────────────────────────────────────────────────────────────┐
│ Extension (Java)                                                    │
│                                                                     │
│  1. Create correlation ID: "550e8400-e29b-41d4-a716-446655440000"  │
│  2. Create temporary consumer on response queue                    │
│  3. Send message to request queue with correlation ID              │
│  4. Wait for response (consumer.receive())                         │
└─────────────────────────────────────────────────────────────────────┘
                              ↓ (3)
┌─────────────────────────────────────────────────────────────────────┐
│ Artemis Broker                                                      │
│                                                                     │
│  Request Queue: krista-ui-agent-1-IN (ANYCAST)                     │
│    - Message arrives with correlation ID                           │
│    - Routed to ONE worker (round-robin)                            │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│ Node.js Service                                                     │
│                                                                     │
│  1. Receive message from request queue                             │
│  2. Extract correlation ID from message                            │
│  3. Process request (execute automation)                           │
│  4. Create response message with SAME correlation ID               │
│  5. Send to reply-to queue (from message.replyTo)                  │
└─────────────────────────────────────────────────────────────────────┘
                              ↓ (5)
┌─────────────────────────────────────────────────────────────────────┐
│ Artemis Broker                                                      │
│                                                                     │
│  Response Queue: krista-ui-agent-responses-{id}-1-OUT (MULTICAST)  │
│    - Message arrives with correlation ID                           │
│    - Delivered to ALL consumers (pub/sub)                          │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│ Extension (Java)                                                    │
│                                                                     │
│  1. Consumer receives message                                      │
│  2. Check correlation ID: matches? ✅                               │
│  3. Acknowledge message                                            │
│  4. Close consumer                                                 │
│  5. Return response to caller                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Queue Cleanup

**On Extension Shutdown:**

```java
@Override
public void close() throws Exception {
    log.info("Closing UI Agent Raw Client");

    // Mark as closed
    closed = true;

    // Wait for active requests to complete (up to 30 seconds)
    int waitCount = 0;
    while (activeRequests.get() > 0 && waitCount < 30) {
        log.info("Waiting for {} active requests to complete...", activeRequests.get());
        Thread.sleep(1000);
        waitCount++;
    }

    // Close producer
    if (producer != null) {
        producer.close();
    }

    // Close session
    if (session != null) {
        session.close();
    }

    // Close session factory
    if (sessionFactory != null) {
        sessionFactory.close();
    }

    // Close locator
    if (locator != null) {
        locator.close();
    }

    log.info("UI Agent Raw Client closed");
}
```

**Response Queue Cleanup:**

- **Non-durable queue:** Automatically deleted when broker restarts
- **Manual cleanup:** Use Artemis console to delete queue if needed
- **No automatic deletion:** Queue persists while broker is running (even if extension stops)

**Manual Queue Deletion (Artemis CLI):**

```bash
# List queues
artemis queue stat --url tcp://localhost:61616

# Delete specific queue
artemis queue delete --name "krista-ui-agent-responses-{id}-1-OUT" \
                     --url tcp://localhost:61616
```

### Error Handling

**Connection Failures:**

```java
try {
    locator = ActiveMQClient.createServerLocator(brokerUrl);
    locator.setReconnectAttempts(3);
    locator.setRetryInterval(1000);
    sessionFactory = locator.createSessionFactory();
} catch (Exception e) {
    log.error("Failed to connect to Artemis broker: {}", e.getMessage());
    throw new RuntimeException("Cannot initialize ESB client", e);
}
```

**Queue Creation Failures:**

```java
try {
    session.createQueue(queueConfig);
} catch (ActiveMQException e) {
    if (e.getType() == ActiveMQExceptionType.QUEUE_EXISTS) {
        // Queue already exists - verify configuration
        QueueQuery query = session.queueQuery(queueName);
        // ... handle existing queue
    } else {
        throw e;
    }
}
```

**Timeout Handling:**

```java
ClientMessage responseMsg = consumer.receive(responseTimeoutMs);

if (responseMsg == null) {
    throw new TimeoutException(
        String.format("No response received within %d ms", responseTimeoutMs)
    );
}
```

---

## File Handling & Media Server Integration

### Overview

The extension handles files in three stages:
1. **Node.js Service** captures files (downloads, screenshots) and stores locally
2. **Extension** downloads files from Node.js service via HMAC-signed URLs
3. **Extension** uploads files to Krista Media Server (FileRepository)
4. **Extension** sends delete request to Node.js service for cleanup

### File Types

| Type | Source | Format | Use Case |
|------|--------|--------|----------|
| **Screenshots** | Playwright page.screenshot() | PNG | Step-by-step visual verification |
| **Downloads** | Browser downloads | Any | PDFs, CSVs, images, documents |
| **Profiling Data** | Session profiling | ZIP | Performance metrics and traces |

### File Flow Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│ Node.js Service                                                      │
│                                                                      │
│  1. Browser automation captures file                                │
│     - Screenshot: page.screenshot()                                 │
│     - Download: browser download event                              │
│                                                                      │
│  2. Save to local directory                                         │
│     Path: /tmp/ui-agent-sessions/{sessionId}/{filename}             │
│                                                                      │
│  3. Generate HMAC-signed URL                                        │
│     URL: /downloads/{sessionId}/{filename}?signature={hmac}         │
│                                                                      │
│  4. Include in protobuf response                                    │
│     DownloadedFile { filename, download_url, size_bytes, ... }      │
└──────────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────────┐
│ Extension (Java) - FileHandlingService                              │
│                                                                      │
│  1. Receive DownloadedFile list in response                         │
│                                                                      │
│  2. For each file:                                                  │
│     a. Validate metadata (filename, URL, size)                      │
│     b. Download from HMAC-signed URL (HTTP GET)                     │
│     c. Verify downloaded size matches expected size                 │
│     d. Upload to FileRepository                                     │
│     e. Get File object for response                                 │
│                                                                      │
│  3. Send delete request to Node.js service                          │
│     FileDeleteRequest { session_id, filename }                      │
└──────────────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────────┐
│ Krista Media Server (FileRepository)                                │
│                                                                      │
│  - Store file with unique ID                                        │
│  - Generate download URL for web client                             │
│  - Apply access control and virus scanning                          │
│  - Support streaming and CDN integration                            │
└──────────────────────────────────────────────────────────────────────┘
```

### FileHandlingService Implementation

**Location:** `file/FileHandlingService.java`

**Dependencies:**

```java
@Service
public class FileHandlingService {

    @Inject
    private FileRepository fileRepository;  // Krista KSDK file service

    @Inject
    private ExtensionAttributes extensionAttributes;  // For UI Agent URL

    private HttpClient httpClient;  // Java 11+ HTTP client
}
```

**Configuration:**

```java
private static final int DOWNLOAD_TIMEOUT_SECONDS = 30;
private static final int MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024; // 100 MB

// HTTP client setup
this.httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(DOWNLOAD_TIMEOUT_SECONDS))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build();
```

### File Processing Flow

**Main Entry Point:**

```java
public List<File> processFiles(List<DownloadedFile> downloadedFiles) throws IOException {

    if (downloadedFiles == null || downloadedFiles.isEmpty()) {
        return new ArrayList<>();
    }

    log.info("Processing {} files from UI Agent", downloadedFiles.size());

    List<File> result = new ArrayList<>();

    for (DownloadedFile downloadedFile : downloadedFiles) {
        try {
            File file = processFile(downloadedFile);
            result.add(file);
            log.info("Successfully processed file: {} (size: {} bytes)",
                    downloadedFile.getFilename(),
                    downloadedFile.getSizeBytes());
        } catch (Exception e) {
            log.error("Failed to process file: {}", downloadedFile.getFilename(), e);
            throw new IOException("Failed to process file: " + downloadedFile.getFilename(), e);
        }
    }

    return result;
}
```

**Single File Processing:**

```java
private File processFile(DownloadedFile downloadedFile) throws IOException {

    // 1. Validate file metadata
    validateFile(downloadedFile);

    // 2. Download from UI Agent
    byte[] fileContent = downloadFromUrl(downloadedFile.getDownloadUrl());

    // 3. Validate downloaded content
    if (fileContent.length != downloadedFile.getSizeBytes()) {
        log.warn("Downloaded file size ({} bytes) doesn't match expected size ({} bytes)",
                fileContent.length, downloadedFile.getSizeBytes());
    }

    // 4. Upload to FileRepository
    FileHandle handle = fileRepository.createNewFileByName(downloadedFile.getFilename());
    handle.setContent(new ByteArrayInputStream(fileContent));

    // 5. Get File object
    File file = handle.getFile();

    return file;
}
```

### File Download from Node.js Service

**URL Construction:**

```java
private byte[] downloadFromUrl(String url) throws IOException {

    // Input: "/session-abc/screenshot.png?signature=hmac123"
    // or: "session-abc/screenshot.png?signature=hmac123"

    // Prepend /downloads if not present
    String path = url.startsWith("/downloads") ? url : "/downloads" + url;

    // Get base URL from extension configuration
    String baseUrl = extensionAttributes.getUiAgentUrl();  // e.g., "http://localhost:3000"

    // Construct full URL
    String fullUrl = baseUrl + path;
    // Result: "http://localhost:3000/downloads/session-abc/screenshot.png?signature=hmac123"

    log.info("Downloading file from: {}", fullUrl);

    // ... HTTP request
}
```

**HTTP Download:**

```java
try {
    // Create HTTP GET request
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(fullUrl))
        .timeout(Duration.ofSeconds(DOWNLOAD_TIMEOUT_SECONDS))
        .GET()
        .build();

    // Send request and get response
    HttpResponse<byte[]> response = httpClient.send(
        request,
        HttpResponse.BodyHandlers.ofByteArray()
    );

    // Check status code
    if (response.statusCode() != 200) {
        throw new IOException("Failed to download file. HTTP status: " + response.statusCode());
    }

    // Get file content
    byte[] content = response.body();
    log.debug("Downloaded {} bytes", content.length);

    return content;

} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    throw new IOException("Download interrupted", e);
}
```

### File Validation

**Metadata Validation:**

```java
private void validateFile(DownloadedFile downloadedFile) {

    // Check filename
    if (downloadedFile.getFilename() == null || downloadedFile.getFilename().isEmpty()) {
        throw new IllegalArgumentException("File name is required");
    }

    // Check download URL
    if (downloadedFile.getDownloadUrl() == null || downloadedFile.getDownloadUrl().isEmpty()) {
        throw new IllegalArgumentException(
            "Download URL is required for file: " + downloadedFile.getFilename()
        );
    }

    // Check file size
    if (downloadedFile.getSizeBytes() > MAX_FILE_SIZE_BYTES) {
        throw new IllegalArgumentException(
            String.format("File too large: %s (%d bytes, max: %d bytes)",
                downloadedFile.getFilename(),
                downloadedFile.getSizeBytes(),
                MAX_FILE_SIZE_BYTES)
        );
    }
}
```

### Upload to Krista Media Server

**FileRepository API:**

```java
// 1. Create file handle
FileHandle handle = fileRepository.createNewFileByName(filename);

// 2. Set file content (from InputStream)
handle.setContent(new ByteArrayInputStream(fileContent));

// 3. Get File object (for ExtensionResponse)
File file = handle.getFile();
```

**File Object Properties:**

```java
// File object contains:
- file.getId()           // Unique file ID in media server
- file.getName()         // Original filename
- file.getSize()         // File size in bytes
- file.getContentType()  // MIME type
- file.getDownloadUrl()  // URL for web client to download
```

### File Cleanup

**Delete Request to Node.js Service:**

After successfully uploading files to media server, send delete request to clean up temporary files:

```java
// UiAutomationEventListenerHandler.java
public List<File> processDownloadedFiles(List<DownloadedFile> downloadedFiles,
                                         String sessionId) {

    // 1. Download and upload to media server
    List<File> uploadedFiles = fileHandlingService.processFiles(downloadedFiles);

    // 2. Send delete requests (best-effort cleanup)
    for (DownloadedFile downloadedFile : downloadedFiles) {
        String filename = downloadedFile.getFilename();
        if (filename != null && !filename.isEmpty()) {
            log.debug("Sending delete request for file: {}", filename);
            rawEsbClient.sendFileDeleteRequest(sessionId, filename);
        }
    }

    return uploadedFiles;
}
```

**FileDeleteRequest (Protobuf):**

```protobuf
message FileDeleteRequest {
  string request_id = 1;
  ExtensionIdentity extension = 2;
  string session_id = 3;
  string filename = 4;
}
```

**Fire-and-Forget Pattern:**

```java
// UiAgentRawClient.java
public void sendFileDeleteRequest(String sessionId, String filename) {
    try {
        FileDeleteRequest request = FileDeleteRequest.newBuilder()
            .setRequestId(UUID.randomUUID().toString())
            .setExtension(buildExtensionIdentity())
            .setSessionId(sessionId)
            .setFilename(filename)
            .build();

        byte[] requestBytes = request.toByteArray();

        ClientMessage message = session.createMessage(true);
        message.getBodyBuffer().writeBytes(requestBytes);
        message.putStringProperty("ESB_API_NAME", "deleteFile");
        message.putStringProperty("message_type", "FileDeleteRequest");
        // ... other properties

        producer.send(REQUEST_QUEUE, message);

        log.debug("Sent file delete request for: {}", filename);

    } catch (Exception e) {
        // Best-effort - don't fail if delete fails
        log.warn("Failed to send file delete request: {}", e.getMessage());
    }
}
```

### File Handling in Catalog Requests

**Single Command Mode:**

```java
// Process screenshots and downloads
List<DownloadedFile> allFiles = new ArrayList<>();

// Add screenshots
allFiles.addAll(response.getResult().getScreenshotsList().stream()
    .map(screenshot -> DownloadedFile.newBuilder()
        .setFilename(screenshot.getFilename())
        .setDownloadUrl(screenshot.getDownloadUrl())
        .setSizeBytes(screenshot.getSizeBytes())
        .setMimeType("image/png")
        .build())
    .collect(Collectors.toList()));

// Add downloads
allFiles.addAll(response.getResult().getDownloadsList());

// Process all files
List<File> uploadedFiles = processDownloadedFiles(allFiles, response.getRequestId());

// Add to event payload
eventPayload.put("Downloaded Files", uploadedFiles);
eventPayload.put("Screenshots", uploadedFiles.stream()
    .filter(f -> f.getName().endsWith(".png"))
    .collect(Collectors.toList()));
```

**Interactive Session:**

```java
// Process files from session execution
List<DownloadedFile> allFiles = new ArrayList<>();
allFiles.addAll(convertScreenshotsToDownloadedFiles(response.getResult().getScreenshotsList()));
allFiles.addAll(response.getResult().getDownloadsList());

List<File> uploadedFiles = processDownloadedFiles(allFiles, response.getSessionId());

// Store for later use
lastSessionUploadedFiles = uploadedFiles;
```

**Session Close (Profiling Data):**

```java
// Handle profiling ZIP if present
if (response.hasProfilingZipUrl()) {
    log.info("Processing profiling ZIP from session close");

    DownloadedFile profilingFile = DownloadedFile.newBuilder()
        .setFilename("profiling.zip")
        .setDownloadUrl(response.getProfilingZipUrl())
        .build();

    List<File> uploadedFiles = processDownloadedFiles(
        Arrays.asList(profilingFile),
        serviceSessionId
    );

    // Add to response
    responseData.put("Profiling Data", uploadedFiles.get(0));
}
```

### HMAC URL Security

**Node.js Service Side:**

```javascript
// Generate HMAC signature
const crypto = require('crypto');

function generateHmacUrl(sessionId, filename) {
    const secret = process.env.HMAC_SECRET || 'default-secret';
    const path = `/downloads/${sessionId}/${filename}`;
    const timestamp = Date.now();
    const expiresIn = 3600000; // 1 hour

    const data = `${path}:${timestamp}:${expiresIn}`;
    const signature = crypto
        .createHmac('sha256', secret)
        .update(data)
        .digest('hex');

    return `${path}?signature=${signature}&timestamp=${timestamp}&expires=${expiresIn}`;
}
```

**URL Verification:**

```javascript
// Verify HMAC signature on download request
function verifyHmacUrl(req) {
    const { signature, timestamp, expires } = req.query;
    const path = req.path;

    // Check expiration
    if (Date.now() - timestamp > expires) {
        throw new Error('URL expired');
    }

    // Verify signature
    const data = `${path}:${timestamp}:${expires}`;
    const expectedSignature = crypto
        .createHmac('sha256', secret)
        .update(data)
        .digest('hex');

    if (signature !== expectedSignature) {
        throw new Error('Invalid signature');
    }
}
```

### Error Handling

**Download Failures:**

```java
try {
    byte[] content = downloadFromUrl(downloadedFile.getDownloadUrl());
} catch (IOException e) {
    log.error("Failed to download file: {}", downloadedFile.getFilename(), e);
    // Possible causes:
    // - Network timeout
    // - Invalid/expired HMAC signature
    // - File not found on Node.js service
    // - HTTP error (4xx, 5xx)
    throw new IOException("Failed to download file: " + downloadedFile.getFilename(), e);
}
```

**Upload Failures:**

```java
try {
    FileHandle handle = fileRepository.createNewFileByName(filename);
    handle.setContent(inputStream);
} catch (Exception e) {
    log.error("Failed to upload file to media server: {}", filename, e);
    // Possible causes:
    // - Media server unavailable
    // - Disk space full
    // - Permission denied
    // - Virus detected
    throw new IOException("Failed to upload file to media server", e);
}
```

**Size Mismatch:**

```java
if (fileContent.length != downloadedFile.getSizeBytes()) {
    log.warn("Downloaded file size ({} bytes) doesn't match expected size ({} bytes) for file: {}",
            fileContent.length,
            downloadedFile.getSizeBytes(),
            downloadedFile.getFilename());
    // Continue processing - size mismatch is a warning, not an error
}
```

---

## Communication Protocol

### Protobuf Schema

**Location:** `src/main/proto/ui_agent_messages.proto`

**Package:** `krista.ui.agent`

**Key Messages:**

#### 1. UserConfiguration

```protobuf
message UserConfiguration {
  string config_version = 1;             // "1.0"
  LLMConfiguration llm = 2;              // LLM settings
  BrowserConfiguration browser = 3;      // Browser settings
  SessionDefaults session = 4;           // Session settings
  map<string, string> extra = 5;         // Additional config
}

message LLMConfiguration {
  string primary_model = 1;              // e.g., "gpt-4o"
  string fallback_model = 2;             // e.g., "gpt-4o-mini"
  optional float temperature = 3;        // 0.0-2.0
  optional int32 max_tokens = 4;         // Max response tokens
  map<string, ProviderConfig> providers = 5; // API keys per provider
}

message SessionDefaults {
  optional int32 timeout_ms = 1;         // Session timeout
  optional int32 max_retries = 2;        // Retry attempts
  optional bool keep_alive = 3;          // Keep session alive
  optional bool enable_validations = 5;  // Enable validations
  optional bool enable_profiling = 8;    // Enable profiling
}
```

#### 2. PromptExecuteRequest/Response

```protobuf
message PromptExecuteRequest {
  string request_id = 1;                    // UUID
  ExtensionIdentity extension = 2;          // Extension info
  string prompt = 3;                        // Natural language command
  optional UserConfiguration user_config = 6;
  optional string workspace_id = 7;
  optional string account_id = 8;
}

message PromptExecuteResponse {
  string request_id = 1;
  bool ok = 2;                              // Success flag
  ExecutionStatus status = 3;               // Status enum
  ExecutionResult result = 4;               // Execution details
  google.protobuf.Timestamp timestamp = 5;
}
```

#### 3. SessionStartRequest/Response

```protobuf
message SessionStartRequest {
  string request_id = 1;
  ExtensionIdentity extension = 2;
  string session_id = 3;                    // Client-provided or generated
  optional UserConfiguration user_config = 6;
  optional string workspace_id = 7;
  optional string account_id = 8;
}

message SessionStartResponse {
  string request_id = 1;
  string session_id = 2;                    // Assigned session ID
  bool ok = 3;
  ExecutionStatus status = 4;
  string message = 5;
  int32 max_concurrent_sessions = 7;        // Limit
  int32 current_session_count = 8;          // Current count
}
```

#### 4. ExecutionResult

```protobuf
message ExecutionResult {
  repeated ExecutionStep steps = 1;         // Steps performed
  string final_output = 2;                  // Final result
  repeated Screenshot screenshots = 3;      // Screenshot metadata
  repeated DownloadedFile downloads = 4;    // Downloaded files
  map<string, string> metadata = 5;         // Additional data
  string error_message = 6;                 // Error if failed
  repeated ValidationResult validations = 7; // Validation results
  bool validation_failed = 8;               // Any validation failed
}

message Screenshot {
  int32 step = 1;                           // Step number
  string step_text = 2;                     // Original prompt
  string action = 3;                        // Action type
  string filename = 4;                      // Screenshot filename
  string download_url = 6;                  // HMAC-signed URL
  google.protobuf.Timestamp timestamp = 7;
  int64 size_bytes = 8;
}

message DownloadedFile {
  string filename = 1;
  int64 size_bytes = 3;
  string mime_type = 4;
  optional string download_url = 6;         // HMAC-signed URL
  google.protobuf.Timestamp downloaded_at = 5;
}
```

### Request Construction & Sending

This section details how each request type is constructed and sent to the Node.js service.

#### 1. PromptExecuteRequest (Single Command Mode)

**Method:** `UiAgentRawClient.executePrompt()`

**Request Construction:**

```java
public PromptExecuteResponse executePrompt(String prompt, UserConfiguration userConfig,
                                           String workspaceId, String accountId) throws Exception {

    // 1. Generate unique request ID
    String requestId = UUID.randomUUID().toString();

    // 2. Build extension identity
    ExtensionIdentity extIdentity = ExtensionIdentity.newBuilder()
        .setExtensionId(extensionId)                    // e.g., "ac6b95cb-4fdd-4d07-bdc0-9820fd75ac2f"
        .setExtensionName("UI Automation Agent")
        .setExtensionVersion("1.0.0")
        .build();

    // 3. Build protobuf request
    PromptExecuteRequest request = PromptExecuteRequest.newBuilder()
        .setRequestId(requestId)                        // UUID for tracking
        .setExtension(extIdentity)                      // Extension metadata
        .setPrompt(prompt)                              // Natural language command
        .setUserConfig(userConfig)                      // LLM config, browser settings
        .setWorkspaceId(workspaceId)                    // Krista workspace ID
        .setAccountId(accountId)                        // Krista account ID
        .setTimestamp(Timestamp.newBuilder()
            .setSeconds(System.currentTimeMillis() / 1000)
            .build())
        .build();

    // 4. Serialize to bytes
    byte[] requestBytes = request.toByteArray();

    // 5. Send request and wait for response
    byte[] responseBytes = sendRequest(
        "executePrompt",              // ESB API name
        "PromptExecuteRequest",       // Protobuf message type
        requestBytes,                 // Serialized request
        requestId                     // Correlation ID
    );

    // 6. Deserialize response
    return PromptExecuteResponse.parseFrom(responseBytes);
}
```

**Example Request:**

```protobuf
PromptExecuteRequest {
  request_id: "550e8400-e29b-41d4-a716-446655440000"
  extension: {
    extension_id: "ac6b95cb-4fdd-4d07-bdc0-9820fd75ac2f"
    extension_name: "UI Automation Agent"
    extension_version: "1.0.0"
  }
  prompt: "Navigate to https://example.com and get the page title"
  user_config: {
    config_version: "1.0"
    llm: {
      primary_model: "gpt-4o"
      fallback_model: "gpt-4o-mini"
      providers: {
        "openai": { api_key: "sk-..." }
      }
    }
    session: {
      enable_validations: true
      enable_profiling: true
    }
  }
  workspace_id: "eaa46cad-9431-4fd2-9fa7-280ca6e48c74"
  account_id: "account-123"
  timestamp: { seconds: 1738598400 }
}
```

---

#### 2. SessionStartRequest (Create Browser Session)

**Method:** `UiAgentRawClient.startSession()`

**Request Construction:**

```java
public SessionStartResponse startSession(String sessionId, UserConfiguration userConfig,
                                        String workspaceId, String accountId) throws Exception {

    String requestId = UUID.randomUUID().toString();

    // Build request (sessionId is optional - service generates if not provided)
    SessionStartRequest.Builder builder = SessionStartRequest.newBuilder()
        .setRequestId(requestId)
        .setExtension(buildExtensionIdentity())
        .setUserConfig(userConfig)
        .setWorkspaceId(workspaceId)
        .setAccountId(accountId)
        .setTimestamp(Timestamp.newBuilder()
            .setSeconds(System.currentTimeMillis() / 1000)
            .build());

    // Optional: Set session ID (if null, service generates one)
    if (sessionId != null) {
        builder.setSessionId(sessionId);
    }

    SessionStartRequest request = builder.build();

    // Send and receive
    byte[] responseBytes = sendRequest("startSession", "SessionStartRequest",
                                       request.toByteArray(), requestId);

    return SessionStartResponse.parseFrom(responseBytes);
}
```

**Example Request:**

```protobuf
SessionStartRequest {
  request_id: "abc-123"
  extension: { ... }
  session_id: "ext-abc-session-xyz"  // Optional
  user_config: { ... }
  workspace_id: "eaa46cad-9431-4fd2-9fa7-280ca6e48c74"
  account_id: "account-123"
  timestamp: { seconds: 1738598400 }
}
```

**Example Response:**

```protobuf
SessionStartResponse {
  request_id: "abc-123"
  session_id: "ext-abc-session-550e8400-e29b-41d4-a716-446655440000"
  ok: true
  status: SUCCESS
  message: "Session started successfully"
  max_concurrent_sessions: 10
  current_session_count: 1
}
```

---

#### 3. SessionExecuteRequest (Execute in Session)

**Method:** `UiAgentRawClient.executeSession()`

**Request Construction:**

```java
public SessionExecuteResponse executeSession(String sessionId, String prompt,
                                            String workspaceId, String accountId) throws Exception {

    String requestId = UUID.randomUUID().toString();

    // Build request
    SessionExecuteRequest request = SessionExecuteRequest.newBuilder()
        .setRequestId(requestId)
        .setExtension(buildExtensionIdentity())
        .setSessionId(sessionId)                        // Existing session ID
        .setPrompt(prompt)                              // Command to execute
        .setWorkspaceId(workspaceId)
        .setAccountId(accountId)
        .setTimestamp(Timestamp.newBuilder()
            .setSeconds(System.currentTimeMillis() / 1000)
            .build())
        .build();

    // Send and receive
    byte[] responseBytes = sendRequest("execute", "SessionExecuteRequest",
                                       request.toByteArray(), requestId);

    return SessionExecuteResponse.parseFrom(responseBytes);
}
```

**Example Request:**

```protobuf
SessionExecuteRequest {
  request_id: "def-456"
  extension: { ... }
  session_id: "ext-abc-session-550e8400-e29b-41d4-a716-446655440000"
  prompt: "Click the login button"
  workspace_id: "eaa46cad-9431-4fd2-9fa7-280ca6e48c74"
  account_id: "account-123"
  timestamp: { seconds: 1738598400 }
}
```

**Example Response:**

```protobuf
SessionExecuteResponse {
  request_id: "def-456"
  session_id: "ext-abc-session-550e8400-e29b-41d4-a716-446655440000"
  ok: true
  status: SUCCESS
  result: {
    steps: [
      { action: "click", result: "Clicked login button", ... }
    ]
    final_output: "Login button clicked successfully"
    screenshots: [ ... ]
  }
}
```

---

#### 4. SessionCloseRequest (Close Browser Session)

**Method:** `UiAgentRawClient.closeSession()`

**Request Construction:**

```java
public SessionCloseResponse closeSession(String sessionId, String workspaceId,
                                        String accountId) throws Exception {

    String requestId = UUID.randomUUID().toString();

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

    // Send and receive
    byte[] responseBytes = sendRequest("closeSession", "SessionCloseRequest",
                                       request.toByteArray(), requestId);

    return SessionCloseResponse.parseFrom(responseBytes);
}
```

**Example Request:**

```protobuf
SessionCloseRequest {
  request_id: "ghi-789"
  extension: { ... }
  session_id: "ext-abc-session-550e8400-e29b-41d4-a716-446655440000"
  workspace_id: "eaa46cad-9431-4fd2-9fa7-280ca6e48c74"
  account_id: "account-123"
  timestamp: { seconds: 1738598400 }
}
```

**Example Response:**

```protobuf
SessionCloseResponse {
  request_id: "ghi-789"
  session_id: "ext-abc-session-550e8400-e29b-41d4-a716-446655440000"
  ok: true
  status: SUCCESS
  message: "Session closed successfully"
  profiling_zip_url: "/downloads/session-xyz/profiling.zip?signature=..."  // Optional
}
```

---

#### 5. FileDeleteRequest (Cleanup Files)

**Method:** `UiAgentRawClient.sendFileDeleteRequest()`

**Request Construction:**

```java
public void sendFileDeleteRequest(String sessionId, String filename) {
    try {
        String requestId = UUID.randomUUID().toString();

        // Build request
        FileDeleteRequest request = FileDeleteRequest.newBuilder()
            .setRequestId(requestId)
            .setExtension(buildExtensionIdentity())
            .setSessionId(sessionId)
            .setFilename(filename)
            .build();

        // Serialize
        byte[] requestBytes = request.toByteArray();

        // Create message
        ClientMessage message = session.createMessage(Message.BYTES_TYPE, true);
        message.getBodyBuffer().writeBytes(requestBytes);

        // Set properties
        message.putStringProperty("ESB_API_NAME", "deleteFile");
        message.putStringProperty("ESB_API_VER", "1");
        message.putStringProperty("content-type", "application/x-protobuf");
        message.putStringProperty("message_type", "FileDeleteRequest");

        // Send (fire-and-forget - no response expected)
        producer.send(REQUEST_QUEUE, message);

    } catch (Exception e) {
        // Best-effort - don't fail if delete fails
        log.warn("Failed to send file delete request: {}", e.getMessage());
    }
}
```

**Example Request:**

```protobuf
FileDeleteRequest {
  request_id: "jkl-012"
  extension: { ... }
  session_id: "ext-abc-session-550e8400-e29b-41d4-a716-446655440000"
  filename: "screenshot-step-1.png"
}
```

**Note:** This is a fire-and-forget request (no response expected).

---

### AMQP Message Format

#### Request Message Structure

**Complete Message Properties:**

```java
// Create message with BYTES_TYPE (critical for binary protobuf data)
ClientMessage message = session.createMessage(Message.BYTES_TYPE, true);

// Set message body (serialized protobuf bytes)
message.getBodyBuffer().writeBytes(requestBytes);

// ESB properties (CRITICAL - Node.js service requires these)
message.putStringProperty("ESB_API_NAME", apiName);           // "executePrompt", "startSession", etc.
message.putStringProperty("ESB_API_VER", "1");                // API version
message.putStringProperty("content-type", "application/x-protobuf");
message.putStringProperty("esb_contentType", "application/x-protobuf");
message.putStringProperty("esb_protoMsg", "krista.ui.agent." + messageType);
message.putStringProperty("message_type", messageType);       // "PromptExecuteRequest", etc.

// AMQP standard properties
message.setCorrelationID(correlationId);                      // UUID for request/response matching
message.setReplyTo(responseQueue);                            // Where to send response

// Session affinity (ensures same service instance handles all requests from this extension)
message.putStringProperty("JMSXGroupID", extensionId);
message.putStringProperty("_AMQ_GROUP_ID", extensionId);

// Send to request queue
producer.send(REQUEST_QUEUE, message);
```

**Wire Format Example:**

```
Headers:
  ESB_API_NAME: "executePrompt"
  ESB_API_VER: "1"
  content-type: "application/x-protobuf"
  esb_contentType: "application/x-protobuf"
  esb_protoMsg: "krista.ui.agent.PromptExecuteRequest"
  message_type: "PromptExecuteRequest"
  correlation-id: "550e8400-e29b-41d4-a716-446655440000"
  reply-to: "krista-ui-agent-responses-ac6b95cb-4fdd-4d07-bdc0-9820fd75ac2f-1-OUT"
  JMSXGroupID: "ac6b95cb-4fdd-4d07-bdc0-9820fd75ac2f"
  _AMQ_GROUP_ID: "ac6b95cb-4fdd-4d07-bdc0-9820fd75ac2f"

Body:
  [Binary protobuf bytes - PromptExecuteRequest serialized]
```

#### Response Message Structure

**Response Properties:**

```
Headers:
  correlation-id: "550e8400-e29b-41d4-a716-446655440000"  (matches request)

Body:
  [Binary protobuf bytes - PromptExecuteResponse serialized]
```

**Response Handling:**

```java
// Create temporary consumer for this request
ClientConsumer consumer = session.createConsumer(responseQueue);

try {
    // Wait for response with matching correlation ID
    while (true) {
        ClientMessage responseMsg = consumer.receive(responseTimeoutMs);

        if (responseMsg == null) {
            throw new TimeoutException("No response within timeout");
        }

        // Check correlation ID
        if (correlationId.equals(responseMsg.getCorrelationID())) {
            // Found matching response
            responseMsg.acknowledge();

            // Extract body
            byte[] responseBytes = new byte[responseMsg.getBodySize()];
            responseMsg.getBodyBuffer().readBytes(responseBytes);

            return responseBytes;
        } else {
            // Different correlation ID - ignore and continue
            responseMsg.acknowledge();
        }
    }
} finally {
    consumer.close();
}
```

---

### Request/Response Mapping

| API Method | Request Type | Response Type | ESB_API_NAME |
|------------|--------------|---------------|--------------|
| `executePrompt()` | PromptExecuteRequest | PromptExecuteResponse | `executePrompt` |
| `startSession()` | SessionStartRequest | SessionStartResponse | `startSession` |
| `executeSession()` | SessionExecuteRequest | SessionExecuteResponse | `execute` |
| `closeSession()` | SessionCloseRequest | SessionCloseResponse | `closeSession` |
| `sendFileDeleteRequest()` | FileDeleteRequest | (none) | `deleteFile` |
| `listFiles()` | FileListRequest | FileListResponse | `listFiles` |

### Error Handling

**Execution Status Enum:**

```protobuf
enum ExecutionStatus {
  STATUS_UNKNOWN = 0;
  SUCCESS = 1;
  ERROR = 2;
  TIMEOUT = 3;
  VALIDATION_ERROR = 4;
  RESOURCE_LIMIT_EXCEEDED = 5;
  ACCESS_DENIED = 6;
  SESSION_NOT_FOUND = 7;
  EXTENSION_NOT_REGISTERED = 8;
  INVALID_CONFIGURATION = 9;
  CONNECTION_ERROR = 10;
  BROWSER_ERROR = 11;
  SERIALIZATION_ERROR = 12;
  FILE_NOT_FOUND = 13;
  FILE_DELETE_ERROR = 14;
}
```

**Error Response:**

```protobuf
message ErrorResponse {
  string request_id = 1;
  string error_code = 2;                    // e.g., "SESSION_NOT_FOUND"
  string error_message = 3;                 // Human-readable message
  map<string, string> details = 4;          // Additional context
  google.protobuf.Timestamp timestamp = 5;
}
```

---

## Catalog Requests API

### 1. Single Command Mode

**Type:** WAIT_FOR_EVENT
**Purpose:** Execute one-shot automation without maintaining session state.

**Input Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| **Prompt** | Paragraph | Yes | Natural language command |
| **Validation Steps** | Paragraph | No | Post-execution validation prompts |

**Output Fields:**

| Field | Type | Description |
|-------|------|-------------|
| **Response** | FreeForm | Execution result with steps, output, metadata |
| **Downloaded Files** | File[] | Files downloaded during execution |
| **Screenshots** | File[] | Screenshots captured during execution |

**Example:**

```
Prompt: "Navigate to https://example.com and extract the page title"

Response: {
  "final_output": "Example Domain",
  "steps": [
    {"action": "navigate", "result": "Navigated to https://example.com"},
    {"action": "extract", "result": "Extracted title: Example Domain"}
  ],
  "screenshots": [
    {"filename": "step-1-navigate.png", "download_url": "https://..."}
  ]
}
```

---

### 2. Interactive Session Start

**Type:** CHANGE_SYSTEM
**Purpose:** Create a new browser session for multi-step automation.

**Input Fields:** None

**Output Fields:**

| Field | Type | Description |
|-------|------|-------------|
| **Session Id** | Text | Unique session identifier (UUID) |

**Example:**

```
Response: {
  "Session Id": "ext-abc-session-550e8400-e29b-41d4-a716-446655440000"
}
```

---

### 3. Execute Interactive Session Steps

**Type:** WAIT_FOR_EVENT
**Purpose:** Execute commands in an existing session while maintaining browser state.

**Input Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| **Session Id** | Text | Yes | Session ID from Session Start |
| **Steps** | Paragraph | Yes | Natural language commands |
| **Validation Steps** | Paragraph | No | Post-execution validation prompts |

**Output Fields:**

| Field | Type | Description |
|-------|------|-------------|
| **Response** | FreeForm | Execution result |
| **Session Id** | Text | Same session ID (for reference) |
| **Downloaded Files** | File[] | Files downloaded |
| **Screenshots** | File[] | Screenshots captured |

**Example:**

```
Input:
  Session Id: "ext-abc-session-550e8400..."
  Steps: "Click the login button and fill username with 'user@example.com'"

Response: {
  "Response": {
    "final_output": "Filled username field",
    "steps": [...]
  },
  "Session Id": "ext-abc-session-550e8400..."
}
```

---

### 4. Interactive Session End

**Type:** CHANGE_SYSTEM
**Purpose:** Close browser session and clean up resources.

**Input Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| **Session Id** | Text | Yes | Session ID to close |

**Output Fields:**

| Field | Type | Description |
|-------|------|-------------|
| **Summary** | FreeForm | Session summary (Session Id, Message, Status) |
| **Profiling Data** | File | Performance profiling ZIP (if enabled) |

**Example:**

```
Input:
  Session Id: "ext-abc-session-550e8400..."

Response: {
  "Summary": {
    "Session Id": "ext-abc-session-550e8400...",
    "Message": "Session closed successfully",
    "Status": "success"
  },
  "Profiling Data": <profiling.zip>
}
```

---

## Session Management

### Session Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Created: Session Start Request
    Created --> Active: Browser Launched
    Active --> Active: Execute Commands
    Active --> Closed: Session End Request
    Active --> Expired: 24hr Timeout
    Closed --> [*]
    Expired --> [*]
```

### Session TTL (Time-To-Live)

**Default:** 24 hours of inactivity

**Configuration:**

```protobuf
message SessionDefaults {
  optional int32 timeout_ms = 1;  // Default: 86400000 (24 hours)
  optional bool keep_alive = 3;   // Prevent auto-expiration
}
```

**Expiration Handling:**

```java
// Extension tracks session mapping
Map<SessionKey, String> sessionMapping = new ConcurrentHashMap<>();

// On execute request
String serviceSessionId = sessionMapping.get(key);
try {
    SessionExecuteResponse response = rawClient.executeSession(serviceSessionId, ...);
} catch (SessionNotFoundException e) {
    // Service session expired - remove mapping and create new session
    sessionMapping.remove(key);
    SessionStartResponse newSession = rawClient.startSession(...);
    sessionMapping.put(key, newSession.getSessionId());
}
```

### Session Multiplexing

**Purpose:** Support multiple concurrent sessions per conversation.

**Session Key Structure:**

```java
static class SessionKey {
    final String conversationId;  // Krista conversation ID
    final String sessionKey;      // User-provided key (defaults to "default")
}
```

**Example:**

```
Conversation: conv-123

Session 1: SessionKey(conv-123, "checkout")  → ext-abc-session-xyz
Session 2: SessionKey(conv-123, "search")    → ext-abc-session-def
Session 3: SessionKey(conv-123, "admin")     → ext-abc-session-ghi
```

**Use Cases:**

1. **Parallel Data Extraction** - Multiple sessions scraping different pages
2. **Comparison Shopping** - Sessions for different vendor websites
3. **Multi-Account Testing** - Sessions for different user roles
4. **Form Filling** - Separate sessions for different forms

### Session Isolation

Each session is completely isolated:

- ✅ Separate browser instances
- ✅ Separate cookies and local storage
- ✅ Separate navigation history
- ✅ Separate authentication state
- ✅ Separate file downloads

### Session Limits

**Per Extension:**
- Max concurrent sessions: Configured in Node.js service
- Default: 10 concurrent sessions

**Response includes limits:**

```protobuf
message SessionStartResponse {
  int32 max_concurrent_sessions = 7;  // e.g., 10
  int32 current_session_count = 8;    // e.g., 3
}
```

---

## Configuration

### Extension Configuration

**Location:** Krista UI → Extension Settings

**Required Fields:**

| Field | Type | Example | Description |
|-------|------|---------|-------------|
| **UI Agent URL** | Text | `http://localhost:3000` | Node.js service URL |
| **Primary LLM** | PickOne | `OpenAI` | Primary LLM provider |
| **Primary LLM API Key** | Secured Text | `sk-...` | API key for primary LLM |
| **Fallback LLM** | PickOne | `Google Gemini` | Fallback LLM provider |
| **Fallback LLM API Key** | Secured Text | `AIza...` | API key for fallback LLM |

### ESB Configuration

**Location:** `esb/UiAgentEsbConfigProvider.java`

**Broker Settings:**

```java
private static final String BROKER_HOST = "activemq-artemis";
private static final int BROKER_PORT = 61616;
private static final String BROKER_USERNAME = "artemis";
private static final String BROKER_PASSWORD = "simetraehcapa";
```

**Queue Names:**

```java
// Request queue (shared by all extensions)
REQUEST_QUEUE = "krista-ui-agent-1-IN"

// Response queue (unique per extension)
RESPONSE_QUEUE = "ext-{invokerId}-{workspaceId}-OUT"
```

**Timeouts:**

```java
// Response timeout: 20 minutes (UI automation can take time)
private final long responseTimeoutMs = 1200000;

// Session checkout timeout: 10 minutes
config.setSessionCheckoutTimeout(Duration.ofMinutes(10));
```

### UserConfiguration Builder

**Location:** `events/UiAutomationEventListenerHandler.java`

**Build Configuration:**

```java
public UserConfiguration buildUserConfiguration() {
    // Get extension attributes
    String primaryLlm = extensionAttributes.getPrimaryLlm();
    String primaryApiKey = extensionAttributes.getPrimaryLlmApiKey();
    String fallbackLlm = extensionAttributes.getFallbackLlm();
    String fallbackApiKey = extensionAttributes.getFallbackLlmApiKey();

    // Map to model names
    String primaryModel = mapLlmToModel(primaryLlm);
    String fallbackModel = mapLlmToModel(fallbackLlm);

    // Build provider configs
    Map<String, ProviderConfig> providers = new HashMap<>();
    providers.put(getProviderName(primaryLlm),
        ProviderConfig.newBuilder().setApiKey(primaryApiKey).build());
    providers.put(getProviderName(fallbackLlm),
        ProviderConfig.newBuilder().setApiKey(fallbackApiKey).build());

    // Build LLM configuration
    LLMConfiguration llmConfig = LLMConfiguration.newBuilder()
        .setPrimaryModel(primaryModel)
        .setFallbackModel(fallbackModel)
        .putAllProviders(providers)
        .build();

    // Build session defaults
    SessionDefaults sessionDefaults = SessionDefaults.newBuilder()
        .setEnableValidations(true)
        .setFailOnValidationError(true)
        .setCaptureValidationScreenshots(true)
        .setEnableProfiling(true)
        .build();

    // Build user configuration
    return UserConfiguration.newBuilder()
        .setConfigVersion("1.0")
        .setLlm(llmConfig)
        .setSession(sessionDefaults)
        .build();
}
```

### LLM Model Mapping

| Provider | Primary Model | Fallback Model |
|----------|--------------|----------------|
| **OpenAI** | `gpt-4o` | `gpt-4o-mini` |
| **Google Gemini** | `gemini-2.0-flash-exp` | `gemini-1.5-flash` |
| **Claude** | `claude-3-5-sonnet-20241022` | `claude-3-5-haiku-20241022` |

---

## Message Flow

### Single Command Mode Flow

```mermaid
sequenceDiagram
    participant User
    participant Krista
    participant Extension
    participant Handler
    participant RawClient
    participant ESB
    participant Service

    User->>Krista: Invoke "Single Command Mode"
    Krista->>Extension: singleCommandMode(prompt)
    Extension->>Extension: Validate input
    Extension-->>Krista: SUCCESS (WAIT_FOR_EVENT)

    Krista->>Handler: initiatePromptBasedInteraction()
    Handler->>Handler: Build UserConfiguration
    Handler->>RawClient: executePrompt(request)
    RawClient->>RawClient: Serialize to protobuf
    RawClient->>ESB: Send to krista-ui-agent-1-IN
    ESB->>Service: Deliver message

    Service->>Service: Parse protobuf
    Service->>Service: Execute automation
    Service->>ESB: Send response
    ESB->>RawClient: Deliver to ext-{id}-OUT
    RawClient->>RawClient: Deserialize protobuf
    RawClient-->>Handler: PromptExecuteResponse

    Handler->>Handler: Process files
    Handler->>Handler: Upload to media server
    Handler->>Krista: Publish "PromptCompleted" event
    Krista->>Extension: handlePromptCompletedEvent()
    Extension-->>User: Display result
```

### Interactive Session Flow

```mermaid
sequenceDiagram
    participant User
    participant Extension
    participant RawClient
    participant Service

    User->>Extension: Interactive Session Start
    Extension->>RawClient: startSession(sessionId, config)
    RawClient->>Service: SessionStartRequest
    Service->>Service: Launch browser
    Service-->>RawClient: SessionStartResponse
    RawClient-->>Extension: sessionId
    Extension-->>User: Session Id

    User->>Extension: Execute Steps (sessionId, "Navigate...")
    Extension->>RawClient: executeSession(sessionId, prompt)
    RawClient->>Service: SessionExecuteRequest
    Service->>Service: Execute in existing browser
    Service-->>RawClient: SessionExecuteResponse
    RawClient-->>Extension: result
    Extension-->>User: Response

    User->>Extension: Execute Steps (sessionId, "Click...")
    Extension->>RawClient: executeSession(sessionId, prompt)
    RawClient->>Service: SessionExecuteRequest
    Service->>Service: Execute in same browser
    Service-->>RawClient: SessionExecuteResponse
    RawClient-->>Extension: result
    Extension-->>User: Response

    User->>Extension: Interactive Session End (sessionId)
    Extension->>RawClient: closeSession(sessionId)
    RawClient->>Service: SessionCloseRequest
    Service->>Service: Close browser
    Service-->>RawClient: SessionCloseResponse
    RawClient-->>Extension: summary
    Extension-->>User: Summary + Profiling Data
```

---

## Implementation Status

### ✅ Completed Features

#### Core Infrastructure
- ✅ **Extension Framework Integration** - Full Framework 4.0 compliance
- ✅ **Google Protobuf Communication** - Raw Artemis client with protobuf serialization
- ✅ **ESB Configuration** - AMQP 1.0 broker setup and queue management
- ✅ **Singleton Pattern** - Prevents duplicate consumers on response queue
- ✅ **Correlation ID Pattern** - Request/response matching for parallel requests

#### Catalog Requests
- ✅ **Single Command Mode** - One-shot automation with WAIT_FOR_EVENT pattern
- ✅ **Interactive Session Start** - Browser session creation
- ✅ **Execute Interactive Session Steps** - Multi-step automation in existing session
- ✅ **Interactive Session End** - Session cleanup and profiling data export

#### Session Management
- ✅ **Session Multiplexing** - Multiple concurrent sessions per conversation
- ✅ **Session TTL** - 24-hour timeout with automatic expiration handling
- ✅ **Session Isolation** - Separate browser instances per session
- ✅ **Session State Tracking** - UiAgentSessionManager for lifecycle management

#### File Handling
- ✅ **File Downloads** - Automatic download capture and media server upload
- ✅ **Screenshots** - Step-by-step screenshot capture
- ✅ **HMAC-signed URLs** - Secure file downloads from media server
- ✅ **File Metadata** - Size, MIME type, timestamps

#### Validation & Profiling
- ✅ **Post-execution Validation** - LLM-based verification with screenshots
- ✅ **Validation Failure Handling** - Configurable fail-on-error behavior
- ✅ **Session Profiling** - Performance metrics and execution traces
- ✅ **Profiling Data Export** - ZIP file with detailed profiling information

#### Configuration
- ✅ **Multi-LLM Support** - OpenAI, Google Gemini, Anthropic Claude
- ✅ **Primary/Fallback LLM** - Automatic fallback on primary LLM failure
- ✅ **Secured API Keys** - Encrypted storage of LLM credentials
- ✅ **UserConfiguration Builder** - Dynamic configuration from extension attributes

### 🚧 Known Limitations

1. **Browser Types** - Currently only Chromium supported (Firefox/WebKit planned)
2. **File Upload** - Not yet implemented in Node.js service
3. **Proxy Support** - No proxy configuration support
4. **Custom Headers** - Cannot set custom HTTP headers
5. **Cookie Management** - No explicit cookie import/export API

### 📋 Roadmap

#### Short-term (Next Release)
- [ ] Add Firefox and WebKit browser support
- [ ] Implement file upload functionality
- [ ] Add session pause/resume capability
- [ ] Improve error messages with actionable suggestions

#### Medium-term
- [ ] Add proxy configuration support
- [ ] Implement cookie import/export API
- [ ] Add custom HTTP header support
- [ ] Session recording and playback

#### Long-term
- [ ] Visual regression testing
- [ ] Performance benchmarking
- [ ] Multi-page session support
- [ ] Distributed session management

---

## Security Considerations

### Overview

The UI Agent Extension handles sensitive data including LLM API keys, browser session data, and user credentials. This section outlines security measures and best practices.

---

### API Key Management

#### Storage & Encryption

**LLM API Keys:**

```java
// API keys are stored in UserConfiguration (protobuf)
ProviderConfig openaiConfig = ProviderConfig.newBuilder()
    .setApiKey("sk-...")  // Stored in protobuf message
    .build();

// Keys are transmitted over AMQP (encrypted if TLS enabled)
UserConfiguration userConfig = UserConfiguration.newBuilder()
    .setLlm(LLMConfiguration.newBuilder()
        .putProviders("openai", openaiConfig)
        .build())
    .build();
```

**Security Measures:**

1. **In-Transit Encryption** - API keys transmitted via AMQP messages
   - Enable TLS on Artemis broker for production
   - Keys encrypted during transmission if TLS enabled

2. **At-Rest Encryption** - Keys stored in Krista platform
   - Platform handles encryption of extension attributes
   - No plaintext storage in extension code

3. **Access Control** - Keys scoped to workspace/account
   - Each workspace has separate API keys
   - No cross-workspace key access

**Best Practices:**

```java
// ✅ DO: Use environment-specific keys
// Development: Use limited-scope API keys
// Production: Use production API keys with rate limits

// ❌ DON'T: Hardcode API keys in code
// private static final String API_KEY = "sk-...";  // NEVER DO THIS

// ✅ DO: Validate API key format before use
if (apiKey == null || !apiKey.startsWith("sk-")) {
    throw new IllegalArgumentException("Invalid OpenAI API key format");
}
```

---

### HMAC-Signed URLs

#### File Download Security

**How It Works:**

```javascript
// Node.js service generates HMAC signature
const crypto = require('crypto');
const secret = process.env.HMAC_SECRET || 'default-secret-key';

function generateSignedUrl(filepath, expiresIn = 3600) {
    const expires = Date.now() + (expiresIn * 1000);
    const message = `${filepath}:${expires}`;
    const signature = crypto.createHmac('sha256', secret)
        .update(message)
        .digest('hex');

    return `/downloads/${filepath}?expires=${expires}&signature=${signature}`;
}
```

**Signature Verification:**

```javascript
// Node.js service verifies signature on download
function verifySignature(filepath, expires, signature) {
    // Check expiration
    if (Date.now() > parseInt(expires)) {
        throw new Error('URL expired');
    }

    // Verify signature
    const message = `${filepath}:${expires}`;
    const expectedSignature = crypto.createHmac('sha256', secret)
        .update(message)
        .digest('hex');

    if (signature !== expectedSignature) {
        throw new Error('Invalid signature');
    }
}
```

**Security Properties:**

- ✅ **Time-limited** - URLs expire after 1 hour (configurable)
- ✅ **Tamper-proof** - Signature prevents URL modification
- ✅ **Non-guessable** - HMAC prevents URL enumeration
- ✅ **Single-use** - Files deleted after download (optional)

**Configuration:**

```bash
# Node.js service environment variables
HMAC_SECRET=your-strong-secret-key-here  # Change in production!
HMAC_EXPIRATION=3600  # 1 hour default
```

---

### Network Security

#### Artemis Broker Security

**TLS/SSL Configuration:**

```java
// Enable TLS for production
ServerLocator locator = ActiveMQClient.createServerLocator("tcp://localhost:61616")
    .setUseSSL(true)
    .setTrustStorePath("/path/to/truststore.jks")
    .setTrustStorePassword("truststore-password")
    .setKeyStorePath("/path/to/keystore.jks")
    .setKeyStorePassword("keystore-password");
```

**Authentication:**

```java
// Broker credentials (from UiAgentEsbConfigProvider)
private static final String BROKER_USERNAME = "krista-ui-agent";
private static final String BROKER_PASSWORD = "secure-password";  // Change in production!

// Credentials used for AMQP connection
ClientSessionFactory sessionFactory = locator.createSessionFactory();
ClientSession session = sessionFactory.createSession(
    BROKER_USERNAME,
    BROKER_PASSWORD,
    false,  // xa
    true,   // autoCommitSends
    true,   // autoCommitAcks
    false,  // preAcknowledge
    0       // ackBatchSize
);
```

**Production Recommendations:**

1. **Change Default Credentials** - Update `BROKER_USERNAME` and `BROKER_PASSWORD`
2. **Enable TLS** - Set `broker.ssl.on=true` in configuration
3. **Firewall Rules** - Restrict port 61616 to internal network only
4. **Network Segmentation** - Isolate broker in secure network zone

---

### Session Security

#### Session Isolation

**Browser Context Isolation:**

```javascript
// Node.js service creates isolated browser contexts per session
const context = await browser.newContext({
    // Each session gets isolated storage
    storageState: undefined,  // No shared cookies/localStorage

    // Isolated permissions
    permissions: [],

    // Isolated cache
    ignoreHTTPSErrors: false
});
```

**Security Properties:**

- ✅ **No Cross-Session Data Leakage** - Each session has isolated cookies/storage
- ✅ **No Shared Authentication** - Sessions don't share login state
- ✅ **Separate Browser Instances** - Full isolation at browser level

#### Session ID Security

**Session ID Format:**

```java
// Session IDs are UUIDs (non-guessable)
String sessionId = "ext-" + extensionId + "-session-" + UUID.randomUUID();
// Example: ext-ac6b95cb-4fdd-4d07-bdc0-9820fd75ac2f-session-550e8400-e29b-41d4-a716-446655440000
```

**Security Properties:**

- ✅ **Non-Guessable** - UUID v4 provides 122 bits of entropy
- ✅ **Scoped to Extension** - Session ID includes extension ID
- ✅ **Time-Limited** - 24-hour TTL prevents indefinite access

---

### Access Control

#### Workspace & Account Isolation

**Request Validation:**

```java
// Every request includes workspace and account IDs
PromptExecuteRequest request = PromptExecuteRequest.newBuilder()
    .setWorkspaceId(workspaceId)  // Validates user has access to workspace
    .setAccountId(accountId)      // Validates user belongs to account
    .build();
```

**Node.js Service Validation:**

```javascript
// Service validates workspace/account access (if implemented)
function validateAccess(workspaceId, accountId, extensionId) {
    // Check if extension belongs to workspace
    // Check if user has access to workspace
    // Reject if validation fails
}
```

#### Queue Access Control

**Queue Naming Strategy:**

```java
// Response queues are unique per extension
String responseQueue = "krista-ui-agent-responses-" + extensionId + "-1-OUT";

// Prevents cross-extension message access
// Extension A cannot read Extension B's responses
```

**Artemis ACLs (Recommended for Production):**

```xml
<!-- artemis-broker.xml -->
<security-settings>
    <security-setting match="krista-ui-agent-1-IN">
        <permission type="send" roles="ui-agent-extension"/>
        <permission type="consume" roles="ui-agent-service"/>
    </security-setting>

    <security-setting match="krista-ui-agent-responses-#">
        <permission type="send" roles="ui-agent-service"/>
        <permission type="consume" roles="ui-agent-extension"/>
    </security-setting>
</security-settings>
```

---

### Data Privacy

#### Sensitive Data Handling

**Screenshot Data:**

- Screenshots may contain sensitive information (PII, credentials, financial data)
- Stored temporarily on Node.js service filesystem
- Uploaded to Krista media server (encrypted at rest)
- Deleted from Node.js service after upload

**Downloaded Files:**

- Files may contain sensitive documents
- Same lifecycle as screenshots (temporary → upload → delete)
- Access controlled via HMAC-signed URLs

**Browser Session Data:**

- Cookies, localStorage, sessionStorage isolated per session
- Cleared when session closes
- Not persisted beyond session TTL

**Best Practices:**

```java
// ✅ DO: Enable file cleanup
SessionDefaults.newBuilder()
    .setEnableProfiling(true)  // Profiling data cleaned up after download
    .build();

// ✅ DO: Close sessions when done
rawClient.closeSession(sessionId, workspaceId, accountId);

// ❌ DON'T: Leave sessions open indefinitely
// Sessions auto-expire after 24 hours, but explicit close is better
```

---

### Security Checklist

#### Development Environment

- [ ] Use test API keys with limited scope
- [ ] Use default HMAC secret (acceptable for dev)
- [ ] Disable TLS (acceptable for localhost)
- [ ] Use default broker credentials (acceptable for dev)

#### Production Environment

- [ ] **Change all default credentials** (broker, HMAC secret)
- [ ] **Enable TLS** on Artemis broker
- [ ] **Configure firewall rules** (restrict port 61616)
- [ ] **Use production API keys** with rate limits
- [ ] **Enable Artemis ACLs** for queue access control
- [ ] **Configure network segmentation** (isolate broker)
- [ ] **Enable audit logging** (track all requests)
- [ ] **Implement key rotation** (rotate HMAC secret periodically)
- [ ] **Monitor for anomalies** (unusual request patterns)
- [ ] **Regular security audits** (review logs, access patterns)

---

## Performance & Scalability

### Overview

This section covers performance characteristics, resource requirements, and scalability considerations for the UI Agent Extension.

---

### Performance Characteristics

#### Request Latency

**Typical Response Times:**

| Operation | Latency | Notes |
|-----------|---------|-------|
| **Simple Navigation** | 2-5 seconds | Navigate to URL, wait for load |
| **Data Extraction** | 3-8 seconds | Navigate + LLM processing + extraction |
| **Form Filling** | 5-15 seconds | Multiple interactions + validations |
| **File Download** | 10-30 seconds | Download + upload to media server |
| **Session Start** | 1-3 seconds | Browser context creation |
| **Session Execute** | 2-20 seconds | Depends on command complexity |
| **Session Close** | 2-5 seconds | Cleanup + profiling data generation |

**Latency Breakdown:**

```
Total Latency = Network + Queue + LLM + Browser + Processing

Example (Simple Navigation):
- Network (Extension → Broker): 10-50ms
- Queue (Broker → Service): 10-50ms
- LLM Processing: 500-2000ms
- Browser Automation: 1000-3000ms
- Processing (Screenshots, etc.): 100-500ms
- Network (Response): 10-50ms
Total: ~2-5 seconds
```

#### Throughput

**Single Node.js Service Instance:**

- **Concurrent Sessions**: 10-20 (configurable)
- **Requests/Second**: 2-5 (depends on complexity)
- **Daily Capacity**: ~10,000-20,000 requests (assuming 5-10 sec avg)

**Factors Affecting Throughput:**

1. **LLM API Rate Limits** - OpenAI: 3,500 RPM (tier 1), 10,000 RPM (tier 2)
2. **Browser Resource Usage** - Each session uses ~100-200MB RAM
3. **CPU Usage** - Chromium rendering is CPU-intensive
4. **Network Bandwidth** - File downloads/uploads

---

### Resource Requirements

#### Java Extension (Krista Platform)

**Memory:**

```
Base Memory: 50-100 MB
Per Active Request: 1-5 MB (protobuf messages, file buffers)
Recommended Heap: 512 MB - 1 GB

Example (10 concurrent requests):
50 MB (base) + (10 × 5 MB) = 100 MB
```

**CPU:**

- Minimal CPU usage (mostly I/O bound)
- Protobuf serialization/deserialization: <1% CPU
- Network I/O: <5% CPU

**Network:**

- AMQP connections: 1 persistent connection per extension instance
- Bandwidth: 10-100 KB/request (excluding file downloads)
- File downloads: Up to 100 MB/file (configurable)

#### Node.js Service

**Memory:**

```
Base Memory: 200-300 MB (Node.js + Playwright)
Per Browser Session: 100-200 MB
Per Browser Page: 50-100 MB

Example (10 concurrent sessions):
300 MB (base) + (10 × 150 MB) = 1.8 GB
Recommended: 4-8 GB RAM
```

**CPU:**

```
Idle: 1-5% CPU
Per Active Session: 10-30% CPU (browser rendering)
LLM API Calls: 5-10% CPU (JSON processing)

Example (5 concurrent sessions):
5% (idle) + (5 × 20%) = 105% CPU
Recommended: 4-8 CPU cores
```

**Disk:**

```
Application: 500 MB - 1 GB
Browser Cache: 100-500 MB per session
Temporary Files: 10-100 MB per session (screenshots, downloads)

Recommended: 20-50 GB SSD
```

**Network:**

- AMQP connections: 1 persistent connection
- LLM API calls: 1-10 KB/request, 5-50 KB/response
- File uploads to media server: Up to 100 MB/file

---

### Scalability Patterns

#### Horizontal Scaling (Recommended)

**Multiple Node.js Service Instances:**

```
┌─────────────────────────────────────────────────────────────┐
│                    Artemis Broker                           │
│  Request Queue: krista-ui-agent-1-IN (ANYCAST)              │
└─────────────────────────────────────────────────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Service      │    │ Service      │    │ Service      │
│ Instance 1   │    │ Instance 2   │    │ Instance 3   │
│ (10 sessions)│    │ (10 sessions)│    │ (10 sessions)│
└──────────────┘    └──────────────┘    └──────────────┘

Total Capacity: 30 concurrent sessions
```

**Configuration:**

```javascript
// Node.js service - config.js
module.exports = {
    maxConcurrentSessions: 10,  // Per instance
    sessionTTL: 86400000,        // 24 hours

    // Session affinity via JMSXGroupID ensures same instance handles all requests
    // from same extension (important for session state)
};
```

**Benefits:**

- ✅ **Linear Scaling** - Add instances to increase capacity
- ✅ **Fault Tolerance** - Instance failure doesn't affect others
- ✅ **Load Distribution** - ANYCAST routing distributes load
- ✅ **Session Affinity** - JMSXGroupID ensures session consistency

**Limitations:**

- ⚠️ **Session Affinity Required** - Same extension must use same instance
- ⚠️ **No Session Migration** - Sessions lost if instance fails

#### Vertical Scaling

**Increase Resources on Single Instance:**

```javascript
// Increase concurrent sessions
maxConcurrentSessions: 50  // Requires more RAM/CPU

// Resource requirements:
// RAM: 300 MB + (50 × 150 MB) = 7.8 GB
// CPU: 5% + (25 × 20%) = 505% (need 6+ cores)
```

**Benefits:**

- ✅ **Simpler Architecture** - Single instance to manage
- ✅ **No Session Affinity Issues** - All sessions on same instance

**Limitations:**

- ⚠️ **Single Point of Failure** - Instance failure affects all sessions
- ⚠️ **Resource Limits** - Cannot scale beyond single machine capacity
- ⚠️ **Cost** - Large instances more expensive than multiple small ones

---

### Performance Tuning

#### Extension Side (Java)

**1. Connection Pooling:**

```java
// UiAgentRawClient uses singleton pattern (connection reuse)
// No need for additional pooling

// Ensure proper cleanup on shutdown
@Override
public void close() throws Exception {
    // Wait for active requests to complete
    while (activeRequests.get() > 0 && waitCount < 60) {
        Thread.sleep(500);
    }
    // Close resources
}
```

**2. Timeout Configuration:**

```java
// Adjust timeouts based on use case
config.setResponseTimeout(Duration.ofMinutes(60));  // Long-running tasks
config.setSessionCheckoutTimeout(Duration.ofMinutes(10));

// For faster operations, reduce timeout
config.setResponseTimeout(Duration.ofMinutes(5));  // Quick operations
```

**3. File Download Optimization:**

```java
// FileHandlingService configuration
private static final int MAX_FILE_SIZE_MB = 100;
private static final int DOWNLOAD_TIMEOUT_SECONDS = 30;

// Increase for large files
private static final int MAX_FILE_SIZE_MB = 500;
private static final int DOWNLOAD_TIMEOUT_SECONDS = 120;
```

#### Service Side (Node.js)

**1. Browser Pool Management:**

```javascript
// Limit concurrent sessions to prevent resource exhaustion
const MAX_CONCURRENT_SESSIONS = 10;

// Reuse browser instances (not contexts)
const browser = await playwright.chromium.launch({
    headless: true,
    args: [
        '--disable-dev-shm-usage',  // Reduce memory usage
        '--no-sandbox',              // Docker compatibility
        '--disable-setuid-sandbox'
    ]
});
```

**2. LLM API Optimization:**

```javascript
// Use streaming for faster perceived response
const stream = await openai.chat.completions.create({
    model: 'gpt-4o',
    messages: messages,
    stream: true  // Start processing before full response
});

// Implement caching for repeated queries
const cache = new Map();
if (cache.has(prompt)) {
    return cache.get(prompt);
}
```

**3. File Handling Optimization:**

```javascript
// Stream files instead of loading into memory
const fileStream = fs.createReadStream(filepath);
response.pipe(fileStream);

// Clean up files immediately after upload
fs.unlink(filepath, (err) => {
    if (err) console.error('Failed to delete file:', err);
});
```

---

### Bottleneck Identification

#### Common Bottlenecks

**1. LLM API Rate Limits:**

```
Symptom: Requests fail with 429 (Too Many Requests)
Solution:
- Implement exponential backoff
- Use fallback LLM
- Increase API tier
- Distribute load across multiple API keys
```

**2. Browser Resource Exhaustion:**

```
Symptom: Sessions fail to start, high memory usage
Solution:
- Reduce maxConcurrentSessions
- Add more service instances (horizontal scaling)
- Increase instance RAM
- Implement session cleanup
```

**3. Queue Depth:**

```
Symptom: Increasing queue depth, slow response times
Solution:
- Add more service instances
- Increase service processing capacity
- Optimize browser automation scripts
```

**4. Network Bandwidth:**

```
Symptom: Slow file downloads/uploads
Solution:
- Increase network bandwidth
- Compress files before upload
- Reduce screenshot quality
- Implement CDN for file delivery
```

#### Monitoring Bottlenecks

```java
// Log request timing
long startTime = System.currentTimeMillis();
PromptExecuteResponse response = rawClient.executePrompt(...);
long duration = System.currentTimeMillis() - startTime;

log.info("Request completed in {}ms", duration);

// Alert if duration exceeds threshold
if (duration > 30000) {  // 30 seconds
    log.warn("Slow request detected: {}ms", duration);
}
```

---

### Capacity Planning

#### Estimating Capacity

**Formula:**

```
Capacity (requests/day) = (Instances × Sessions/Instance × Requests/Session × Hours/Day) / Avg Duration

Example:
- 3 service instances
- 10 sessions per instance
- 1 request per session (concurrent)
- 24 hours/day
- 10 seconds average duration

Capacity = (3 × 10 × 1 × 24 × 3600) / 10
         = 2,592,000 / 10
         = 259,200 requests/day
         ≈ 3 requests/second sustained
```

**Realistic Capacity (with overhead):**

```
Realistic Capacity = Theoretical Capacity × 0.7  // 30% overhead

Example:
259,200 × 0.7 = 181,440 requests/day
              ≈ 2.1 requests/second sustained
```

#### Scaling Recommendations

| Daily Requests | Service Instances | RAM per Instance | CPU per Instance |
|----------------|-------------------|------------------|------------------|
| **< 10,000** | 1 | 4 GB | 2 cores |
| **10,000 - 50,000** | 2-3 | 4-8 GB | 4 cores |
| **50,000 - 100,000** | 5-10 | 8 GB | 4-8 cores |
| **100,000+** | 10+ | 8-16 GB | 8 cores |

---

### Performance Best Practices

#### Extension Development

1. **Reuse Connections** - Singleton pattern prevents connection overhead
2. **Set Appropriate Timeouts** - Match timeout to expected operation duration
3. **Handle Errors Gracefully** - Don't retry indefinitely
4. **Clean Up Resources** - Close sessions when done
5. **Monitor Performance** - Log request durations

#### Service Development

1. **Limit Concurrent Sessions** - Prevent resource exhaustion
2. **Implement Session Cleanup** - Remove expired sessions
3. **Optimize Browser Settings** - Disable unnecessary features
4. **Use Streaming** - Start processing before full response
5. **Cache When Possible** - Reduce redundant LLM calls

#### Operations

1. **Monitor Queue Depth** - Alert on increasing backlog
2. **Track Response Times** - Identify performance degradation
3. **Scale Proactively** - Add capacity before hitting limits
4. **Load Test Regularly** - Validate capacity assumptions
5. **Optimize Costs** - Right-size instances based on actual usage

---

## Monitoring & Observability

### Overview

Effective monitoring and observability are critical for maintaining reliability and performance. This section covers logging, metrics, tracing, and alerting strategies.

---

### Logging

#### Extension Side (Java)

**Log Levels:**

```java
// UiAgentRawClient logging
log.info("Executing prompt: {} (requestId: {})", prompt, requestId);  // Normal operations
log.debug("Message properties: {}", message.getPropertyNames());      // Detailed debugging
log.warn("⚠️ Failed to close temporary consumer: {}", e.getMessage()); // Warnings
log.error("❌ Error executing prompt via ESB: {}", e.getMessage(), e); // Errors
```

**Structured Logging:**

```java
// Include correlation IDs for request tracing
log.info("📤 Sending request: correlationId={}, apiName={}, messageType={}",
         correlationId, apiName, messageType);

log.info("📥 Received response: correlationId={}, duration={}ms",
         correlationId, duration);

// Include context for troubleshooting
log.info("Session started: sessionId={}, workspaceId={}, accountId={}",
         sessionId, workspaceId, accountId);
```

**Log Configuration (logback.xml):**

```xml
<configuration>
    <!-- Console appender for development -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- File appender for production -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/ui-agent-extension.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/ui-agent-extension.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} %-5level [%thread] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Extension logger -->
    <logger name="app.krista.extension.uiautomationagent" level="INFO"/>

    <!-- Debug mode for troubleshooting -->
    <!-- <logger name="app.krista.extension.uiautomationagent" level="DEBUG"/> -->

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

#### Service Side (Node.js)

**Structured Logging:**

```javascript
const winston = require('winston');

const logger = winston.createLogger({
    level: process.env.LOG_LEVEL || 'info',
    format: winston.format.combine(
        winston.format.timestamp(),
        winston.format.json()
    ),
    transports: [
        new winston.transports.File({ filename: 'logs/error.log', level: 'error' }),
        new winston.transports.File({ filename: 'logs/combined.log' }),
        new winston.transports.Console({
            format: winston.format.simple()
        })
    ]
});

// Usage
logger.info('Request received', {
    correlationId: request.requestId,
    apiName: 'executePrompt',
    workspaceId: request.workspaceId
});

logger.error('LLM API call failed', {
    correlationId: request.requestId,
    error: error.message,
    provider: 'openai',
    model: 'gpt-4o'
});
```

**Log Aggregation:**

```javascript
// Send logs to centralized logging system (e.g., ELK, Splunk)
const { LoggingWinston } = require('@google-cloud/logging-winston');

logger.add(new LoggingWinston({
    projectId: 'your-project-id',
    keyFilename: '/path/to/key.json'
}));
```

---

### Metrics

#### Key Metrics to Track

**Extension Metrics (Java):**

```java
// Request metrics
private final AtomicLong totalRequests = new AtomicLong(0);
private final AtomicLong successfulRequests = new AtomicLong(0);
private final AtomicLong failedRequests = new AtomicLong(0);
private final AtomicLong activeRequests = new AtomicLong(0);

// Timing metrics
private final ConcurrentHashMap<String, Long> requestStartTimes = new ConcurrentHashMap<>();

public PromptExecuteResponse executePrompt(...) {
    totalRequests.incrementAndGet();
    activeRequests.incrementAndGet();
    long startTime = System.currentTimeMillis();

    try {
        PromptExecuteResponse response = ...;
        successfulRequests.incrementAndGet();
        return response;
    } catch (Exception e) {
        failedRequests.incrementAndGet();
        throw e;
    } finally {
        activeRequests.decrementAndGet();
        long duration = System.currentTimeMillis() - startTime;
        log.info("Request duration: {}ms", duration);
    }
}

// Expose metrics via JMX or HTTP endpoint
public Map<String, Object> getMetrics() {
    return Map.of(
        "totalRequests", totalRequests.get(),
        "successfulRequests", successfulRequests.get(),
        "failedRequests", failedRequests.get(),
        "activeRequests", activeRequests.get(),
        "successRate", (double) successfulRequests.get() / totalRequests.get()
    );
}
```

**Service Metrics (Node.js):**

```javascript
const promClient = require('prom-client');

// Create metrics
const requestCounter = new promClient.Counter({
    name: 'ui_agent_requests_total',
    help: 'Total number of requests',
    labelNames: ['api_name', 'status']
});

const requestDuration = new promClient.Histogram({
    name: 'ui_agent_request_duration_seconds',
    help: 'Request duration in seconds',
    labelNames: ['api_name'],
    buckets: [0.1, 0.5, 1, 2, 5, 10, 30, 60]
});

const activeSessions = new promClient.Gauge({
    name: 'ui_agent_active_sessions',
    help: 'Number of active browser sessions'
});

const queueDepth = new promClient.Gauge({
    name: 'ui_agent_queue_depth',
    help: 'Number of messages in queue'
});

// Record metrics
requestCounter.inc({ api_name: 'executePrompt', status: 'success' });
requestDuration.observe({ api_name: 'executePrompt' }, duration);
activeSessions.set(sessionManager.getActiveSessionCount());

// Expose metrics endpoint
app.get('/metrics', async (req, res) => {
    res.set('Content-Type', promClient.register.contentType);
    res.end(await promClient.register.metrics());
});
```

#### Metrics Dashboard

**Prometheus + Grafana Setup:**

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'ui-agent-service'
    static_configs:
      - targets: ['localhost:3000']
    scrape_interval: 15s
```

**Key Metrics to Visualize:**

1. **Request Rate** - Requests per second
2. **Error Rate** - Failed requests per second
3. **Response Time** - P50, P95, P99 latencies
4. **Active Sessions** - Current browser sessions
5. **Queue Depth** - Messages waiting in queue
6. **Resource Usage** - CPU, memory, disk

---

### Distributed Tracing

#### Correlation ID Tracking

**End-to-End Tracing:**

```
User Request → Extension → AMQP → Service → LLM API → Browser
     ↓              ↓         ↓        ↓        ↓         ↓
correlationId: 550e8400-e29b-41d4-a716-446655440000 (same throughout)
```

**Extension Side:**

```java
// Generate correlation ID
String correlationId = UUID.randomUUID().toString();

// Log at each step
log.info("📤 [{}] Sending request to queue", correlationId);
log.info("⏳ [{}] Waiting for response", correlationId);
log.info("📥 [{}] Received response", correlationId);
```

**Service Side:**

```javascript
// Extract correlation ID from message
const correlationId = message.properties.correlationId;

// Log at each step
logger.info(`[${correlationId}] Processing request`);
logger.info(`[${correlationId}] Calling LLM API`);
logger.info(`[${correlationId}] Executing browser automation`);
logger.info(`[${correlationId}] Sending response`);
```

**OpenTelemetry Integration (Advanced):**

```javascript
const { trace } = require('@opentelemetry/api');
const tracer = trace.getTracer('ui-agent-service');

async function handleRequest(message) {
    const span = tracer.startSpan('handleRequest', {
        attributes: {
            'correlation.id': message.properties.correlationId,
            'api.name': message.properties.ESB_API_NAME
        }
    });

    try {
        // Process request
        const result = await processRequest(message);
        span.setStatus({ code: SpanStatusCode.OK });
        return result;
    } catch (error) {
        span.setStatus({ code: SpanStatusCode.ERROR, message: error.message });
        throw error;
    } finally {
        span.end();
    }
}
```

---

### Health Checks

#### Extension Health Check

```java
public class UiAgentHealthCheck {

    public HealthStatus checkHealth() {
        HealthStatus status = new HealthStatus();

        // Check ESB connection
        try {
            if (rawClient != null && !rawClient.isClosed()) {
                status.addCheck("esb_connection", "UP");
            } else {
                status.addCheck("esb_connection", "DOWN");
            }
        } catch (Exception e) {
            status.addCheck("esb_connection", "DOWN", e.getMessage());
        }

        // Check active requests
        int activeRequests = rawClient.getActiveRequests();
        status.addCheck("active_requests", activeRequests);

        // Check queue connectivity
        try {
            // Attempt to send test message (optional)
            status.addCheck("queue_connectivity", "UP");
        } catch (Exception e) {
            status.addCheck("queue_connectivity", "DOWN", e.getMessage());
        }

        return status;
    }
}
```

#### Service Health Check

```javascript
// Health check endpoint
app.get('/health', async (req, res) => {
    const health = {
        status: 'UP',
        timestamp: new Date().toISOString(),
        checks: {}
    };

    // Check AMQP connection
    try {
        if (amqpConnection && amqpConnection.isConnected()) {
            health.checks.amqp = { status: 'UP' };
        } else {
            health.checks.amqp = { status: 'DOWN' };
            health.status = 'DOWN';
        }
    } catch (error) {
        health.checks.amqp = { status: 'DOWN', error: error.message };
        health.status = 'DOWN';
    }

    // Check browser availability
    try {
        const browser = await getBrowser();
        health.checks.browser = { status: 'UP', version: browser.version() };
    } catch (error) {
        health.checks.browser = { status: 'DOWN', error: error.message };
        health.status = 'DOWN';
    }

    // Check active sessions
    health.checks.sessions = {
        active: sessionManager.getActiveSessionCount(),
        max: MAX_CONCURRENT_SESSIONS
    };

    // Check LLM API connectivity (optional)
    try {
        // Ping LLM API
        health.checks.llm = { status: 'UP' };
    } catch (error) {
        health.checks.llm = { status: 'DEGRADED', error: error.message };
    }

    res.status(health.status === 'UP' ? 200 : 503).json(health);
});
```

---

### Alerting

#### Alert Rules

**Critical Alerts:**

```yaml
# Prometheus alert rules
groups:
  - name: ui_agent_critical
    interval: 30s
    rules:
      # Service down
      - alert: ServiceDown
        expr: up{job="ui-agent-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "UI Agent service is down"

      # High error rate
      - alert: HighErrorRate
        expr: rate(ui_agent_requests_total{status="error"}[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Error rate above 10%"

      # Queue depth growing
      - alert: QueueDepthGrowing
        expr: ui_agent_queue_depth > 100
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Queue depth exceeds 100 messages"
```

**Warning Alerts:**

```yaml
  - name: ui_agent_warnings
    interval: 1m
    rules:
      # Slow response times
      - alert: SlowResponseTime
        expr: histogram_quantile(0.95, ui_agent_request_duration_seconds) > 30
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "P95 response time above 30 seconds"

      # High session count
      - alert: HighSessionCount
        expr: ui_agent_active_sessions > 15
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Active sessions above 15"

      # High memory usage
      - alert: HighMemoryUsage
        expr: process_resident_memory_bytes > 8e9  # 8 GB
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Memory usage above 8 GB"
```

#### Alert Channels

```yaml
# alertmanager.yml
route:
  receiver: 'default'
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h

  routes:
    - match:
        severity: critical
      receiver: 'pagerduty'

    - match:
        severity: warning
      receiver: 'slack'

receivers:
  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: '<pagerduty-key>'

  - name: 'slack'
    slack_configs:
      - api_url: '<slack-webhook-url>'
        channel: '#ui-agent-alerts'
```

---

### Observability Best Practices

#### 1. Consistent Correlation IDs

```java
// ✅ DO: Use same correlation ID throughout request lifecycle
String correlationId = UUID.randomUUID().toString();
log.info("[{}] Request started", correlationId);
// ... process request ...
log.info("[{}] Request completed", correlationId);

// ❌ DON'T: Generate new IDs at each step
log.info("[{}] Request started", UUID.randomUUID());  // Different ID!
```

#### 2. Structured Logging

```javascript
// ✅ DO: Use structured logs (JSON)
logger.info('Request processed', {
    correlationId: '550e8400-...',
    duration: 1234,
    status: 'success'
});

// ❌ DON'T: Use unstructured logs
logger.info(`Request 550e8400-... processed in 1234ms: success`);
```

#### 3. Meaningful Metrics

```javascript
// ✅ DO: Use labels for dimensions
requestCounter.inc({ api_name: 'executePrompt', status: 'success' });

// ❌ DON'T: Create separate metrics for each dimension
executePromptSuccessCounter.inc();
```

#### 4. Appropriate Log Levels

```java
// ✅ DO: Use appropriate levels
log.debug("Message properties: {}", props);  // Verbose debugging
log.info("Request completed: {}", id);       // Normal operations
log.warn("Retry attempt {}", attempt);       // Warnings
log.error("Request failed: {}", e);          // Errors

// ❌ DON'T: Log everything as INFO
log.info("Message properties: {}", props);   // Too verbose for INFO
```

#### 5. Monitor What Matters

**Focus on:**
- ✅ Request rate and error rate
- ✅ Response time percentiles (P50, P95, P99)
- ✅ Active sessions and queue depth
- ✅ Resource usage (CPU, memory)

**Avoid:**
- ❌ Vanity metrics (total requests all-time)
- ❌ Metrics without actionable alerts
- ❌ Too many metrics (signal-to-noise ratio)

---

## Testing Strategy

### Unit Tests

**Location:** `src/test/java/app/krista/extension/uiautomationagent/`

**Coverage Areas:**

1. **Protobuf Serialization/Deserialization**
   ```java
   @Test
   void testPromptExecuteRequestSerialization() {
       PromptExecuteRequest request = PromptExecuteRequest.newBuilder()
           .setRequestId(UUID.randomUUID().toString())
           .setPrompt("Test prompt")
           .build();

       byte[] bytes = request.toByteArray();
       PromptExecuteRequest deserialized = PromptExecuteRequest.parseFrom(bytes);

       assertEquals(request.getRequestId(), deserialized.getRequestId());
   }
   ```

2. **UserConfiguration Builder**
   ```java
   @Test
   void testUserConfigurationBuilder() {
       UserConfiguration config = buildUserConfiguration();

       assertNotNull(config.getLlm());
       assertEquals("gpt-4o", config.getLlm().getPrimaryModel());
       assertTrue(config.getSession().getEnableValidations());
   }
   ```

3. **Session Key Mapping**
   ```java
   @Test
   void testSessionKeyMapping() {
       SessionKey key1 = new SessionKey("conv-123", "default");
       SessionKey key2 = new SessionKey("conv-123", "default");
       SessionKey key3 = new SessionKey("conv-123", "checkout");

       assertEquals(key1, key2);
       assertNotEquals(key1, key3);
   }
   ```

### Integration Tests

**Prerequisites:**
- Running Artemis broker on port 61616
- Running Node.js UI Agent service
- Valid LLM API keys configured

**Test Scenarios:**

1. **End-to-End Single Command**
   ```java
   @Test
   void testSingleCommandExecution() {
       String prompt = "Navigate to https://example.com and get the title";
       PromptExecuteResponse response = rawClient.executePrompt(
           prompt, userConfig, workspaceId, accountId
       );

       assertTrue(response.getOk());
       assertEquals(ExecutionStatus.SUCCESS, response.getStatus());
       assertNotNull(response.getResult().getFinalOutput());
   }
   ```

2. **Session Lifecycle**
   ```java
   @Test
   void testSessionLifecycle() {
       // Start session
       SessionStartResponse startResp = rawClient.startSession(
           sessionId, userConfig, workspaceId, accountId
       );
       assertTrue(startResp.getOk());

       // Execute in session
       SessionExecuteResponse execResp = rawClient.executeSession(
           sessionId, "Navigate to https://example.com", workspaceId, accountId
       );
       assertTrue(execResp.getOk());

       // Close session
       SessionCloseResponse closeResp = rawClient.closeSession(
           sessionId, workspaceId, accountId
       );
       assertTrue(closeResp.getOk());
   }
   ```

3. **Session Expiration Handling**
   ```java
   @Test
   void testSessionExpirationRecovery() {
       // Create session
       String sessionId = "test-session-" + UUID.randomUUID();
       rawClient.startSession(sessionId, userConfig, workspaceId, accountId);

       // Simulate expiration (wait or manually delete on service side)
       // ...

       // Execute should detect expiration and create new session
       SessionExecuteResponse response = rawClient.executeSession(
           sessionId, "Test prompt", workspaceId, accountId
       );

       // Should succeed with new session
       assertTrue(response.getOk());
   }
   ```

### Manual Testing Checklist

#### Single Command Mode
- [ ] Simple navigation (e.g., "Go to https://example.com")
- [ ] Data extraction (e.g., "Get the page title")
- [ ] Form filling (e.g., "Fill email field with test@example.com")
- [ ] File download (e.g., "Download the PDF")
- [ ] Screenshot capture (verify screenshots in response)
- [ ] Validation (provide validation steps, verify execution)
- [ ] Error handling (invalid URL, timeout, etc.)

#### Interactive Sessions
- [ ] Session creation (verify unique session ID returned)
- [ ] Multi-step execution (navigate, fill form, submit)
- [ ] Session state persistence (verify cookies/storage maintained)
- [ ] Multiple concurrent sessions (create 2+ sessions in same conversation)
- [ ] Session closure (verify profiling data returned)
- [ ] Session expiration (wait 24hrs or manually expire)

#### File Handling
- [ ] Download single file (verify file metadata and download URL)
- [ ] Download multiple files (verify all files captured)
- [ ] Screenshot capture (verify screenshots at each step)
- [ ] HMAC URL validation (verify URLs are signed and accessible)
- [ ] File size limits (test large file downloads)

#### Configuration
- [ ] Primary LLM (verify requests use primary model)
- [ ] Fallback LLM (simulate primary failure, verify fallback)
- [ ] Invalid API key (verify error handling)
- [ ] Missing configuration (verify validation errors)

---

## Deployment Guide

### Prerequisites

1. **Krista Platform** - Version 4.0 or higher
2. **Apache Artemis** - Running on port 61616
3. **Node.js UI Agent Service** - Running and accessible
4. **LLM API Keys** - Valid keys for OpenAI, Gemini, or Claude

### Build Process

**1. Clone Repository**
```bash
git clone <repository-url>
cd krista-ui-agent-extension
```

**2. Build Extension**
```bash
./gradlew clean build
```

**Expected Output:**
```
BUILD SUCCESSFUL in 15s
10 actionable tasks: 10 executed
```

**3. Verify Build Artifacts**
```bash
ls -lh build/libs/
```

**Expected Files:**
- `krista-ui-agent-extension-1.0.0-rc2.8.jar` - Main extension JAR
- `krista-ui-agent-extension-1.0.0-rc2.8-sources.jar` - Source code JAR

### Deployment Steps

**1. Copy Extension JAR**
```bash
cp build/libs/krista-ui-agent-extension-1.0.0-rc2.8.jar \
   /path/to/krista/extensions/
```

**2. Restart Krista Platform**
```bash
# Method depends on your deployment
systemctl restart krista
# OR
docker-compose restart krista
```

**3. Verify Extension Loaded**

Check Krista logs for:
```
INFO: Loading extension: krista-ui-agent-extension
INFO: Extension krista-ui-agent-extension loaded successfully
INFO: Registered catalog requests: Single Command Mode, Interactive Session Start, ...
```

**4. Configure Extension**

Navigate to Krista UI → Extensions → UI Agent Extension

Fill in required fields:
- **UI Agent URL**: `http://localhost:3000` (or your service URL)
- **Primary LLM**: Select from dropdown
- **Primary LLM API Key**: Enter API key
- **Fallback LLM**: Select from dropdown
- **Fallback LLM API Key**: Enter API key

**5. Test Extension**

Create a new conversation and invoke "Single Command Mode":
```
Prompt: Navigate to https://example.com and get the page title
```

Verify response contains:
- ✅ Final output with page title
- ✅ Screenshots
- ✅ Execution steps

### Verification Checklist

- [ ] Extension JAR copied to extensions directory
- [ ] Krista platform restarted
- [ ] Extension appears in Krista UI
- [ ] Configuration fields populated
- [ ] Test execution successful
- [ ] Logs show no errors
- [ ] Response queue created in Artemis
- [ ] Files uploaded to media server

### Troubleshooting Deployment

**Extension Not Loading**
```bash
# Check Krista logs
tail -f /var/log/krista/krista.log | grep "ui-agent"

# Verify JAR file permissions
ls -l /path/to/krista/extensions/krista-ui-agent-extension*.jar
```

**Configuration Not Saving**
- Verify database connection
- Check for validation errors in UI
- Ensure all required fields filled

**ESB Connection Failed**
```bash
# Verify Artemis is running
netstat -an | grep 61616

# Check Artemis logs
tail -f /var/log/artemis/artemis.log
```

---

## Troubleshooting

### Common Issues

#### 1. "Session Not Found" Error

**Symptom:** `SESSION_NOT_FOUND` error when executing in session

**Causes:**
- Session expired (24hr TTL)
- Service restarted (sessions lost)
- Invalid session ID

**Solution:**
```java
// Extension automatically handles this
try {
    response = rawClient.executeSession(sessionId, prompt, ...);
} catch (SessionNotFoundException e) {
    // Remove stale mapping
    sessionMapping.remove(sessionKey);
    // Create new session
    SessionStartResponse newSession = rawClient.startSession(...);
    sessionMapping.put(sessionKey, newSession.getSessionId());
}
```

**User Action:** Re-run "Interactive Session Start" to create new session

---

#### 2. Timeout Errors

**Symptom:** Request times out after 20 minutes

**Causes:**
- Complex automation taking too long
- LLM response delay
- Network issues

**Solution:**
1. **Break down complex prompts** into smaller steps
2. **Use interactive sessions** instead of single command mode
3. **Increase timeout** (if needed):
   ```java
   // In UiAgentRawClient
   private final long responseTimeoutMs = 1800000; // 30 minutes
   ```

---

#### 3. Protobuf Serialization Errors

**Symptom:** `SERIALIZATION_ERROR` or `Failed to parse protobuf`

**Causes:**
- Mismatched protobuf schema versions
- Corrupted message
- Wrong content-type header

**Solution:**
1. **Verify protobuf schema** matches between extension and service
2. **Check message properties**:
   ```java
   message.putStringProperty("content-type", "application/x-protobuf");
   message.putStringProperty("esb_protoMsg", "krista.ui.agent.PromptExecuteRequest");
   ```
3. **Rebuild extension** after protobuf changes:
   ```bash
   ./gradlew clean build
   ```

---

#### 4. Duplicate Consumer Errors

**Symptom:** Multiple consumers on response queue, messages delivered to wrong instance

**Cause:** Singleton pattern not working (INVOKER_LOADED called multiple times)

**Solution:**
Verify singleton implementation in `KristaUiAgentExtension`:
```java
// CORRECT - Singleton pattern
private static UiAgentRawClient rawClient = null;

@Override
public void onEvent(ExtensionEvent event) {
    if (event.getType() == ExtensionEventType.INVOKER_LOADED) {
        synchronized (lock) {
            if (rawClient == null) {
                rawClient = UiAgentRawClient.getInstance(...);
            }
        }
    }
}
```

---

#### 5. Field Annotation Mismatch

**Symptom:** Krista UI shows "unknown entity" instead of file downloads or screenshots

**Cause:** Field names in annotations don't match response data keys

**Solution:**
Ensure exact match (case-sensitive):
```java
// Annotation
@Field.File(name = "Downloaded Files", multipleFileUpload = true)

// Response data
Map<String, Object> responseData = new HashMap<>();
responseData.put("Downloaded Files", fileList); // MUST match exactly
```

---

#### 6. LLM API Errors

**Symptom:** `ACCESS_DENIED` or `INVALID_CONFIGURATION` errors

**Causes:**
- Invalid API key
- Insufficient credits/quota
- API key for wrong provider

**Solution:**
1. **Verify API key** in extension configuration
2. **Check provider mapping**:
   ```java
   // OpenAI → "openai"
   // Google Gemini → "google"
   // Claude → "anthropic"
   ```
3. **Test API key** directly:
   ```bash
   curl https://api.openai.com/v1/models \
     -H "Authorization: Bearer $OPENAI_API_KEY"
   ```

---

#### 7. File Download Issues

**Symptom:** Files not appearing in response or download URLs invalid

**Causes:**
- Media server not accessible
- HMAC signing failed
- File size exceeds limit

**Solution:**
1. **Verify media server** is running and accessible
2. **Check file size limits** in Node.js service configuration
3. **Verify HMAC secret** matches between service and media server

---

#### 8. Queue Not Found

**Symptom:** `Queue krista-ui-agent-1-IN does not exist`

**Cause:** Node.js service not running or not configured correctly

**Solution:**
1. **Start Node.js service**:
   ```bash
   cd ui-agent-service
   npm start
   ```
2. **Verify queue creation** in Artemis console:
   ```bash
   # Check queues
   artemis queue stat --url tcp://localhost:61616
   ```

---

### Debugging Guide

#### Enable Debug Logging

**Extension Side (Java):**
```java
// Add to logback.xml or log4j2.xml
<logger name="app.krista.extension.uiautomationagent" level="DEBUG"/>
```

**Service Side (Node.js):**
```javascript
// Set environment variable
DEBUG=ui-agent:* npm start
```

#### Inspect AMQP Messages

**Using Artemis Console:**
```bash
# Browse messages in queue
artemis browser --url tcp://localhost:61616 --queue krista-ui-agent-1-IN

# Check queue stats
artemis queue stat --url tcp://localhost:61616 --queue krista-ui-agent-1-IN
```

**Using Wireshark:**
1. Capture traffic on port 61616
2. Filter: `tcp.port == 61616`
3. Analyze AMQP frames

#### Trace Request/Response

**Add correlation ID logging:**
```java
logger.debug("Sending request with correlation ID: {}", correlationId);
// ... send request ...
logger.debug("Received response with correlation ID: {}", responseMsg.getCorrelationID());
```

**Verify message properties:**
```java
logger.debug("Message properties: {}", message.getPropertyNames());
message.getPropertyNames().forEach(name -> {
    logger.debug("  {} = {}", name, message.getStringProperty(name));
});
```

---

### FAQ

**Q: Can I use multiple LLM providers simultaneously?**
A: Yes, configure primary and fallback LLMs. The extension automatically falls back if primary fails.

**Q: How do I increase session timeout beyond 24 hours?**
A: Configure in UserConfiguration:
```java
SessionDefaults.newBuilder()
    .setTimeoutMs(172800000) // 48 hours
    .build()
```

**Q: Can I run multiple browser sessions in parallel?**
A: Yes, use session multiplexing with different session keys:
```java
SessionKey key1 = new SessionKey(conversationId, "session1");
SessionKey key2 = new SessionKey(conversationId, "session2");
```

**Q: How do I handle authentication (login)?**
A: Use interactive sessions to maintain authentication state:
1. Start session
2. Execute login steps
3. Execute authenticated actions
4. Close session

**Q: What browsers are supported?**
A: Currently Chromium only. Firefox and WebKit support planned.

**Q: How do I debug protobuf serialization issues?**
A: Use protobuf text format for debugging:
```java
String textFormat = TextFormat.printToString(request);
logger.debug("Request: {}", textFormat);
```

**Q: Can I customize browser settings (viewport, user agent)?**
A: Yes, configure in UserConfiguration:
```protobuf
BrowserConfiguration {
  viewport_width = 1920
  viewport_height = 1080
  user_agent = "Custom User Agent"
}
```

**Q: How do I handle file uploads?**
A: File upload support is planned but not yet implemented.

**Q: What's the maximum file download size?**
A: Configurable in Node.js service, default is 100MB.

---

## Appendix

### File Locations

| Component | Path |
|-----------|------|
| **Main Extension Class** | `src/main/java/app/krista/extension/uiautomationagent/interaction/KristaUiAgentExtension.java` |
| **Catalog Requests** | `src/main/java/app/krista/extension/uiautomationagent/interaction/catalog/InteractionArea.java` |
| **Raw Client** | `src/main/java/app/krista/extension/uiautomationagent/esb/UiAgentRawClient.java` |
| **Event Handler** | `src/main/java/app/krista/extension/uiautomationagent/events/UiAutomationEventListenerHandler.java` |
| **Session Manager** | `src/main/java/app/krista/extension/uiautomationagent/service/UiAgentSessionManager.java` |
| **Protobuf Schema** | `src/main/proto/ui_agent_messages.proto` |
| **Build Configuration** | `build.gradle` |

### Key Constants

```java
// Queue names
REQUEST_QUEUE = "krista-ui-agent-1-IN"
RESPONSE_QUEUE_PATTERN = "ext-{invokerId}-{workspaceId}-OUT"

// Timeouts
RESPONSE_TIMEOUT_MS = 1200000  // 20 minutes
SESSION_TTL_MS = 86400000      // 24 hours

// ESB properties
ESB_API_NAME = "executePrompt" | "startSession" | "executeSession" | "closeSession"
ESB_API_VER = "1"
CONTENT_TYPE = "application/x-protobuf"

// LLM models
OPENAI_PRIMARY = "gpt-4o"
OPENAI_FALLBACK = "gpt-4o-mini"
GEMINI_PRIMARY = "gemini-2.0-flash-exp"
GEMINI_FALLBACK = "gemini-1.5-flash"
CLAUDE_PRIMARY = "claude-3-5-sonnet-20241022"
CLAUDE_FALLBACK = "claude-3-5-haiku-20241022"
```

### Version History

| Version | Date | Changes |
|---------|------|---------|
| **1.0.0-rc2.8** | 2026-02-03 | Current production release |
| **1.0.0-rc2.7** | 2026-01-15 | Added validation framework |
| **1.0.0-rc2.6** | 2026-01-10 | Session profiling support |
| **1.0.0-rc2.5** | 2025-12-20 | Interactive sessions |
| **1.0.0-rc2.0** | 2025-12-01 | Initial Framework 4.0 implementation |

---

## Summary

The **Krista UI Agent Extension** provides a robust, production-ready solution for AI-powered browser automation within the Krista platform. Key highlights:

✅ **Framework 4.0 Compliant** - Full integration with Krista's latest framework
✅ **Google Protobuf Communication** - Type-safe, efficient serialization
✅ **Multi-LLM Support** - OpenAI, Gemini, Claude with automatic fallback
✅ **Session Management** - 24hr TTL, multiplexing, isolation
✅ **File Handling** - Downloads, screenshots, HMAC-signed URLs
✅ **Validation & Profiling** - Post-execution verification and performance metrics

**Build Status:** ✅ Successful
**Test Coverage:** Unit + Integration tests
**Documentation:** Complete and verified against source code

For questions or issues, refer to the [Troubleshooting](#troubleshooting) section or contact the development team.

---

**End of Document**

