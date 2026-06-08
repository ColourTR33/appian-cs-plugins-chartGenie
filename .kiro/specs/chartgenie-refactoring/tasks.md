# Implementation Plan: ChartGenie Refactoring

## Overview

This plan implements six independent refactoring work streams for the ChartGenie Appian plugin: converting manual test runners to JUnit 5, extracting client branding from chart strategies, fixing plugin version inconsistency, decomposing WordDocumentService into section handlers, cleaning up tracked temp files, and introducing constructor-based dependency injection. Tasks are ordered so that foundational changes (DI interfaces, palette utility) land first, then decomposition, then test conversions that validate all prior changes.

## Tasks

- [x] 1. Set up test dependencies and project infrastructure
  - [x] 1.1 Add jqwik and Mockito test dependencies to build.gradle
    - Add `testImplementation 'net.jqwik:jqwik:1.9.1'` and `testImplementation 'org.mockito:mockito-core:5.14.2'` to the dependencies block in `build.gradle`
    - Verify the build resolves dependencies with `./gradlew dependencies --configuration testCompileClasspath`
    - _Requirements: 1.1, 2.4 (testing infrastructure needed for property tests)_

  - [x] 1.2 Fix plugin version inconsistency in appian-plugin.xml
    - Open `src/main/resources/appian-plugin.xml` and remove the `version="1.0.0"` attribute from the root `<appian-plugin>` element
    - Ensure the `<plugin-info><version>1.3.1</version>` element remains as the single authoritative version source
    - Verify `build.gradle` still resolves `project.version` from `<plugin-info><version>` via XmlSlurper
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 1.3 Remove tracked Word temp files and verify .gitignore
    - Confirm `.gitignore` contains the `~$*` pattern on its own line
    - Run `git rm --cached` for each tracked `~$*.docx` file in `src/test/java/com/appiancs/plugins/chartgenie/` (`~$a-full-report-v2.docx`, `~$a-full-report.docx`, `~$l-components-report.docx`, `~$sted-tables-report.docx`)
    - Verify `git status` no longer shows these files as tracked
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 2. Implement MonochromaticPaletteGenerator utility
  - [x] 2.1 Create MonochromaticPaletteGenerator class
    - Create `src/main/java/com/appiancs/plugins/chartgenie/service/MonochromaticPaletteGenerator.java`
    - Implement `generate(String hexColor, int count)` — convert hex to HSL, distribute lightness evenly from 20% (darkest) to 80% (lightest), return `Paint[]` of at least 6 elements
    - Implement `getDefaultPalette()` — return a static grayscale `Paint[]` with at least 6 shades
    - Implement `resolve(String primaryColor)` — return generated palette for valid hex input, or default palette for null/empty/invalid input
    - Handle hex strings with or without leading "#"
    - _Requirements: 2.2, 2.3, 2.4_

  - [ ]* 2.2 Write property test for monochromatic palette generation (Property 1)
    - **Property 1: Monochromatic Palette Generation Invariants**
    - Create `src/test/java/com/appiancs/plugins/chartgenie/properties/PaletteGeneratorPropertyTest.java`
    - Write a jqwik `@Property` test that for any valid 6-char hex string, `resolve()` returns at least 6 paints with the same hue (within tolerance) and lightness ordered darkest-to-lightest
    - **Validates: Requirements 2.2**

  - [ ]* 2.3 Write property test for invalid colour fallback (Property 2)
    - **Property 2: Invalid Colour Fallback Consistency**
    - In `PaletteGeneratorPropertyTest.java`, write a jqwik `@Property` test that for any null, empty, whitespace-only, or invalid hex input, `resolve()` returns a palette equal to `getDefaultPalette()`
    - **Validates: Requirements 2.3**

  - [ ]* 2.4 Write property test for palette colour cycling (Property 3)
    - **Property 3: Palette Colour Cycling**
    - In `PaletteGeneratorPropertyTest.java`, write a jqwik `@Property` test that for any palette of size N and any index I ≥ N, accessing `palette[I % N]` produces no exception and equals the expected element
    - **Validates: Requirements 2.6**

  - [ ]* 2.5 Write unit tests for MonochromaticPaletteGenerator
    - Create `src/test/java/com/appiancs/plugins/chartgenie/unit/MonochromaticPaletteGeneratorTest.java`
    - Test specific cases: valid hex "3366CC", hex with "#", null input, empty string, "ZZZZZZ" invalid
    - Assert palette size >= 6, darkest shade has lowest lightness, lightest has highest
    - _Requirements: 2.2, 2.3_

