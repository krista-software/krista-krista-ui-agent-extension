# Understanding Automation Modes

## Overview

The Agentic Browser Extension offers two distinct automation modes, each designed for different use cases. Understanding when to use each mode will help you build more efficient and reliable automation workflows.

## Mode Comparison

| Feature | Single Command Mode | Interactive Session Mode |
|---------|---------------------|-------------------------|
| **Session State** | No state persistence | Maintains state across steps |
| **Browser Instance** | New browser for each request | Same browser across multiple requests |
| **Use Case** | Quick, one-off tasks | Complex multi-step workflows |
| **Setup Complexity** | Simple - just provide prompt | Requires session management |
| **Resource Usage** | Lower (browser closes immediately) | Higher (browser stays open) |
| **Execution Speed** | Slower for multiple tasks | Faster for related tasks |
| **Context Awareness** | No context between requests | Full context between steps |
| **Best For** | Independent tasks | Related sequential tasks |

## Single Command Mode

### When to Use

✅ **Perfect for**:
- One-off automation tasks
- Independent web interactions
- Simple data extraction
- Quick form submissions
- Single-page operations

❌ **Not ideal for**:
- Multi-step workflows requiring state
- Complex authentication flows
- Tasks spanning multiple pages
- Workflows with dependencies between steps

### Example Scenarios

**Scenario 1: Quick Web Search**
```
Task: Search Google and extract first result
Mode: Single Command Mode
Reason: Simple, independent task with no state requirements
```

**Scenario 2: Form Submission**
```
Task: Fill and submit a contact form
Mode: Single Command Mode
Reason: Single-page operation, no need for session persistence
```

**Scenario 3: Data Extraction**
```
Task: Extract product prices from a single page
Mode: Single Command Mode
Reason: One-time data collection, no navigation required
```

### Advantages

- **Simplicity**: No session management required
- **Isolation**: Each request is independent
- **Resource Efficiency**: Browser closes immediately after completion
- **No Cleanup**: No need to manually end sessions

### Limitations

- **No State**: Cannot reference previous actions
- **Slower for Multiple Tasks**: Each request starts fresh
- **No Context**: Cannot build on previous steps

## Interactive Session Mode

### When to Use

✅ **Perfect for**:
- Multi-step authentication flows
- Complex workflows spanning multiple pages
- Data collection across multiple pages
- Tasks requiring browser state persistence
- Workflows with dependencies between steps

❌ **Not ideal for**:
- Simple one-off tasks
- Independent operations
- When you don't need state persistence

### Example Scenarios


**Scenario 1: Multi-Page Data Collection**
```
Task: Collect data from paginated results
Mode: Interactive Session Mode
Reason: Need to maintain navigation state across pages

Steps:
1. Start session
2. Navigate to search page
3. Enter search criteria
4. Extract data from page 1
5. Click next page
6. Extract data from page 2
7. Repeat for all pages
8. End session
```

**Scenario 2: Complex Form Workflow**
```
Task: Fill multi-page form with review step
Mode: Interactive Session Mode
Reason: Form state must persist across pages

Steps:
1. Start session
2. Fill page 1 of form
3. Navigate to page 2
4. Fill page 2 of form
5. Navigate to review page
6. Verify all data
7. Submit form
8. End session
```

### Advantages

- **State Persistence**: Browser state maintained across steps
- **Context Awareness**: Each step builds on previous steps
- **Efficiency**: Faster for multiple related tasks
- **Flexibility**: Can adjust workflow based on results

### Limitations

- **Resource Usage**: Browser stays open, consuming resources
- **Session Management**: Must manually end sessions
- **Complexity**: Requires more setup and management

## Decision Tree

```
Is this a one-off task?
├─ Yes → Use Single Command Mode
└─ No → Does it require multiple steps?
    ├─ No → Use Single Command Mode
    └─ Yes → Do the steps depend on each other?
        ├─ No → Use Single Command Mode (multiple independent requests)
        └─ Yes → Use Interactive Session Mode
```

## Best Practices

### For Single Command Mode

1. **Keep Prompts Focused**: One clear task per request
2. **Include All Context**: Provide complete information in the prompt
3. **Use for Independence**: When tasks don't depend on each other

### For Interactive Session Mode

1. **Always End Sessions**: Free resources when done
2. **Break into Steps**: Divide complex workflows into manageable steps
3. **Validate Between Steps**: Ensure each step succeeded before continuing
4. **Handle Errors**: Plan for recovery or session cleanup on errors

## Hybrid Approach

You can combine both modes in your workflows:

```
Example: Daily Report Generation

1. Use Single Command Mode to check if report is ready
2. If ready, use Interactive Session Mode to:
   - Login to system
   - Navigate to reports
   - Download report
   - Verify download
   - Logout
   - End session
3. Use Single Command Mode to email the report
```

## Performance Considerations

### Single Command Mode
- **Startup Time**: ~2-5 seconds per request (browser launch)
- **Execution Time**: Depends on task complexity
- **Total Time**: Startup + Execution

### Interactive Session Mode
- **Startup Time**: ~2-5 seconds (once, at session start)
- **Execution Time**: Depends on task complexity per step
- **Total Time**: Startup + (Execution × Number of Steps)

**Example Comparison**:
```
Task: Perform 5 related web operations

Single Command Mode:
- 5 requests × (2s startup + 3s execution) = 25 seconds

Interactive Session Mode:
- 1 session × (2s startup + 5 × 3s execution) = 17 seconds
- 32% faster!
```

## Next Steps

- Try [Single Command Mode](/pages/SingleCommandMode.md) for your first automation
- Learn [Interactive Session Start](/pages/InteractiveSessionStart.md) for complex workflows
- Review [Writing Effective Prompts](/pages/WritingPrompts.md) for both modes

---

> **💡 Tip**: Start with Single Command Mode for simplicity. Upgrade to Interactive Session Mode when you need state persistence or have multiple related steps.

