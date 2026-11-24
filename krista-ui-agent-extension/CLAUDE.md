# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Krista Platform Extension** for UI browser automation (called "Agentic Browser" / "UI Automation Agent"). It's a Java 21 Gradle project that acts as a bridge between the Krista orchestration platform and a remote Node.js UI Agent service. The extension sends natural language prompts to the service, which executes browser automation via an LLM-driven agent.

Communication with the UI Agent service uses **Google Protobuf over ActiveMQ Artemis** (Core protocol, port 61616). The protobuf schema is defined in `src/main/proto/ui_agent_messages.proto`.

## Build Commands

```bash
./gradlew build          # Full build (clean + compile + test + jar)
./gradlew compileJava    # Compile only (triggers clean + updateVersion)
./gradlew test           # Run tests (JUnit 5)
./gradlew jar            # Build fat JAR (includes all runtime dependencies)
```

Note: `compileJava` depends on `clean` and `updateVersion`, so every compile is a clean build. The build number in `build-nr.txt` auto-increments on each compile.

## Architecture

### Extension Lifecycle (Krista Framework)

`KristaUiAgentExtension` is the main entry point, discovered via `@Service` + `@Extension` annotations. The Krista framework calls lifecycle methods annotated with `@InvokerRequest`:

- `INVOKER_LOADED` — Called 3 times (once per mode: DRAFT, TEST, LIVE). Initializes ESB clients.
- `INVOKER_UNLOADED` / `INVOKER_REMOVED` — Cleanup ESB connections.
- `VALIDATE_ATTRIBUTES` / `TEST_CONNECTION` — Validates LLM API keys (OpenAI, Claude, Gemini) with real API calls.
- `REGISTER_EVENT_LISTENER` — Routes to `UiAutomationEventListenerHandler`.
- `CUSTOM_TABS` — Exposes "Playground" UI and "Documentation" tabs.

### Two ESB Client Paths

1. **`UiAgentClient`** (legacy) — Uses Krista's `ServiceClientBase` with Infinispan ProtoStream serialization. DTOs are in `esb/dto/`.
2. **`UiAgentRawClient`** (primary) — Singleton, uses raw Artemis Core API with Google Protobuf. Creates per-request temporary consumers with correlation ID filtering for parallel processing. This is the client actually used for browser automation.

Both use the shared request queue `krista-ui-agent-1-IN`. Response queues are unique per extension instance.

### Catalog Requests (InteractionArea)

`InteractionArea` defines the extension's callable operations via `@CatalogRequest` annotations:

- **Single Command Mode** (`WAIT_FOR_EVENT`) — Fire-and-forget prompt execution. Event-driven: initial request returns pending, completion comes via event listener.
- **Interactive Session Start** (`CHANGE_SYSTEM`) — Creates a browser session, returns session ID.
- **Execute Interactive Session Steps** (`WAIT_FOR_EVENT`) — Runs prompts within an existing session. Supports validation steps.
- **Interactive Session End** (`CHANGE_SYSTEM`) — Closes session, returns profiling data if enabled.

### Event Handling

`UiAutomationEventListenerHandler` manages the async event flow for `WAIT_FOR_EVENT` catalog requests. It builds `UserConfiguration` protobuf messages from extension attributes and processes ESB responses (screenshots, downloaded files, validation results).

### Key Supporting Classes

- `ExtensionAttributes` / `ExtensionAttributesProvider` — Static singleton pattern for sharing config (LLM provider, API keys, UI Agent URL) across components.
- `UiAgentSessionManager` — Thread-safe in-memory session tracking with `ConcurrentHashMap`.
- `FileHandlingService` — Downloads files from HMAC-signed URLs and uploads to Krista's media server.
- `ExtensionResponseFactory` — Standardized `ExtensionResponse` construction for success/error cases.
- `UiAgentResponseParser` — Parses protobuf `ExecutionResult` into structured response maps.

### ESB Configuration

`UiAgentEsbConfigProvider` holds broker connection details (host, port, credentials). Currently hardcoded; planned migration to KSDK platform config. The commented-out defaults (`activemq-artemis` / `artemis` / `simetraehcapa`) are for Krista platform deployment.

### Protobuf

The `.proto` file generates Java classes into `build/generated/source/proto/main/java`. Key message types: `SessionStartRequest/Response`, `SessionExecuteRequest/Response`, `PromptExecuteRequest/Response`, `SessionCloseRequest/Response`, `UserConfiguration`, `ExecutionResult`.

## Testing

Tests use JUnit 5 + Mockito. Run a single test:
```bash
./gradlew test --tests "app.krista.extensions.uiagent.service.UiAgentSessionManagerTest"
./gradlew test --tests "*.UiAgentResponseParserTest"
```

## Skills (Slash Commands)

### `/fix-issue [ticket-id]`
Fetches a JIRA bug ticket (details, comments, attachments/screenshots), performs root cause analysis across both the extension and the UI Agent service, presents the RCA for confirmation, then implements the fix.

Requires `.env` with JIRA credentials (see `.env.example`). Uses `tools/jira_client.py` for JIRA API access.

## Related Service

The **krista-ui-agent** Node.js service lives at `/Users/mayurmistry/development/krista-ui-agent`. This is the backend that receives protobuf messages from this extension over Artemis and performs actual browser automation via Playwright + LLM.

## Extension Deployment

The extension is deployed as a fat JAR to the Krista platform. The `extension-config.json` contains deployment target info (studio URL, workspace ID). Node.js binary is excluded from the JAR (~98MB savings) and installed at runtime.
