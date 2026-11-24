---
name: add-catalog-request
description: Add a new catalog request to the extension following Krista patterns (QUERY_SYSTEM, CHANGE_SYSTEM, or WAIT_FOR_EVENT)
disable-model-invocation: true
argument-hint: [request-name]
allowed-tools: Read, Grep, Glob, Bash, Edit, Write, Agent, AskUserQuestion
---

# Add Catalog Request Skill

You are adding a new catalog request to the **krista-ui-agent-extension**.

**Request Name**: $ARGUMENTS

---

## Phase 1: Gather Requirements

Use AskUserQuestion to ask the user:

1. **Request type**: QUERY_SYSTEM (read-only), CHANGE_SYSTEM (modifies data), or WAIT_FOR_EVENT (async with event listener)?
2. **Description**: What does this request do?
3. **Input fields**: What parameters does it need? (names, types, required/optional)
4. **Output fields**: What does it return?
5. **Does it communicate with the UI Agent service via ESB?** (most requests in this extension do)

---

## Phase 2: Understand Existing Patterns

Before writing code, read these files to understand the current patterns:

1. Read `InteractionArea.java` — the existing catalog area with all current requests:
```
src/main/java/app/krista/extension/uiautomationagent/interaction/catalog/InteractionArea.java
```

2. Read `ExtensionResponseFactory.java` — how responses are constructed:
```
src/main/java/app/krista/extension/uiautomationagent/factory/ExtensionResponseFactory.java
```

3. Read the protobuf schema:
```
src/main/proto/ui_agent_messages.proto
```

4. If WAIT_FOR_EVENT, also read the event handler:
```
src/main/java/app/krista/extension/uiautomationagent/events/UiAutomationEventListenerHandler.java
```

---

## Phase 3: Implement

Follow these Krista extension patterns:

### Catalog Request Annotations

```java
@CatalogRequest(
    name = "{Request Name}",
    domain = "UI Automation Agent",
    type = CatalogRequest.Type.{TYPE},
    description = "{Description}"
)
```

### Input/Output Field Annotations

- `@Field.Text(name = "...", required = true/false)` — text input
- `@Field.TextArea(name = "...", required = false)` — multi-line text
- `@Field.PickOne(name = "...", values = {...})` — dropdown
- `@Field.Checkbox(name = "...", required = false)` — boolean
- `@Field.Output(name = "...", type = Field.Output.Type.TEXT)` — output

### Request Types

**QUERY_SYSTEM**: Synchronous read-only. Return `ExtensionResponse` directly.
```java
public ExtensionResponse queryMethod(Map<String, Object> params) {
    // Validate → Call ESB/service → Parse response → Return ExtensionResponse
}
```

**CHANGE_SYSTEM**: Synchronous write. Return `ExtensionResponse` directly.
```java
public ExtensionResponse changeMethod(Map<String, Object> params) {
    // Validate → Call ESB/service → Parse response → Return ExtensionResponse
}
```

**WAIT_FOR_EVENT**: Async. Returns pending, completion via event listener.
```java
public ExtensionResponse asyncMethod(Map<String, Object> params) {
    // Validate → Send ESB message → Return PENDING response
    // Completion handled by UiAutomationEventListenerHandler
}
```

### Error Handling

Use `ExtensionResponseFactory` for consistent responses:
- Success: `ExtensionResponseFactory.createSuccessResponse(...)`
- Error: `ExtensionResponseFactory.createErrorResponse(...)`
- Validate inputs before making ESB calls

### ESB Communication

If the request communicates with the UI Agent service:
1. Build the protobuf message using the generated builders
2. Send via `UiAgentRawClient`
3. Parse the protobuf response via `UiAgentResponseParser`

### If Protobuf Changes Needed

If new message types are needed:
1. Update `src/main/proto/ui_agent_messages.proto`
2. Also update `protos/ui_agent_messages.proto` in the Node.js service at `/Users/mayurmistry/development/krista-ui-agent/`
3. Run `./gradlew build` to regenerate Java classes

---

## Phase 4: Test

1. Add the catalog request method to `InteractionArea.java`
2. Run the build:
```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && ./gradlew build
```
3. If there are testable components, write unit tests

---

## Important Rules

- Follow existing patterns in InteractionArea.java exactly
- Use ExtensionResponseFactory for all responses — never construct ExtensionResponse directly
- Validate all required inputs before making ESB calls
- Keep catalog request methods under 30 lines — delegate to helpers
- If adding protobuf messages, update BOTH repos' .proto files
- Use proper error categorization (INPUT_ERROR, SYSTEM_ERROR, etc.)
