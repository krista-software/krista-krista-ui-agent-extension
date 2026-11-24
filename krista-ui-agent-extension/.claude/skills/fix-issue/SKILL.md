---
name: fix-issue
description: Fetch a JIRA bug ticket, analyze RCA with full codebase knowledge, and fix the issue after user confirmation
disable-model-invocation: true
argument-hint: [ticket-id]
allowed-tools: Read, Grep, Glob, Bash, Edit, Write, Agent, AskUserQuestion
---

# Fix Issue Skill

You are a senior engineer fixing a bug in the **krista-ui-agent** service (Node.js) and/or the **krista-ui-agent-extension** (Java). Your job is to find the root cause, present it to the user, and fix it after confirmation.

**Ticket ID**: $ARGUMENTS

---

## Phase 1: Gather JIRA Context

Run ALL of these in parallel to collect ticket information:

```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && python tools/jira_client.py fetch $ARGUMENTS
```

```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && python tools/jira_client.py comments $ARGUMENTS
```

```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && python tools/jira_client.py attachments $ARGUMENTS
```

After fetching, if there are image attachments (screenshots), read them using the Read tool to visually inspect the bug.

---

## Phase 2: Root Cause Analysis (RCA)

Using the ticket details (summary, description, comments, screenshots), identify which codebase the bug belongs to and search for the root cause.

### Codebase Knowledge

You have access to TWO codebases that work together:

**1. krista-ui-agent-extension (THIS REPO - Java 21, Gradle)**
Location: `/Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension`
Role: Krista Platform extension that bridges Krista orchestration with the UI Agent service.
Key areas:
- `src/main/java/app/krista/extension/uiautomationagent/interaction/KristaUiAgentExtension.java` — Main extension lifecycle
- `src/main/java/app/krista/extension/uiautomationagent/interaction/catalog/InteractionArea.java` — Catalog requests (Single Command, Interactive Session Start/Execute/End)
- `src/main/java/app/krista/extension/uiautomationagent/esb/UiAgentRawClient.java` — Raw Artemis client (Google Protobuf, per-request consumers)
- `src/main/java/app/krista/extension/uiautomationagent/events/UiAutomationEventListenerHandler.java` — Async event handling, file downloads, response parsing
- `src/main/java/app/krista/extension/uiautomationagent/factory/ExtensionResponseFactory.java` — Response construction
- `src/main/java/app/krista/extension/uiautomationagent/events/UiAgentResponseParser.java` — Protobuf response parsing
- `src/main/java/app/krista/extension/uiautomationagent/file/FileHandlingService.java` — HMAC-signed file downloads
- `src/main/java/app/krista/extension/uiautomationagent/config/ExtensionAttributes.java` — Extension configuration
- `src/main/java/app/krista/extension/uiautomationagent/esb/UiAgentEsbConfigProvider.java` — ESB broker config
- `src/main/proto/ui_agent_messages.proto` — Protobuf schema (shared contract)

