# ChartGenie — Product Overview

## What It Is

ChartGenie is a high-performance document generation plugin for the Appian low-code platform. It enables developers to generate Microsoft Word (.docx) reports containing dynamically rendered, professional-grade charts and styled tables from simple JSON payloads passed through Appian Smart Services.

It is developed and maintained by **Appian Customer Success**.

---

## Target Users

- Appian developers building reporting workflows
- Enterprise clients requiring branded, data-driven Word documents
- Audit, compliance, and analytics teams needing automated report generation

---

## Key Features

| Feature | Status | Description |
| :--- | :--- | :--- |
| Dynamic Charting | ✅ Complete | Bar, Column, Line, Area, Pie, Donut, Stacked, Scatter chart types |
| Word Integration | ✅ Complete | Embeds charts and tables into .docx templates |
| Enterprise Branding | ✅ Complete | Configurable primary color, font family, page size |
| Sidebar Layout | ✅ Complete | Two-column layout with main content and sidebar |
| Headers & Footers | ✅ Complete | Configurable header text, subheader, footer, audit reference, date |
| Rich Text Styling | ✅ Complete | HTML rich text (`<b>`, `<i>`, `<u>`, `<span style="color">`, `<ul>`, `<li>`) in table cells |
| Font Control | ✅ Complete | Per-table `headerFontSize` and `bodyFontSize` (8–72pt) |
| Nested Tables | ✅ Complete | Tables within table cells, up to 3 levels deep |
| QR Code Generation | ✅ Complete | Embeds QR codes from URLs |
| Security Hardening | ✅ Complete | CWE-94, CWE-117/93, CWE-22/23, CWE-398 all fixed |

---

## Smart Services

### 1. Generate Chart Report
Core service. Accepts a JSON payload and a Word template, produces a full .docx report.

**Inputs:** JSON Payload, Template Document, New Document Name, Save In Folder
**Outputs:** New Document, Error Occurred (Boolean), Error Message (Text)

### 2. Generate Chart Image
Generates a single PNG chart image. Useful for Appian interfaces or debugging.

**Inputs:** Chart Type, Categories (List), Values (List), Primary Color, Target Folder

### 3. Insert Chart Into Document
Inserts a pre-generated chart image into an existing Word document.

---

## Supported Section Types (JSON)

| Type | Description |
| :--- | :--- |
| `HEADING` | Bold heading text (H1 style) |
| `HEADING2` | Coloured banner heading (H2 style) |
| `TEXT` / `PARAGRAPH` | Standard paragraph text |
| `RICH_TEXT` | HTML rich text block |
| `CHART` | Rendered chart image |
| `REPORT_TABLE` | Styled data table with optional nested tables |
| `SIDEBAR_LAYOUT` | Two-column layout section |
| `PAGE_BREAK` | Forces a new page |
| `STATUS_BADGE` | Coloured status badge widget |
| `QR_CODE` | QR code from a URL string |

---

## Supported Chart Types

`BAR`, `COLUMN`, `LINE`, `AREA`, `PIE`, `DONUT`, `STACKED`, `SCATTER`

---

## Requirements

- **Appian Version:** 23.x or higher
- **Java Version:** 11 or 17
- **Safety Caps:** Charts capped at 2000×2000px to prevent OOM
- **Font Fallback:** Falls back to SansSerif if requested font not installed on server
