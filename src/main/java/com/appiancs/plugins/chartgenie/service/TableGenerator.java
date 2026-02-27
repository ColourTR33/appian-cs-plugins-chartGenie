package com.appiancs.plugins.chartgenie.service;

import java.math.BigInteger;
import java.util.List;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import com.appiancs.plugins.chartgenie.dto.TableCellConfig;
import com.appiancs.plugins.chartgenie.dto.TableConfiguration;

public class TableGenerator {

  private final HtmlRichTextRenderer htmlRenderer;
  private static final int LANDSCAPE_A4_TWIPS = 14000;

  public TableGenerator() {
    this.htmlRenderer = new HtmlRichTextRenderer();
  }

  public void createStyledTable(XWPFDocument doc, XWPFTableCell parentCell, TableConfiguration config) {
    XWPFTable table = null;

    // 1. Table Creation
    if (parentCell != null) {
      if (parentCell.getParagraphs().isEmpty()) {
        parentCell.addParagraph();
      }
      XWPFParagraph p = parentCell.getParagraphs().get(0);
      XmlCursor cursor = p.getCTP().newCursor();
      table = parentCell.insertNewTbl(cursor);
      cursor.dispose();
    } else {
      table = doc.createTable();
    }
    if (table == null)
      return;

    // Phase 1: Build Structure and Geometry (Safely)
    boolean isFirstRowUsed = buildTableStructure(table, config);

    // Phase 2: Deep XML Sync
    syncTableRows(table);

    // Phase 3: Apply Content, Widths, and Styles
    applyTablePolishing(doc, table, config, isFirstRowUsed, parentCell == null);

    if (parentCell == null) {
      doc.createParagraph();
    }
  }

  private boolean buildTableStructure(XWPFTable table, TableConfiguration config) {
    boolean headerUsed = false;

    // Clear any default rows POI might have added to ensure a clean structural map
    while (table.getNumberOfRows() > 0) {
      table.removeRow(0);
    }

    // Initialize Headers
    if (config.getHeaders() != null && !config.getHeaders().isEmpty()) {
      XWPFTableRow headerRow = table.createRow();
      while (headerRow.getTableCells().size() < config.getHeaders().size()) {
        headerRow.createCell();
      }
      headerUsed = true;
    }

    // Initialize Data Rows and Colspans
    if (config.getRows() != null) {
      for (List<TableCellConfig> rowData : config.getRows()) {
        XWPFTableRow row = table.createRow();
        while (row.getTableCells().size() < rowData.size()) {
          row.createCell();
        }

        // Apply Colspan Geometry
        for (int i = 0; i < rowData.size(); i++) {
          TableCellConfig cellData = rowData.get(i);
          if (cellData.getColspan() != null && cellData.getColspan() > 1) {
            XWPFTableCell cell = row.getCell(i);
            if (cell != null) {
              CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
              tcPr.addNewGridSpan().setVal(BigInteger.valueOf(cellData.getColspan()));
            }

            int toRemove = cellData.getColspan() - 1;
            for (int k = 0; k < toRemove; k++) {
              if (row.getTableCells().size() > i + 1)
                row.removeCell(i + 1);
            }
          }
        }
      }
    }
    return headerUsed;
  }

  private void syncTableRows(XWPFTable table) {
    try {
      java.lang.reflect.Field field = XWPFTable.class.getDeclaredField("tableRows");
      field.setAccessible(true);
      List<XWPFTableRow> tableRows = (List<XWPFTableRow>) field.get(table);
      tableRows.clear();
      for (CTRow ctRow : table.getCTTbl().getTrList()) {
        tableRows.add(new XWPFTableRow(ctRow, table));
      }
    } catch (Exception e) {
      // Fallback for restricted environments
    }
  }

  private void applyTablePolishing(XWPFDocument doc, XWPFTable table, TableConfiguration config, boolean isFirstRowUsed,
    boolean isFullWidth) {
    int targetTwips = isFullWidth ? LANDSCAPE_A4_TWIPS : 11000;

    CTTblPr tblPr = table.getCTTbl().getTblPr();
    if (tblPr == null)
      tblPr = table.getCTTbl().addNewTblPr();
    tblPr.addNewTblLayout().setType(STTblLayoutType.FIXED);

    int dataRowCounter = 0;
    for (int r = 0; r < table.getNumberOfRows(); r++) {
      XWPFTableRow row = table.getRow(r);
      int colIndex = 0;

      boolean isHeaderRow = (r == 0 && isFirstRowUsed);
      List<XWPFTableCell> cells = row.getTableCells();

      for (int c = 0; c < cells.size(); c++) {
        XWPFTableCell cell = cells.get(c);

        if (isHeaderRow) {
          setCellColor(cell, config.getHeaderBackgroundColor());
          XWPFParagraph p = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
          p.setAlignment(ParagraphAlignment.CENTER);
          XWPFRun run = p.createRun();
          run.setText(config.getHeaders().get(c));
          run.setBold(true);
          run.setColor(config.getHeaderTextColor() != null ? config.getHeaderTextColor() : "FFFFFF");
        } else {
          if (config.getRows() != null && dataRowCounter < config.getRows().size()) {
            List<TableCellConfig> dataRow = config.getRows().get(dataRowCounter);
            if (c < dataRow.size()) {
              TableCellConfig cellData = dataRow.get(c);
              if (cellData.getBackgroundColor() != null) {
                setCellColor(cell, cellData.getBackgroundColor());
              } else if (dataRowCounter % 2 != 0 && config.getOddRowColor() != null) {
                setCellColor(cell, config.getOddRowColor());
              }
              htmlRenderer.render(doc, cell, cellData.getText());
            }
          }
        }

        applyCellMargins(cell);

        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        int span = tcPr.isSetGridSpan() ? tcPr.getGridSpan().getVal().intValue() : 1;

        int cellWidth = 0;
        for (int k = 0; k < span && (colIndex + k) < config.getColumnWidths().size(); k++) {
          cellWidth += (int) (targetTwips * (config.getColumnWidths().get(colIndex + k) / 100.0));
        }

        CTTblWidth w = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        w.setType(STTblWidth.DXA);
        w.setW(BigInteger.valueOf(cellWidth));

        colIndex += span;
      }
      if (!isHeaderRow)
        dataRowCounter++;
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
  }

  private void setCellColor(XWPFTableCell cell, String hexColor) {
    if (hexColor == null || hexColor.isEmpty())
      return;
    String fill = hexColor.replace("#", "").toUpperCase();
    CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
    shd.setVal(STShd.CLEAR);
    shd.setFill(fill);
  }
}