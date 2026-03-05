package com.appiancs.plugins.chartgenie.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.List;

import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
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

  private final HtmlRichTextRenderer htmlRenderer = new HtmlRichTextRenderer();
  private final TableGenerator tableGenerator = new TableGenerator();

  public File generateReport(File templateFile, ReportSettings settings, List<ReportSection> sections) throws Exception {
    try (FileInputStream fis = new FileInputStream(templateFile);
      XWPFDocument doc = new XWPFDocument(fis)) {

      int currentAvailableWidth = PAGE_CONTENT_WIDTH_TWIPS;

      if (settings != null) {
        currentAvailableWidth = applyPageSettings(doc, settings.getPageSize(), settings.getOrientation());
        applyHeaderFooter(doc, settings, settings.getHeaderColor(), settings.getFooterText());
      }

      if (doc.getBodyElements().size() > 0 && doc.getBodyElements().get(0).getElementType() == BodyElementType.PARAGRAPH) {
        XWPFParagraph firstPara = (XWPFParagraph) doc.getBodyElements().get(0);
        if (firstPara.getText().trim().isEmpty()) {
          firstPara.setSpacingAfter(0);
          firstPara.setSpacingBefore(0);
          CTPPr ppr = firstPara.getCTP().isSetPPr() ? firstPara.getCTP().getPPr() : firstPara.getCTP().addNewPPr();
          CTSpacing spacing = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
          spacing.setLineRule(STLineSpacingRule.EXACT);
          spacing.setLine(BigInteger.valueOf(20));
        }
      }

      if (sections != null) {
        processSections(doc, sections, null, currentAvailableWidth, false);
      }

      // ---------------- NEW CLEANUP BLOCK ----------------
      // 1. Vertically center text in ALL tables (Fixes Appendix)
      for (XWPFTable t : doc.getTables()) {
        for (XWPFTableRow r : t.getRows()) {
          for (XWPFTableCell c : r.getTableCells()) {
            c.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            // Force internal paragraph margins to 0 for true centering
            for (XWPFParagraph cp : c.getParagraphs()) {
              cp.setSpacingBefore(0);
              cp.setSpacingAfter(0);
            }
          }
        }
      }

      // 2. Eradicate trailing blank pages
      int lastIdx = doc.getBodyElements().size() - 1;
      while (lastIdx >= 0 && doc.getBodyElements().get(lastIdx).getElementType() == BodyElementType.PARAGRAPH) {
        XWPFParagraph lastPara = (XWPFParagraph) doc.getBodyElements().get(lastIdx);
        if (lastPara.getText().trim().isEmpty() && lastPara.getRuns().isEmpty() && !lastPara.isPageBreak()) {
          doc.removeBodyElement(lastIdx);
          lastIdx--;
        } else {
          break;
        }
      }
      // ----------------------------------------------------

      File outputFile = File.createTempFile("genie_report_", ".docx");
      try (FileOutputStream out = new FileOutputStream(outputFile)) {
        doc.write(out);
      }
      return outputFile;
    }
  }

  private void processSections(XWPFDocument doc, List<ReportSection> sections, XWPFTableCell cell, int availableWidthTwips,
    boolean isSidebar) throws Exception {
    for (ReportSection section : sections) {
      String type = section.getType() != null ? section.getType().toUpperCase().trim() : "TEXT";

      XWPFParagraph p = null;
      // Add PAGE_BREAK here so it doesn't create orphan paragraphs
      if (!type.equals("REPORT_TABLE") && !type.equals("RICH_TEXT") && !type.equals("SIDEBAR_LAYOUT") &&
        !type.equals("HEADING2") && !type.equals("STATUS_BADGE") && !type.equals("PAGE_BREAK")) {

        p = (cell != null) ? cell.addParagraph() : doc.createParagraph();
        if (isSidebar)
          p.setAlignment(ParagraphAlignment.CENTER);
      }

      switch (type) {
        case "HEADING":
          p.setStyle("Heading1");
          XWPFRun rH = p.createRun();
          rH.setText(section.getText());
          rH.setBold(true);
          rH.setFontSize(14);
          rH.setColor("00395D");
          break;

        case "HEADING2":
          XWPFTable h2Table;
          if (cell != null) {
            XWPFParagraph sep = cell.addParagraph();
            sep.setSpacingAfter(0);
            sep.setSpacingBefore(0);
            XmlCursor cursor = sep.getCTP().newCursor();
            h2Table = cell.insertNewTbl(cursor);
            cursor.dispose();
          } else {
            h2Table = doc.createTable(1, 1);
            doc.createParagraph();
          }

          XWPFTableRow h2Row = null;
          if (!h2Table.getRows().isEmpty())
            h2Row = h2Table.getRow(0);
          if (h2Row == null)
            h2Row = h2Table.createRow();
          while (h2Row.getTableCells().size() < 1)
            h2Row.createCell();

          CTTblPr h2TblPr = h2Table.getCTTbl().getTblPr() != null ? h2Table.getCTTbl().getTblPr() : h2Table.getCTTbl().addNewTblPr();
          h2TblPr.addNewTblW().setType(STTblWidth.DXA);
          h2TblPr.getTblW().setW(BigInteger.valueOf(5000));

          CTTblBorders h2Borders = h2TblPr.isSetTblBorders() ? h2TblPr.getTblBorders() : h2TblPr.addNewTblBorders();
          h2Borders.addNewBottom().setVal(STBorder.NONE);
          h2Borders.addNewLeft().setVal(STBorder.NONE);
          h2Borders.addNewRight().setVal(STBorder.NONE);
          h2Borders.addNewTop().setVal(STBorder.NONE);

          XWPFTableCell h2Cell = h2Row.getCell(0);
          h2Cell.setColor("00395D");

          CTTcPr h2TcPr = h2Cell.getCTTc().isSetTcPr() ? h2Cell.getCTTc().getTcPr() : h2Cell.getCTTc().addNewTcPr();
          CTTblWidth h2TcW = h2TcPr.isSetTcW() ? h2TcPr.getTcW() : h2TcPr.addNewTcW();
          h2TcW.setType(STTblWidth.DXA);
          h2TcW.setW(BigInteger.valueOf(5000));

          if (!h2TcPr.isSetNoWrap())
            h2TcPr.addNewNoWrap();

          CTTcMar h2Mar = h2TcPr.isSetTcMar() ? h2TcPr.getTcMar() : h2TcPr.addNewTcMar();
          h2Mar.addNewTop().setW(BigInteger.valueOf(100));
          h2Mar.addNewBottom().setW(BigInteger.valueOf(100));
          h2Mar.addNewLeft().setW(BigInteger.valueOf(150));
          h2Mar.addNewRight().setW(BigInteger.valueOf(150));

          XWPFParagraph h2P = h2Cell.getParagraphs().isEmpty() ? h2Cell.addParagraph() : h2Cell.getParagraphs().get(0);
          h2P.setSpacingBefore(0);
          h2P.setSpacingAfter(0);
          XWPFRun h2R = h2P.createRun();
          h2R.setText(section.getText());
          h2R.setBold(true);
          h2R.setColor("FFFFFF");
          h2R.setFontSize(11);
          break;

        case "STATUS_BADGE":
          boolean hasTitle = section.getTitle() != null && !section.getTitle().trim().isEmpty();
          int numCols = hasTitle ? 2 : 1;

          XWPFTable bTable;
          if (cell != null) {
            XWPFParagraph sep = cell.addParagraph();
            sep.setSpacingAfter(0);
            sep.setSpacingBefore(0);
            XmlCursor cursor = sep.getCTP().newCursor();
            bTable = cell.insertNewTbl(cursor);
            cursor.dispose();
          } else {
            bTable = doc.createTable(1, numCols);
            doc.createParagraph();
          }

          XWPFTableRow bRow = null;
          if (!bTable.getRows().isEmpty())
            bRow = bTable.getRow(0);
          if (bRow == null)
            bRow = bTable.createRow();
          while (bRow.getTableCells().size() < numCols)
            bRow.createCell();

          CTTblPr bTblPr = bTable.getCTTbl().getTblPr() != null ? bTable.getCTTbl().getTblPr() : bTable.getCTTbl().addNewTblPr();
          bTblPr.addNewJc().setVal(STJcTable.RIGHT);

          bTblPr.addNewTblW().setType(STTblWidth.DXA);
          bTblPr.getTblW().setW(BigInteger.valueOf(hasTitle ? 5000 : 3500));

          CTTblBorders bBorders = bTblPr.isSetTblBorders() ? bTblPr.getTblBorders() : bTblPr.addNewTblBorders();
          bBorders.addNewBottom().setVal(STBorder.NONE);
          bBorders.addNewLeft().setVal(STBorder.NONE);
          bBorders.addNewRight().setVal(STBorder.NONE);
          bBorders.addNewTop().setVal(STBorder.NONE);
          bBorders.addNewInsideH().setVal(STBorder.NONE);
          bBorders.addNewInsideV().setVal(STBorder.NONE);

          int currentCellIdx = 0;

          if (hasTitle) {
            XWPFTableCell lblCell = bRow.getCell(currentCellIdx++);
            CTTcPr lblTcPr = lblCell.getCTTc().isSetTcPr() ? lblCell.getCTTc().getTcPr() : lblCell.getCTTc().addNewTcPr();
            CTTblWidth lblTcW = lblTcPr.isSetTcW() ? lblTcPr.getTcW() : lblTcPr.addNewTcW();
            lblTcW.setW(BigInteger.valueOf(1500));

            if (!lblTcPr.isSetNoWrap())
              lblTcPr.addNewNoWrap();

            lblCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

            XWPFParagraph lblP = lblCell.getParagraphs().isEmpty() ? lblCell.addParagraph() : lblCell.getParagraphs().get(0);
            lblP.setAlignment(ParagraphAlignment.RIGHT);
            lblP.setSpacingBefore(0);
            lblP.setSpacingAfter(0);
            XWPFRun lblR = lblP.createRun();
            lblR.setText(section.getTitle() + "  ");
            lblR.setBold(true);
            lblR.setFontSize(10);
          }

          XWPFTableCell valCell = bRow.getCell(currentCellIdx);
          CTTcPr valTcPr = valCell.getCTTc().isSetTcPr() ? valCell.getCTTc().getTcPr() : valCell.getCTTc().addNewTcPr();
          CTTblWidth valTcW = valTcPr.isSetTcW() ? valTcPr.getTcW() : valTcPr.addNewTcW();
          valTcW.setW(BigInteger.valueOf(3500));

          // Make the badge the exact same height/margins as HEADING2
          CTTcMar badgeMar = valTcPr.isSetTcMar() ? valTcPr.getTcMar() : valTcPr.addNewTcMar();
          badgeMar.addNewTop().setW(BigInteger.valueOf(100));
          badgeMar.addNewBottom().setW(BigInteger.valueOf(100));
          badgeMar.addNewLeft().setW(BigInteger.valueOf(150));
          badgeMar.addNewRight().setW(BigInteger.valueOf(150));

          if (!valTcPr.isSetNoWrap())
            valTcPr.addNewNoWrap();

          String hexColor = (section.getAccentColor() != null) ? section.getAccentColor().replace("#", "") : "00395D";
          valCell.setColor(hexColor);
          valCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

          XWPFParagraph valP = valCell.getParagraphs().isEmpty() ? valCell.addParagraph() : valCell.getParagraphs().get(0);
          valP.setAlignment(ParagraphAlignment.CENTER);
          valP.setSpacingBefore(0);
          valP.setSpacingAfter(0);
          XWPFRun valR = valP.createRun();
          valR.setText(" " + section.getText().toUpperCase() + " ");
          valR.setBold(true);
          valR.setColor("FFFFFF");
          valR.setFontSize(10);
          break;

        case "REPORT_TABLE":
          if (section.getTableConfig() != null) {
            tableGenerator.createStyledTable(doc, cell, section.getTableConfig());
            if (cell != null) {
              XWPFParagraph sep = cell.addParagraph();
              sep.setSpacingAfter(0);
              sep.setSpacingBefore(0);
            }
          }
          break;

        case "RICH_TEXT":
          if (cell != null) {
            htmlRenderer.render(doc, cell, section.getText());
          } else {
            XWPFTable rtTable = doc.createTable(1, 1);
            CTTblPr rtTblPr = rtTable.getCTTbl().getTblPr() != null ? rtTable.getCTTbl().getTblPr() : rtTable.getCTTbl().addNewTblPr();

            CTTblBorders rtBorders = rtTblPr.isSetTblBorders() ? rtTblPr.getTblBorders() : rtTblPr.addNewTblBorders();
            rtBorders.addNewBottom().setVal(STBorder.NONE);
            rtBorders.addNewLeft().setVal(STBorder.NONE);
            rtBorders.addNewRight().setVal(STBorder.NONE);
            rtBorders.addNewTop().setVal(STBorder.NONE);
            rtBorders.addNewInsideH().setVal(STBorder.NONE);
            rtBorders.addNewInsideV().setVal(STBorder.NONE);

            CTTblWidth rtTblW = rtTblPr.addNewTblW();
            rtTblW.setType(STTblWidth.DXA);
            rtTblW.setW(BigInteger.valueOf(availableWidthTwips));

            XWPFTableCell rtCell = rtTable.getRow(0).getCell(0);

            CTTcPr rtTcPr = rtCell.getCTTc().isSetTcPr() ? rtCell.getCTTc().getTcPr() : rtCell.getCTTc().addNewTcPr();
            CTTcMar rtMar = rtTcPr.isSetTcMar() ? rtTcPr.getTcMar() : rtTcPr.addNewTcMar();
            rtMar.addNewTop().setW(BigInteger.valueOf(0));
            rtMar.addNewBottom().setW(BigInteger.valueOf(0));
            rtMar.addNewLeft().setW(BigInteger.valueOf(0));
            rtMar.addNewRight().setW(BigInteger.valueOf(0));

            if (!rtCell.getParagraphs().isEmpty()) {
              rtCell.removeParagraph(0);
            }

            htmlRenderer.render(doc, rtCell, section.getText());
            doc.createParagraph();
          }
          break;

        case "TEXT":
        case "PARAGRAPH":
          String content = section.getText();

          // FIX: Strict HTML detection to prevent <insert> placeholders from triggering the HTML renderer
          if (content != null && content.matches("(?i).*<(/?)(b|i|u|ul|ol|li|p|br|strong|em)(\\s+[^>]*)?>.*")) {
            if (cell != null) {
              htmlRenderer.render(doc, cell, content);
            } else {
              XWPFTable rtTable = doc.createTable(1, 1);
              CTTblPr rtTblPr = rtTable.getCTTbl().getTblPr() != null ? rtTable.getCTTbl().getTblPr() : rtTable.getCTTbl().addNewTblPr();

              CTTblBorders rtBorders = rtTblPr.isSetTblBorders() ? rtTblPr.getTblBorders() : rtTblPr.addNewTblBorders();
              rtBorders.addNewBottom().setVal(STBorder.NONE);
              rtBorders.addNewLeft().setVal(STBorder.NONE);
              rtBorders.addNewRight().setVal(STBorder.NONE);
              rtBorders.addNewTop().setVal(STBorder.NONE);
              rtBorders.addNewInsideH().setVal(STBorder.NONE);
              rtBorders.addNewInsideV().setVal(STBorder.NONE);

              CTTblWidth rtTblW = rtTblPr.addNewTblW();
              rtTblW.setType(STTblWidth.DXA);
              rtTblW.setW(BigInteger.valueOf(availableWidthTwips));

              XWPFTableCell rtCell = rtTable.getRow(0).getCell(0);

              CTTcPr rtTcPr = rtCell.getCTTc().isSetTcPr() ? rtCell.getCTTc().getTcPr() : rtCell.getCTTc().addNewTcPr();
              CTTcMar rtMar = rtTcPr.isSetTcMar() ? rtTcPr.getTcMar() : rtTcPr.addNewTcMar();
              rtMar.addNewTop().setW(BigInteger.valueOf(0));
              rtMar.addNewBottom().setW(BigInteger.valueOf(0));
              rtMar.addNewLeft().setW(BigInteger.valueOf(0));
              rtMar.addNewRight().setW(BigInteger.valueOf(0));

              if (!rtCell.getParagraphs().isEmpty()) {
                rtCell.removeParagraph(0);
              }

              htmlRenderer.render(doc, rtCell, content);
              doc.createParagraph();
            }

            // Clean up the empty default paragraph we created for TEXT
            if (cell == null && p != null) {
              int pos = doc.getPosOfParagraph(p);
              if (pos >= 0) {
                doc.removeBodyElement(pos);
              }
            }
          } else {
            // Standard plain text fallback
            XWPFRun rPara = p.createRun();
            rPara.setText(content);
          }
          break;

        case "SIDEBAR_LAYOUT":
          if (cell == null) {
            createSidebarLayout(doc, section, availableWidthTwips);
          }
          break;

        case "PAGE_BREAK":
          // Attach page break cleanly to prevent blank pages
          if (cell == null) {
            int lastIdx = doc.getBodyElements().size() - 1;
            if (lastIdx >= 0 && doc.getBodyElements().get(lastIdx).getElementType() == BodyElementType.PARAGRAPH) {
              ((XWPFParagraph) doc.getBodyElements().get(lastIdx)).setPageBreak(true);
            } else {
              doc.createParagraph().setPageBreak(true);
            }
          }
          break;

        case "CHART":
          if (section.getChartConfig() != null) {
            generateAndInsertChart(doc, cell, p, section.getChartConfig(), availableWidthTwips, isSidebar);
          }
          break;

        case "QR_CODE":
          p.setAlignment(ParagraphAlignment.CENTER);
          File qrFile = File.createTempFile("qr_code_", ".png");
          QRCodeWriter qrCodeWriter = new QRCodeWriter();
          BitMatrix bitMatrix = qrCodeWriter.encode(section.getText(), BarcodeFormat.QR_CODE, 200, 200);
          MatrixToImageWriter.writeToPath(bitMatrix, "PNG", qrFile.toPath());

          try (FileInputStream is = new FileInputStream(qrFile)) {
            XWPFRun rQR = p.createRun();
            rQR.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG, "qr.png", org.apache.poi.util.Units.toEMU(150),
              org.apache.poi.util.Units.toEMU(150));
          } finally {
            qrFile.delete();
          }
          break;
      }
    }
  }

  private void cleanCellParagraphs(XWPFTableCell cell) {
    for (XWPFParagraph p : cell.getParagraphs()) {
      if (p.getText().trim().isEmpty() && p.getRuns().isEmpty()) {
        p.setSpacingAfter(0);
        p.setSpacingBefore(0);
        CTPPr ppr = p.getCTP().isSetPPr() ? p.getCTP().getPPr() : p.getCTP().addNewPPr();
        CTSpacing spacing = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
        spacing.setLineRule(STLineSpacingRule.EXACT);
        spacing.setLine(BigInteger.valueOf(20));
      }
    }

    if (cell.getParagraphs().isEmpty() ||
      cell.getBodyElements().get(cell.getBodyElements().size() - 1).getElementType() != BodyElementType.PARAGRAPH) {
      cell.addParagraph();
    }
  }

  private void applyHeaderFooter(XWPFDocument doc, ReportSettings settings, String headerColor, String footerText) {
    try {
      CTSectPr sectPr = doc.getDocument().getBody().isSetSectPr() ? doc.getDocument().getBody().getSectPr()
        : doc.getDocument().getBody().addNewSectPr();
      XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(doc, sectPr);

      // --- 1. HEADER LOGIC (Restored to your exact working structure) ---
      if (policy.getDefaultHeader() == null) {
        policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
      }

      if (settings.getHeaderText() != null && !settings.getHeaderText().isEmpty()) {
        for (XWPFHeader h : doc.getHeaderList()) {
          XWPFParagraph p;
          if (h.getParagraphs().isEmpty()) {
            p = h.createParagraph();
          } else {
            // This cursor insertion preserves your dark blue background structure!
            XmlCursor cursor = h.getParagraphs().get(0).getCTP().newCursor();
            p = h.insertNewParagraph(cursor);
            cursor.dispose();
          }

          p.setAlignment(ParagraphAlignment.LEFT);
          p.setSpacingBefore(0);
          p.setSpacingAfter(0);

          XWPFRun r1 = p.createRun();
          r1.setText(settings.getHeaderText());
          r1.setBold(true);
          r1.setFontSize(16);
          r1.setColor(headerColor != null ? headerColor : "FFFFFF");

          if (settings.getSubheaderText() != null && !settings.getSubheaderText().isEmpty()) {
            r1.addBreak();
            XWPFRun r2 = p.createRun();
            r2.setText(settings.getSubheaderText());
            r2.setBold(false);
            r2.setFontSize(12);
            r2.setColor("FFFFFF");
          }
        }
      }

      // --- 2. FOOTER LOGIC (Dynamic combination of text, ref, and date) ---
      if (footerText != null && !footerText.isEmpty()) {
        StringBuilder fullFooterText = new StringBuilder();
        fullFooterText.append(footerText);

        if (settings.getAuditReference() != null && !settings.getAuditReference().isEmpty()) {
          if (fullFooterText.length() > 0)
            fullFooterText.append(" | ");
          fullFooterText.append("ID: ").append(settings.getAuditReference());
        }

        if (settings.getReportDate() != null && !settings.getReportDate().isEmpty()) {
          if (fullFooterText.length() > 0)
            fullFooterText.append(" | ");
          fullFooterText.append("Date: ").append(settings.getReportDate());
        }

        if (fullFooterText.length() > 0) {
          XWPFFooter footer = policy.getDefaultFooter();
          if (footer == null) {
            footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);
          }

          XWPFParagraph p;
          if (footer.getParagraphs().isEmpty()) {
            p = footer.createParagraph();
          } else {
            p = footer.getParagraphs().get(0);
            // It is safe to clear the footer runs because there is no colored background here
            for (int i = p.getRuns().size() - 1; i >= 0; i--) {
              p.removeRun(i);
            }
          }

          p.setAlignment(ParagraphAlignment.CENTER);
          p.setSpacingBefore(0);
          p.setSpacingAfter(0);

          XWPFRun r = p.createRun();
          r.setText(fullFooterText.toString());
          r.setFontSize(9);
          r.setColor("666666");
        }
      }
    } catch (Exception e) {
      System.err.println("Warning: Failed to apply header/footer: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private int applyPageSettings(XWPFDocument doc, String pageSize, String orientation) {
    CTBody body = doc.getDocument().getBody();
    CTSectPr section = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
    CTPageSz pgSz = section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz();

    long width = 11906;
    long height = 16838;
    if ("LETTER".equalsIgnoreCase(pageSize)) {
      width = 12240;
      height = 15840;
    }

    long activeWidth = width;
    if ("LANDSCAPE".equalsIgnoreCase(orientation)) {
      pgSz.setOrient(STPageOrientation.LANDSCAPE);
      pgSz.setW(BigInteger.valueOf(height));
      pgSz.setH(BigInteger.valueOf(width));
      activeWidth = height;
    } else {
      pgSz.setOrient(STPageOrientation.PORTRAIT);
      pgSz.setW(BigInteger.valueOf(width));
      pgSz.setH(BigInteger.valueOf(height));
    }

    CTPageMar pageMar = section.isSetPgMar() ? section.getPgMar() : section.addNewPgMar();
    pageMar.setTop(BigInteger.valueOf(1440));
    pageMar.setBottom(BigInteger.valueOf(1440));
    pageMar.setLeft(BigInteger.valueOf(1440));
    pageMar.setRight(BigInteger.valueOf(1440));
    // Move Header title up
    pageMar.setHeader(BigInteger.valueOf(100));
    pageMar.setFooter(BigInteger.valueOf(340));

    return (int) (activeWidth - 2880);
  }

  private void createSidebarLayout(XWPFDocument doc, ReportSection section, int availableWidthTwips) throws Exception {
    XWPFTable table;
    if (doc.getTables().isEmpty() && !doc.getParagraphs().isEmpty()) {
      XmlCursor cursor = doc.getParagraphs().get(0).getCTP().newCursor();
      table = doc.insertNewTbl(cursor);
      cursor.dispose();
    } else {
      table = doc.createTable();
    }

    XWPFTableRow row = table.getRow(0) == null ? table.createRow() : table.getRow(0);
    if (row.getCell(0) == null)
      row.createCell();
    if (row.getCell(1) == null)
      row.createCell();

    CTTblPr tblPr = table.getCTTbl().getTblPr() != null ? table.getCTTbl().getTblPr() : table.getCTTbl().addNewTblPr();
    CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
    borders.addNewBottom().setVal(STBorder.NONE);
    borders.addNewLeft().setVal(STBorder.NONE);
    borders.addNewRight().setVal(STBorder.NONE);
    borders.addNewTop().setVal(STBorder.NONE);
    borders.addNewInsideH().setVal(STBorder.NONE);
    borders.addNewInsideV().setVal(STBorder.NONE);

    CTTblWidth tblInd = tblPr.isSetTblInd() ? tblPr.getTblInd() : tblPr.addNewTblInd();
    tblInd.setType(STTblWidth.DXA);
    tblInd.setW(java.math.BigInteger.valueOf(-283));

    double leftRatio = section.getLeftColumnRatio() != null ? section.getLeftColumnRatio() : 0.65;

    int targetWidthTwips = availableWidthTwips + 567;
    int leftWidth = (int) (targetWidthTwips * leftRatio);
    int rightWidth = targetWidthTwips - leftWidth;

    tblPr.addNewTblLayout().setType(STTblLayoutType.FIXED);
    CTTblWidth tblW = tblPr.addNewTblW();
    tblW.setType(STTblWidth.DXA);
    tblW.setW(java.math.BigInteger.valueOf(targetWidthTwips));

    XWPFTableCell leftCell = row.getCell(0);
    CTTcPr leftTcPr = leftCell.getCTTc().isSetTcPr() ? leftCell.getCTTc().getTcPr() : leftCell.getCTTc().addNewTcPr();
    CTTblWidth leftTcW = leftTcPr.isSetTcW() ? leftTcPr.getTcW() : leftTcPr.addNewTcW();
    leftTcW.setType(STTblWidth.DXA);
    leftTcW.setW(java.math.BigInteger.valueOf(leftWidth));

    if (!leftCell.getParagraphs().isEmpty())
      leftCell.removeParagraph(0);
    if (section.getMainContent() != null)
      processSections(doc, section.getMainContent(), leftCell, leftWidth, false);
    cleanCellParagraphs(leftCell);

    XWPFTableCell rightCell = row.getCell(1);
    CTTcPr rightTcPr = rightCell.getCTTc().isSetTcPr() ? rightCell.getCTTc().getTcPr() : rightCell.getCTTc().addNewTcPr();
    CTTblWidth rightTcW = rightTcPr.isSetTcW() ? rightTcPr.getTcW() : rightTcPr.addNewTcW();
    rightTcW.setType(STTblWidth.DXA);
    rightTcW.setW(java.math.BigInteger.valueOf(rightWidth));

    if (!rightCell.getParagraphs().isEmpty())
      rightCell.removeParagraph(0);
    if (section.getSidebarContent() != null)
      processSections(doc, section.getSidebarContent(), rightCell, rightWidth, true);
    cleanCellParagraphs(rightCell);

    doc.createParagraph();
  }

  private void generateAndInsertChart(XWPFDocument doc, XWPFTableCell cell, XWPFParagraph p, ChartConfiguration config, int width,
    boolean isSidebar) throws Exception {
    if (config.getTitle() != null && !config.getTitle().isEmpty()) {
      XWPFRun rTitle = p.createRun();
      rTitle.setText(config.getTitle());
      rTitle.setBold(true);
      rTitle.setFontSize(11);
      p = (cell != null) ? cell.addParagraph() : doc.createParagraph();
      if (isSidebar)
        p.setAlignment(ParagraphAlignment.CENTER);
    }

    ChartGenerationService chartGen = new ChartGenerationService();
    File chartImage = chartGen.generateChartImage(config);

    try (FileInputStream is = new FileInputStream(chartImage)) {
      XWPFRun r = p.createRun();

      double aspectRatio = (double) config.getHeight() / config.getWidth();
      int finalWidthEMU;
      int finalHeightEMU;

      if (isSidebar) {
        finalWidthEMU = org.apache.poi.util.Units.toEMU(115);
        finalHeightEMU = (int) (finalWidthEMU * aspectRatio);
      } else {
        double maxDisplayWidthEMU = (width * 635.0) * 0.95;
        finalWidthEMU = (int) maxDisplayWidthEMU;
        finalHeightEMU = (int) (finalWidthEMU * aspectRatio);
      }

      r.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG, chartImage.getName(), finalWidthEMU, finalHeightEMU);
    } finally {
      if (chartImage.exists())
        chartImage.delete();
    }
  }
}