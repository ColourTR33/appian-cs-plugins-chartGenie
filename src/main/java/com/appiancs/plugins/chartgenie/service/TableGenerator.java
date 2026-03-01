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
  private static final int TARGET_FULL_WIDTH_TWIPS = 13958; // Landscape A4 minus standard margins
  private static final int TARGET_MAIN_COL_TWIPS = 11000; // ~79% of Landscape A4 (Sidebar layout)

  public TableGenerator() {
    this.htmlRenderer = new HtmlRichTextRenderer();
  }

  public void createStyledTable(XWPFDocument doc, XWPFTableCell parentCell, TableConfiguration config) {
    XWPFTable table = null;

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

    int targetTwips = (parentCell == null) ? TARGET_FULL_WIDTH_TWIPS : TARGET_MAIN_COL_TWIPS;

    buildTableStructure(table, config, targetTwips);
    applyTablePolishing(doc, table, config, targetTwips);

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

        // CRITICAL FIX: Eradicate ghost cells that POI tacked onto the right-hand side
        while (row.getTableCells().size() > rowData.size()) {
          row.removeCell(row.getTableCells().size() - 1);
        }
      }
    }
  }

  // Completely wipe a cell so it doesn't inherit background colors or widths from the row above it
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

  private void applyTablePolishing(XWPFDocument doc, XWPFTable table, TableConfiguration config, int targetTwips) {
    CTTblPr tblPr = table.getCTTbl().getTblPr();
    if (tblPr == null)
      tblPr = table.getCTTbl().addNewTblPr();

    tblPr.addNewTblLayout().setType(STTblLayoutType.FIXED);

    CTTblWidth tblW = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
    tblW.setType(STTblWidth.DXA);
    tblW.setW(BigInteger.valueOf(targetTwips));

    CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
    borders.addNewBottom().setVal(STBorder.SINGLE);
    borders.getBottom().setColor("BFBFBF");
    borders.addNewTop().setVal(STBorder.SINGLE);
    borders.getTop().setColor("BFBFBF");
    borders.addNewLeft().setVal(STBorder.SINGLE);
    borders.getLeft().setColor("BFBFBF");
    borders.addNewRight().setVal(STBorder.SINGLE);
    borders.getRight().setColor("BFBFBF");
    borders.addNewInsideH().setVal(STBorder.SINGLE);
    borders.getInsideH().setColor("BFBFBF");
    borders.addNewInsideV().setVal(STBorder.SINGLE);
    borders.getInsideV().setColor("BFBFBF");

    int dataRowCounter = 0;
    boolean hasHeaders = config.getHeaders() != null && !config.getHeaders().isEmpty();

    for (int r = 0; r < table.getNumberOfRows(); r++) {
      XWPFTableRow row = table.getRow(r);
      boolean isHeaderRow = (r == 0 && hasHeaders);

      List<XWPFTableCell> cells = row.getTableCells();
      int gridColumnIndex = 0;

      for (int c = 0; c < cells.size(); c++) {
        XWPFTableCell cell = cells.get(c);
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        int span = tcPr.isSetGridSpan() ? tcPr.getGridSpan().getVal().intValue() : 1;

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

        if (isHeaderRow) {
          setCellColor(cell, config.getHeaderBackgroundColor());
          XWPFParagraph p = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
          p.setAlignment(ParagraphAlignment.CENTER);
          XWPFRun run = p.createRun();
          if (c < config.getHeaders().size()) {
            run.setText(config.getHeaders().get(c));
          }
          run.setBold(true);
          run.setColor(config.getHeaderTextColor() != null ? config.getHeaderTextColor() : "FFFFFF");
        } else {
          if (config.getRows() != null && dataRowCounter < config.getRows().size()) {
            List<TableCellConfig> dataRow = config.getRows().get(dataRowCounter);
            if (c < dataRow.size()) {
              TableCellConfig cellData = dataRow.get(c);

              // Color mapping
              if (cellData.getBackgroundColor() != null) {
                setCellColor(cell, cellData.getBackgroundColor());
              } else if (dataRowCounter % 2 != 0 && config.getOddRowColor() != null) {
                setCellColor(cell, config.getOddRowColor());
              } else {
                setCellColor(cell, null); // Clear to ensure no inheritance
              }

              // Render Content
              if (!cell.getParagraphs().isEmpty()) {
                cell.removeParagraph(0); // clear dummy paragraph
              }
              htmlRenderer.render(doc, cell, cellData.getText());
              if (cell.getParagraphs().isEmpty())
                cell.addParagraph(); // failsafe

              // Text colors
              if (cellData.getTextColor() != null) {
                for (XWPFParagraph p : cell.getParagraphs()) {
                  for (XWPFRun run : p.getRuns()) {
                    run.setColor(cellData.getTextColor().replace("#", ""));
                  }
                }
              }
            }
          }
        }

        applyCellMargins(cell);
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

        gridColumnIndex += span;
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
    if (!margins.isSetTop())
      margins.addNewTop().setW(padding);
    if (!margins.isSetBottom())
      margins.addNewBottom().setW(padding);
  }

  private void setCellColor(XWPFTableCell cell, String hexColor) {
    CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    if (hexColor == null || hexColor.isEmpty()) {
      if (tcPr.isSetShd())
        tcPr.unsetShd(); // Erase cloned color
      return;
    }
    String fill = hexColor.replace("#", "").toUpperCase();
    CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
    shd.setVal(STShd.CLEAR);
    shd.setFill(fill);
  }
}