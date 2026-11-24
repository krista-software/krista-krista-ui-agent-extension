---
name: build
description: Build the extension, run tests, and report results
disable-model-invocation: true
allowed-tools: Bash, Read, Grep
---

# Build Skill

Build the **krista-ui-agent-extension** and report results.

---

## Step 1: Full Build

Run the full build (includes clean, compile, test, jar):

```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && ./gradlew build 2>&1
```

## Step 2: Report Results

After the build completes, report:

1. **Build status**: SUCCESS or FAILED
2. **Version**: Read from build output (look for "Building extension version:")
3. **Test results**: Number of tests run, passed, failed
4. **JAR output**: Size of the built JAR

If the build failed:
- Show the exact error message
- Identify the failing file and line number
- Suggest a fix if the error is straightforward

If tests failed:
- List the failing test names
- Show the assertion/error messages
- Read the failing test source to understand what was expected

Check JAR size:
```bash
ls -lh /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension/build/libs/*.jar 2>/dev/null
```
