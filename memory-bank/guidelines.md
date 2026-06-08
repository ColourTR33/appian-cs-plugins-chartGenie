# ChartGenie — Development Guidelines

## Code Style

- **Formatter:** Eclipse JDT (`.settings/org.eclipse.jdt.core.prefs`) enforced by Spotless
- **Import order:** `java`, `org`, `com` — enforced by Spotless
- **Encoding:** UTF-8 everywhere (`options.encoding = 'UTF-8'`)
- **Checkstyle:** `config/checkstyle.xml` — `ignoreFailures = false`, `maxWarnings = 0`
- Run `gradlew.bat spotlessApply` before committing to auto-fix formatting

---

## Logging

Always use **SLF4J** — never `System.out.println()` or `java.util.logging`.

```java
// Correct
private static final Logger logger = LoggerFactory.getLogger(MyClass.class);
logger.info("Processing table with {} rows", rowCount);
logger.error("Failed to generate chart: {}", e.getMessage(), e);

// Wrong — CWE-398
System.out.println("Processing table");
```

Always use **parameterised logging** (curly braces `{}`). Never concatenate strings in log calls — this defeats lazy evaluation and opens CWE-117 injection vectors.

---

## Security Rules

### HTML Input (CWE-94)
All HTML content entering `TableGenerator` must pass through `sanitizeHtmlInput()` (JSoup Safelist) before rendering. All plain text headers must pass through `sanitizeTextInput()`. Never render raw user input directly into POI runs.

### Logging (CWE-117/93)
Any user-supplied string that appears in a log message must first pass through `BaseSmartService.sanitizeForLogging()`. This strips `\r`, `\n`, `\t`, and control characters.

### File Paths (CWE-22/23)
Any `File` object derived from user input must be validated via `DocumentUtils.validateAndSecureFilePath()` before use. This checks canonical vs absolute path equality and blocks encoded traversal patterns.

### Never
- Never log raw exception messages from user-controlled input without sanitization
- Never use `String.format()` in log calls — use SLF4J `{}`
- Never accept file paths from JSON payload without validation

---

## Adding a New Chart Type

1. Create `src/main/java/.../strategies/impl/MyChartStrategy.java` implementing `ChartGeneratorStrategy`
2. Register it in `ChartStrategyFactory.getStrategy()` with the new type string
3. Add the type string to the README supported chart types table
4. Add a test case in `AllChartTypesTest`

---

## Adding a New Section Type

1. Add the type string to `SectionType.java` enum
2. Add a `case` branch in `WordDocumentService.processSection()`
3. Add a `case` in `createParagraphIfNeeded()` if the section manages its own paragraph
4. Document the new type in README under "Supported Section Types"
5. Add an integration test in `IntegrationValidationTest`

---

## Adding a New Table Feature

1. Add the field to `TableConfiguration` or `TableCellConfig` with getter/setter and JavaDoc
2. Apply the field in `TableGenerator.applyTablePolishing()` (for table-level) or the cell processing block (for cell-level)
3. Update README `tableConfig` or cell config schema table
4. Add a test in `FontControlValidationTest` or `NestedTableValidationTest` as appropriate
5. Update `DocumentationValidationTest` assertions if README content changes

---

## Testing Standards

- All new features must have a dedicated test class with real POI/service assertions — no `return true` stubs
- Test methods must be `public` if called from `ComprehensiveValidationSuite`
- Use `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` and `@Order` for deterministic execution
- Use `@DisplayName` with the phase prefix (e.g. `"1.1a: ..."`) for traceability
- Use `Assumptions.assumeTrue(template.exists())` to skip tests that require `template.docx` in CI environments where it may not be present
- Assert against `CTTc.getTblArray()` (not `cell.getTables()`) for nested table presence

---

## Running Tests

```bash
# All tests
gradlew.bat test

# Specific phase
gradlew.bat test --tests com.appiancs.plugins.chartgenie.FontControlValidationTest
gradlew.bat test --tests com.appiancs.plugins.chartgenie.NestedTableValidationTest
gradlew.bat test --tests com.appiancs.plugins.IntegrationValidationTest
gradlew.bat test --tests com.appiancs.plugins.DocumentationValidationTest

# Full validation suite
gradlew.bat test --tests com.appiancs.plugins.ComprehensiveValidationSuite

# Manual local runners (generate actual .docx files)
gradlew.bat runLocalTest
gradlew.bat runAllChartTypesTest
gradlew.bat runSecurityRegressionTest
```

---

## Build & Release

```bash
# Build the plugin JAR
gradlew.bat jar

# The JAR is output to build/libs/chartGenie-<version>.jar
# Version is read automatically from appian-plugin.xml
```

The JAR bundles all `implementation` dependencies into `META-INF/lib/`. `compileOnly` dependencies (Appian SDK, SLF4J API) are provided by the Appian runtime and must not be bundled.

---

## CI/CD

- Pipeline defined in `.gitlab-ci.yml`
- Sonar analysis configured in `sonar-project.properties`
- SpotBugs runs as part of `check` with FindSecBugs plugin (`findsecbugs-plugin:1.12.0`) for SAST
- OWASP Dependency Check (`dependencycheck`) for SCA
- JaCoCo coverage report generated at `build/jacoco/jacoco.xml` with minimum 25% threshold
- PMD runs with `errorprone` and `bestpractices` rulesets (`ignoreFailures = true`)

---

## Dependency Management

- Never add `implementation` dependencies that duplicate `compileOnly` ones (causes JAR conflicts in Appian)
- POI Log4j exclusion is intentional — Appian provides its own logging runtime:
  ```groovy
  implementation("org.apache.poi:poi-ooxml:5.5.1") {
      exclude group: 'org.apache.logging.log4j'
  }
  ```
- Do not add `poi-ooxml-schemas` — it conflicts with `poi-ooxml-lite` bundled in POI 5.x
