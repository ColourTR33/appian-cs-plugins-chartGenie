package com.appiancs.plugins.chartgenie.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;
import com.appiancs.plugins.chartgenie.dto.structure.ReportSettings;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class WordDocumentService {
  private static final int PAGE_CONTENT_WIDTH_TWIPS = 11054;
  private static final int GEN_RES_WIDTH = 600;
  private static final int GEN_RES_HEIGHT = 400;

  public File generateReport(File templateFile, ReportSettings settings, List<ReportSection> sections) throws Exception {
    try (FileInputStream fis = new FileInputStream(templateFile);
      XWPFDocument doc = new XWPFDocument(fis)) {

      if (settings != null) {
        applyHeaderFooter(doc, settings.getHeaderText(), settings.getHeaderFont(), settings.getHeaderColor(), settings.getFooterText());
      }

      if (sections != null) {
        processSections(doc, sections, null, PAGE_CONTENT_WIDTH_TWIPS, false);
      }

      File outputFile = File.createTempFile("genie_report_", ".docx");
      try (FileOutputStream out = new FileOutputStream(outputFile)) {
        doc.write(out);
      }
      return outputFile;
    }
  }

  private void applyHeaderFooter(XWPFDocument doc, String headerText, String headerFont, String headerColor, String footerText) {
    try {
      XWPFHeaderFooterPolicy policy = doc.getHeaderFooterPolicy();
      if (policy == null)
        policy = doc.createHeaderFooterPolicy();

      if (headerText != null && !headerText.isEmpty()) {
        XWPFHeader header = policy.getHeader(XWPFHeaderFooterPolicy.DEFAULT);
        if (header == null)
          header = policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);

        XWPFParagraph p = header.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun r = p.createRun();
        r.setText(headerText);
        r.setBold(true);
        r.setFontFamily(headerFont);
        r.setFontSize(16);
        r.setColor(headerColor);
      }

      if (footerText != null && !footerText.isEmpty()) {
        XWPFFooter footer = policy.getFooter(XWPFHeaderFooterPolicy.DEFAULT);
        if (footer == null)
          footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);

        XWPFParagraph p = footer.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setText(footerText);
        r.setFontSize(9);
      }
    } catch (Exception e) {
      System.err.println("Warning: Failed to apply header/footer: " + e.getMessage());
    }
  }

  private void processSections(XWPFDocument doc, List<ReportSection> sections, XWPFTableCell cell, int availableWidthTwips,
    boolean isSidebar) throws Exception {
    for (ReportSection section : sections) {
      try {
        renderSection(doc, section, cell, availableWidthTwips, isSidebar);
      } catch (Exception e) {
        XWPFParagraph p = (cell != null) ? cell.addParagraph() : doc.createParagraph();
        p.createRun().setText("[ERROR: " + e.getMessage() + "]");
      }
    }
  }

  private void renderSection(XWPFDocument doc, ReportSection section, XWPFTableCell cell, int availableWidthTwips, boolean isSidebar)
    throws Exception {
    String type = section.getType() != null ? section.getType().toUpperCase().trim() : "TEXT";
    XWPFParagraph p = (cell != null) ? cell.addParagraph() : doc.createParagraph();

    if (isSidebar) {
      p.setAlignment(ParagraphAlignment.CENTER);
    }

    switch (type) {
      case "HEADING":
        p.setStyle("Heading1");
        XWPFRun rH = p.createRun();
        rH.setText(section.getText());
        rH.setBold(true);
        rH.setFontSize(14);
        rH.setColor("2F5496");
        break;

      case "TEXT":
      case "PARAGRAPH":
        XWPFRun rPara = p.createRun();
        rPara.setText(section.getText());
        if (isSidebar) {
          rPara.setFontSize(10);
          rPara.setItalic(true);
        }
        break;

      case "SIDEBAR_LAYOUT":
        if (cell == null) {
          int pos = doc.getPosOfParagraph(p);
          doc.removeBodyElement(pos);
          createSidebarLayout(doc, section);
        }
        break;

      case "PAGE_BREAK":
        if (cell == null)
          p.setPageBreak(true);
        break;

      case "STATUS_BADGE":
        String badgeText = section.getText();

        // Uses getAccentColor (ensure ReportSection.java is updated!)
        String hexColor = (section.getAccentColor() != null && !section.getAccentColor().isEmpty())
          ? section.getAccentColor().replace("#", "")
          : "666666";

        XWPFRun rBadge = p.createRun();
        rBadge.setText(" " + badgeText.toUpperCase() + " ");
        rBadge.setBold(true);
        rBadge.setColor("FFFFFF");
        rBadge.setFontSize(10);

        setTextHighlight(rBadge, hexColor);
        break;

      case "CHART":
        if (section.getChartConfig() != null) {
          ChartConfiguration config = section.getChartConfig();

          if (config.getTitle() != null && !config.getTitle().isEmpty()) {
            if (isSidebar)
              p.setAlignment(ParagraphAlignment.CENTER);

            XWPFRun rTitle = p.createRun();
            rTitle.setText(config.getTitle());
            rTitle.setBold(true);
            rTitle.setFontSize(10);
            rTitle.setColor("444444");

            p = (cell != null) ? cell.addParagraph() : doc.createParagraph();
            if (isSidebar)
              p.setAlignment(ParagraphAlignment.CENTER);
          }

          generateAndInsertChart(doc, p, config, availableWidthTwips, isSidebar);

          // Uses getMetrics (ensure ChartConfiguration.java is updated!)
          if (config.getMetrics() != null && !config.getMetrics().isEmpty()) {
            XWPFParagraph pMetrics = (cell != null) ? cell.addParagraph() : doc.createParagraph();
            pMetrics.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun rMetrics = pMetrics.createRun();
            rMetrics.setText(config.getMetrics());
            rMetrics.setFontSize(9);
            rMetrics.setItalic(true);
            rMetrics.setColor("666666");
          }
        }
        break;

      case "QR_CODE":
        File qrFile = File.createTempFile("qr_code_", ".png");
        generateQRCodeImage(section.getText(), 150, 150, qrFile);

        try (FileInputStream is = new FileInputStream(qrFile)) {
          XWPFRun rQR = p.createRun();
          rQR.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG, "qr.png",
            org.apache.poi.util.Units.toEMU(100),
            org.apache.poi.util.Units.toEMU(100));

          // Fixed: Add break to the RUN, not the paragraph
          rQR.addBreak();

          XWPFRun rLink = p.createRun();
          rLink.setText(section.getText());
          rLink.setFontSize(8);
          rLink.setColor("666666");
        } finally {
          qrFile.delete();
        }
        break;

      default:
        p.createRun().setText(section.getText());
    }
  }

  private void createSidebarLayout(XWPFDocument doc, ReportSection section) throws Exception {
    XWPFTable table = doc.createTable(1, 2);
    removeTableBorders(table);

    int totalWidth = PAGE_CONTENT_WIDTH_TWIPS;
    int leftWidth = (int) (totalWidth * 0.68);
    int rightWidth = (int) (totalWidth * 0.32);

    applyFixedTableLayout(table, leftWidth, rightWidth);
    setRowMinHeight(table.getRow(0), 1000);

    XWPFTableCell leftCell = table.getRow(0).getCell(0);
    setCellWidth(leftCell, leftWidth);
    setCellTransparent(leftCell);
    if (leftCell.getParagraphs().size() > 0)
      leftCell.removeParagraph(0);

    if (section.getMainContent() != null) {
      processSections(doc, section.getMainContent(), leftCell, leftWidth, false);
    }

    XWPFTableCell rightCell = table.getRow(0).getCell(1);
    setCellWidth(rightCell, rightWidth);
    setCellTransparent(rightCell);
    if (rightCell.getParagraphs().size() > 0)
      rightCell.removeParagraph(0);

    if (section.getSidebarContent() != null) {
      processSections(doc, section.getSidebarContent(), rightCell, rightWidth, true);
    }

    doc.createParagraph();
  }

  private void generateAndInsertChart(XWPFDocument doc, XWPFParagraph p, ChartConfiguration config, int availableWidthTwips,
    boolean isSidebar) throws Exception {
    boolean isCircular = "PIE".equalsIgnoreCase(config.getChartType()) || "DONUT".equalsIgnoreCase(config.getChartType());
    int targetGenWidth = GEN_RES_WIDTH;
    int targetGenHeight = isCircular ? GEN_RES_WIDTH : GEN_RES_HEIGHT;

    String originalTitle = config.getTitle();
    config.setTitle("");

    Integer userW = config.getWidth();
    Integer userH = config.getHeight();
    config.setWidth(targetGenWidth);
    config.setHeight(targetGenHeight);

    ChartGenerationService chartGen = new ChartGenerationService();
    File chartImage = chartGen.generateChartImage(config);

    config.setWidth(userW);
    config.setHeight(userH);
    config.setTitle(originalTitle);

    try (FileInputStream is = new FileInputStream(chartImage)) {
      XWPFRun r = p.createRun();

      double maxDisplayWidthEMU = (availableWidthTwips * 635.0) * 0.95;
      double aspectRatio = (double) targetGenHeight / targetGenWidth;

      int finalWidthEMU = (int) maxDisplayWidthEMU;
      int finalHeightEMU = (int) (finalWidthEMU * aspectRatio);

      r.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG, chartImage.getName(), finalWidthEMU, finalHeightEMU);
    } finally {
      chartImage.delete();
    }
  }

  private void setCellTransparent(XWPFTableCell cell) {
    CTTcPr pr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    CTShd shd = pr.isSetShd() ? pr.getShd() : pr.addNewShd();
    shd.setVal(STShd.CLEAR);
    shd.setColor("auto");
    shd.setFill("auto");
  }

  private void setTextHighlight(XWPFRun run, String hexColor) {
    if (run.getCTR() == null)
      return;

    CTRPr rpr = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();

    // FIX: Directly ADD shading. Since this is a new run, we don't need to 'get' it first.
    // This avoids the "cannot find symbol: getShd()" error.
    CTShd shd = rpr.addNewShd();

    shd.setVal(STShd.CLEAR);
    shd.setColor("auto");
    shd.setFill(hexColor);
  }

  private void generateQRCodeImage(String text, int width, int height, File outputFile) throws Exception {
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
    Path path = outputFile.toPath();
    MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
  }

  private void removeTableBorders(XWPFTable table) {
    CTTblPr tblPr = table.getCTTbl().getTblPr();
    if (tblPr == null)
      tblPr = table.getCTTbl().addNewTblPr();
    CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
    borders.addNewBottom().setVal(STBorder.NONE);
    borders.addNewLeft().setVal(STBorder.NONE);
    borders.addNewRight().setVal(STBorder.NONE);
    borders.addNewTop().setVal(STBorder.NONE);
    borders.addNewInsideH().setVal(STBorder.NONE);
    borders.addNewInsideV().setVal(STBorder.NONE);
  }

  private void applyFixedTableLayout(XWPFTable table, int leftTwips, int rightTwips) {
    CTTbl ctTbl = table.getCTTbl();
    CTTblPr pr = ctTbl.getTblPr() != null ? ctTbl.getTblPr() : ctTbl.addNewTblPr();
    CTTblLayoutType type = pr.isSetTblLayout() ? pr.getTblLayout() : pr.addNewTblLayout();
    type.setType(STTblLayoutType.FIXED);
    CTTblWidth tblW = pr.isSetTblW() ? pr.getTblW() : pr.addNewTblW();
    tblW.setType(STTblWidth.DXA);
    tblW.setW(BigInteger.valueOf(leftTwips + rightTwips));
    CTTblGrid grid = ctTbl.getTblGrid();
    if (grid == null)
      grid = ctTbl.addNewTblGrid();
    if (grid.sizeOfGridColArray() == 0) {
      grid.addNewGridCol().setW(BigInteger.valueOf(leftTwips));
      grid.addNewGridCol().setW(BigInteger.valueOf(rightTwips));
    }
  }

  private void setRowMinHeight(XWPFTableRow row, int heightTwips) {
    CTTrPr trPr = row.getCtRow().isSetTrPr() ? row.getCtRow().getTrPr() : row.getCtRow().addNewTrPr();
    CTHeight ht = trPr.sizeOfTrHeightArray() > 0 ? trPr.getTrHeightArray(0) : trPr.addNewTrHeight();
    ht.setVal(BigInteger.valueOf(heightTwips));
  }

  private void setCellWidth(XWPFTableCell cell, int widthTwips) {
    CTTcPr pr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    CTTblWidth width = pr.isSetTcW() ? pr.getTcW() : pr.addNewTcW();
    width.setType(STTblWidth.DXA);
    width.setW(BigInteger.valueOf(widthTwips));
  }
}