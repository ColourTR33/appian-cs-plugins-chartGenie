# ChartGenie for Appian

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg) ![Appian](https://img.shields.io/badge/Appian-23.x%2B-orange.svg)

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

---

## 📦 Installation

1.  Download the `chartGenie-1.0.0.jar`.
2.  Log in to the **Appian Admin Console**.
3.  Navigate to **Plug-ins** > **Add Plug-in**.
4.  Upload the JAR file.
5.  Wait for the status to turn green (Active).

---

## ⚠️ Requirements & Limits

* **Appian Version:** 23.x or higher.
* **Java Version:** 11 or 17 (Standard for modern Appian).
* **Safety Caps:** Charts are limited to a maximum resolution of 2000x2000px to prevent server memory exhaustion.
* **Fonts:** The plugin will attempt to use the requested font; if not installed on the server, it falls back to standard SansSerif.

---

**Developed by Appian Customer Success**
