package com.appiancs.plugins.chartgenie.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.Cell;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Table;
import com.lowagie.text.alignment.HorizontalAlignment;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Converts a generated DOCX (as bytes) to a PDF document (as bytes).
 * <p>
 * Uses Apache POI to traverse the DOCX structure and OpenPDF to render the PDF.
 * This is a pure-Java, server-safe implementation — no LibreOffice or native processes
 * required, making it suitable for Appian Cloud deployments.
 * <p>
 * Supported elements: headings, paragraphs, tables, and embedded images (charts).
 */
public class PdfConversionService {

  private static final Logger LOG = LoggerFactory.getLogger(PdfConversionService.class);

  private static final float HEADING_FONT_SIZE = 16f;
  private static final float BODY_FONT_SIZE = 11f;
  private static final float TABLE_FONT_SIZE = 10f;
  private static final float IMAGE_MAX_WIDTH_PT = 480f;
  private static final int TABLE_WIDTH_PERCENT = 100;
  private static final float HEADING_SPACING_BEFORE = 12f;
  private static final float HEADING_SPACING_AFTER = 6f;
  private static final float PARA_SPACING_AFTER = 4f;

  /**
   * Converts DOCX bytes to PDF bytes.
   *
   * @param docxBytes
   *          the generated DOCX content
   * @param pageSize
   *          "A4" or "LETTER" — defaults to A4
   * @return PDF as a byte array
   * @throws Exception
   *           if conversion fails
   */
  public byte[] convert(byte[] docxBytes, String pageSize) throws Exception {
    LOG.debug("Starting DOCX-to-PDF conversion, input size: {} bytes", docxBytes.length);

    com.lowagie.text.Rectangle pdfPageSize = resolvePdfPageSize(pageSize);

    try (ByteArrayInputStream in = new ByteArrayInputStream(docxBytes);
      XWPFDocument docx = new XWPFDocument(in);
      ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Document pdf = new Document(pdfPageSize, 72, 72, 72, 72);
      PdfWriter.getInstance(pdf, out);
      pdf.open();

      for (int i = 0; i < docx.getBodyElements().size(); i++) {
        if (docx.getBodyElements().get(i).getElementType() == BodyElementType.PARAGRAPH) {
          XWPFParagraph para = (XWPFParagraph) docx.getBodyElements().get(i);
          addParagraph(pdf, para);
        } else if (docx.getBodyElements().get(i).getElementType() == BodyElementType.TABLE) {
          XWPFTable table = (XWPFTable) docx.getBodyElements().get(i);
          addTable(pdf, table);
        }
      }

      pdf.close();
      LOG.info("DOCX-to-PDF conversion successful, output size: {} bytes", out.size());
      return out.toByteArray();
    }
  }

  private void addParagraph(Document pdf, XWPFParagraph para) throws Exception {
    String text = para.getText();
    if (text == null || text.trim().isEmpty()) {
      return;
    }

    // Check for embedded images in the runs first
    for (XWPFRun run : para.getRuns()) {
      List<XWPFPicture> pictures = run.getEmbeddedPictures();
      for (XWPFPicture pic : pictures) {
        addImage(pdf, pic);
      }
    }

    // Skip if the paragraph only contained images
    if (text.trim().isEmpty()) {
      return;
    }

    boolean isHeading = isHeadingStyle(para.getStyle());
    Font font = resolveFont(para, isHeading);

    Paragraph pdfPara = new Paragraph(text, font);
    pdfPara.setAlignment(resolveAlignment(para));

    if (isHeading) {
      pdfPara.setSpacingBefore(HEADING_SPACING_BEFORE);
      pdfPara.setSpacingAfter(HEADING_SPACING_AFTER);
    } else {
      pdfPara.setSpacingAfter(PARA_SPACING_AFTER);
    }

    pdf.add(pdfPara);
  }

  private void addImage(Document pdf, XWPFPicture pic) throws Exception {
    byte[] imgData = pic.getPictureData().getData();
    if (imgData == null || imgData.length == 0) {
      return;
    }
    try {
      Image img = Image.getInstance(imgData);

      // Scale down to fit the page width while preserving aspect ratio
      if (img.getWidth() > IMAGE_MAX_WIDTH_PT) {
        img.scaleToFit(IMAGE_MAX_WIDTH_PT, img.getHeight() * (IMAGE_MAX_WIDTH_PT / img.getWidth()));
      }

      img.setAlignment(Image.ALIGN_CENTER);
      pdf.add(img);
    } catch (Exception e) {
      LOG.warn("Could not embed image in PDF, skipping: {}", e.getMessage());
    }
  }

