---
name: write-tests
description: Generate comprehensive unit tests for extension components following Krista testing patterns
disable-model-invocation: true
argument-hint: [class-name or file-path]
allowed-tools: Read, Grep, Glob, Bash, Edit, Write, Agent, AskUserQuestion
---

# Write Tests Skill

You are writing comprehensive unit tests for the **krista-ui-agent-extension** (Java 21, JUnit 5, Mockito).

**Target**: $ARGUMENTS

---

## Phase 1: Analyze the Target

1. Find and read the target class/file. If `$ARGUMENTS` is a class name, search for it:
```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && find src/main -name "*.java" | xargs grep -l "$ARGUMENTS" | head -5
```

2. Read the full source of the target class.

3. Identify:
   - What the class does (catalog request, service, utility, ESB client, etc.)
   - Its dependencies (what it injects/imports)
   - Public methods to test
   - Error paths and edge cases
   - Any protobuf message types used

4. Check for existing tests:
```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && find src/test -name "*.java" | xargs grep -l "$ARGUMENTS" 2>/dev/null
```

---

## Phase 2: Generate Tests

Follow these Krista testing patterns:

### Test Structure (JUnit 5 + Mockito)

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("{ClassName} Tests")
class {ClassName}Test {

    @Mock private {Dependency} mock{Dependency};
    @InjectMocks private {ClassName} target;

    // Use @Nested classes to group: Success, Validation, Error, Edge Cases
    // Use @ParameterizedTest for similar scenarios with different inputs
    // Follow AAA pattern: Arrange, Act, Assert
    // Use @DisplayName for human-readable test names
}
```

### What to Test

For **Catalog Requests** (InteractionArea):
- Valid input produces correct ExtensionResponse
- Missing/invalid required params return appropriate errors
- Protobuf message construction is correct
- ESB client is called with right parameters
- Response parsing handles all fields
- Timeout/error scenarios

For **Services** (UiAgentSessionManager, FileHandlingService, etc.):
- Core business logic
- Thread safety (ConcurrentHashMap operations)
- Edge cases (null inputs, empty collections)
- Error propagation

For **ESB Clients** (UiAgentRawClient):
- Message construction
- Correlation ID handling
- Consumer creation/cleanup
- Timeout behavior

For **Parsers** (UiAgentResponseParser):
- All protobuf field mappings
- Missing/optional fields
- Malformed responses

For **Config** (ExtensionAttributes):
- Attribute loading from map
- Default values when attributes missing
- URL validation

### Naming Convention
- Test file: `{ClassName}Test.java`
- Location: `src/test/java/{same-package-as-source}/`
- Test methods: `{methodName}_{scenario}_{expectedBehavior}`

---

## Phase 3: Write and Verify

1. Write the test file to the correct location under `src/test/`
2. Run the tests:
```bash
cd /Users/mayurmistry/development/krista-global-catalog/krista-ui-agent-extension && ./gradlew test --tests "*.$ARGUMENTS*Test"
```
3. If tests fail, fix them and re-run
4. Report coverage summary: how many methods tested, scenarios covered

---

## Important Rules

- Use JUnit 5 (`org.junit.jupiter`) not JUnit 4
- Use Mockito for mocking dependencies — this extension uses mocks (unlike the Krista PR standard which prefers integration tests, this project's existing tests use Mockito)
- Follow existing test patterns in `src/test/` — read at least one existing test first for consistency
- Test one thing per test method
- Cover both happy path AND error paths
- For protobuf-related tests, use the generated protobuf builders
- Don't test trivial getters/setters
- Keep tests focused and fast (no real network calls)
