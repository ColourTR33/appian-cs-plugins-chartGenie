package com.appiancs.plugins.chartgenie.service;

import java.math.BigInteger;
import java.util.List;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;

import com.appiancs.plugins.chartgenie.dto.TableCellConfig;
import com.appiancs.plugins.chartgenie.dto.TableConfiguration;

/**
 * Generates styled Apache POI {@link XWPFTable} instances from a {@link TableConfiguration}.
 * <p>
 * Features:
 * <ul>
 * <li>Fixed-layout tables with explicit column widths</li>
 * <li>Styled header rows with configurable background/text color and font size (8–72pt)</li>
 * <li>Data rows with HTML rich text rendering, alternating row colors, and per-cell overrides</li>
 * <li>Nested tables up to {@code MAX_NESTING_DEPTH} (3) levels deep via {@link TableCellConfig#getNestedTable()}</li>
 * <li>CWE-94 protection: all cell content is sanitized via JSoup before rendering</li>
 * </ul>
 */
public class TableGenerator {

  private final HtmlRichTextRenderer htmlRenderer;
  private static final int TARGET_FULL_WIDTH_TWIPS = 13958;
  private static final int TARGET_MAIN_COL_TWIPS = 11000;
  private static final int MAX_NESTING_DEPTH = 3;
  private static final String BORDER_COLOR = "BFBFBF";

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TableGenerator.class);

  // CWE-94: Allowlist of safe HTML tags — mirrors HtmlRichTextRenderer.HTML_SAFELIST
  private static final org.jsoup.safety.Safelist CELL_HTML_SAFELIST = org.jsoup.safety.Safelist.none()
    .addTags("p", "br", "strong", "b", "em", "i", "u", "span", "div", "ul", "ol", "li")
    .addAttributes("span", "style")
    .addProtocols("span", "style", "color");

  private static final java.util.regex.Pattern CELL_DANGEROUS_PROTOCOLS = java.util.regex.Pattern.compile("(javascript|vbscript|data):",
    java.util.regex.Pattern.CASE_INSENSITIVE);

  public TableGenerator() {
    this.htmlRenderer = new HtmlRichTextRenderer();
  }

  public void createStyledTable(XWPFDocument doc, XWPFTableCell parentCell, TableConfiguration config) {
    createStyledTable(doc, parentCell, config, 0);
  }

  private void createStyledTable(XWPFDocument doc, XWPFTableCell parentCell, TableConfiguration config, int depth) {
    XWPFTable table = null;

    if (parentCell != null) {
      if (parentCell.getParagraphs().isEmpty()) {
        parentCell.addParagraph();
      }
      XWPFParagraph p = parentCell.getParagraphs().get(0);
      try (XmlCursor cursor = p.getCTP().newCursor()) {
        table = parentCell.insertNewTbl(cursor);
      }
    } else {
      table = doc.createTable();
    }
    if (table == null)
      return;

    int targetTwips = (parentCell == null) ? TARGET_FULL_WIDTH_TWIPS : TARGET_MAIN_COL_TWIPS;

    buildTableStructure(table, config, targetTwips);
    applyTablePolishing(doc, table, config, targetTwips, depth);

    if (parentCell == null) {
      doc.createParagraph();
    }
  }

  private void buildTableStructure(XWPFTable table, TableConfiguration config, int targetTwips) {
    // Clear default POI row
    while (table.getNumberOfRows() > 0) {
      table.removeRow(0);
    }

    // 1. Build the explicit Table Grid
    CTTblGrid grid = table.getCTTbl().getTblGrid() != null ? table.getCTTbl().getTblGrid() : table.getCTTbl().addNewTblGrid();
    grid.setGridColArray(new CTTblGridCol[0]);

    if (config.getColumnWidths() != null) {
      for (Integer wPct : config.getColumnWidths()) {
        int colWidthTwips = (int) (targetTwips * (wPct / 100.0));
        grid.addNewGridCol().setW(BigInteger.valueOf(colWidthTwips));
      }
    }

    // 2. Build Headers
    if (config.getHeaders() != null && !config.getHeaders().isEmpty()) {
      XWPFTableRow headerRow = table.createRow();
      for (int i = 0; i < config.getHeaders().size(); i++) {
        if (i >= headerRow.getTableCells().size())
          headerRow.createCell();
        cleanClonedCell(headerRow.getCell(i));
      }
      // Strip any ghost cells POI cloned
      while (headerRow.getTableCells().size() > config.getHeaders().size()) {
        headerRow.removeCell(headerRow.getTableCells().size() - 1);
      }
    }

    // 3. Build Data Rows with strict GridSpans
    if (config.getRows() != null) {
      for (List<TableCellConfig> rowData : config.getRows()) {
        XWPFTableRow row = table.createRow(); // NOTE: POI clones the previous row here!

        for (int i = 0; i < rowData.size(); i++) {
          if (i >= row.getTableCells().size())
            row.createCell();
          XWPFTableCell cell = row.getCell(i);
          cleanClonedCell(cell); // Wipe any inherited formatting

          TableCellConfig cellData = rowData.get(i);
          int span = (cellData.getColspan() != null && cellData.getColspan() > 1) ? cellData.getColspan() : 1;
          if (span > 1) {
            CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
            tcPr.addNewGridSpan().setVal(BigInteger.valueOf(span));
          }
        }

        while (row.getTableCells().size() > rowData.size()) {
          row.removeCell(row.getTableCells().size() - 1);
        }
      }
    }
  }

  private void cleanClonedCell(XWPFTableCell cell) {
    if (cell.getCTTc().isSetTcPr()) {
      if (cell.getCTTc().getTcPr().isSetGridSpan())
        cell.getCTTc().getTcPr().unsetGridSpan();
      if (cell.getCTTc().getTcPr().isSetShd())
        cell.getCTTc().getTcPr().unsetShd();
    }
    while (!cell.getParagraphs().isEmpty()) {
      cell.removeParagraph(0);
    }
    cell.addParagraph(); // Leave one blank paragraph to prevent POI crashes
  }

  private void applyTablePolishing(XWPFDocument doc, XWPFTable table, TableConfiguration config, int targetTwips, int depth) {
    applyTableStyle(tblPrFrom(table), targetTwips, config.isBordersEnabled());

    int dataRowCounter = 0;
    boolean hasHeaders = config.getHeaders() != null && !config.getHeaders().isEmpty();

    for (int r = 0; r < table.getNumberOfRows(); r++) {
      XWPFTableRow row = table.getRow(r);
      boolean isHeaderRow = (r == 0 && hasHeaders);
      dataRowCounter = applyRowPolishing(doc, row, config, targetTwips, depth, isHeaderRow, dataRowCounter);
      if (!isHeaderRow)
        dataRowCounter++;
    }
  }

  private CTTblPr tblPrFrom(XWPFTable table) {
    CTTblPr tblPr = table.getCTTbl().getTblPr();
    return tblPr != null ? tblPr : table.getCTTbl().addNewTblPr();
  }

  private void applyTableStyle(CTTblPr tblPr, int targetTwips, boolean bordersEnabled) {
    tblPr.addNewTblLayout().setType(STTblLayoutType.FIXED);

    CTTblWidth tblW = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
    tblW.setType(STTblWidth.DXA);
    tblW.setW(BigInteger.valueOf(targetTwips));

    CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
    if (bordersEnabled) {
      borders.addNewBottom().setVal(STBorder.SINGLE);
      borders.getBottom().setColor(BORDER_COLOR);
      borders.addNewTop().setVal(STBorder.SINGLE);
      borders.getTop().setColor(BORDER_COLOR);
      borders.addNewLeft().setVal(STBorder.SINGLE);
      borders.getLeft().setColor(BORDER_COLOR);
      borders.addNewRight().setVal(STBorder.SINGLE);
      borders.getRight().setColor(BORDER_COLOR);
      borders.addNewInsideH().setVal(STBorder.SINGLE);
      borders.getInsideH().setColor(BORDER_COLOR);
      borders.addNewInsideV().setVal(STBorder.SINGLE);
      borders.getInsideV().setColor(BORDER_COLOR);
    } else {
      borders.addNewBottom().setVal(STBorder.NONE);
      borders.addNewTop().setVal(STBorder.NONE);
      borders.addNewLeft().setVal(STBorder.NONE);
      borders.addNewRight().setVal(STBorder.NONE);
      borders.addNewInsideH().setVal(STBorder.NONE);
      borders.addNewInsideV().setVal(STBorder.NONE);
    }
  }

  private int applyRowPolishing(XWPFDocument doc, XWPFTableRow row, TableConfiguration config,
    int targetTwips, int depth, boolean isHeaderRow, int dataRowCounter) {
    List<XWPFTableCell> cells = row.getTableCells();
    int gridColumnIndex = 0;
    for (int c = 0; c < cells.size(); c++) {
      XWPFTableCell cell = cells.get(c);
      CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
      int span = tcPr.isSetGridSpan() ? tcPr.getGridSpan().getVal().intValue() : 1;
      applyCellWidth(tcPr, config, targetTwips, gridColumnIndex, span);
      if (isHeaderRow) {
        applyHeaderCell(cell, config, c);
      } else {
        applyDataCell(doc, cell, config, targetTwips, depth, dataRowCounter, c);
      }
      applyCellMargins(cell);
      cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
      gridColumnIndex += span;
    }
    return dataRowCounter;
  }

  private void applyCellWidth(CTTcPr tcPr, TableConfiguration config, int targetTwips,
    int gridColumnIndex, int span) {
    int cellWidth = 0;
    for (int k = 0; k < span; k++) {
      if (config.getColumnWidths() != null && (gridColumnIndex + k) < config.getColumnWidths().size()) {
        cellWidth += (int) (targetTwips * (config.getColumnWidths().get(gridColumnIndex + k) / 100.0));
      }
    }
    if (cellWidth > 0) {
      CTTblWidth w = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
      w.setType(STTblWidth.DXA);
      w.setW(BigInteger.valueOf(cellWidth));
    }
  }

  private void applyHeaderCell(XWPFTableCell cell, TableConfiguration config, int c) {
    setCellColor(cell, config.getHeaderBackgroundColor());
    XWPFParagraph p = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
    p.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun run = p.createRun();
    if (c < config.getHeaders().size()) {
      run.setText(sanitizeTextInput(config.getHeaders().get(c)));
    }
    run.setBold(true);
    run.setColor(config.getHeaderTextColor() != null ? config.getHeaderTextColor() : "FFFFFF");
    if (config.getHeaderFontSize() != null) {
      run.setFontSize(Math.max(8, Math.min(72, config.getHeaderFontSize())));
    }
  }

  private void applyDataCell(XWPFDocument doc, XWPFTableCell cell, TableConfiguration config,
    int targetTwips, int depth, int dataRowCounter, int c) {
    if (config.getRows() == null || dataRowCounter >= config.getRows().size())
      return;
    List<TableCellConfig> dataRow = config.getRows().get(dataRowCounter);
    if (c >= dataRow.size())
      return;
    TableCellConfig cellData = dataRow.get(c);

    applyDataCellColor(cell, config, cellData, dataRowCounter);
    renderCellContent(doc, cell, config, targetTwips, depth, cellData, dataRowCounter);
  }

  private void applyDataCellColor(XWPFTableCell cell, TableConfiguration config,
    TableCellConfig cellData, int dataRowCounter) {
    // Per-cell override takes highest priority
    if (cellData.getBackgroundColor() != null) {
      setCellColor(cell, cellData.getBackgroundColor());
      return;
    }
    // Conditional formatting rules — first match wins
    if (config.getConditionalFormats() != null && config.getRows() != null && dataRowCounter < config.getRows().size()) {
      List<TableCellConfig> row = config.getRows().get(dataRowCounter);
      for (TableConfiguration.ConditionalFormat rule : config.getConditionalFormats()) {
        if (rule.getColumnIndex() >= 0 && rule.getColumnIndex() < row.size()) {
          String cellValue = row.get(rule.getColumnIndex()).getText();
          if (evaluateCondition(cellValue, rule.getOperator(), rule.getValue())) {
            if (rule.getBackgroundColor() != null) {
              setCellColor(cell, rule.getBackgroundColor());
            }
            return;
          }
        }
      }
    }
    // Alternating row colour
    if (dataRowCounter % 2 != 0 && config.getOddRowColor() != null) {
      setCellColor(cell, config.getOddRowColor());
    } else {
      setCellColor(cell, null);
    }
  }

  /**
   * Evaluates a conditional formatting operator against a cell value.
   * Numeric comparisons are attempted first; falls back to string comparison.
   */
  private boolean evaluateCondition(String cellValue, String operator, String ruleValue) {
    if (operator == null || ruleValue == null) {
      return false;
    }
    String cv = cellValue != null ? cellValue.trim() : "";
    String rv = ruleValue.trim();
    String op = operator.trim().toLowerCase(java.util.Locale.ROOT);

    // String operators
    switch (op) {
      case "contains":
        return cv.toLowerCase(java.util.Locale.ROOT).contains(rv.toLowerCase(java.util.Locale.ROOT));
      case "startswith":
        return cv.toLowerCase(java.util.Locale.ROOT).startsWith(rv.toLowerCase(java.util.Locale.ROOT));
      case "endswith":
        return cv.toLowerCase(java.util.Locale.ROOT).endsWith(rv.toLowerCase(java.util.Locale.ROOT));
      case "=":
        // Try numeric first, fall back to string
        try {
          return Double.parseDouble(cv) == Double.parseDouble(rv);
        } catch (NumberFormatException e) {
          return cv.equalsIgnoreCase(rv);
        }
      case "!=":
        try {
          return Double.parseDouble(cv) != Double.parseDouble(rv);
        } catch (NumberFormatException e) {
          return !cv.equalsIgnoreCase(rv);
        }
      default:
        break;
    }

    // Numeric-only operators
    try {
      double numCell = Double.parseDouble(cv);
      double numRule = Double.parseDouble(rv);
      switch (op) {
        case ">":
          return numCell > numRule;
        case "<":
          return numCell < numRule;
        case ">=":
          return numCell >= numRule;
        case "<=":
          return numCell <= numRule;
        default:
          return false;
      }
    } catch (NumberFormatException e) {
      LOG.debug("Conditional format: could not parse numeric values '{}' / '{}'", cv, rv);
      return false;
    }
  }

  private void renderCellContent(XWPFDocument doc, XWPFTableCell cell, TableConfiguration config,
    int targetTwips, int depth, TableCellConfig cellData, int dataRowCounter) {
    if (!cell.getParagraphs().isEmpty())
      cell.removeParagraph(0);
    // CWE-94: validate input against allowlist before passing to renderer
    String rawText = cellData.getText();
    if (rawText == null)
      rawText = "";
    String safeHtml = sanitizeCellHtml(rawText);
    // Secondary allowlist check: reject if dangerous protocols survived sanitization
    if (CELL_DANGEROUS_PROTOCOLS.matcher(safeHtml).find()) {
      safeHtml = org.jsoup.Jsoup.parse(safeHtml).text();
    }
    htmlRenderer.render(doc, cell, safeHtml);
    if (cell.getParagraphs().isEmpty())
      cell.addParagraph();

    applyRunStyles(cell, config, cellData, dataRowCounter);

    if (cellData.getNestedTable() != null && depth < MAX_NESTING_DEPTH) {
      renderNestedTable(cell, cellData.getNestedTable(), targetTwips, depth);
    }
  }

  /**
   * Sanitizes HTML cell content against an allowlist (CWE-94).
   * Strips all tags not in the safelist and limits input length.
   */
  private static String sanitizeCellHtml(String input) {
    if (input == null || input.isEmpty())
      return input == null ? "" : input;
    String value = input.length() > 10000 ? input.substring(0, 10000) : input;
    return org.jsoup.Jsoup.clean(value, "", CELL_HTML_SAFELIST);
  }

  private void applyRunStyles(XWPFTableCell cell, TableConfiguration config,
    TableCellConfig cellData, int dataRowCounter) {
    // Resolve the effective text color: per-cell > conditional format > none
    String effectiveTextColor = cellData.getTextColor();
    if (effectiveTextColor == null && config.getConditionalFormats() != null && config.getRows() != null &&
      dataRowCounter < config.getRows().size()) {
      List<TableCellConfig> row = config.getRows().get(dataRowCounter);
      for (TableConfiguration.ConditionalFormat rule : config.getConditionalFormats()) {
        if (rule.getTextColor() != null && rule.getColumnIndex() >= 0 && rule.getColumnIndex() < row.size()) {
          if (evaluateCondition(row.get(rule.getColumnIndex()).getText(),
            rule.getOperator(), rule.getValue())) {
            effectiveTextColor = rule.getTextColor();
            break;
          }
        }
      }
    }

    if (effectiveTextColor == null && config.getBodyFontSize() == null) {
      return;
    }
    int clampedBodySize = config.getBodyFontSize() != null
      ? Math.max(8, Math.min(72, config.getBodyFontSize()))
      : -1;
    String finalColor = effectiveTextColor;
    for (XWPFParagraph p : cell.getParagraphs()) {
      for (XWPFRun run : p.getRuns()) {
        if (finalColor != null) {
          run.setColor(finalColor.replace("#", ""));
        }
        if (clampedBodySize > 0) {
          run.setFontSize(clampedBodySize);
        }
      }
    }
  }

  /**
   * Renders a nested table inside a cell by building a fresh XWPFDocument,
   * constructing the table there (safe from cursor disconnection), then copying
   * the resulting CTTbl XML directly into the parent cell's CT element.
   */
  private void renderNestedTable(XWPFTableCell parentCell,
    TableConfiguration nestedConfig, int parentTwips, int depth) {
    try {
      int nestedWidth = (int) (parentTwips * 0.9);
      try (XWPFDocument scratch = new XWPFDocument()) {
        XWPFTable scratchTable = scratch.createTable();
        buildTableStructure(scratchTable, nestedConfig, nestedWidth);
        applyTablePolishing(scratch, scratchTable, nestedConfig, nestedWidth, depth + 1);
        CTTbl nestedCtTbl = (CTTbl) scratchTable.getCTTbl().copy();
        parentCell.getCTTc().addNewTbl().set(nestedCtTbl);
      }
      parentCell.addParagraph();
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Nested table rendering failed, skipping: {}", e.getMessage());
      }
    }
  }

  private void applyCellMargins(XWPFTableCell cell) {
    CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    CTTcMar margins = tcPr.isSetTcMar() ? tcPr.getTcMar() : tcPr.addNewTcMar();
    BigInteger padding = BigInteger.valueOf(144);
    if (!margins.isSetLeft())
      margins.addNewLeft().setW(padding);
    if (!margins.isSetRight())
      margins.addNewRight().setW(padding);
    if (!margins.isSetTop())
      margins.addNewTop().setW(padding);
    if (!margins.isSetBottom())
      margins.addNewBottom().setW(padding);
  }

  private void setCellColor(XWPFTableCell cell, String hexColor) {
    CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    if (hexColor == null || hexColor.isEmpty()) {
      if (tcPr.isSetShd())
        tcPr.unsetShd();
      return;
    }
    String fill = hexColor.replace("#", "").toUpperCase(java.util.Locale.ROOT);
    CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
    shd.setVal(STShd.CLEAR);
    shd.setFill(fill);
  }

  /**
   * Sanitizes plain text input for header cells (CWE-94).
   * Uses regex tag stripping to avoid invoking the JSoup parser on user input.
   */
  private String sanitizeTextInput(String textContent) {
    if (textContent == null || textContent.isEmpty())
      return textContent;
    String value = textContent.length() > 1000 ? textContent.substring(0, 1000) : textContent;
    return value.replaceAll("<[^>]*>", "").replaceAll("[<>\"'&]", "").trim();
  }
}