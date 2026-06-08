# ChartGenie — Technical Architecture

## Build System

- **Gradle 7.6** with Java toolchain targeting **Java 17**
- **Gradle wrapper** (`gradlew.bat`) — no system Gradle required
- Key tasks: `test`, `jar`, `runLocalTest`, `runAllChartTypesTest`, `runSecurityRegressionTest`
- JAR is built with `duplicatesStrategy = FAIL` and bundles all `implementation` dependencies into `META-INF/lib/`

---

## Core Dependencies

| Library | Version | Purpose |
| :--- | :--- | :--- |
| Apache POI (`poi-ooxml`) | 5.5.1 | Word document (.docx) generation via OOXML |
| Apache XMLBeans | 5.1.1 | Low-level XML manipulation for POI CTTbl/CTTc operations |
| JFreeChart | 1.5.6 | Chart image generation (PNG output) |
| JSoup | 1.17.2 | HTML sanitization (CWE-94) and rich text parsing |
| Google Gson | 2.8.9 | JSON payload deserialization |
| ZXing (`core` + `javase`) | 3.5.1 | QR code generation |
| SLF4J + Log4j2 | 1.7.36 / 2.24.1 | Logging framework |
| JAXB | 2.3.1 | XML binding (required by POI on Java 11+) |
| Appian Plugin SDK | 24.2 (stub) | Appian Smart Service framework (compile-only) |
| JUnit Jupiter | 5.7.0 | Test framework |

---

## Architectural Patterns

### Strategy Pattern — Chart Generation
`ChartStrategyFactory` maps a chart type string to a `ChartGeneratorStrategy` implementation. Each strategy is responsible for building a `JFreeChart` object from a `ChartConfiguration`. Adding a new chart type requires only a new strategy class and a factory registration — no changes to `ChartGenerationService`.

### DTO Layer
All data flows through immutable-friendly DTOs:
- `ReportRequest` → `ReportSettings` + `List<ReportSection>`
- `ReportSection` contains a union of possible content (chart config, table config, text, etc.)
- `TableConfiguration` → `List<List<TableCellConfig>>`
- `TableCellConfig` optionally contains a nested `TableConfiguration`

### Service Orchestration
`WordDocumentService` is the central orchestrator. It processes sections recursively (supporting `SIDEBAR_LAYOUT` which contains nested sections), delegates table rendering to `TableGenerator`, chart rendering to `ChartGenerationService`, and HTML rendering to `HtmlRichTextRenderer`.

---

## Key Implementation Details

### Nested Table Rendering
POI's `XWPFTableCell.insertNewTbl(cursor)` causes `XmlValueDisconnectedException` when `buildTableStructure()` subsequently calls `table.removeRow(0)` on the inserted table. The solution:
1. Build the nested table in a **scratch `XWPFDocument`** (isolated from the parent document)
2. Copy the fully-built `CTTbl` XML via `scratchTable.getCTTbl().copy()`
3. Inject into the parent cell via `parentCell.getCTTc().addNewTbl().set(nestedCtTbl)`

Note: `XWPFTableCell.getTables()` does **not** reflect tables added via `addNewTbl()` after construction. Assertions must use `cell.getCTTc().getTblArray()` (raw XML).

### Font Size Application
Font sizes are applied in `TableGenerator.applyTablePolishing()`:
- Header rows: applied directly to the `XWPFRun` after `run.setText()`
- Body rows: applied to all runs in all paragraphs after `HtmlRichTextRenderer.render()`
- Both are clamped: `Math.max(8, Math.min(72, fontSize))`

### HTML Rich Text Rendering
`HtmlRichTextRenderer` uses JSoup to parse HTML fragments into a DOM tree, then recursively traverses nodes to create `XWPFRun` objects with appropriate styling. Supports: `<b>`, `<strong>`, `<i>`, `<em>`, `<u>`, `<span style="color:...">`, `<p>`, `<div>`, `<ul>`, `<ol>`, `<li>`, `<br>`. Color supports both `#HEX` and `rgb(r,g,b)` formats.

### Table Width Model
Tables use `STTblLayoutType.FIXED` with explicit column widths in twips (1/1440 inch):
- Full-width tables: `TARGET_FULL_WIDTH_TWIPS = 13958` (Landscape A4 minus margins)
- Sidebar main column: `TARGET_MAIN_COL_TWIPS = 11000`
- Nested tables: 90% of parent width

### Security Architecture

| CWE | Location | Fix |
| :--- | :--- | :--- |
| CWE-94 (Code Injection) | `TableGenerator` | JSoup `Safelist` sanitizes all HTML cell content and header text before rendering |
| CWE-117/93 (Log Injection) | `BaseSmartService` | `sanitizeForLogging()` strips `\r\n\t` and control characters; all logging uses SLF4J parameterized format |
| CWE-22/23 (Path Traversal) | `DocumentUtils`, `InsertChartIntoDocument` | `validateAndSecureFilePath()` compares canonical vs absolute path, checks for null bytes and encoded traversal patterns |
| CWE-398 (Poor Logging) | All test classes | Replaced `System.out.println()` with SLF4J `Logger` |

---

## Page Layout Model

| Setting | Value |
| :--- | :--- |
| A4 Portrait | 11906 × 16838 twips |
| A4 Landscape | 16838 × 11906 twips |
| Letter Portrait | 12240 × 15840 twips |
| Standard margin | 1440 twips (1 inch) |
| Content width (Portrait) | `pageWidth - 2 × margin - 480` twips |

---

## Appian Integration

Smart Services extend `BaseSmartService` which extends Appian's `AppianSmartService`. The `ContentService` is injected by the Appian runtime via constructor. Documents are uploaded using the "re-fetch" strategy in `DocumentUtils`: create a metadata shell first, then stream content into the live object via `liveDoc.getOutputStream()`.

---

## Known Constraints

- `XWPFTableCell.getTables()` does not reflect `addNewTbl()` additions — use `getCTTc().getTblArray()` for nested table assertions
- Chart sections in JSON payloads require `width` and `height` fields — omitting them causes NPE in `ChartGenerationService`
- POI header list (`doc.getHeaderList()`) may be empty when reading from `ByteArrayInputStream` — write to a temp file and re-read for reliable header access
- Appian `ContentService` is compile-only (stub JAR) — Smart Service tests require mocking or the full Appian runtime