  private void addTable(Document pdf, XWPFTable xwpfTable) throws Exception {
    List<XWPFTableRow> rows = xwpfTable.getRows();
    if (rows.isEmpty()) {
      return;
    }

    int numCols = rows.get(0).getTableCells().size();
    if (numCols == 0) {
      return;
    }

    Table pdfTable = new Table(numCols, rows.size());
    pdfTable.setWidth(TABLE_WIDTH_PERCENT);
    pdfTable.setPadding(4);
    pdfTable.setSpacing(0);
    pdfTable.setBorderWidth(0.5f);

    for (int r = 0; r < rows.size(); r++) {
      XWPFTableRow row = rows.get(r);
      boolean isHeaderRow = (r == 0 && hasHeaderStyle(xwpfTable));

      for (XWPFTableCell xwpfCell : row.getTableCells()) {
        String cellText = xwpfCell.getText();
        Font cellFont = isHeaderRow
          ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, TABLE_FONT_SIZE)
          : FontFactory.getFont(FontFactory.HELVETICA, TABLE_FONT_SIZE);

        Cell pdfCell = new Cell(new Phrase(cellText != null ? cellText : "", cellFont));

        if (isHeaderRow) {
          pdfCell.setBackgroundColor(new java.awt.Color(0, 57, 93));
          pdfCell.setHorizontalAlignment(HorizontalAlignment.CENTER);
        }

        pdfTable.addCell(pdfCell);
      }
    }

    pdf.add(pdfTable);
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────

  private com.lowagie.text.Rectangle resolvePdfPageSize(String pageSize) {
    if ("LETTER".equalsIgnoreCase(pageSize)) {
      return PageSize.LETTER;
    }
    return PageSize.A4;
  }

  private boolean isHeadingStyle(String style) {
    if (style == null) {
      return false;
    }
    String lower = style.toLowerCase(java.util.Locale.ROOT);
    return lower.startsWith("heading") || lower.startsWith("title");
  }

  private boolean hasHeaderStyle(XWPFTable table) {
    // Infer header row presence from first row bold formatting
    if (table.getRows().isEmpty()) {
      return false;
    }
    XWPFTableRow firstRow = table.getRow(0);
    if (firstRow.getTableCells().isEmpty()) {
      return false;
    }
    for (XWPFRun run : firstRow.getTableCells().get(0).getParagraphs().get(0).getRuns()) {
      if (Boolean.TRUE.equals(run.isBold())) {
        return true;
      }
    }
    return false;
  }

  private Font resolveFont(XWPFParagraph para, boolean isHeading) {
    if (isHeading) {
      return FontFactory.getFont(FontFactory.HELVETICA_BOLD, HEADING_FONT_SIZE);
    }

    // Check if any run is bold/italic
    boolean bold = false;
    boolean italic = false;
    for (XWPFRun run : para.getRuns()) {
      if (Boolean.TRUE.equals(run.isBold())) {
        bold = true;
      }
      if (Boolean.TRUE.equals(run.isItalic())) {
        italic = true;
      }
    }

    int style = Font.NORMAL;
    if (bold && italic) {
      style = Font.BOLDITALIC;
    } else if (bold) {
      style = Font.BOLD;
    } else if (italic) {
      style = Font.ITALIC;
    }

    return FontFactory.getFont(FontFactory.HELVETICA, BODY_FONT_SIZE, style);
  }

  private int resolveAlignment(XWPFParagraph para) {
    if (para.getAlignment() == null) {
      return com.lowagie.text.Element.ALIGN_LEFT;
    }
    switch (para.getAlignment()) {
      case CENTER:
        return com.lowagie.text.Element.ALIGN_CENTER;
      case RIGHT:
        return com.lowagie.text.Element.ALIGN_RIGHT;
      case BOTH:
        return com.lowagie.text.Element.ALIGN_JUSTIFIED;
      default:
        return com.lowagie.text.Element.ALIGN_LEFT;
    }
  }
}
