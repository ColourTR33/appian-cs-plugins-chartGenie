# Requirements Document

## Introduction

This document specifies the requirements for a refactoring effort on the ChartGenie Appian plugin. The goals are to improve testability, maintainability, and correctness by converting manual test runners to proper JUnit 5 tests, extracting hard-coded client branding, fixing version metadata inconsistencies, decomposing a large service class, cleaning up tracked temp files, and introducing constructor-based dependency injection.

## Glossary

- **Test_Runner**: A Java class with a `public static void main(String[])` entry point used for local manual testing that does not execute under the JUnit 5 platform
- **JUnit_5_Test**: A test class using JUnit Jupiter `@Test` annotations discovered and executed by the Gradle `test` task via `useJUnitPlatform()`
- **CI_Pipeline**: The automated GitLab CI pipeline that executes `./gradlew test` to validate the build
- **Chart_Strategy**: A class implementing `ChartGeneratorStrategy` that generates a `JFreeChart` from a `ChartConfiguration`
- **Color_Palette**: An ordered array of `java.awt.Paint` values used by JFreeChart's `DefaultDrawingSupplier` to color chart series
- **Monochromatic_Palette**: A set of colours derived programmatically from a single base colour by varying lightness or saturation
- **Plugin_Descriptor**: The `appian-plugin.xml` file that declares Smart Services, version, and vendor metadata for the Appian runtime
- **WordDocumentService**: The central orchestrator class (~895 lines) responsible for processing all report section types into a Word document
- **Section_Handler**: A class responsible for rendering a single `SectionType` into an `XWPFDocument` or `XWPFTableCell`
- **Constructor_Injection**: A design pattern where dependencies are provided via constructor parameters rather than instantiated internally with `new`
- **Word_Temp_File**: A lock file matching the pattern `~$*.docx` created by Microsoft Word when a document is open

## Requirements

### Requirement 1: Convert Manual Test Runners to JUnit 5 Tests

**User Story:** As a developer, I want all test runners to execute automatically via `./gradlew test`, so that regressions are caught in CI without manual intervention.

#### Acceptance Criteria

1. WHEN `./gradlew test` is executed, THE CI_Pipeline SHALL discover and execute all tests that were previously only available as Test_Runner classes (`AllChartTypesTest`, `SecurityRegressionTest`, `ScatterPlotUnitTest`, `LocalRunner`, `BiaLocalRunner`, `AllComponentsLocalRunner`, `NestedTablesLocalRunner`, `HtmlMarkupLocalRunner`, `ScatterPlotTestRunner`, `JsonRunner`)
2. THE JUnit_5_Test classes SHALL use `@Test` annotations from `org.junit.jupiter.api.Test` instead of `public static void main(String[])` entry points, and the converted classes SHALL NOT retain a `public static void main` method
3. THE JUnit_5_Test classes SHALL contain at least one JUnit 5 assertion (`assertNotNull`, `assertDoesNotThrow`, `assertTrue`, or equivalent) per test method to verify expected outcomes
4. WHEN a converted test requires a template file that is not present in the CI environment, THE JUnit_5_Test SHALL use `Assumptions.assumeTrue()` to skip gracefully rather than fail, where "not present" means `java.nio.file.Files.exists()` returns false for the expected path
5. THE Gradle build file SHALL remove all `JavaExec` runner task registrations (`runLocalTest`, `runJsonTest`, `runScatterTest`, `runScatterUnitTest`, `runSecurityRegressionTest`, `runHtmlMarkupTest`, `runAllComponentsTest`, `runNestedTablesTest`, `runBiaLocalTest`, `runAllChartTypesTest`) after the corresponding classes are converted to JUnit_5_Test classes
6. WHEN all conversions are complete, THE CI_Pipeline SHALL report zero test failures for the converted classes on a clean build (`./gradlew clean test` with no prior build artifacts)
7. WHEN a converted test references test resource files (JSON payloads or template documents), THE JUnit_5_Test SHALL resolve those files relative to the classpath or the project's `src/test` directory rather than relying on a hardcoded absolute path

### Requirement 2: Extract Client-Specific Branding from Chart Strategies

**User Story:** As a developer, I want chart colour palettes derived from the configuration rather than hard-coded for a specific client, so that the plugin remains client-agnostic and reusable.

#### Acceptance Criteria

1. THE Chart_Strategy implementations SHALL NOT contain hard-coded colour hex literals, colour array constants, or client name references (e.g., "Barclays", "BIA_PALETTE", "BARCLAYS_BLUES") within strategy class source files
2. WHEN `ChartConfiguration.primaryColor` is provided as a valid 6-character hexadecimal colour string (with or without a leading "#"), THE Chart_Strategy SHALL generate a Monochromatic_Palette of at least 6 shades by varying the lightness of that primary colour from darkest to lightest
3. IF `ChartConfiguration.primaryColor` is null, empty, whitespace-only, or not a valid 6-character hexadecimal string, THEN THE Chart_Strategy SHALL fall back to a default neutral grayscale palette containing at least 6 shades, defined in a single shared location
4. THE Monochromatic_Palette generation logic SHALL reside in a dedicated utility class separate from any Chart_Strategy implementation
5. THE Chart_Strategy implementations SHALL obtain their Color_Palette from the `ChartConfiguration` at generation time rather than from static constants within the strategy class
6. WHEN a Chart_Strategy requires more colours than the generated palette contains, THE Chart_Strategy SHALL cycle through the palette from the beginning rather than producing an error or rendering without colour