- [x] 3. Refactor chart strategies to use palette utility
  - [x] 3.1 Remove hard-coded palettes from all chart strategy implementations
    - In each strategy (`BarChartStrategy`, `PieChartStrategy`, `LineChartStrategy`, `AreaChartStrategy`, `ColumnChartStrategy`, `DonutChartStrategy`, `ScatterPlotStrategy`, `StackedColumnStrategy`) in `src/main/java/com/appiancs/plugins/chartgenie/strategies/impl/`:
      - Remove `BIA_PALETTE`, `BARCLAYS_BLUES`, and any hard-coded colour array constants
      - Remove any client name references (e.g., "Barclays")
      - Replace palette usage with `MonochromaticPaletteGenerator.resolve(config.getPrimaryColor())`
      - Implement colour cycling via `palette[index % palette.length]` when more series than palette colours
    - _Requirements: 2.1, 2.2, 2.5, 2.6_

  - [ ]* 3.2 Write unit tests for chart strategy palette integration
    - Create `src/test/java/com/appiancs/plugins/chartgenie/unit/ChartStrategyFactoryTest.java`
    - Test that each strategy resolves palette from `ChartConfiguration.primaryColor`
    - Test that strategy source files contain no hard-coded colour hex literals or client name references
    - _Requirements: 2.1, 2.5_

- [x] 4. Checkpoint - Verify palette refactoring
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Decompose WordDocumentService into section handlers
  - [x] 5.1 Create SectionHandler interface and SectionRenderContext
    - Create `src/main/java/com/appiancs/plugins/chartgenie/service/handlers/SectionHandler.java` with a `void render(SectionRenderContext context, ReportSection section) throws Exception` method
    - Create `src/main/java/com/appiancs/plugins/chartgenie/service/handlers/SectionRenderContext.java` with fields: `XWPFDocument document`, `XWPFTableCell cell` (nullable), `int availableWidthTwips`, `boolean isSidebar`
    - _Requirements: 4.3_

  - [x] 5.2 Extract section rendering logic into individual handler classes
    - Create handler classes in `src/main/java/com/appiancs/plugins/chartgenie/service/handlers/`:
      - `HeadingSectionHandler`, `Heading2SectionHandler`, `StatusBadgeSectionHandler`, `TableSectionHandler`, `TextSectionHandler`, `ChartSectionHandler`, `ImageSectionHandler`, `PageBreakSectionHandler`, `QrCodeSectionHandler`, `DividerSectionHandler`, `SpacerSectionHandler`, `SidebarLayoutSectionHandler`, `DefaultSectionHandler`
    - Move corresponding rendering logic from the `processSection` method in `WordDocumentService` into each handler's `render` method
    - `DefaultSectionHandler` renders section text as a plain paragraph (for unrecognized types)
    - _Requirements: 4.1, 4.4, 4.7_

  - [x] 5.3 Refactor WordDocumentService to use handler registry and dispatch
    - Replace the `processSection` switch/if-else block with a `Map<String, SectionHandler>` registry
    - Register all handlers in the constructor (matching type strings to handler instances)
    - Dispatch to `DefaultSectionHandler` for unrecognized types
    - Remove all inline section rendering code — the class should only orchestrate
    - Ensure final class is under 300 lines (excluding imports and blank lines)
    - _Requirements: 4.1, 4.2, 4.5, 4.7_

  - [ ]* 5.4 Write property test for section handler dispatch (Property 4)
    - **Property 4: Section Handler Dispatch Correctness**
    - Create `src/test/java/com/appiancs/plugins/chartgenie/properties/SectionHandlerPropertyTest.java`
    - Write a jqwik `@Property` test that for any known section type string, the correct registered handler is invoked exactly once
    - **Validates: Requirements 4.2**

  - [ ]* 5.5 Write property test for unknown section type default rendering (Property 6)
    - **Property 6: Unknown Section Type Default Rendering**
    - In `SectionHandlerPropertyTest.java`, write a jqwik `@Property` test that for any arbitrary string not in the known type set, processing does not throw and renders a plain paragraph
    - **Validates: Requirements 4.7**

