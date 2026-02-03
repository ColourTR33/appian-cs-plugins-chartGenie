package com.appiancs.plugins.chartgenie.service;

import java.util.List;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;

import com.appiancs.plugins.chartgenie.dto.TableConfiguration;

public class TableGenerator {

  // We pass the HtmlRenderer here so cells can support rich text!
  private final HtmlRichTextRenderer htmlRenderer;

  public TableGenerator() {
    this.htmlRenderer = new HtmlRichTextRenderer();
  }

  public void createStyledTable(XWPFDocument doc, XWPFTableCell parentCell, TableConfiguration config) {
    XWPFTable table;

    if (parentCell != null) {
      if (parentCell.getParagraphs().isEmpty())
        parentCell.addParagraph();
      XWPFParagraph p = parentCell.getParagraphs().get(0);
      XmlCursor cursor = p.getCTP().newCursor();
      cursor.toEndToken();
      table = parentCell.insertNewTbl(cursor);
      cursor.dispose();
    } else {
      table = doc.createTable();
    }

    if (config.getHeaders() != null) {
      XWPFTableRow headerRow = (table.getRows().size() > 0) ? table.getRow(0) : table.createRow();
      for (int i = 0; i < config.getHeaders().size(); i++) {
        XWPFTableCell cell = headerRow.getCell(i);
        if (cell == null)
          cell = headerRow.createCell();

        // Headers are usually simple text, but we could make them rich too
        setCellColor(cell, config.getHeaderBackgroundColor());

        XWPFParagraph p = (cell.getParagraphs().size() > 0) ? cell.getParagraphs().get(0) : cell.addParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setText(config.getHeaders().get(i));
        r.setBold(true);
        r.setColor(config.getHeaderTextColor() != null ? config.getHeaderTextColor() : "FFFFFF");
      }
    }

    if (config.getRows() != null) {
      int rowIndex = 0;
      for (List<String> rowData : config.getRows()) {
        XWPFTableRow row = table.createRow();
        boolean isOdd = (rowIndex % 2 != 0);
        for (int i = 0; i < rowData.size(); i++) {
          XWPFTableCell cell = row.getCell(i);
          if (cell == null)
            cell = row.createCell();

          // HERE IS THE MAGIC: Use HTML Renderer for cell content
          htmlRenderer.render(doc, cell, rowData.get(i));

          if (isOdd && config.getOddRowColor() != null) {
            setCellColor(cell, config.getOddRowColor());
          }
        }
        rowIndex++;
      }
    }

    if (parentCell == null)
      doc.createParagraph();
  }

  private void setCellColor(XWPFTableCell cell, String hexColor) {
    if (hexColor == null || hexColor.isEmpty())
      return;
    hexColor = hexColor.replace("#", "");
    CTTcPr pr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    CTShd shd = pr.isSetShd() ? pr.getShd() : pr.addNewShd();
    shd.setVal(STShd.CLEAR);
    shd.setColor("auto");
    shd.setFill(hexColor);
  }
}