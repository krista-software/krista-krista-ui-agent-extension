# Agentic Browser Extension

## Overview

The Agentic Browser Extension enables browser automation through AI-powered agents using natural language prompts. This extension provides intelligent web interaction capabilities, allowing you to automate complex browser tasks without writing code.

## Key Features

**Natural Language Automation**
- Execute browser tasks using simple English commands
- AI-powered understanding of web page structure and interactions
- Intelligent element detection and interaction

**Dual Operation Modes**
- **Single Command Mode**: Execute independent, one-off automation tasks
- **Interactive Session Mode**: Maintain browser state across multiple steps for complex workflows

**Advanced Capabilities**
- Screenshot capture and visual validation
- File download handling
- Multi-step workflow execution
- Session management and state persistence
- Validation step execution

**Enterprise Integration**
- Seamless integration with Krista workflows
- Event-driven automation
- Comprehensive error handling and logging
- Health monitoring and diagnostics

## Quick Start Guide

### 1. Prerequisites

- Krista platform access with extension configuration permissions
- UI Agent Service deployed and accessible
- LLM API keys (OpenAI, Google Gemini, or Claude)

### 2. Setup Steps

#### Step 1: Configure Extension

1. Navigate to your Krista platform
2. Go to **Extensions** > **Agentic Browser**
3. Click **Configure** or **Setup**

#### Step 2: Enter Configuration Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| UI Agent URL | Text | Yes | URL of the UI Agent Service | "http://localhost:3000" |
| Primary LLM | Pick One | Yes | Primary AI model provider | "OpenAI" / "Google Gemini" / "Claude" |
| Primary LLM API Key | Text (Secured) | Yes | API key for primary LLM | "sk-..." |
| Fallback LLM | Pick One | Yes | Backup AI model provider | "Google Gemini" / "OpenAI" / "Claude" |
| Fallback LLM API Key | Text (Secured) | Yes | API key for fallback LLM | "..." |

#### Step 3: Test Connection

1. Use the **Test Connection** feature to verify your setup
2. Ensure the UI Agent Service is accessible
3. Verify LLM API keys are valid

### 3. Choose Your Automation Mode

#### Single Command Mode (Recommended for Simple Tasks)
- Perfect for one-off automation tasks
- Each request is independent
- No session management required
- Immediate execution and completion

#### Interactive Session Mode (Recommended for Complex Workflows)
- Maintains browser state across multiple steps
- Build complex workflows step-by-step
- Session persistence between commands
- Ideal for multi-step processes

## Documentation Structure

### Getting Started
- [Extension Configuration](pages/ExtensionConfiguration.md) - Complete setup guide
- [Understanding Automation Modes](pages/AutomationModes.md) - Choose the right mode for your task

### Catalog Requests
- [Single Command Mode](pages/SingleCommandMode.md) - Execute independent automation tasks
- [Interactive Session Start](pages/InteractiveSessionStart.md) - Begin a stateful automation session
- [Execute Interactive Session Steps](pages/ExecuteInteractiveSessionSteps.md) - Run steps within an active session
- [Interactive Session End](pages/InteractiveSessionEnd.md) - Close and clean up a session

### Advanced Topics
- [Writing Effective Prompts](pages/WritingPrompts.md) - Best practices for natural language commands
- [Validation Steps](pages/ValidationSteps.md) - Verify automation results
- [Error Handling](pages/ErrorHandling.md) - Troubleshooting common issues

## Support & Resources

### Version Information
- **Extension Version**: 1.0.0-rc2.9
- **Implementation Model**: Krista 4.0
- **Java Version**: 21
- **Base Image Version**: 3.5.7

### Supported LLM Providers
- **OpenAI**: gpt-5-mini (faster, more cost-efficient)
- **Google Gemini**: gemini-2.5-pro (fast and capable)
- **Claude**: claude-sonnet-4-5 (latest Claude model)

### Architecture
- **Communication Protocol**: AMQP 1.0 (Apache Artemis)
- **Message Format**: Google Protocol Buffers
- **Queue Pattern**: Request-Response with unique response queues
- **Execution Mode**: LIVE only

---

> **💡 Tip**: Start with [Extension Configuration](pages/ExtensionConfiguration.md) to set up your connection, then try [Single Command Mode](pages/SingleCommandMode.md) for your first automation task. For complex workflows, explore [Interactive Session Mode](pages/InteractiveSessionStart.md).

