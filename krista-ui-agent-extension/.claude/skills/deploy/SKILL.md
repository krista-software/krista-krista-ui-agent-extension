---
name: deploy
description: Build fat JAR and deploy the extension to the Krista platform
disable-model-invocation: true
allowed-tools: Read, Grep, Glob, Bash, Edit, AskUserQuestion
---

# Deploy Extension Skill

You are deploying the **krista-ui-agent-extension** to the Krista platform.

---

## Phase 1: Pre-Deploy Checks

Run these in parallel:

1. Check for uncommitted changes:
```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && git status --short
```

2. Run the full test suite:
```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && ./gradlew test
```

3. Read the current extension version:
```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && cat build-nr.txt
```

4. Read deployment config:
```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && cat extension-config.json
```

If tests fail, STOP and report the failures. Do NOT proceed with deployment.

If there are uncommitted changes, warn the user and ask whether to continue.

---

## Phase 2: Build Fat JAR

```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && ./gradlew build
```

After build completes, verify the JAR was created:
```bash
ls -lh /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension/build/libs/*.jar
```

---

## Phase 3: Deploy to Krista Platform

Read the `.env` file for Krista platform credentials (`KRISTA_STUDIO_URL`, `KRISTA_USERNAME`, `KRISTA_PASSWORD`, `KRISTA_WORKSPACE_ID`).

Read `extension-config.json` for deployment target info.

Use the Krista extension deployment API:

1. Authenticate to get a session token:
```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && source .env && curl -s -X POST "${KRISTA_STUDIO_URL}api/auth/login" -H "Content-Type: application/json" -d "{\"email\": \"${KRISTA_USERNAME}\", \"password\": \"${KRISTA_PASSWORD}\"}" | python3 -m json.tool
```

2. Upload the JAR using the deployment endpoint. Check `extension-config.json` for the exact workspace and extension IDs.

3. Report the deployment result including:
   - Version deployed
   - Target environment
   - JAR size
   - Build number

---

## Phase 4: Post-Deploy Verification

After deployment, suggest the user verify:
- Extension loads correctly in Krista Studio
- Test Connection succeeds
- Validate Attributes works with valid/invalid inputs

---

## Important Rules

- NEVER deploy if tests are failing
- ALWAYS show the user what version/build is being deployed before deploying
- Read `.env` for credentials — never hardcode them
- If deployment fails, show the full error response
- The fat JAR excludes Node.js binary (~98MB savings) — this is expected
