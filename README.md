# ChartGenie for Appian

![Version](https://img.shields.io/badge/version-1.4.0-blue.svg) ![Appian](https://img.shields.io/badge/Appian-25.2%2B-orange.svg)

**ChartGenie** is a high-performance document generation plugin for Appian. It allows developers to generate Microsoft Word (.docx) reports containing dynamically rendered, professional-grade charts based on simple JSON payloads.

Unlike standard document generation tools, ChartGenie focuses specifically on **Data Visualization**, supporting modern "Flat Design" aesthetics, dynamic branding (colors/fonts), and vector-quality chart rendering.

---

## 🚀 Key Features

* **Dynamic Charting:** Generate Bar, Column, Line, Area, Pie, and Donut charts on the fly.
* **Word Integration:** Embeds charts directly into DOCX templates using a replacement tag system.
* **Enterprise Branding:** Fully configurable Primary/Secondary colors and Font families via JSON.
* **Smart Layouts:** Supports Sidebars, Page Breaks, Headings, and Paragraphs.
* **Resilience:** Built-in protection against memory overruns, null data, and invalid inputs.
* **Modern Aesthetics:** Charts use automatic monochromatic palette generation and clean, flat design principles.
* **Rich Text Styling:** Full HTML rich text support (`<b>`, `<i>`, `<u>`, `<span style="color">`, `<ul>`, `<li>`) in table cells.
* **Font Control:** Per-table `headerFontSize` and `bodyFontSize` (8–72pt) for precise typography.
* **Nested Tables:** Embed tables within table cells up to 3 levels deep for complex report layouts.

---

## 🛠 Smart Services

### 1. Generate Chart Report
The core service. Takes a JSON payload and a Word template, produces a full report.

| Input Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| **JSON Payload** | Text | Yes | The configuration defining settings, data, and chart types (see Examples). |
| **Template Document** | Document | Yes | A .docx file acting as the base. Use `{{chart}}` tags for placement. |
| **New Document Name** | Text | Yes | Name of the output file. |
| **Save In Folder** | Folder | Yes | Appian Folder to save the report. |

| Output Parameter | Type | Description |
| :--- | :--- | :--- |
| **New Document** | Document | The generated Word report. |
| **Error Occurred** | Boolean | True if generation failed. |
| **Error Message** | Text | Detailed failure reason (if any). |

### 2. Generate Chart Image
Generates a single PNG image file of a chart. Useful for interfaces or debugging.

| Input Parameter | Type | Description |
| :--- | :--- | :--- |
| **Chart Type** | Text | `BAR`, `COLUMN`, `LINE`, `AREA`, `PIE`, `DONUT`, `STACKED` |
| **Categories** | Text (List) | X-Axis labels or Pie slices (e.g., `["Jan", "Feb"]`). |
| **Values** | Number (List) | Y-Axis values (e.g., `[10, 20]`). |
| **Primary Color** | Text | Hex code (e.g., `1E3C96`). |
| **Target Folder** | Folder | Where to save the PNG. |

### 3. Insert Chart Into Document
Inserts a pre-generated chart image into an existing Word document (appending it) or creates a new one.

---

## 📄 JSON Configuration Schema

The `Generate Chart Report` service expects a JSON structure with two main blocks: `settings` and `sections`.

### Global Settings
| Field | Description | Default |
| :--- | :--- | :--- |
| `fontFamily` | Font for chart titles and axes. | `Calibri` |
| `primaryColor` | Hex code for branding. | `000000` (Black) |
| `pageSize` | `A4` or `LETTER`. | `A4` |

### Supported Section Types
* **HEADING:** Adds a bold header text.
* **PARAGRAPH:** Adds standard text.
* **PAGE_BREAK:** Forces a new page.
* **CHART:** Renders a visual chart.
* **REPORT_TABLE:** Renders a styled data table.
* **SIDEBAR_LAYOUT:** Two-column layout with main content and a sidebar.
* **RICH_TEXT:** Renders HTML rich text content.

### REPORT_TABLE Configuration (`tableConfig`)
| Field | Type | Description | Default |
| :--- | :--- | :--- | :--- |
| `headers` | String[] | Column header labels. | `null` |
| `columnWidths` | Integer[] | Column widths as percentages (must sum to 100). | `null` |
| `rows` | Cell[][] | 2D array of cell config objects. | `null` |
| `headerBackgroundColor` | String | Hex color for header row background. | `000000` |
| `headerTextColor` | String | Hex color for header row text. | `FFFFFF` |
| `oddRowColor` | String | Hex color for alternating row shading. | `null` |
| `bordersEnabled` | Boolean | Show/hide table borders. | `true` |
| `headerFontSize` | Integer | Font size for header row (8–72pt). | document default |
| `bodyFontSize` | Integer | Font size for data rows (8–72pt). | document default |

### Cell Configuration (`rows[n][n]`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `text` | String | Plain text or HTML rich text content. |
| `colspan` | Integer | Number of columns this cell spans. |
| `backgroundColor` | String | Hex color override for this cell's background. |
| `textColor` | String | Hex color override for this cell's text. |
| `nestedTable` | TableConfig | A full `tableConfig` object to render as a nested table inside this cell (max 3 levels deep). |

### Nested Table Example
```json
{
  "type": "REPORT_TABLE",
  "tableConfig": {
    "headers": ["Category", "Details"],
    "columnWidths": [30, 70],
    "headerFontSize": 12,
    "bodyFontSize": 10,
    "rows": [
      [
        { "text": "Risk" },
        {
          "text": "",
          "nestedTable": {
            "headers": ["Level", "Score"],
            "columnWidths": [50, 50],
            "headerBackgroundColor": "CC0000",
            "headerTextColor": "FFFFFF",
            "rows": [
              [{ "text": "High" }, { "text": "9.2" }],
              [{ "text": "Medium" }, { "text": "5.1" }]
            ]
          }
        }
      ]
    ]
  }
}
```

---

## 🏗 Architecture (v1.4.0 Refactoring)

Version 1.4.0 introduces significant internal refactoring for maintainability, testability, and clean separation of concerns.

### Monochromatic Palette Generation
Hard-coded client-specific colour palettes have been replaced with a dynamic `MonochromaticPaletteGenerator`. Given any hex colour, it generates a monochromatic palette by distributing lightness evenly across HSL space. Invalid or missing colours fall back to a default grayscale palette. Colour cycling (`palette[index % palette.length]`) handles datasets with more series than palette entries.

### Section Handler Decomposition
`WordDocumentService` has been decomposed from a monolithic class into a handler-based architecture:
- A `SectionHandler` interface defines the contract for rendering individual section types.
- Dedicated handler classes (`HeadingSectionHandler`, `TableSectionHandler`, `ChartSectionHandler`, `SidebarLayoutSectionHandler`, etc.) encapsulate the rendering logic for each section type.
- A handler registry dispatches section processing, with a `DefaultSectionHandler` for unrecognized types.
- `WordDocumentService` now orchestrates rather than implements rendering directly.

### Constructor-Based Dependency Injection
Services now accept their dependencies via parameterized constructors:
- `WordDocumentService` accepts `HtmlRichTextRenderer`, `TableGenerator`, and `TemplateVariableSubstitutor`.
- `ChartGenerationService` accepts a `ChartStrategyFactory` instance.
- No-arg constructors delegate with production defaults, preserving backward compatibility with Appian Smart Service entry points.
- All constructor parameters are validated with null checks.

### JUnit 5 Test Conversions
All legacy manual test runners (`LocalRunner`, `BiaLocalRunner`, `AllComponentsLocalRunner`, etc.) have been converted to proper JUnit 5 tests:
- `public static void main` methods replaced with `@Test` annotations.
- `Assumptions.assumeTrue` guards tests that depend on local template files.
- Proper assertions validate outputs.
- JavaExec Gradle task registrations removed in favour of standard `./gradlew test`.

### Testing Infrastructure
- **JUnit 5** for unit and integration tests.
- **jqwik** for property-based testing of palette generation, section dispatch, and DI contracts.
- **Mockito** for dependency injection verification.

---

## 📦 Installation

1.  Download the `chartGenie-1.4.0.jar`.
2.  Log in to the **Appian Admin Console**.
3.  Navigate to **Plug-ins** > **Add Plug-in**.
4.  Upload the JAR file.
5.  Wait for the status to turn green (Active).

---

## ⚠️ Requirements & Limits

* **Appian Version:** 25.2 or higher.
* **Java Version:** 11 or 17 (Standard for modern Appian).
* **Safety Caps:** Charts are limited to a maximum resolution of 2000x2000px to prevent server memory exhaustion.
* **Fonts:** The plugin will attempt to use the requested font; if not installed on the server, it falls back to standard SansSerif.

---

**Developed by Appian Customer Success**
