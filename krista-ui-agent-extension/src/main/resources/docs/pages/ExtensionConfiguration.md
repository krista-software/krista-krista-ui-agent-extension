# Extension Configuration

## Overview

The Agentic Browser Extension requires configuration of the UI Agent Service URL and LLM (Large Language Model) providers. This guide provides comprehensive setup instructions to get your extension up and running.

## Prerequisites

- Krista platform access with extension configuration permissions
- UI Agent Service deployed and accessible (Node.js service)
- API keys for at least two LLM providers (for primary and fallback)
- Network connectivity between Krista platform and UI Agent Service

## Configuration Parameters

| Parameter | Type | Required | Description | Example | Notes |
|-----------|------|----------|-------------|---------|-------|
| **UI Agent URL** | Text | Yes | Base URL of the UI Agent Service | `http://localhost:3000` or `https://ui-agent.company.com` | Must be accessible from Krista platform |
| **Primary LLM** | Pick One | Yes | Primary AI model provider | `OpenAI`, `Google Gemini`, or `Claude` | Used for all automation requests |
| **Primary LLM API Key** | Text (Secured) | Yes | API key for primary LLM | `sk-...` (OpenAI format) | Stored securely, never logged |
| **Fallback LLM** | Pick One | Yes | Backup AI model provider | `Google Gemini`, `OpenAI`, or `Claude` | Used if primary fails |
| **Fallback LLM API Key** | Text (Secured) | Yes | API key for fallback LLM | Provider-specific format | Stored securely, never logged |

## Step-by-Step Setup

### Step 1: Deploy UI Agent Service

The UI Agent Service is a Node.js application that handles browser automation. It must be deployed and accessible before configuring the extension.

**Deployment Options:**
- **Local Development**: Run on `localhost:3000`
- **Docker Container**: Deploy as containerized service
- **Cloud Hosting**: Deploy to AWS, Azure, GCP, etc.

**Verify Service is Running:**
```bash
curl http://your-ui-agent-url/health
# Should return: {"status": "ok"}
```

### Step 2: Obtain LLM API Keys

You need API keys for two different LLM providers (primary and fallback).

#### OpenAI
1. Visit [platform.openai.com](https://platform.openai.com)
2. Navigate to **API Keys**
3. Click **Create new secret key**
4. Copy the key (starts with `sk-`)
5. Store securely

#### Google Gemini
1. Visit [makersuite.google.com/app/apikey](https://makersuite.google.com/app/apikey)
2. Click **Create API Key**
3. Select your Google Cloud project
4. Copy the generated key
5. Store securely

#### Claude (Anthropic)
1. Visit [console.anthropic.com](https://console.anthropic.com)
2. Navigate to **API Keys**
3. Click **Create Key**
4. Copy the key
5. Store securely

### Step 3: Configure Extension in Krista

1. **Access Extension Configuration**
   - Navigate to Krista platform
   - Go to **Extensions** > **Agentic Browser**
   - Click **Configure** or **Setup**

2. **Enter UI Agent URL**
   - Enter the full URL of your UI Agent Service
   - Include protocol (`http://` or `https://`)
   - Do not include trailing slash
   - Example: `http://localhost:3000`

3. **Select Primary LLM**
   - Choose your preferred AI model provider
   - Recommended: **OpenAI** for best performance
   - Alternative: **Google Gemini** for cost-effectiveness

4. **Enter Primary LLM API Key**
   - Paste the API key for your primary LLM
   - The key will be encrypted and stored securely
   - Never share or log this key

5. **Select Fallback LLM**
   - Choose a different provider than your primary
   - This ensures redundancy if primary fails
   - Recommended: Different provider for diversity

6. **Enter Fallback LLM API Key**
   - Paste the API key for your fallback LLM
   - Must be for the provider selected in step 5

7. **Save Configuration**
   - Click **Save** or **Apply**
   - Configuration will be validated
   - Extension will restart with new settings

### Step 4: Test Connection

After configuration, verify the setup:

1. Use the **Test Connection** feature in the extension
2. This will:
   - Verify UI Agent Service is accessible
   - Test LLM API keys are valid
   - Confirm AMQP message queue connectivity
3. Expected result: "Connection successful"

## Configuration Best Practices

### Security
- **Never commit API keys** to version control
- **Rotate keys regularly** (every 90 days recommended)
- **Use environment variables** for sensitive data in production
- **Restrict API key permissions** to minimum required

### Reliability
- **Choose different providers** for primary and fallback LLMs
- **Monitor API usage** to avoid rate limits
- **Set up alerts** for service failures
- **Test failover** scenarios regularly

### Performance
- **Deploy UI Agent Service close** to Krista platform (same region/network)
- **Use HTTPS** for production deployments
- **Monitor response times** and optimize as needed
- **Scale UI Agent Service** horizontally if needed

## Troubleshooting

### UI Agent Service Not Accessible

**Symptoms**: Connection errors, timeouts

**Solutions**:
- Verify service is running: `curl http://your-url/health`
- Check firewall rules allow traffic
- Verify network connectivity from Krista platform
- Check service logs for errors

### Invalid LLM API Key

**Symptoms**: Authentication errors, 401 responses

**Solutions**:
- Verify key is copied correctly (no extra spaces)
- Check key hasn't been revoked or expired
- Verify key has correct permissions
- Try regenerating the key

### Extension Not Starting

**Symptoms**: Extension shows as "Stopped" or "Error"

**Solutions**:
- Check all required fields are filled
- Verify configuration syntax is correct
- Review extension logs for specific errors
- Try removing and re-adding configuration

## Next Steps

After successful configuration:
1. Try your first automation with [Single Command Mode](/pages/SingleCommandMode.md)
2. Learn about [Automation Modes](/pages/AutomationModes.md)
3. Explore [Writing Effective Prompts](/pages/WritingPrompts.md)

---

> **💡 Tip**: Start with OpenAI as primary and Google Gemini as fallback for the best balance of performance and reliability.

