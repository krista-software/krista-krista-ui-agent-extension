# Execute Interactive Session Steps

## Overview

Execute automation steps within an active browser session. This request maintains browser state and context between commands, allowing you to build complex workflows step-by-step. The browser remains open and remembers previous actions.

## Catalog Request Details

- **Request ID**: `localDomainRequest_3b7651c3-e71e-4bca-ae06-5ccc65b79ed0`
- **Request Name**: Execute Interactive Session Steps
- **Area**: Interaction
- **Type**: WAIT_FOR_EVENT

## Input Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| **Session Id** | Text | Yes | The session ID returned from "Interactive Session Start" | "session_abc123def456" |
| **Steps** | Paragraph | Yes | Natural language description of the steps to perform | "Click the login button and enter credentials" |
| **Validation Steps** | Paragraph | No | Optional validation steps to verify the task completed successfully | "Verify the dashboard is displayed" |

## Output Fields

| Field | Type | Description |
|-------|------|-------------|
| **Response** | FreeForm | Structured response containing step execution results, status, and any extracted data |
| **Session Id** | Text | The session ID (echoed back for reference) |
| **Downloaded Files** | File (Multiple) | Any files downloaded during the steps |
| **Screenshots** | File (Multiple) | Screenshots captured during step execution |

## How It Works

1. **Provide Session ID**: Use the session ID from "Interactive Session Start"
2. **Describe Steps**: Write natural language steps to execute
3. **AI Processing**: The LLM interprets your steps in the context of the current browser state
4. **Browser Execution**: Steps are executed in the existing browser session
5. **State Preservation**: Browser state is maintained for subsequent requests
6. **Result Return**: Results, screenshots, and files are returned

## Example Use Cases

### Example 1: Multi-Step Login Flow

**Step 1: Start Session**
```
Request: Interactive Session Start
Response: { "sessionId": "session_123" }
```

**Step 2: Navigate to Login**
```
Request: Execute Interactive Session Steps
Input:
  - Session Id: "session_123"
  - Steps: "Navigate to https://example.com/login"
  - Validation Steps: "Verify login form is visible"
```

**Step 3: Enter Credentials**
```
Request: Execute Interactive Session Steps
Input:
  - Session Id: "session_123"
  - Steps: "Enter 'john@example.com' in the email field and 'password123' in the password field"
```

**Step 4: Submit and Verify**
```
Request: Execute Interactive Session Steps
Input:
  - Session Id: "session_123"
  - Steps: "Click the login button"
  - Validation Steps: "Verify the dashboard page is displayed and user name is shown"
```

### Example 2: Data Collection Across Pages

**Step 1: Start Session and Navigate**
```
Session Id: "session_456"
Steps: "Go to https://example.com/products and click on the first product"
```

**Step 2: Extract Product Details**
```
Session Id: "session_456"
Steps: "Extract the product name, price, description, and availability status"
```

**Step 3: Navigate to Next Product**
```
Session Id: "session_456"
Steps: "Click the 'Next Product' button"
```

**Step 4: Extract More Data**
```
Session Id: "session_456"
Steps: "Extract the product name, price, description, and availability status"
```

### Example 3: Form Filling with Review

**Step 1: Fill First Page**
```
Session Id: "session_789"
Steps: "Fill in personal information: Name='John Doe', Email='john@example.com', Phone='555-1234'"
```

**Step 2: Navigate to Next Page**
```
Session Id: "session_789"
Steps: "Click 'Next' button to go to address page"
```

**Step 3: Fill Address**
```
Session Id: "session_789"
Steps: "Fill in address: Street='123 Main St', City='Springfield', ZIP='12345'"
```

**Step 4: Review and Submit**
```
Session Id: "session_789"
Steps: "Click 'Review' button, verify all information is correct, then click 'Submit'"
Validation Steps: "Verify confirmation message is displayed"
```

## Best Practices

### Writing Steps for Sessions

1. **Reference Previous State**: You can refer to elements from previous steps
   ```
   ✅ Good: "Click the submit button that appeared after the previous step"
   ```

2. **Build on Context**: Each step builds on the browser's current state
   ```
   Step 1: "Navigate to products page"
   Step 2: "Click on the first product" (knows we're on products page)
   Step 3: "Add to cart" (knows we're viewing a product)
   ```

3. **Use Incremental Steps**: Break complex workflows into smaller, manageable steps
   ```
   ✅ Good: Multiple small steps
   ❌ Bad: One giant step trying to do everything
   ```

4. **Validate Between Steps**: Use validation to ensure each step succeeded
   ```
   Steps: "Click login button"
   Validation: "Verify we're on the dashboard page"
   ```

### Session State Management

- **Browser State Persists**: Cookies, local storage, and page state are maintained
- **Navigation History**: You can use browser back/forward if needed
- **Downloads**: Files download to the session's download directory
- **Authentication**: Login state persists across steps

### Error Recovery

If a step fails:
1. **Review Error**: Check the error message and screenshot
2. **Retry**: Try the step again with adjusted instructions
3. **Recovery Steps**: Execute recovery steps if needed
4. **Continue or Abort**: Either continue the workflow or end the session

## Example Response

```json
{
  "status": "success",
  "sessionId": "session_123",
  "data": {
    "productName": "Example Product",
    "price": "$29.99",
    "inStock": true
  },
  "message": "Steps executed successfully",
  "screenshots": ["screenshot1.png"],
  "executionTime": "3.2s"
}
```

## Limitations

- **Session Must Exist**: Session ID must be from an active session
- **Sequential Execution**: Steps execute in order, not in parallel
- **Timeout**: Each step request has a timeout (default: 5 minutes)
- **Browser Limits**: Subject to browser capabilities and limitations

## Error Handling

### Invalid Session ID

**Error**: "Session not found"

**Solutions**:
- Verify session ID is correct
- Check session hasn't timed out
- Ensure session wasn't already ended

### Step Execution Failure

**Error**: "Failed to execute step: [reason]"

**Solutions**:
- Review screenshot to see current state
- Adjust step instructions
- Add wait time if elements are loading
- Check if previous steps completed successfully

## Next Steps

- Learn how to [End Sessions](/pages/InteractiveSessionEnd.md)
- Review [Writing Effective Prompts](/pages/WritingPrompts.md)
- Understand [Validation Steps](/pages/ValidationSteps.md)

---

> **💡 Tip**: Break complex workflows into small, focused steps. This makes debugging easier and improves reliability.

