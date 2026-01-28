package com.appiancs.plugins.chartgenie.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import com.appiancs.plugins.chartgenie.dto.structure.DocumentSettings;
import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;

public class WordDocumentService {

  // A4 Width (~11900) - Margins = ~10771 Twips.
  private static final int PAGE_CONTENT_WIDTH_TWIPS = 10771;

  /**
   * Creates the base document using the provided InputStream (from Appian).
   */
  public File createBaseDocument(InputStream templateStream) throws IOException {
    XWPFDocument doc;

    if (templateStream != null) {
      doc = new XWPFDocument(templateStream);
    } else {
      // Fallback if user passes null template
      doc = new XWPFDocument();
    }

    File outputFile = File.createTempFile("Report_Base_", ".docx");
    try (FileOutputStream out = new FileOutputStream(outputFile)) {
      doc.write(out);
    } finally {
      // We do not close the input stream here; let the caller handle it.
      doc.close();
    }
    return outputFile;
  }

  /**
   * Applies Header and Footer text ONCE to the document.
   */
  public File applyGlobalHeaderFooter(File inputFile, DocumentSettings settings) throws IOException {
    try (FileInputStream in = new FileInputStream(inputFile);
      XWPFDocument doc = new XWPFDocument(in)) {

      if (settings != null) {
        applyHeaderFooterLogic(doc, settings.getHeaderText(), settings.getFooterText());
      }

      File outputFile = File.createTempFile("Report_Header_", ".docx");
      try (FileOutputStream out = new FileOutputStream(outputFile)) {
        doc.write(out);
      }
      return outputFile;
    }
  }

