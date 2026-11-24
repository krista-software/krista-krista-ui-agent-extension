# Interactive Session Start

## Overview

Start a new browser automation session that maintains state across multiple commands. The session keeps the browser open and remembers context between actions, allowing you to build complex workflows step-by-step.

## Catalog Request Details

- **Request ID**: `localDomainRequest_4d1d1df7-83f1-487a-98bb-66e31681ca1c`
- **Request Name**: Interactive Session Start
- **Area**: Interaction
- **Type**: CHANGE_SYSTEM

## Input Parameters

This request requires no input parameters. Simply invoke it to start a new session.

## Output Fields

| Field | Type | Description |
|-------|------|-------------|
| **Session Id** | Text | Unique identifier for the session. Use this ID in subsequent requests to execute steps within this session |

## How It Works

1. **Invoke Request**: Call "Interactive Session Start" with no parameters
2. **Session Creation**: A new browser session is created with a unique ID
3. **Browser Launch**: A browser instance is launched and kept alive
4. **Session ID Return**: The session ID is returned for use in subsequent requests
5. **State Persistence**: The browser and its state remain active until session is ended

## Example Response

```json
{
  "sessionId": "session_abc123def456",
  "status": "active",
  "message": "Session started successfully",
  "createdAt": "2024-02-04T10:30:00Z"
}
```

## Example Workflow

### Step 1: Start Session
```
Request: Interactive Session Start
Response: { "sessionId": "session_abc123" }
```

### Step 2: Execute Steps (using the session ID)
```
Request: Execute Interactive Session Steps
Input:
  - Session Id: "session_abc123"
  - Steps: "Navigate to https://example.com/login"
```

### Step 3: Continue with More Steps
```
Request: Execute Interactive Session Steps
Input:
  - Session Id: "session_abc123"
  - Steps: "Enter username 'john@example.com' and password, then click login"
```

### Step 4: End Session
```
Request: Interactive Session End
Input:
  - Session Id: "session_abc123"
```

## Use Cases

### Multi-Step Authentication Flow
1. Start session
2. Navigate to login page
3. Enter credentials
4. Navigate to dashboard
5. Perform authenticated actions
6. End session

### Data Collection Across Multiple Pages
1. Start session
2. Navigate to search page
3. Enter search criteria
4. Iterate through result pages
5. Collect data from each page
6. End session

### Complex Form Workflows
1. Start session
2. Fill multi-page form
3. Review and edit entries
4. Submit final form
5. Download confirmation
6. End session

## Best Practices

### Session Management

1. **Always End Sessions**: Call "Interactive Session End" when done to free resources
   ```
   ✅ Good: Start → Execute Steps → End Session
   ❌ Bad: Start → Execute Steps (session left open)
   ```

2. **Store Session ID**: Save the session ID for use in subsequent requests
   ```
   sessionId = response.sessionId
   ```

3. **Handle Errors**: If a step fails, you can continue or end the session
   ```
   If error occurs:
     - Option 1: Try recovery steps
     - Option 2: End session and start fresh
   ```

4. **Session Timeout**: Sessions automatically timeout after inactivity (default: 30 minutes)

### Performance Optimization

- **Reuse Sessions**: Execute multiple related steps in one session
- **Minimize Session Duration**: End sessions promptly to free resources
- **Batch Operations**: Group related steps together

## Limitations

- **Concurrent Sessions**: Limited number of concurrent sessions per extension instance
- **Session Timeout**: Inactive sessions are automatically closed after timeout period
- **Resource Usage**: Each session consumes browser resources
- **No Persistence**: Sessions are lost if extension restarts

## Error Handling

### Session Creation Failures

**Possible Causes**:
- Maximum concurrent sessions reached
- UI Agent Service unavailable
- Insufficient resources

**Solutions**:
- End unused sessions
- Verify UI Agent Service is running
- Check resource availability

## Next Steps

- Learn how to [Execute Interactive Session Steps](/pages/ExecuteInteractiveSessionSteps.md)
- Understand how to [End Sessions](/pages/InteractiveSessionEnd.md)
- Review [Automation Modes](/pages/AutomationModes.md) to choose the right approach

---

> **💡 Tip**: Interactive sessions are perfect for complex workflows that require multiple steps with shared context. Always remember to end your sessions to free up resources.

