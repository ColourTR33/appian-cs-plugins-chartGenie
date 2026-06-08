# ChartGenie — Project Structure

## Repository Layout

```
chartGenie/
├── src/
│   ├── main/
│   │   ├── java/com/appiancs/plugins/chartgenie/
│   │   │   ├── base/               # Abstract base classes
│   │   │   ├── dto/                # Data Transfer Objects
│   │   │   │   └── structure/      # Report request/section/settings DTOs
│   │   │   ├── service/            # Core business logic services
│   │   │   ├── strategies/         # Chart generation strategies (Strategy Pattern)
│   │   │   │   └── impl/           # Concrete strategy implementations
│   │   │   ├── GenerateChartReport.java      # Smart Service 1
│   │   │   ├── GenerateChartImage.java       # Smart Service 2
│   │   │   └── InsertChartIntoDocument.java  # Smart Service 3
│   │   └── resources/
│   │       ├── appian-plugin.xml   # Appian plugin descriptor
│   │       └── com/appiancs/plugins/chartgenie/
│   │           ├── *.properties    # Smart Service i18n labels
│   │           └── *.images/       # Smart Service icons
│   └── test/
│       └── java/com/appiancs/plugins/
│           ├── chartgenie/         # Feature-level unit tests
│           │   ├── AllChartTypesTest.java
│           │   ├── FontControlValidationTest.java
│           │   ├── NestedTableValidationTest.java
│           │   ├── SecurityRegressionTest.java
│           │   ├── ScatterPlotUnitTest.java
│           │   ├── LocalRunner.java            # Manual local test runner
│           │   ├── ScatterPlotTestRunner.java
│           │   ├── payload.json                # Test payload
│           │   └── template.docx               # Test template
│           ├── ComprehensiveValidationSuite.java  # Master test suite
│           ├── DocumentationValidationTest.java   # Phase 4.2
│           ├── EndToEndValidationTest.java        # Phase 5.2
│           ├── IntegrationValidationTest.java     # Phase 4.1
│           ├── MemoryBankValidationTest.java       # Phase 2
│           ├── PerformanceValidationTest.java      # Phase 5.1
│           ├── SecurityValidationTest.java         # Phase 3
│           └── ValidationSuiteRunner.java
├── memory-bank/                    # Project documentation
│   ├── product.md
│   ├── structure.md
│   ├── tech.md
│   └── guidelines.md
├── examples/
│   ├── fulljson.json               # Full feature example payload
│   └── minimaljson.json            # Minimal example payload
├── config/
│   └── checkstyle.xml
├── libs/
│   └── appian-plug-in-sdk-24.2-stub.jar
├── build.gradle
├── settings.gradle
└── README.md
```

---

## Package Responsibilities

### `base/`
- **`BaseSmartService`** — Abstract parent for all Smart Services. Handles `ContentService` injection, error state management (`errorOccurred`, `errorMessage`), SLF4J logging, and CWE-117/93 log injection protection via `sanitizeForLogging()`.

### `dto/`
- **`ChartConfiguration`** — Chart type, dimensions, colors, series data, legend position
- **`ChartDataPoint`** — Single data point (category + value)
- **`TableConfiguration`** — Full table config: headers, rows, column widths, colors, font sizes, borders
- **`TableCellConfig`** — Single cell: text (plain or HTML), colspan, background/text color, nested table
- **`ServiceResult<T>`** — Generic success/failure wrapper returned by services

### `dto/structure/`
- **`ReportRequest`** — Top-level JSON payload wrapper (settings + sections)
- **`ReportSettings`** — Page size, orientation, header/footer text, branding colors, audit reference
- **`ReportSection`** — A single document section (type + content union)
- **`SectionType`** — Enum of supported section types
- **`DocumentSettings`** — Document-level settings

### `service/`
- **`WordDocumentService`** — Orchestrates full report generation. Processes sections, applies page settings, headers/footers, and delegates to `TableGenerator` and `ChartGenerationService`
- **`TableGenerator`** — Builds styled `XWPFTable` instances from `TableConfiguration`. Handles font sizes, nested tables (via scratch-document CTTbl copy), HTML rendering, and CWE-94 sanitization
- **`HtmlRichTextRenderer`** — Parses HTML fragments into native POI `XWPFRun` styling (bold, italic, underline, color, bullets)
- **`ChartGenerationService`** — Delegates to strategy factory, applies dimension safety caps, returns PNG bytes
- **`ChartStrategyFactory`** — Maps chart type string to `ChartGeneratorStrategy` implementation
- **`DocumentUtils`** — Appian document upload/versioning utility with CWE-22/23 path traversal protection
- **`AppianDocumentUploader`** — Wraps `DocumentUtils` for Smart Service use

### `strategies/`
- **`ChartGeneratorStrategy`** — Interface: `generate(ChartConfiguration) → JFreeChart`
- **`impl/`** — One class per chart type: `BarChartStrategy`, `ColumnChartStrategy`, `LineChartStrategy`, `AreaChartStrategy`, `PieChartStrategy`, `DonutChartStrategy`, `StackedColumnStrategy`, `ScatterPlotStrategy`

### Smart Service Entry Points
- **`GenerateChartReport`** — Parses JSON, calls `WordDocumentService`, uploads result via `AppianDocumentUploader`
- **`GenerateChartImage`** — Calls `ChartGenerationService` directly, uploads PNG
- **`InsertChartIntoDocument`** — Inserts a chart image into an existing document

---

## Test Organisation

| Test Class | Phase | Type |
| :--- | :--- | :--- |
| `FontControlValidationTest` | 1.1 | Unit — real POI assertions |
| `NestedTableValidationTest` | 1.2 | Unit — real CTTc XML assertions |
| `MemoryBankValidationTest` | 2 | File system — checks memory-bank files exist |
| `SecurityValidationTest` | 3 | Unit — validates CWE fixes (partially stubbed) |
| `IntegrationValidationTest` | 4.1 | Integration — real WordDocumentService calls |
| `DocumentationValidationTest` | 4.2 | File system — checks README and JavaDoc |
| `PerformanceValidationTest` | 5.1 | Performance — timing benchmarks (partially stubbed) |
| `EndToEndValidationTest` | 5.2 | E2E — full workflow (partially stubbed) |
| `ComprehensiveValidationSuite` | All | Orchestrator — runs all phases in order |
| `AllChartTypesTest` | — | Manual — generates all chart types |
| `SecurityRegressionTest` | — | Manual — regression test for security fixes |
| `ScatterPlotUnitTest` | — | Unit — scatter plot specific tests |