**2. krista-ui-agent (Node.js 22, ES Modules)**
Location: `/Users/mayurmistry/development/krista-ui-agent`
Role: The actual UI automation service that runs browser automation via Playwright + LLM.
Key areas:
- `node/src/cli/index.js` — Entry point (handles ESB, HTTP, interactive modes)
- `node/src/cli/apiRouter.js` — Routes API requests to handlers, config merging
- `node/src/cli/httpServer.js` — Express HTTP server with WebUI
- `node/src/core/engine.js` — Main execution: parsePrompt → steps → actions
- `node/src/core/actions/` — Action handlers (click, fill, navigate, waitFor, evaluate, download, screenshot, validation, processWithLLM, conditional, repeat, forEach, iframeAction, etc.)
- `node/src/core/browser/manager.js` — Playwright browser initialization
- `node/src/core/browser/sessionManager.js` — Session lifecycle (create/get/close, 20-min timeout)
- `node/src/core/llm/modelRouter.js` — Multi-provider LLM router (Claude, GPT-4, Gemini) with fallback
- `node/src/core/llm/promptTemplates.js` — Prompt generation for step parsing and selectors
- `node/src/core/selectors/resolver.js` — Selector resolution with LLM healing
- `node/src/core/selectors/cacheManager.js` — Multi-tenant selector cache (per-domain, 7-day TTL)
- `node/src/core/selectors/healer.js` — LLM-powered broken selector healing
- `node/src/core/utils/retry.js` — Retry with exponential backoff
- `node/src/core/utils/errorReporting.js` — Enhanced error reporting
- `node/src/core/utils/screenshots.js` — Screenshot management
- `node/src/core/utils/downloadManager.js` — File download tracking
- `node/src/core/security/hmacUrlSigner.js` — HMAC download URL signing
- `node/src/core/profiling/profilingService.js` — Session profiling
- `node/src/esb/esbAdapter.js` — AMQP message handling (rhea library)
- `node/src/esb/protobufCodec.js` — Protobuf encode/decode
- `node/src/esb/replyBuilder.js` — Standardized response building
- `node/src/esb/connectionManager.js` — AMQP connection lifecycle
- `node/src/esb/extensionRegistry.js` — Extension tier management
- `node/src/config/index.js` — ConfigManager
- `config/app.config.json` — Application config (ESB, logging, cache)
- `protos/ui_agent_messages.proto` — Protobuf schema (same as extension's)

### Communication Flow
```
Krista Platform → Extension (Java) → [Protobuf over Artemis Core port 61616] → UI Agent Service (Node.js)
                                      Request queue: krista-ui-agent-1-IN (shared)
                                      Response queue: per-extension unique queue
                                      Session affinity: JMSXGroupId
```

### RCA Process

1. Read the ticket description and comments carefully to understand the bug symptoms
2. Look at any screenshots to understand the visual context
3. Determine which codebase is likely affected based on the bug:
   - **Extension bugs**: Response formatting, file handling, ESB communication, session tracking, attribute validation
   - **Service bugs**: Browser automation failures, selector issues, LLM integration, action execution, prompt parsing, screenshot capture, download handling
   - **Integration bugs**: Protobuf serialization mismatches, queue routing, correlation ID issues, timeout handling
4. Search the relevant codebase(s) for the root cause using Grep and Read
5. Trace the code flow from entry point to the failure point

---

## Phase 3: Present RCA and Proposed Fix

Present your findings to the user in this format:

```
## Root Cause Analysis

**Ticket**: $ARGUMENTS — {summary}
**Affected Component**: {component name and file path}
**Root Cause**: {clear explanation of why the bug occurs}

## Evidence
- {code references with file:line_number}
- {how ticket symptoms map to the code issue}

## Proposed Fix
{description of the fix with specific files and changes}

## Impact Assessment
- **Risk**: Low/Medium/High
- **Files Changed**: {list}
- **Tests Needed**: {what to verify}
```

Then use AskUserQuestion to ask:
> "Do you approve this fix? (yes/no/modify)"

---

## Phase 4: Implement Fix

Only proceed after user confirms. When implementing:

1. Make the minimal changes needed to fix the bug
2. Ensure protobuf schema consistency if both repos are affected
3. Run tests after fixing:
   - Extension: `cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && ./gradlew test`
   - Service: `cd /Users/mayurmistry/development/krista-ui-agent && npm test`
4. Report test results to the user

---

## Phase 5: Generate JIRA Comment

After the fix is implemented and tests pass, generate a formatted JIRA comment for the user to copy. The comment MUST include all four sections below:

```
## RCA
{Root cause explanation — what was wrong in the code, which file/line, why it caused the observed symptoms. Be specific and technical but concise. Reference the actual code path that led to the failure.}

## Fix
{What was changed and why. List the specific file(s) changed and describe the code modification. Keep it factual — what the code did before vs what it does now.}

**File(s) Changed:** {list of files}

## Regression Area
{List the functional areas that could be affected by this change. Include both the direct area being fixed AND adjacent areas that share code paths or data structures. Use bullet points.}

## Dev Testcases
{Numbered list of specific test scenarios the developer should verify. Include:
- Happy path (the bug is fixed)
- Edge cases related to the fix
- Negative cases (error handling still works)
- Regression cases (existing functionality not broken)
Each testcase should be actionable: describe the steps and expected result.}
```

Present the comment to the user so they can copy it to the JIRA ticket.

---

## Important Rules

- NEVER guess the root cause. Always trace through the actual code.
- If the bug spans both codebases, fix both and ensure protobuf compatibility.
- Read actual files before proposing changes. Do not assume code structure.
- For ESB/messaging bugs, check BOTH the sender (extension) and receiver (service) sides.
- The extension uses Java 21 with Krista framework annotations. The service uses Node.js ES Modules.
- Keep fixes minimal. Don't refactor surrounding code.
- ALWAYS generate the JIRA comment after implementing the fix — do not skip Phase 5.
