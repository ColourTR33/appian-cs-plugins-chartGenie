package com.appiancs.plugins.chartgenie.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.util.List;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJcTable;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.dto.ChartConfiguration;
import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;
import com.appiancs.plugins.chartgenie.dto.structure.ReportSettings;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class WordDocumentService {

  // --- LOGGER ---
  private static final Logger logger = LoggerFactory.getLogger(WordDocumentService.class);

  // --- DESIGN CONSTANTS ---
  private static final String COLOR_PRIMARY = "00395D";
  private static final String COLOR_WHITE = "FFFFFF";
  private static final String COLOR_GREY_TEXT = "666666";

  private static final int FONT_SIZE_H1 = 14;
  private static final int FONT_SIZE_H2 = 11;
  private static final int FONT_SIZE_BADGE = 10;
  private static final int FONT_SIZE_HEADER = 16;
  private static final int FONT_SIZE_SUBHEADER = 12;
  private static final int FONT_SIZE_FOOTER = 9;

  // --- LAYOUT CONSTANTS ---
  private static final int PAGE_CONTENT_WIDTH_TWIPS = 11054;
  private static final BigInteger MARGIN_STANDARD = BigInteger.valueOf(1440);
  private static final BigInteger MARGIN_HEADER_Y = BigInteger.valueOf(100);
  private static final BigInteger MARGIN_FOOTER_Y = BigInteger.valueOf(340);
  private static final BigInteger EXACT_LINE_SPACING = BigInteger.valueOf(20);

  private static final BigInteger BADGE_TOTAL_WIDTH = BigInteger.valueOf(5000);
  private static final BigInteger BADGE_LABEL_WIDTH = BigInteger.valueOf(1500);
  private static final BigInteger BADGE_VALUE_WIDTH = BigInteger.valueOf(3500);
  private static final BigInteger BADGE_MARGIN_Y = BigInteger.valueOf(100);
  private static final BigInteger BADGE_MARGIN_X = BigInteger.valueOf(150);

  private static final long A4_WIDTH_TWIPS = 11906;
  private static final long A4_HEIGHT_TWIPS = 16838;
  private static final long LETTER_WIDTH_TWIPS = 12240;
  private static final long LETTER_HEIGHT_TWIPS = 15840;

  private static final int QR_DIMENSION_PX = 200;
  private static final int QR_DISPLAY_SIZE_PT = 150;
  private static final int CHART_SIDEBAR_WIDTH_PT = 115;
  private static final double CHART_WIDTH_MULTIPLIER = 635.0;
  private static final double CHART_MAX_WIDTH_PCT = 0.95;

  private static final java.util.regex.Pattern HTML_TAG_PATTERN = java.util.regex.Pattern
    .compile("(?i).*<(/?)(b|i|u|ul|ol|li|p|br|strong|em)(\\s+[^>]*)?>.*");

  private final HtmlRichTextRenderer htmlRenderer = new HtmlRichTextRenderer();
  private final TableGenerator tableGenerator = new TableGenerator();
  private final TemplateVariableSubstitutor substitutor = new TemplateVariableSubstitutor();

  // Optional — set by callers that need IMAGE section support
  private com.appiancorp.suiteapi.content.ContentService contentService;

  public void setContentService(com.appiancorp.suiteapi.content.ContentService contentService) {
    this.contentService = contentService;
  }

  public byte[] generateReport(
    File templateFile, ReportSettings settings, List<ReportSection> sections,
    java.util.Map<String, String> variables) throws Exception {
    logger.debug("Starting report generation for file: {}", templateFile.getName());

    // CWE-22/23: Resolve canonical path to block any traversal sequences
    java.nio.file.Path safePath = templateFile.toPath().normalize().toRealPath();

    try (java.io.InputStream fis = java.nio.file.Files.newInputStream(safePath);
      XWPFDocument doc = new XWPFDocument(fis)) {

      int currentAvailableWidth = PAGE_CONTENT_WIDTH_TWIPS;

      if (settings != null) {
        currentAvailableWidth = applyPageSettings(doc, settings.getPageSize(), settings.getOrientation());
        applyHeaderFooter(doc, settings, settings.getHeaderColor(), settings.getFooterText());
      }

      // Apply template variable substitution before sections are rendered
      substitutor.substitute(doc, variables);

      if (doc.getBodyElements().size() > 0 && doc.getBodyElements().get(0).getElementType() == BodyElementType.PARAGRAPH) {
        XWPFParagraph firstPara = (XWPFParagraph) doc.getBodyElements().get(0);
        if (firstPara.getText().trim().isEmpty()) {
          firstPara.setSpacingAfter(0);
          firstPara.setSpacingBefore(0);
          CTPPr ppr = firstPara.getCTP().isSetPPr() ? firstPara.getCTP().getPPr() : firstPara.getCTP().addNewPPr();
          CTSpacing spacing = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
          spacing.setLineRule(STLineSpacingRule.EXACT);
          spacing.setLine(EXACT_LINE_SPACING);
        }
      }

      if (sections != null) {
        processSections(doc, sections, null, currentAvailableWidth, false);
      }

      for (XWPFTable t : doc.getTables()) {
        for (XWPFTableRow r : t.getRows()) {
          for (XWPFTableCell c : r.getTableCells()) {
            c.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            for (XWPFParagraph cp : c.getParagraphs()) {
              cp.setSpacingBefore(0);
              cp.setSpacingAfter(0);
            }
          }
        }
      }

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

      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        doc.write(baos);
        logger.info("Report generation successful.");
        return baos.toByteArray();
      }
    } catch (Exception e) {
      logger.error("Error generating Word report: {}", e.getMessage(), e);
      throw e;
    }
  }

  private void processSections(XWPFDocument doc, List<ReportSection> sections, XWPFTableCell cell, int availableWidthTwips,
    boolean isSidebar) throws Exception {
    for (ReportSection section : sections) {
      String type = section.getType() != null ? section.getType().toUpperCase(java.util.Locale.ROOT).trim() : "TEXT";
      processSection(doc, section, type, cell, availableWidthTwips, isSidebar);
    }
  }

  private void processSection(XWPFDocument doc, ReportSection section, String type, XWPFTableCell cell,
    int availableWidthTwips, boolean isSidebar) throws Exception {

    XWPFParagraph p = createParagraphIfNeeded(doc, cell, type, isSidebar);

    switch (type) {
      case "HEADING":
        processHeading(section, p);
        break;
      case "HEADING2":
        processHeading2(doc, cell, section);
        break;
      case "STATUS_BADGE":
        processStatusBadge(doc, cell, section);
        break;
      case "REPORT_TABLE":
        processReportTable(doc, cell, section);
        break;
      case "RICH_TEXT":
      case "TEXT":
      case "PARAGRAPH":
        processTextContent(doc, cell, section, type, availableWidthTwips, p);
        break;
      case "SIDEBAR_LAYOUT":
        if (cell == null) {
          createSidebarLayout(doc, section, availableWidthTwips);
        }
        break;
      case "PAGE_BREAK":
        processPageBreak(doc, cell);
        break;
      case "CHART":
        if (section.getChartConfig() != null) {
          generateAndInsertChart(doc, cell, p, section.getChartConfig(), availableWidthTwips, isSidebar);
        }
        break;
      case "QR_CODE":
        processQRCode(section, p);
        break;
      case "IMAGE":
        processImage(doc, cell, section);
        break;
      case "DIVIDER":
        processDivider(doc, cell);
        break;
      case "SPACER":
        processSpacer(doc, cell, section);
        break;
      default:
        // Handle unknown types as text
        if (p != null) {
          XWPFRun run = p.createRun();
          run.setText(section.getText() != null ? section.getText() : "");
        }
        break;
    }
  }

  private XWPFParagraph createParagraphIfNeeded(XWPFDocument doc, XWPFTableCell cell, String type, boolean isSidebar) {
    // Types that don't need a paragraph
    if ("REPORT_TABLE".equals(type) || "RICH_TEXT".equals(type) || "SIDEBAR_LAYOUT".equals(type) || "HEADING2".equals(type) ||
      "STATUS_BADGE".equals(type) || "PAGE_BREAK".equals(type) || "IMAGE".equals(type) || "DIVIDER".equals(type) || "SPACER".equals(type)) {
      return null;
    }

    XWPFParagraph p = (cell != null) ? cell.addParagraph() : doc.createParagraph();
    if (isSidebar) {
      p.setAlignment(ParagraphAlignment.CENTER);
    }
    return p;
  }

  private void processHeading(ReportSection section, XWPFParagraph p) {
    if (p == null)
      return;

    p.setStyle("Heading1");
    XWPFRun run = p.createRun();
    run.setText(section.getText());
    run.setBold(true);
    run.setFontSize(FONT_SIZE_H1);
    run.setColor(COLOR_PRIMARY);
  }

  private void processHeading2(XWPFDocument doc, XWPFTableCell cell, ReportSection section) {
    XWPFTable h2Table = createInlineTable(doc, cell, 1, 1);
    XWPFTableRow h2Row = getOrCreateFirstRow(h2Table, 1);

    CTTblPr h2TblPr = getOrCreateTblPr(h2Table);
    h2TblPr.addNewTblW().setType(STTblWidth.DXA);
    h2TblPr.getTblW().setW(BADGE_TOTAL_WIDTH);
    removeTableBorders(h2TblPr);

    XWPFTableCell h2Cell = h2Row.getCell(0);
    h2Cell.setColor(COLOR_PRIMARY);

    CTTcPr h2TcPr = getOrCreateTcPr(h2Cell);
    setCellWidth(h2TcPr, BADGE_TOTAL_WIDTH);
    if (!h2TcPr.isSetNoWrap())
      h2TcPr.addNewNoWrap();
    setCellMargins(h2TcPr, BADGE_MARGIN_Y, BADGE_MARGIN_X);

    XWPFParagraph h2P = h2Cell.getParagraphs().isEmpty() ? h2Cell.addParagraph() : h2Cell.getParagraphs().get(0);
    h2P.setSpacingBefore(0);
    h2P.setSpacingAfter(0);
    XWPFRun h2R = h2P.createRun();
    h2R.setText(section.getText());
    h2R.setBold(true);
    h2R.setColor(COLOR_WHITE);
    h2R.setFontSize(FONT_SIZE_H2);
  }

  private void processStatusBadge(XWPFDocument doc, XWPFTableCell cell, ReportSection section) {
    boolean hasTitle = section.getTitle() != null && !section.getTitle().trim().isEmpty();
    int numCols = hasTitle ? 2 : 1;

    XWPFTable bTable = createInlineTable(doc, cell, 1, numCols);
    XWPFTableRow bRow = getOrCreateFirstRow(bTable, numCols);

    // SECURITY FIX: Add null check to prevent NullPointerException
    if (bRow == null) {
      logger.warn("Failed to create table row for status badge");
      return;
    }

    CTTblPr bTblPr = getOrCreateTblPr(bTable);
    bTblPr.addNewJc().setVal(STJcTable.RIGHT);
    bTblPr.addNewTblW().setType(STTblWidth.DXA);
    bTblPr.getTblW().setW(hasTitle ? BADGE_TOTAL_WIDTH : BADGE_VALUE_WIDTH);
    removeTableBorders(bTblPr);

    int currentCellIdx = 0;

    if (hasTitle) {
      XWPFTableCell lblCell = bRow.getCell(currentCellIdx++);
      if (lblCell == null) {
        logger.warn("Failed to get label cell for status badge");
        return;
      }

      CTTcPr lblTcPr = getOrCreateTcPr(lblCell);
      setCellWidth(lblTcPr, BADGE_LABEL_WIDTH);
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
      lblR.setFontSize(FONT_SIZE_BADGE);
    }

    XWPFTableCell valCell = bRow.getCell(currentCellIdx);
    if (valCell == null) {
      logger.warn("Failed to get value cell for status badge");
      return;
    }

    CTTcPr valTcPr = getOrCreateTcPr(valCell);
    setCellWidth(valTcPr, BADGE_VALUE_WIDTH);
    setCellMargins(valTcPr, BADGE_MARGIN_Y, BADGE_MARGIN_X);
    if (!valTcPr.isSetNoWrap())
      valTcPr.addNewNoWrap();

    String hexColor = (section.getAccentColor() != null) ? section.getAccentColor().replace("#", "") : COLOR_PRIMARY;
    valCell.setColor(hexColor);
    valCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

    XWPFParagraph valP = valCell.getParagraphs().isEmpty() ? valCell.addParagraph() : valCell.getParagraphs().get(0);
    valP.setAlignment(ParagraphAlignment.CENTER);
    valP.setSpacingBefore(0);
    valP.setSpacingAfter(0);
    XWPFRun valR = valP.createRun();
    valR.setText(" " + section.getText().toUpperCase(java.util.Locale.ROOT) + " ");
    valR.setBold(true);
    valR.setColor(COLOR_WHITE);
    valR.setFontSize(FONT_SIZE_BADGE);
  }

  private void processReportTable(XWPFDocument doc, XWPFTableCell cell, ReportSection section) {
    if (section.getTableConfig() != null) {
      tableGenerator.createStyledTable(doc, cell, section.getTableConfig());
      if (cell != null) {
        XWPFParagraph sep = cell.addParagraph();
        sep.setSpacingAfter(0);
        sep.setSpacingBefore(0);
      }
    }
  }

  private void processTextContent(XWPFDocument doc, XWPFTableCell cell, ReportSection section, String type,
    int availableWidthTwips, XWPFParagraph p) {
    String content = section.getText();
    boolean isHtml = content != null && HTML_TAG_PATTERN.matcher(content).matches();

    if (isHtml || "RICH_TEXT".equals(type)) {
      processRichTextContent(doc, cell, content, availableWidthTwips, p);
    } else {
      if (p != null) {
        XWPFRun run = p.createRun();
        run.setText(content);
      }
    }
  }

  private void processRichTextContent(XWPFDocument doc, XWPFTableCell cell, String content, int availableWidthTwips, XWPFParagraph p) {
    if (cell != null) {
      htmlRenderer.render(doc, cell, content);
    } else {
      XWPFTable rtTable = doc.createTable(1, 1);
      CTTblPr rtTblPr = getOrCreateTblPr(rtTable);
      removeTableBorders(rtTblPr);

      CTTblWidth rtTblW = rtTblPr.addNewTblW();
      rtTblW.setType(STTblWidth.DXA);
      rtTblW.setW(BigInteger.valueOf(availableWidthTwips));

      XWPFTableCell rtCell = rtTable.getRow(0).getCell(0);
      CTTcPr rtTcPr = getOrCreateTcPr(rtCell);
      setCellMargins(rtTcPr, BigInteger.ZERO, BigInteger.ZERO);

      if (!rtCell.getParagraphs().isEmpty()) {
        rtCell.removeParagraph(0);
      }

      htmlRenderer.render(doc, rtCell, content);
      doc.createParagraph();
    }

    if (cell == null && p != null) {
      int pos = doc.getPosOfParagraph(p);
      if (pos >= 0)
        doc.removeBodyElement(pos);
    }
  }

  private void processPageBreak(XWPFDocument doc, XWPFTableCell cell) {
    if (cell == null) {
      int lastIdx = doc.getBodyElements().size() - 1;
      if (lastIdx >= 0 && doc.getBodyElements().get(lastIdx).getElementType() == BodyElementType.PARAGRAPH) {
        ((XWPFParagraph) doc.getBodyElements().get(lastIdx)).setPageBreak(true);
      } else {
        doc.createParagraph().setPageBreak(true);
      }
    }
  }

  private void processQRCode(ReportSection section, XWPFParagraph p) throws Exception {
    if (p == null)
      return;

    p.setAlignment(ParagraphAlignment.CENTER);
    // CWE-22/23: anchor QR temp file to real temp dir
    java.nio.file.Path safeTempDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir")).toRealPath();
    java.nio.file.Path qrPath = java.nio.file.Files.createTempFile(safeTempDir, "qr_code_", ".png");
    try {
      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      BitMatrix bitMatrix = qrCodeWriter.encode(section.getText(), BarcodeFormat.QR_CODE, QR_DIMENSION_PX, QR_DIMENSION_PX);
      MatrixToImageWriter.writeToPath(bitMatrix, "PNG", qrPath);

      try (java.io.InputStream is = java.nio.file.Files.newInputStream(qrPath)) {
        XWPFRun rQR = p.createRun();
        int sizeEmu = Units.toEMU(QR_DISPLAY_SIZE_PT);
        rQR.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG, "qr.png", sizeEmu, sizeEmu);
      }
    } finally {
      if (!qrPath.toFile().delete()) {
        logger.warn("Failed to delete temporary QR code file: {}", qrPath);
      }
    }
  }

  private void processImage(XWPFDocument doc, XWPFTableCell cell, ReportSection section) {
    if (section.getImageDocumentId() == null) {
      logger.warn("IMAGE section has no imageDocumentId, skipping.");
      return;
    }
    if (contentService == null) {
      logger.warn("IMAGE section requires contentService — not available in this context, skipping.");
      return;
    }
    try {
      com.appiancorp.suiteapi.knowledge.Document imgDoc = contentService.download(section.getImageDocumentId(),
        com.appiancorp.suiteapi.content.ContentConstants.VERSION_CURRENT, false)[0];

      byte[] imgBytes;
      try (java.io.InputStream in = imgDoc.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
          baos.write(buf, 0, n);
        }
        imgBytes = baos.toByteArray();
      }

      // Determine display width in EMU based on widthPercent (default 90%)
      int widthPercent = (section.getImageWidthPercent() != null && section.getImageWidthPercent() > 0 &&
        section.getImageWidthPercent() <= 100)
          ? section.getImageWidthPercent()
          : 90;
      int availTwips = PAGE_CONTENT_WIDTH_TWIPS;
      int widthEmu = (int) (availTwips * (widthPercent / 100.0) * CHART_WIDTH_MULTIPLIER);

      // Detect image type from extension
      String ext = imgDoc.getExtension() != null
        ? imgDoc.getExtension().toLowerCase(java.util.Locale.ROOT)
        : "png";
      int pictureType = "jpg".equals(ext) || "jpeg".equals(ext)
        ? XWPFDocument.PICTURE_TYPE_JPEG
        : XWPFDocument.PICTURE_TYPE_PNG;

      // Resolve paragraph alignment
      ParagraphAlignment alignment = ParagraphAlignment.CENTER;
      if (section.getImageAlignment() != null) {
        switch (section.getImageAlignment().toUpperCase(java.util.Locale.ROOT)) {
          case "LEFT":
            alignment = ParagraphAlignment.LEFT;
            break;
          case "RIGHT":
            alignment = ParagraphAlignment.RIGHT;
            break;
          default:
            break;
        }
      }

      XWPFParagraph imgPara = (cell != null) ? cell.addParagraph() : doc.createParagraph();
      imgPara.setAlignment(alignment);
      XWPFRun imgRun = imgPara.createRun();
      try (ByteArrayInputStream bis = new ByteArrayInputStream(imgBytes)) {
        imgRun.addPicture(bis, pictureType, "image." + ext, widthEmu,
          Units.toEMU(widthEmu / 72.0 * 96)); // approximate height — aspect ratio preserved by Word
      }
    } catch (Exception e) {
      logger.error("Failed to embed IMAGE section (docId={}): {}",
        section.getImageDocumentId(), e.getMessage(), e);
    }
  }

  private void processDivider(XWPFDocument doc, XWPFTableCell cell) {
    XWPFParagraph para = (cell != null) ? cell.addParagraph() : doc.createParagraph();
    para.setSpacingBefore(120);
    para.setSpacingAfter(120);

    // Use a bottom border on the paragraph as the visual divider line
    org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr ppr = para.getCTP().isSetPPr() ? para.getCTP().getPPr()
      : para.getCTP().addNewPPr();
    org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr pbd = ppr.isSetPBdr() ? ppr.getPBdr() : ppr.addNewPBdr();
    org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder bottom = pbd.isSetBottom() ? pbd.getBottom() : pbd.addNewBottom();
    bottom.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
    bottom.setColor("BFBFBF");
    bottom.setSz(java.math.BigInteger.valueOf(6));
    bottom.setSpace(java.math.BigInteger.valueOf(1));
  }

  private void processSpacer(XWPFDocument doc, XWPFTableCell cell, ReportSection section) {
    // Default spacer height is 120 twips (~2mm); caller can pass height in text field as integer
    int heightTwips = 240;
    if (section.getText() != null && !section.getText().trim().isEmpty()) {
      try {
        heightTwips = Math.max(20, Math.min(5760, Integer.parseInt(section.getText().trim())));
      } catch (NumberFormatException e) {
        logger.debug("SPACER text '{}' is not a valid integer, using default", section.getText());
      }
    }
    XWPFParagraph spacer = (cell != null) ? cell.addParagraph() : doc.createParagraph();
    spacer.setSpacingBefore(0);
    spacer.setSpacingAfter(0);
    org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr ppr = spacer.getCTP().isSetPPr() ? spacer.getCTP().getPPr()
      : spacer.getCTP().addNewPPr();
    org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing spacing = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
    spacing.setLineRule(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.EXACT);
    spacing.setLine(java.math.BigInteger.valueOf(heightTwips));
  }

  // --- REUSABLE HELPER METHODS ---

  private void removeTableBorders(CTTblPr tblPr) {
    CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
    borders.addNewBottom().setVal(STBorder.NONE);
    borders.addNewLeft().setVal(STBorder.NONE);
    borders.addNewRight().setVal(STBorder.NONE);
    borders.addNewTop().setVal(STBorder.NONE);
    borders.addNewInsideH().setVal(STBorder.NONE);
    borders.addNewInsideV().setVal(STBorder.NONE);
  }

  private XWPFTable createInlineTable(XWPFDocument doc, XWPFTableCell cell, int rows, int cols) {
    if (cell != null) {
      XWPFParagraph sep = cell.addParagraph();
      sep.setSpacingAfter(0);
      sep.setSpacingBefore(0);
      try (XmlCursor cursor = sep.getCTP().newCursor()) {
        XWPFTable table = cell.insertNewTbl(cursor);
        return table;
      }
    } else {
      XWPFTable table = doc.createTable(rows, cols);
      doc.createParagraph();
      return table;
    }
  }

  private XWPFTableRow getOrCreateFirstRow(XWPFTable table, int requiredCols) {
    XWPFTableRow row;
    if (table.getRows().isEmpty()) {
      row = table.createRow();
    } else {
      row = table.getRow(0);
    }

    // Ensure row is not null and has required columns
    if (row != null) {
      while (row.getTableCells().size() < requiredCols) {
        row.createCell();
      }
    } else {
      // Fallback: create a new row if somehow null
      row = table.createRow();
      for (int i = 0; i < requiredCols; i++) {
        row.createCell();
      }
    }

    return row;
  }

  private CTTblPr getOrCreateTblPr(XWPFTable table) {
    return table.getCTTbl().getTblPr() != null ? table.getCTTbl().getTblPr() : table.getCTTbl().addNewTblPr();
  }

  private CTTcPr getOrCreateTcPr(XWPFTableCell cell) {
    return cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
  }

  private void setCellWidth(CTTcPr tcPr, BigInteger widthDxa) {
    CTTblWidth tcW = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
    tcW.setType(STTblWidth.DXA);
    tcW.setW(widthDxa);
  }

  private void setCellMargins(CTTcPr tcPr, BigInteger marginY, BigInteger marginX) {
    CTTcMar mar = tcPr.isSetTcMar() ? tcPr.getTcMar() : tcPr.addNewTcMar();
    mar.addNewTop().setW(marginY);
    mar.addNewBottom().setW(marginY);
    mar.addNewLeft().setW(marginX);
    mar.addNewRight().setW(marginX);
  }

  private void cleanCellParagraphs(XWPFTableCell cell) {
    for (XWPFParagraph p : cell.getParagraphs()) {
      if (p.getText().trim().isEmpty() && p.getRuns().isEmpty()) {
        p.setSpacingAfter(0);
        p.setSpacingBefore(0);
        CTPPr ppr = p.getCTP().isSetPPr() ? p.getCTP().getPPr() : p.getCTP().addNewPPr();
        CTSpacing spacing = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
        spacing.setLineRule(STLineSpacingRule.EXACT);
        spacing.setLine(EXACT_LINE_SPACING);
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

      createHeaderIfNeeded(policy, settings, headerColor, doc);
      createFooterIfNeeded(policy, settings, footerText);

    } catch (Exception e) {
      logger.error("Failed to apply header/footer to document: {}", e.getMessage(), e);
    }
  }

  private void createHeaderIfNeeded(XWPFHeaderFooterPolicy policy, ReportSettings settings, String headerColor, XWPFDocument doc) {
    if (settings.getHeaderText() == null || settings.getHeaderText().isEmpty()) {
      return;
    }

    if (policy.getDefaultHeader() == null) {
      policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
    }

    for (XWPFHeader h : doc.getHeaderList()) {
      XWPFParagraph p = createHeaderParagraph(h);
      addHeaderContent(p, settings, headerColor);
    }
  }

  private XWPFParagraph createHeaderParagraph(XWPFHeader header) {
    XWPFParagraph p;
    if (header.getParagraphs().isEmpty()) {
      p = header.createParagraph();
    } else {
      try (XmlCursor cursor = header.getParagraphs().get(0).getCTP().newCursor()) {
        p = header.insertNewParagraph(cursor);
      }
    }

    p.setAlignment(ParagraphAlignment.LEFT);
    p.setSpacingBefore(0);
    p.setSpacingAfter(0);
    return p;
  }

  private void addHeaderContent(XWPFParagraph p, ReportSettings settings, String headerColor) {
    XWPFRun r1 = p.createRun();
    r1.setText(settings.getHeaderText());
    r1.setBold(true);
    r1.setFontSize(FONT_SIZE_HEADER);
    r1.setColor(headerColor != null ? headerColor : COLOR_WHITE);

    if (settings.getSubheaderText() != null && !settings.getSubheaderText().isEmpty()) {
      r1.addBreak();
      XWPFRun r2 = p.createRun();
      r2.setText(settings.getSubheaderText());
      r2.setBold(false);
      r2.setFontSize(FONT_SIZE_SUBHEADER);
      r2.setColor(COLOR_WHITE);
    }
  }

  private void createFooterIfNeeded(XWPFHeaderFooterPolicy policy, ReportSettings settings, String footerText) {
    String fullFooterText = buildFooterText(settings, footerText);

    if (fullFooterText.isEmpty()) {
      return;
    }

    XWPFFooter footer = policy.getDefaultFooter();
    if (footer == null) {
      footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);
    }

    XWPFParagraph p = footer.getParagraphs().isEmpty() ? footer.createParagraph() : footer.getParagraphs().get(0);

    // Clear existing runs
    for (int i = p.getRuns().size() - 1; i >= 0; i--) {
      p.removeRun(i);
    }

    p.setAlignment(ParagraphAlignment.CENTER);
    p.setSpacingBefore(0);
    p.setSpacingAfter(0);

    XWPFRun r = p.createRun();
    r.setText(fullFooterText);
    r.setFontSize(FONT_SIZE_FOOTER);
    r.setColor(COLOR_GREY_TEXT);
  }

  private String buildFooterText(ReportSettings settings, String footerText) {
    StringBuilder fullFooterText = new StringBuilder();

    if (footerText != null && !footerText.isEmpty()) {
      fullFooterText.append(footerText);
    }

    if (settings.getAuditReference() != null && !settings.getAuditReference().isEmpty()) {
      if (fullFooterText.length() > 0) {
        fullFooterText.append(" | ");
      }
      fullFooterText.append("ID: ").append(settings.getAuditReference());
    }

    if (settings.getReportDate() != null && !settings.getReportDate().isEmpty()) {
      if (fullFooterText.length() > 0) {
        fullFooterText.append(" | ");
      }
      fullFooterText.append("Date: ").append(settings.getReportDate());
    }

    return fullFooterText.toString();
  }

  private int applyPageSettings(XWPFDocument doc, String pageSize, String orientation) {
    CTBody body = doc.getDocument().getBody();
    CTSectPr section = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
    CTPageSz pgSz = section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz();

    long width = A4_WIDTH_TWIPS;
    long height = A4_HEIGHT_TWIPS;
    if ("LETTER".equalsIgnoreCase(pageSize)) {
      width = LETTER_WIDTH_TWIPS;
      height = LETTER_HEIGHT_TWIPS;
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
    pageMar.setTop(MARGIN_STANDARD);
    pageMar.setBottom(MARGIN_STANDARD);
    pageMar.setLeft(MARGIN_STANDARD);
    pageMar.setRight(MARGIN_STANDARD);
    pageMar.setHeader(MARGIN_HEADER_Y);
    pageMar.setFooter(MARGIN_FOOTER_Y);

    return (int) (activeWidth - 2880);
  }

  private void createSidebarLayout(XWPFDocument doc, ReportSection section, int availableWidthTwips) throws Exception {
    XWPFTable table;
    if (doc.getTables().isEmpty() && !doc.getParagraphs().isEmpty()) {
      try (XmlCursor cursor = doc.getParagraphs().get(0).getCTP().newCursor()) {
        table = doc.insertNewTbl(cursor);
      }
    } else {
      table = doc.createTable();
    }

    XWPFTableRow row = table.getRow(0) == null ? table.createRow() : table.getRow(0);
    if (row.getCell(0) == null)
      row.createCell();
    if (row.getCell(1) == null)
      row.createCell();

    CTTblPr tblPr = getOrCreateTblPr(table);
    removeTableBorders(tblPr);

    CTTblWidth tblInd = tblPr.isSetTblInd() ? tblPr.getTblInd() : tblPr.addNewTblInd();
    tblInd.setType(STTblWidth.DXA);
    tblInd.setW(BigInteger.valueOf(-283));

    double leftRatio = section.getLeftColumnRatio() != null ? section.getLeftColumnRatio() : 0.65;
    int targetWidthTwips = availableWidthTwips + 567;
    int leftWidth = (int) (targetWidthTwips * leftRatio);
    int rightWidth = targetWidthTwips - leftWidth;

    tblPr.addNewTblLayout().setType(STTblLayoutType.FIXED);
    CTTblWidth tblW = tblPr.addNewTblW();
    tblW.setType(STTblWidth.DXA);
    tblW.setW(BigInteger.valueOf(targetWidthTwips));

    XWPFTableCell leftCell = row.getCell(0);
    CTTcPr leftTcPr = getOrCreateTcPr(leftCell);
    setCellWidth(leftTcPr, BigInteger.valueOf(leftWidth));
    if (!leftCell.getParagraphs().isEmpty())
      leftCell.removeParagraph(0);
    if (section.getMainContent() != null)
      processSections(doc, section.getMainContent(), leftCell, leftWidth, false);
    cleanCellParagraphs(leftCell);

    XWPFTableCell rightCell = row.getCell(1);
    CTTcPr rightTcPr = getOrCreateTcPr(rightCell);
    setCellWidth(rightTcPr, BigInteger.valueOf(rightWidth));
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
      rTitle.setFontSize(FONT_SIZE_H2);
      p = (cell != null) ? cell.addParagraph() : doc.createParagraph();
      if (isSidebar)
        p.setAlignment(ParagraphAlignment.CENTER);
    }

    ChartGenerationService chartGen = new ChartGenerationService();
    byte[] chartImageBytes = chartGen.generateChartImage(config);

    try (ByteArrayInputStream is = new ByteArrayInputStream(chartImageBytes)) {
      XWPFRun r = p.createRun();

      double aspectRatio = (double) config.getHeight() / config.getWidth();
      int finalWidthEMU;
      int finalHeightEMU;

      if (isSidebar) {
        finalWidthEMU = Units.toEMU(CHART_SIDEBAR_WIDTH_PT);
        finalHeightEMU = (int) (finalWidthEMU * aspectRatio);
      } else {
        double maxDisplayWidthEMU = (width * CHART_WIDTH_MULTIPLIER) * CHART_MAX_WIDTH_PCT;
        finalWidthEMU = (int) maxDisplayWidthEMU;
        finalHeightEMU = (int) (finalWidthEMU * aspectRatio);
      }

      r.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG, "chart.png", finalWidthEMU, finalHeightEMU);
    }
  }
}