- [x] 6. Introduce constructor-based dependency injection
  - [x] 6.1 Refactor WordDocumentService for constructor injection
    - Add a parameterized constructor accepting `HtmlRichTextRenderer`, `TableGenerator`, and `TemplateVariableSubstitutor`
    - Store dependencies as `final` instance fields; replace all inline `new` instantiations with field references
    - Add null checks with `IllegalArgumentException` for each parameter (message must contain parameter name)
    - Add a no-arg constructor that delegates to the parameterized constructor with default production instances
    - Pass `tableGenerator` and `htmlRenderer` to handlers that need them (`TableSectionHandler`, `TextSectionHandler`)
    - _Requirements: 6.1, 6.3, 6.6_

  - [x] 6.2 Refactor ChartGenerationService for constructor injection
    - Convert `ChartStrategyFactory` from a class with private constructor and static `getStrategy` to an instantiable class with an instance `getStrategy` method
    - Add a parameterized constructor to `ChartGenerationService` accepting `ChartStrategyFactory`
    - Store as a `final` field; add null check with `IllegalArgumentException`
    - Add a no-arg constructor delegating with `new ChartStrategyFactory()`
    - _Requirements: 6.2, 6.3, 6.6_

  - [x] 6.3 Update Smart Service entry points to use no-arg constructors
    - Verify `GenerateChartReport`, `GenerateChartImage`, and `InsertChartIntoDocument` construct service instances using no-arg constructors
    - If they already do, confirm no change needed; otherwise update to use the no-arg constructor
    - _Requirements: 6.4_

  - [ ]* 6.4 Write property test for null dependency rejection (Property 7)
    - **Property 7: Null Dependency Rejection**
    - Create `src/test/java/com/appiancs/plugins/chartgenie/properties/DependencyInjectionPropertyTest.java`
    - Write a jqwik `@Property` test that for each parameterized constructor, setting any single parameter to null throws `IllegalArgumentException` with the null parameter's name in the message
    - **Validates: Requirements 6.6**

  - [ ]* 6.5 Write unit tests for DI with mocks
    - Create `src/test/java/com/appiancs/plugins/chartgenie/unit/WordDocumentServiceDITest.java`
    - Use Mockito to instantiate `WordDocumentService` with mock dependencies
    - Verify mock interactions when processing sections
    - Test that no-arg constructor produces a working instance with real dependencies
    - _Requirements: 6.1, 6.5_

