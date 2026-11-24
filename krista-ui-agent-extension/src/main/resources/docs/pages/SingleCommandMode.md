# Single Command Mode

## Overview

Single Command Mode allows you to execute browser automation tasks using natural language commands. Each request is independent and completes immediately without maintaining state or session context. This mode is ideal for quick, one-off automation tasks.

## Catalog Request Details

- **Request ID**: `WaitForEventRequests.PROMPT_BASED_INTERACTION`
- **Request Name**: Single Command Mode
- **Area**: Interaction
- **Type**: WAIT_FOR_EVENT

## Input Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| **Prompt** | Paragraph | Yes | Natural language description of the task to perform | "Go to google.com and search for 'Krista automation'" |
| **Validation Steps** | Paragraph | No | Optional validation steps to verify the task completed successfully | "Verify search results are displayed" |

## Output Fields

| Field | Type | Description |
|-------|------|-------------|
| **Response** | FreeForm | Structured response containing task execution results, status, and any extracted data |
| **Downloaded Files** | File (Multiple) | Any files downloaded during the automation task |
| **Screenshots** | File (Multiple) | Screenshots captured during task execution |

## How It Works

1. **Submit Request**: Provide a natural language prompt describing the automation task
2. **AI Processing**: The LLM interprets your prompt and generates browser automation steps
3. **Browser Execution**: The UI Agent Service executes the steps in a browser
4. **Result Collection**: Results, screenshots, and downloaded files are collected
5. **Response Return**: All outputs are returned to Krista

## Example Use Cases

### Example 1: Web Search
**Prompt**:
```
Go to google.com and search for "Krista automation platform". 
Click on the first result and extract the page title.
```

**Expected Response**:
```json
{
  "status": "success",
  "data": {
    "pageTitle": "Krista - Intelligent Automation Platform"
  },
  "message": "Successfully navigated and extracted page title"
}
```

### Example 2: Form Submission
**Prompt**:
```
Navigate to https://example.com/contact-form
Fill in the name field with "John Doe"
Fill in the email field with "john@example.com"
Fill in the message field with "Test message"
Click the submit button
```

**Validation Steps**:
```
Verify that a success message is displayed
Take a screenshot of the confirmation page
```

### Example 3: Data Extraction
**Prompt**:
```
Go to https://example.com/products
Extract the names and prices of all products on the page
Return the data as a structured list
```

**Expected Response**:
```json
{
  "status": "success",
  "data": {
    "products": [
      {"name": "Product A", "price": "$29.99"},
      {"name": "Product B", "price": "$49.99"}
    ]
  }
}
```

### Example 4: File Download
**Prompt**:
```
Navigate to https://example.com/downloads
Click on the "Download Report" button
Wait for the download to complete
```

**Output**: Downloaded file will be available in the "Downloaded Files" field

## Best Practices

### Writing Effective Prompts

1. **Be Specific**: Clearly describe each step you want to perform
   - ❌ Bad: "Search for something"
   - ✅ Good: "Go to google.com and search for 'Krista automation'"

2. **Include URLs**: Always specify full URLs when navigating
   - ❌ Bad: "Go to the login page"
   - ✅ Good: "Navigate to https://example.com/login"

3. **Specify Data to Extract**: Clearly state what information you need
   - ❌ Bad: "Get the data from the page"
   - ✅ Good: "Extract the product name, price, and description from each item"

4. **Use Sequential Steps**: Break complex tasks into clear steps
   - ✅ Good: "1. Go to URL, 2. Click login, 3. Enter credentials, 4. Submit form"

### Using Validation Steps

Validation steps help ensure your automation completed successfully:

```
Verify that the page title contains "Success"
Check that the confirmation message is visible
Ensure the download button is no longer loading
```

### Error Handling

If the automation fails, the response will include:
- **Error message**: Description of what went wrong
- **Screenshot**: Visual state when error occurred
- **Partial results**: Any data collected before failure

## Limitations

- **No State Persistence**: Each request starts with a fresh browser session
- **Single Page Focus**: Best for tasks on a single page or simple navigation
- **Timeout**: Requests timeout after a configured duration (default: 5 minutes)
- **No Session Reuse**: Cannot reference previous requests

## When to Use Single Command Mode

✅ **Use Single Command Mode when**:
- Performing quick, one-off tasks
- Automating simple web interactions
- No need to maintain browser state
- Each task is independent

❌ **Use Interactive Session Mode instead when**:
- Building complex multi-step workflows
- Need to maintain browser state across steps
- Performing multiple related tasks
- Require session persistence

## Next Steps

- Learn about [Interactive Session Mode](/pages/InteractiveSessionStart.md) for complex workflows
- Explore [Writing Effective Prompts](/pages/WritingPrompts.md) for better results
- Review [Validation Steps](/pages/ValidationSteps.md) for quality assurance

---

> **💡 Tip**: Start with simple prompts and gradually add complexity. Use validation steps to ensure reliability in production workflows.