  private void applyHeaderFooterLogic(XWPFDocument doc, String headerText, String footerText) {
    XWPFHeaderFooterPolicy policy = doc.getHeaderFooterPolicy();
    if (policy == null) {
      policy = doc.createHeaderFooterPolicy();
    }

    // --- HEADER ---
    if (headerText != null && !headerText.isEmpty()) {
      XWPFHeader header = policy.getDefaultHeader();
      if (header == null)
        header = policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);

      // Check for existing to avoid dupes if running multiple times
      if (header.getParagraphs().isEmpty() || !header.getParagraphs().get(0).getText().equals(headerText)) {
        XWPFParagraph p = header.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT); // Left Aligned

        XWPFRun r = p.createRun();
        r.setText(headerText);
        r.setFontFamily("Segoe UI");
        r.setFontSize(16); // Large
        r.setBold(true); // Bold
        r.setColor("FFFFFF"); // White text (requires dark background in template)
      }
    }

    // --- FOOTER ---
    if (footerText != null && !footerText.isEmpty()) {
      XWPFFooter footer = policy.getDefaultFooter();
      if (footer == null)
        footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);

      XWPFParagraph p = footer.createParagraph();
      p.setAlignment(ParagraphAlignment.CENTER);

      XWPFRun r = p.createRun();
      r.setText(footerText);
      r.setFontFamily("Segoe UI");
      r.setFontSize(9);
      r.setColor("666666");
    }
  }

  /**
   * Appends a new page with the Sidebar Layout.
   */
  public File appendSidebarLayout(File inputFile, DocumentSettings settings, List<ReportSection> leftItems, List<ReportSection> rightItems,
    ChartGenerationService chartService) throws Exception {
    try (FileInputStream in = new FileInputStream(inputFile);
      XWPFDocument doc = new XWPFDocument(in)) {

      // 1. CLEANUP "Ghost" paragraphs at start of doc so table sits at the top
      for (int i = doc.getBodyElements().size() - 1; i >= 0; i--) {
        IBodyElement elem = doc.getBodyElements().get(i);
        if (elem instanceof XWPFParagraph) {
          XWPFParagraph p = (XWPFParagraph) elem;
          if (p.getRuns().isEmpty() && p.getText().trim().isEmpty()) {
            doc.removeBodyElement(i);
          }
        }
      }

      // 2. Create Invisible Grid Table
      XWPFTable table = doc.createTable(1, 2);
      removeTableBorders(table);

      // 3. Apply Fixed Layout (70% / 30%)
      int leftWidth = (int) (PAGE_CONTENT_WIDTH_TWIPS * 0.70);
      int rightWidth = (int) (PAGE_CONTENT_WIDTH_TWIPS * 0.30);
      applyFixedTableLayout(table, leftWidth, rightWidth);

      // 4. Configure Row Height (Approx 9.8 inches to allow footer space)
      XWPFTableRow row = table.getRow(0);
      setRowMinHeight(row, 14200);

      // 5. Configure Left Cell (Text Area)
      XWPFTableCell leftCell = row.getCell(0);
      setCellWidth(leftCell, leftWidth);
      // REMOVED SHADING: Left cell is transparent to allow cleaner look or custom backgrounds
      // setCellShading(leftCell, "FFFFFF");

      // 6. Configure Right Cell (Sidebar Area)
      XWPFTableCell rightCell = row.getCell(1);
      setCellWidth(rightCell, rightWidth);
      // TRANSPARENT: No shading applied here. It allows the template's grey bar/logo to show through.

      // 7. Render Content with Auto-Scaling
      renderSectionsToCell(leftCell, leftItems, chartService, leftWidth);
      renderSectionsToCell(rightCell, rightItems, chartService, rightWidth);

      File outputFile = File.createTempFile("Report_Sidebar_", ".docx");
      try (FileOutputStream out = new FileOutputStream(outputFile)) {
        doc.write(out);
      }
      return outputFile;
    }
  }

  // --- RENDER HELPER WITH AUTO-SCALING ---
  private void renderSectionsToCell(XWPFTableCell cell, List<ReportSection> items, ChartGenerationService chartService, int colWidthTwips)
    throws Exception {
    if (cell.getParagraphs().size() > 0) {
      cell.removeParagraph(0);
    }

    for (ReportSection item : items) {
      switch (item.getType()) {
        case HEADING:
          XWPFParagraph pHead = cell.addParagraph();
          pHead.setStyle("Heading1");
          XWPFRun rHead = pHead.createRun();
          rHead.setText(item.getText());
          rHead.setBold(true);
          rHead.setFontSize(14);
          rHead.setColor("2F5496"); // Appian Blue Header
          break;

        case PARAGRAPH:
          XWPFParagraph pPara = cell.addParagraph();
          XWPFRun rPara = pPara.createRun();
          rPara.setText(item.getText());
          break;

        case CHART:
          if (item.getChartConfig() != null) {
            File chartImg = chartService.generateChartImage(item.getChartConfig());
            XWPFParagraph pChart = cell.addParagraph();

            // ALIGNMENT FIX: Right Align & Zero Indent
            pChart.setAlignment(ParagraphAlignment.RIGHT);
            pChart.setIndentationRight(0);

            XWPFRun rChart = pChart.createRun();
            try (FileInputStream imgStream = new FileInputStream(chartImg)) {
              // AUTO-SCALE LOGIC:
              // Use 100% of column width (remove 0.90 buffer) so it hits the edge
              double targetWidthEMU = (colWidthTwips * 1.0) * (914400.0 / 1440.0);

              // Get Source Dimensions (from Config)
              double sourceWidth = item.getChartConfig().getWidth();
              double sourceHeight = item.getChartConfig().getHeight();
              double ratio = sourceHeight / sourceWidth;

              // Force final dimensions to fill the column width
              int finalWidthEMU = (int) targetWidthEMU;
              int finalHeightEMU = (int) (finalWidthEMU * ratio);

              rChart.addPicture(imgStream, XWPFDocument.PICTURE_TYPE_PNG, "Chart", finalWidthEMU, finalHeightEMU);
            }
          }
          break;
      }
    }
  }

  // =========================================================================
  // 3. XML & LAYOUT HELPERS
  // =========================================================================

  private void setCellShading(XWPFTableCell cell, String hexColor) {
    CTTcPr pr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    CTShd shd = pr.isSetShd() ? pr.getShd() : pr.addNewShd();
    shd.setFill(hexColor);
    shd.setColor("auto");
    shd.setVal(STShd.CLEAR);
  }

  private void applyFixedTableLayout(XWPFTable table, int leftTwips, int rightTwips) {
    CTTbl ctTbl = table.getCTTbl();
    CTTblPr pr = ctTbl.getTblPr() != null ? ctTbl.getTblPr() : ctTbl.addNewTblPr();

    CTTblLayoutType type = pr.isSetTblLayout() ? pr.getTblLayout() : pr.addNewTblLayout();
    type.setType(STTblLayoutType.FIXED);

    CTTblWidth tblW = pr.isSetTblW() ? pr.getTblW() : pr.addNewTblW();
    tblW.setType(STTblWidth.DXA);
    tblW.setW(BigInteger.valueOf(leftTwips + rightTwips));

    CTTblGrid grid = ctTbl.addNewTblGrid();
    grid.addNewGridCol().setW(BigInteger.valueOf(leftTwips));
    grid.addNewGridCol().setW(BigInteger.valueOf(rightTwips));
  }

  private void setRowMinHeight(XWPFTableRow row, int heightTwips) {
    CTTrPr trPr = row.getCtRow().isSetTrPr() ? row.getCtRow().getTrPr() : row.getCtRow().addNewTrPr();
    CTHeight ht;
    if (trPr.sizeOfTrHeightArray() > 0) {
      ht = trPr.getTrHeightArray(0);
    } else {
      ht = trPr.addNewTrHeight();
    }
    ht.setVal(BigInteger.valueOf(heightTwips));
    ht.setHRule(STHeightRule.AT_LEAST);
  }

  private void setCellWidth(XWPFTableCell cell, int widthTwips) {
    CTTcPr pr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    CTTblWidth width = pr.isSetTcW() ? pr.getTcW() : pr.addNewTcW();
    width.setType(STTblWidth.DXA);
    width.setW(BigInteger.valueOf(widthTwips));
  }

  private void removeTableBorders(XWPFTable table) {
    CTTblPr tblPr = table.getCTTbl().getTblPr();
    CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
    borders.addNewBottom().setVal(STBorder.NONE);
    borders.addNewLeft().setVal(STBorder.NONE);
    borders.addNewRight().setVal(STBorder.NONE);
    borders.addNewTop().setVal(STBorder.NONE);
    borders.addNewInsideH().setVal(STBorder.NONE);
    borders.addNewInsideV().setVal(STBorder.NONE);
  }

  // =========================================================================
  // 4. LEGACY / UTILITY METHODS
  // =========================================================================

  public File addPageBreak(File inputFile) throws IOException {
    try (FileInputStream in = new FileInputStream(inputFile);
      XWPFDocument doc = new XWPFDocument(in)) {

      XWPFParagraph paragraph = doc.createParagraph();
      XWPFRun run = paragraph.createRun();
      run.addBreak(BreakType.PAGE);

      File outputFile = File.createTempFile("Report_Break_", ".docx");
      try (FileOutputStream out = new FileOutputStream(outputFile)) {
        doc.write(out);
      }
      return outputFile;
    }
  }

  public File appendTextToDocument(File existingDocFile, String text, boolean isHeading) throws IOException {
    File outputFile = File.createTempFile("UpdatedDoc_Text_", ".docx");
    try (FileInputStream fis = new FileInputStream(existingDocFile);
      XWPFDocument doc = new XWPFDocument(fis)) {

      XWPFParagraph p = doc.createParagraph();
      if (isHeading) {
        p.setAlignment(ParagraphAlignment.LEFT);
        p.setSpacingBefore(100);
      } else {
        p.setAlignment(ParagraphAlignment.BOTH);
      }

      XWPFRun r = p.createRun();
      r.setText(text);
      if (isHeading) {
        r.setBold(true);
        r.setFontSize(14);
        r.setColor("2F5496");
      } else {
        r.setFontSize(11);
        r.setFontFamily("Calibri");
      }
      try (FileOutputStream out = new FileOutputStream(outputFile)) {
        doc.write(out);
      }
    }
    return outputFile;
  }

  public File createDocumentWithChart(File imageFile) throws IOException, InvalidFormatException {
    // Simple fallback creation
    XWPFDocument doc = new XWPFDocument();
    File outputFile = File.createTempFile("OutputDoc_", ".docx");
    try {
      insertImageIntoDoc(doc, imageFile);
      try (FileOutputStream out = new FileOutputStream(outputFile)) {
        doc.write(out);
      }
    } finally {
      doc.close();
    }
    return outputFile;
  }

  public File appendChartToDocument(File existingDocFile, File imageFile) throws IOException, InvalidFormatException {
    File outputFile = File.createTempFile("UpdatedDoc_", ".docx");
    try (FileInputStream fis = new FileInputStream(existingDocFile);
      XWPFDocument doc = new XWPFDocument(fis)) {
      insertImageIntoDoc(doc, imageFile);
      try (FileOutputStream out = new FileOutputStream(outputFile)) {
        doc.write(out);
      }
    }
    return outputFile;
  }

  public File appendTableToDocument(File existingDocFile, List<String[]> data) throws IOException {
    File outputFile = File.createTempFile("UpdatedDoc_Table_", ".docx");
    try (FileInputStream fis = new FileInputStream(existingDocFile);
      XWPFDocument doc = new XWPFDocument(fis)) {
      XWPFTable table = doc.createTable();
      for (int i = 0; i < data.size(); i++) {
        String[] rowData = data.get(i);
        XWPFTableRow tableRow = (i == 0) ? table.getRow(0) : table.createRow();
        for (int col = 0; col < rowData.length; col++) {
          if (col == 0) {
            tableRow.getCell(0).setText(rowData[col]);
          } else {
            tableRow.addNewTableCell().setText(rowData[col]);
          }
        }
      }
      doc.createParagraph().createRun().addBreak();
      try (FileOutputStream out = new FileOutputStream(outputFile)) {
        doc.write(out);
      }
    }
    return outputFile;
  }

  private void insertImageIntoDoc(XWPFDocument doc, File imageFile) throws IOException, InvalidFormatException {
    XWPFParagraph p = doc.createParagraph();
    p.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun r = p.createRun();
    try (InputStream is = new FileInputStream(imageFile)) {
      r.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG, imageFile.getName(), Units.toEMU(500), Units.toEMU(300));
    }
  }
}