- [x] 7. Checkpoint - Verify decomposition and DI
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Convert manual test runners to JUnit 5 tests
  - [x] 8.1 Convert AllChartTypesTest to JUnit 5
    - Refactor `src/test/java/com/appiancs/plugins/chartgenie/AllChartTypesTest.java`: replace `public static void main` with `@Test` methods
    - Add JUnit 5 assertions (`assertNotNull`) for each chart type generation
    - Remove `public static void main` method entirely
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 8.2 Convert SecurityRegressionTest to JUnit 5
    - Refactor `src/test/java/com/appiancs/plugins/chartgenie/SecurityRegressionTest.java`
    - Use `Assumptions.assumeTrue(Files.exists(template))` for template-dependent test
    - Add assertions for successful report generation
    - Remove `public static void main` method entirely
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.7_

  - [x] 8.3 Convert ScatterPlotUnitTest to JUnit 5
    - Refactor `src/test/java/com/appiancs/plugins/chartgenie/ScatterPlotUnitTest.java`
    - Replace `main` with `@Test` method, add assertions
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 8.4 Convert LocalRunner to JUnit 5
    - Create `src/test/java/com/appiancs/plugins/chartgenie/LocalRunnerTest.java` (new class replacing `LocalRunner.java`)
    - Use `Assumptions.assumeTrue` for template files
    - Resolve test resource files relative to classpath or `src/test` directory
    - Add at least one assertion per test method
    - Delete or repurpose `LocalRunner.java`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.7_

  - [x] 8.5 Convert BiaLocalRunner to JUnit 5
    - Create `src/test/java/com/appiancs/plugins/chartgenie/BiaLocalRunnerTest.java`
    - Use `Assumptions.assumeTrue` for template files
    - Add assertions for report output
    - Delete or repurpose `BiaLocalRunner.java`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.7_

  - [x] 8.6 Convert AllComponentsLocalRunner to JUnit 5
    - Create `src/test/java/com/appiancs/plugins/chartgenie/AllComponentsLocalRunnerTest.java`
    - Use `Assumptions.assumeTrue` for template files
    - Add assertions for report output
    - Delete or repurpose `AllComponentsLocalRunner.java`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.7_

  - [x] 8.7 Convert NestedTablesLocalRunner to JUnit 5
    - Create `src/test/java/com/appiancs/plugins/chartgenie/NestedTablesLocalRunnerTest.java`
    - Use `Assumptions.assumeTrue` for template files
    - Add assertions for report output
    - Delete or repurpose `NestedTablesLocalRunner.java`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.7_

  - [x] 8.8 Convert HtmlMarkupLocalRunner to JUnit 5
    - Create `src/test/java/com/appiancs/plugins/chartgenie/HtmlMarkupLocalRunnerTest.java`
    - Use `Assumptions.assumeTrue` for template files
    - Add assertions for report output
    - Delete or repurpose `HtmlMarkupLocalRunner.java`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.7_

  - [x] 8.9 Convert ScatterPlotTestRunner to JUnit 5
    - Create `src/test/java/com/appiancs/plugins/chartgenie/ScatterPlotTestRunnerTest.java`
    - Use `Assumptions.assumeTrue` for template files
    - Add assertions for report output
    - Delete or repurpose `ScatterPlotTestRunner.java`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.7_

  - [x] 8.10 Create JsonRunnerTest as JUnit 5 test
    - Create `src/test/java/com/appiancs/plugins/chartgenie/JsonRunnerTest.java` for JSON payload parsing validation
    - Add assertions that JSON payloads parse correctly into `ReportRequest` objects
    - Use classpath-relative paths for test JSON files
    - _Requirements: 1.1, 1.2, 1.3, 1.7_

- [x] 9. Remove JavaExec task registrations from build.gradle
  - [x] 9.1 Remove all JavaExec runner task registrations
    - Remove the following task registrations from `build.gradle`: `runLocalTest`, `runJsonTest`, `runScatterTest`, `runScatterUnitTest`, `runSecurityRegressionTest`, `runHtmlMarkupTest`, `runAllComponentsTest`, `runNestedTablesTest`, `runBiaLocalTest`, `runAllChartTypesTest`
    - Verify `./gradlew tasks` no longer lists these tasks
    - _Requirements: 1.5_

- [x] 10. Final checkpoint - Full validation
  - Ensure all tests pass with `./gradlew clean test`, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation after major change sets
- Property tests validate universal correctness properties from the design document using jqwik
- Unit tests validate specific examples and edge cases using JUnit 5
- The six work streams are largely independent but task ordering ensures Requirement 4 (decomposition) completes before Requirement 6 (DI) since DI depends on the decomposed structure
- All code is Java targeting the existing Gradle build with JUnit 5 platform

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4", "2.5", "5.1"] },
    { "id": 3, "tasks": ["3.1"] },
    { "id": 4, "tasks": ["3.2", "5.2"] },
    { "id": 5, "tasks": ["5.3"] },
    { "id": 6, "tasks": ["5.4", "5.5", "6.1", "6.2"] },
    { "id": 7, "tasks": ["6.3", "6.4", "6.5"] },
    { "id": 8, "tasks": ["8.1", "8.2", "8.3", "8.4", "8.5", "8.6", "8.7", "8.8", "8.9", "8.10"] },
    { "id": 9, "tasks": ["9.1"] }
  ]
}
```
