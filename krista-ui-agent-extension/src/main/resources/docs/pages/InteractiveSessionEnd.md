# Interactive Session End

## Overview

Close an active browser automation session. This request closes the browser, cleans up resources, and returns a summary of the session. Always end sessions when you're done to free up system resources.

## Catalog Request Details

- **Request ID**: `localDomainRequest_d705685e-b9e3-422b-9716-7ae91916e5f9`
- **Request Name**: Interactive Session End
- **Area**: Interaction
- **Type**: CHANGE_SYSTEM

## Input Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| **Session Id** | Text | Yes | The session ID to end | "session_abc123def456" |

## Output Fields

| Field | Type | Description |
|-------|------|-------------|
| **Summary** | Object | Session summary containing Session Id, Message, and Status | `{ "Session Id": "session_123", "Message": "Session ended successfully", "Status": "closed" }` |
| **Profiling Data** | File | Optional performance profiling data from the session |

## How It Works

1. **Provide Session ID**: Specify the session ID to end
2. **Browser Closure**: The browser instance is closed
3. **Resource Cleanup**: All session resources are freed
4. **Summary Generation**: A summary of the session is created
5. **Response Return**: Summary and profiling data are returned

## Example Response

```json
{
  "summary": {
    "sessionId": "session_abc123def456",
    "message": "Session ended successfully",
    "status": "closed",
    "duration": "5m 32s",
    "stepsExecuted": 8,
    "screenshotsCaptured": 3,
    "filesDownloaded": 1
  },
  "profilingData": "session_abc123_profile.json"
}
```

## Example Workflow

### Complete Session Lifecycle

**Step 1: Start Session**
```
Request: Interactive Session Start
Response: { "sessionId": "session_123" }
```

**Step 2: Execute Multiple Steps**
```
Request: Execute Interactive Session Steps
Input:
  - Session Id: "session_123"
  - Steps: "Navigate to https://example.com"

Request: Execute Interactive Session Steps
Input:
  - Session Id: "session_123"
  - Steps: "Click login and enter credentials"

Request: Execute Interactive Session Steps
Input:
  - Session Id: "session_123"
  - Steps: "Navigate to dashboard and extract data"
```

**Step 3: End Session**
```
Request: Interactive Session End
Input:
  - Session Id: "session_123"
Response: {
  "summary": {
    "sessionId": "session_123",
    "message": "Session ended successfully",
    "status": "closed"
  }
}
```

## Use Cases

### Normal Session Completion
After completing all automation steps, end the session to free resources:
```
1. Start session
2. Execute all required steps
3. End session ← Clean completion
```

### Error Recovery
If an error occurs during automation, end the session to clean up:
```
1. Start session
2. Execute steps
3. Error occurs
4. End session ← Clean up after error
```

### Resource Management
End sessions promptly to ensure resources are available for other workflows:
```
1. Complete workflow
2. End session immediately ← Free resources
3. Start new session if needed
```

## Best Practices

### Always End Sessions

**✅ Good Practice**:
```
try {
  sessionId = startSession()
  executeSteps(sessionId, steps)
  extractData(sessionId)
} finally {
  endSession(sessionId)  // Always end, even if error occurs
}
```

**❌ Bad Practice**:
```
sessionId = startSession()
executeSteps(sessionId, steps)
// Forgot to end session - resources leak!
```

### Handle Errors Gracefully

Even if steps fail, always end the session:
```
1. Start session
2. Execute steps
3. If error: Log error, then end session
4. If success: End session
```

### Monitor Session Duration

- **Short Sessions**: End immediately after completing tasks
- **Long Sessions**: Consider breaking into multiple shorter sessions
- **Idle Sessions**: Automatically timeout after inactivity

### Review Session Summary

The summary provides valuable information:
- **Duration**: How long the session ran
- **Steps Executed**: Number of step requests processed
- **Resources Used**: Screenshots, downloads, etc.
- **Status**: Final session status

## Session Summary Fields

| Field | Description | Example |
|-------|-------------|---------|
| **Session Id** | Unique session identifier | "session_abc123" |
| **Message** | Human-readable status message | "Session ended successfully" |
| **Status** | Final session status | "closed", "error", "timeout" |
| **Duration** | Total session duration | "5m 32s" |
| **Steps Executed** | Number of step requests | 8 |
| **Screenshots Captured** | Number of screenshots taken | 3 |
| **Files Downloaded** | Number of files downloaded | 1 |

## Error Handling

### Session Not Found

**Error**: "Session not found: session_123"

**Possible Causes**:
- Session ID is incorrect
- Session already ended
- Session timed out
- Session never existed

**Solutions**:
- Verify session ID is correct
- Check if session was already ended
- Review session creation logs

### Session Already Closed

**Error**: "Session already closed: session_123"

**Cause**: Attempting to end a session that's already been ended

**Solution**: This is usually harmless - the session is already cleaned up

### Cleanup Failure

**Error**: "Failed to clean up session resources"

**Cause**: System error during resource cleanup

**Solution**: 
- Resources may still be freed automatically
- Check system logs for details
- Contact support if persists

## Profiling Data

The profiling data file contains:
- **Performance Metrics**: Execution times for each step
- **Resource Usage**: Memory, CPU usage during session
- **Network Activity**: Requests made, data transferred
- **Browser Events**: Page loads, navigation events

Use profiling data to:
- Optimize slow steps
- Identify bottlenecks
- Monitor resource usage
- Debug performance issues

## Automatic Session Cleanup

Sessions are automatically ended in these scenarios:
- **Timeout**: No activity for configured timeout period (default: 30 minutes)
- **Extension Restart**: All sessions are closed when extension restarts
- **System Shutdown**: Sessions are cleaned up during shutdown
- **Error Threshold**: Too many consecutive errors in a session

## Next Steps

- Review complete [Interactive Session Workflow](/pages/InteractiveSessionStart.md)
- Learn about [Automation Modes](/pages/AutomationModes.md)
- Explore [Error Handling](/pages/ErrorHandling.md) best practices

---

> **💡 Tip**: Always end your sessions! Use try-finally blocks or equivalent error handling to ensure sessions are closed even when errors occur.

