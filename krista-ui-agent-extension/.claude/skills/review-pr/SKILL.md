---
name: review-pr
description: Review code against Krista extension PR requirements and quality standards before submission
disable-model-invocation: true
allowed-tools: Read, Grep, Glob, Bash, Agent, AskUserQuestion
---

# Review PR Skill

You are reviewing the **krista-ui-agent-extension** code against Krista's PR quality standards before submission.

---

## Phase 1: Gather Changes

Run these in parallel:

1. Get the diff against the base branch:
```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && git diff develop-1.0...HEAD --stat
```

2. Get the full diff:
```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && git diff develop-1.0...HEAD
```

3. Get commit history:
```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && git log develop-1.0..HEAD --oneline
```

4. Run the build:
```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && ./gradlew build 2>&1
```

---

## Phase 2: Review Checklist

Review ALL changed files against these Krista PR requirements:

### CRITICAL (Immediate Rejection)

1. **No test methods in production code**
   - Search for methods named "test*", "debug*" in `src/main/`:
   ```bash
   cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && grep -rn "public.*\b\(test\|debug\|Test\)\w*(" src/main/java/ --include="*.java" || echo "PASS: No test methods found"
   ```

2. **Package structure follows convention**
   - Must be under `app.krista.extension.uiautomationagent.*`
   - No custom top-level packages

3. **Version consistency**
   - `@Extension(version = "X.Y.Z")` must match `build-nr.txt` pattern
   - Check: read `KristaUiAgentExtension.java` for @Extension annotation

4. **Compilation succeeds**
   - `./gradlew compileJava` must pass (already checked via build above)

### MAJOR (Revision Required)

5. **Method size limits**
   - Catalog request methods: max 30 lines
   - Helper methods: max 50 lines
   - Check changed files for oversized methods

6. **No boilerplate duplication**
   - Look for copy-paste patterns across changed methods
   - Repeated try/catch blocks should use shared error handling

7. **Error handling follows fail-fast**
   - Stop on first failure for sequential operations
   - Clear, actionable error messages
   - Appropriate log levels (ERROR for failures, WARN for recoverable)

### RECOMMENDED

8. **Error categorization**
   - INPUT_ERROR for user input issues
   - LOGIC_ERROR for business rule violations
   - SYSTEM_ERROR for infrastructure failures
   - AUTHORIZATION_ERROR for auth issues

9. **Tests exist for new/changed code**
   - Check if corresponding test files exist in `src/test/`

10. **Protobuf compatibility**
    - If `.proto` file changed, verify both extension and service schemas are compatible

---

## Phase 3: Report

Present findings in this format:

```
## PR Review Report

### Build Status: PASS/FAIL

### Critical Issues (must fix)
- [ ] {issue description} — {file:line}

### Major Issues (should fix)
- [ ] {issue description} — {file:line}

### Minor Suggestions
- [ ] {suggestion} — {file:line}

### Summary
- Files changed: {count}
- Lines added/removed: +{added} / -{removed}
- Tests: {status}
- Overall: READY / NEEDS CHANGES
```

---

## Important Rules

- Read actual changed code, don't guess
- Focus on the diff — don't review unchanged code
- Be specific: always include file:line references
- Distinguish between blocking issues and suggestions
- If the build fails, that's an immediate blocker — don't continue reviewing code style