### Requirement 3: Fix Plugin Version Inconsistency

**User Story:** As a release engineer, I want a single authoritative version declaration in the plugin descriptor, so that build tooling and the Appian runtime read the same version.

#### Acceptance Criteria

1. THE Plugin_Descriptor SHALL declare the plugin version exactly once, as the text content of the `<plugin-info><version>` element, using a semantic versioning format (MAJOR.MINOR.PATCH)
2. THE Plugin_Descriptor SHALL NOT carry a `version` attribute on the root `<appian-plugin>` element
3. WHEN the Gradle build reads the project version from `appian-plugin.xml`, THE build script SHALL resolve the version string from the `<plugin-info><version>` element and complete the configuration phase with a non-empty `project.version` value matching the declared version string
4. WHEN the Gradle `jar` task produces the plugin archive, THE build script SHALL embed the version resolved from `<plugin-info><version>` in the JAR manifest `Implementation-Version` attribute

### Requirement 4: Decompose WordDocumentService into Section Handlers

**User Story:** As a developer, I want each section type rendered by a dedicated handler class, so that the WordDocumentService is easier to understand, test, and extend.

#### Acceptance Criteria

1. THE WordDocumentService SHALL delegate section rendering to individual Section_Handler implementations rather than containing all rendering logic inline
2. WHEN a `ReportSection` is processed, THE WordDocumentService SHALL resolve the appropriate Section_Handler based on the section type string and invoke its render method
3. THE Section_Handler interface SHALL define a method accepting the document context, section data, and layout parameters required for rendering
4. WHEN a new section type is added in the future, THE developer SHALL only need to create a new Section_Handler implementation and register it with the dispatcher, without modifying other handlers
5. THE WordDocumentService class SHALL contain fewer than 300 lines of code after decomposition (excluding imports and blank lines)
6. THE refactored WordDocumentService and Section_Handler implementations SHALL produce semantically equivalent Word document output for the same input payload and template as the original implementation
7. WHEN an unrecognized section type string is encountered, THE WordDocumentService SHALL render the section text as a plain paragraph (matching current default behaviour) rather than throwing an exception

### Requirement 5: Add Word Temp Files to .gitignore and Remove from Tracking

**User Story:** As a developer, I want Word lock files excluded from version control, so that they do not pollute diffs or cause merge conflicts.

#### Acceptance Criteria

1. THE `.gitignore` file SHALL contain the pattern `~$*` on its own line, which excludes all Word_Temp_File artifacts (files matching `~$*.docx`) from version control
2. WHEN a developer runs `git rm --cached` for each `~$*.docx` file currently tracked under `src/test/java/com/appiancs/plugins/chartgenie/`, THE Git index SHALL no longer contain those files, while the files remain on the local filesystem
3. WHEN a developer runs `git status` after the `.gitignore` update and index removal are committed, THE repository SHALL NOT list any `~$*.docx` files as tracked or untracked
4. IF a new `~$*.docx` file is created anywhere in the repository working tree, THEN THE file SHALL NOT appear in `git status` output as an untracked file

### Requirement 6: Introduce Constructor-Based Dependency Injection in Service Classes

**User Story:** As a developer, I want service dependencies provided through constructors, so that I can substitute mock implementations in unit tests without modifying production code.

#### Acceptance Criteria

1. THE WordDocumentService SHALL accept `HtmlRichTextRenderer`, `TableGenerator`, and `TemplateVariableSubstitutor` as constructor parameters and store them as final instance fields used in place of inline `new` instantiation
2. THE ChartGenerationService SHALL accept a `ChartStrategyFactory` instance as a constructor parameter and delegate strategy lookups to that instance instead of calling `ChartStrategyFactory.getStrategy` statically, requiring `ChartStrategyFactory.getStrategy` to be converted from a static method to an instance method
3. WHEN a service class is constructed without arguments, THE service class SHALL provide a no-argument constructor that instantiates the same default production dependencies previously created inline (i.e., `new HtmlRichTextRenderer()`, `new TableGenerator()`, `new TemplateVariableSubstitutor()` for WordDocumentService; `new ChartStrategyFactory()` for ChartGenerationService) ensuring identical runtime behaviour
4. THE Smart Service entry points (`GenerateChartReport`, `GenerateChartImage`, `InsertChartIntoDocument`) SHALL construct service instances using the no-argument constructor to preserve current runtime behaviour
5. WHEN a unit test instantiates a service class using the parameterized constructor with mock or stub implementations, THE service class SHALL use those provided dependencies for all operations without requiring reflection, framework configuration, or modification to the service class source code
6. IF any dependency parameter is null when provided via the parameterized constructor, THEN THE service class SHALL throw an `IllegalArgumentException` with a message that contains the name of the null parameter
