# Error Handling

## Overview

Understanding how to handle errors in browser automation is crucial for building reliable workflows. This guide covers common errors, troubleshooting strategies, and best practices for error recovery.

## Common Error Types

### 1. Connection Errors

**Symptoms**: "Failed to connect to UI Agent Service"

**Causes**:
- UI Agent Service is not running
- Network connectivity issues
- Incorrect URL configuration
- Firewall blocking connection

**Solutions**:
- Verify service is running: `curl http://your-url/health`
- Check URL configuration in extension settings
- Verify network connectivity
- Check firewall rules

### 2. Authentication Errors

**Symptoms**: "Invalid LLM API key", "Authentication failed"

**Causes**:
- Incorrect API key
- Expired or revoked API key
- API key lacks required permissions
- Rate limit exceeded

**Solutions**:
- Verify API key is correct (no extra spaces)
- Regenerate API key if expired
- Check API key permissions
- Monitor API usage and rate limits

### 3. Element Not Found Errors

**Symptoms**: "Could not find element", "Element not visible"

**Causes**:
- Element doesn't exist on page
- Page hasn't finished loading
- Element is hidden or obscured
- Incorrect element selector

**Solutions**:
- Add wait time for page to load
- Verify element exists by checking screenshot
- Use more specific element descriptions
- Check if element is in an iframe

### 4. Timeout Errors

**Symptoms**: "Request timed out", "Operation exceeded timeout"

**Causes**:
- Page takes too long to load
- Network is slow
- Complex automation takes too long
- Infinite loop or stuck operation

**Solutions**:
- Increase timeout settings if needed
- Break complex tasks into smaller steps
- Check network performance
- Simplify automation steps

### 5. Session Errors

**Symptoms**: "Session not found", "Session expired"

**Causes**:
- Session ID is incorrect
- Session timed out due to inactivity
- Session was already ended
- Extension restarted

**Solutions**:
- Verify session ID is correct
- Reduce time between session steps
- Check if session was already ended
- Start a new session if needed

## Error Response Format

When an error occurs, the response includes error details:

```json
{
  "status": "error",
  "error": {
    "type": "ElementNotFoundError",
    "message": "Could not find element with text 'Submit'",
    "details": "The element may not exist or the page may not have loaded completely"
  },
  "screenshot": "error_screenshot.png",
  "timestamp": "2024-02-04T10:30:00Z"
}
```

## Error Recovery Strategies

### Strategy 1: Retry with Adjusted Instructions

If an element isn't found, try with more specific instructions:

```
Original: "Click the submit button"
Retry: "Click the blue button with text 'Submit' at the bottom of the form"
```

### Strategy 2: Add Wait Time

If page hasn't loaded, add explicit wait:

```
Original: "Click the login button"
Retry: "Wait for the page to finish loading, then click the login button"
```

### Strategy 3: Verify State First

Check page state before proceeding:

```
Step 1: "Navigate to login page"
Step 2: "Verify the login form is visible"
Step 3: "Enter credentials and submit"
```

### Strategy 4: Use Validation Steps

Catch errors early with validation:

```
Prompt: "Click the submit button"
Validation: "Verify the success message is displayed"
```

### Strategy 5: Graceful Degradation

Have fallback options:

```
"Try to click the 'Login' button. If it's not found, look for a button with text 'Sign In' instead."
```

## Error Handling Patterns

### Pattern 1: Try-Catch Workflow

```
Workflow:
1. Try main automation
2. If error occurs:
   - Log error details
   - Take screenshot
   - Attempt recovery
   - If recovery fails, end session and report error
```

### Pattern 2: Validation-Based Error Detection

```
Step 1: Execute automation
Step 2: Validate result
Step 3: If validation fails:
   - Review error
   - Retry with adjusted approach
   - Or abort and report failure
```

### Pattern 3: Progressive Fallback

```
Attempt 1: Try primary method
If fails → Attempt 2: Try alternative method
If fails → Attempt 3: Try simplified method
If fails → Report error and abort
```

## Best Practices

### 1. Always Use Validation Steps for Critical Operations

```
✅ Good:
Prompt: "Submit payment form"
Validation: "Verify payment confirmation is displayed"

❌ Bad:
Prompt: "Submit payment form"
(No validation - might not know if it failed)
```

### 2. Include Error Context in Prompts

```
✅ Good:
"Click the 'Submit' button. If it's disabled, first fill in any required fields marked with a red asterisk."

❌ Bad:
"Click the submit button"
(No handling for disabled state)
```

### 3. Use Screenshots for Debugging

Always review screenshots when errors occur:
- Shows exact page state when error happened
- Helps identify what went wrong
- Guides adjustments to prompts

### 4. Log Errors for Analysis

Keep track of errors to identify patterns:
- Which steps fail most often?
- What types of errors are common?
- Are errors related to specific sites or operations?

### 5. Clean Up After Errors

For Interactive Session Mode:

```
try {
  sessionId = startSession()
  executeSteps(sessionId, steps)
} catch (error) {
  logError(error)
  takeScreenshot()
} finally {
  endSession(sessionId)  // Always clean up
}
```

## Troubleshooting Guide

### Issue: Automation Works Sometimes But Not Always

**Possible Causes**:
- Page load timing varies
- Dynamic content loads at different speeds
- Network latency fluctuates

**Solutions**:
- Add explicit wait times
- Use validation to ensure page is ready
- Make prompts more robust to timing variations

### Issue: Element Found in Screenshot But Not by Automation

**Possible Causes**:
- Element is in an iframe
- Element is dynamically loaded
- Element description is ambiguous

**Solutions**:
- Specify if element is in an iframe
- Wait for element to appear
- Use more specific element descriptions

### Issue: Session Keeps Timing Out

**Possible Causes**:
- Steps take too long
- Too much time between steps
- Session timeout is too short

**Solutions**:
- Execute steps more quickly
- Reduce time between session requests
- Break into smaller sessions

### Issue: Data Extraction Returns Empty or Incorrect Data

**Possible Causes**:
- Wrong element selected
- Page structure changed
- Data hasn't loaded yet

**Solutions**:
- Review screenshot to verify element
- Wait for data to load
- Use more specific extraction instructions
- Validate extracted data

## Error Prevention

### 1. Test in Development First

- Test automation in dev/staging environment
- Verify prompts work reliably
- Identify potential issues early

### 2. Use Specific Selectors

```
❌ Vague: "Click the button"
✅ Specific: "Click the blue 'Submit' button at the bottom of the form"
```

### 3. Handle Edge Cases

```
"If the popup appears, close it. Then proceed with the main task."
```

### 4. Monitor and Alert

- Set up monitoring for automation failures
- Alert on repeated errors
- Track success/failure rates

## Getting Help

If you encounter persistent errors:

1. **Review Documentation**: Check relevant guides
2. **Check Screenshots**: Visual debugging is powerful
3. **Simplify**: Try simpler version of the automation
4. **Test Manually**: Verify the task is possible manually
5. **Contact Support**: Provide error details, screenshots, and steps to reproduce

## Next Steps

- Review [Writing Effective Prompts](/pages/WritingPrompts.md) to prevent errors
- Learn about [Validation Steps](/pages/ValidationSteps.md) for error detection
- Practice with [Single Command Mode](/pages/SingleCommandMode.md)

---

> **💡 Tip**: Most errors can be prevented with specific prompts, proper validation, and adequate wait times. When errors do occur, screenshots are your best debugging tool.

