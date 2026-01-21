package com.appiancs.plugins.chartgenie.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

public class WordDocumentService {

  /**
   * Helper to get the template from the classpath (src/main/resources).
   * Ensure you have a file named "company_template.docx" in that folder.
   */
  private InputStream getTemplateStream() {
    return getClass().getClassLoader().getResourceAsStream("temp1.docx");
  }

  public File createBaseDocument() throws IOException {
    InputStream templateStream = getTemplateStream();
    XWPFDocument doc;

    if (templateStream != null) {
      System.out.println("   > Engine: Loading Company Template...");
      doc = new XWPFDocument(templateStream);
    } else {
      System.out.println("   > Engine: Template not found, creating blank doc.");
      doc = new XWPFDocument();
    }

    File outputFile = File.createTempFile("Report_Base_", ".docx");
    try (FileOutputStream out = new FileOutputStream(outputFile)) {
      doc.write(out);
    } finally {
      doc.close();
      if (templateStream != null)
        templateStream.close();
    }
    return outputFile;
  }

  /**
   * Creates a NEW document (from template if available) and inserts the chart.
   */
  public File createDocumentWithChart(File imageFile) throws IOException, InvalidFormatException {
    InputStream templateStream = getTemplateStream();
    XWPFDocument doc;

    // 1. Initialize the document (Template vs Blank)
    if (templateStream != null) {
      System.out.println("   > Applying Company Theme (Template Loaded)");
      doc = new XWPFDocument(templateStream);
    } else {
      System.out.println("   > Template not found, using Blank Document");
      doc = new XWPFDocument();
    }

    File outputFile = File.createTempFile("OutputDoc_", ".docx");

    try {
      insertImageIntoDoc(doc, imageFile);

      try (FileOutputStream out = new FileOutputStream(outputFile)) {
        doc.write(out);
      }
    } finally {
      doc.close();
      if (templateStream != null) {
        templateStream.close();
      }
    }
    return outputFile;
  }

  /**
   * Appends an image to an EXISTING document file.
   */
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

  /**
   * Appends a text paragraph to an EXISTING document.
   * 
   * @param isHeading
   *          If true, makes the text larger and bold.
   */
  public File appendTextToDocument(File existingDocFile, String text, boolean isHeading) throws IOException {
    File outputFile = File.createTempFile("UpdatedDoc_Text_", ".docx");

    try (FileInputStream fis = new FileInputStream(existingDocFile);
      XWPFDocument doc = new XWPFDocument(fis)) {

      XWPFParagraph p = doc.createParagraph();

      // Style the paragraph
      if (isHeading) {
        p.setAlignment(ParagraphAlignment.LEFT);
        p.setSpacingBefore(200); // Add space before heading
      } else {
        p.setAlignment(ParagraphAlignment.BOTH); // Justify normal text
      }

      XWPFRun r = p.createRun();
      r.setText(text);

      // Apply Font Styles
      if (isHeading) {
        r.setBold(true);
        r.setFontSize(16);
        r.setColor("2F5496"); // Professional dark blue
      } else {
        r.setFontSize(12);
        r.setFontFamily("Calibri");
      }

      try (FileOutputStream out = new FileOutputStream(outputFile)) {
        doc.write(out);
      }
    }
    return outputFile;
  }

  /**
   * Appends a simple data table to the document.
   * 
   * @param data
   *          A list of rows, where each row is an array of Strings.
   */
  public File appendTableToDocument(File existingDocFile, List<String[]> data) throws IOException {
    File outputFile = File.createTempFile("UpdatedDoc_Table_", ".docx");

    try (FileInputStream fis = new FileInputStream(existingDocFile);
      XWPFDocument doc = new XWPFDocument(fis)) {

      // 1. Create the Table
      XWPFTable table = doc.createTable();

      // 2. Loop through data and fill table
      for (int i = 0; i < data.size(); i++) {
        String[] rowData = data.get(i);
        XWPFTableRow tableRow;

        // Get or create the row
        if (i == 0) {
          tableRow = table.getRow(0); // First row exists by default
        } else {
          tableRow = table.createRow();
        }

        // Loop through columns
        for (int col = 0; col < rowData.length; col++) {
          if (col == 0) {
            tableRow.getCell(0).setText(rowData[col]);
          } else {
            tableRow.addNewTableCell().setText(rowData[col]);
          }
        }
      }

      // Add a break after table
      doc.createParagraph().createRun().addBreak();

      try (FileOutputStream out = new FileOutputStream(outputFile)) {
        doc.write(out);
      }
    }
    return outputFile;
  }

  /**
   * Shared helper to insert image
   */
  private void insertImageIntoDoc(XWPFDocument doc, File imageFile) throws IOException, InvalidFormatException {
    XWPFParagraph p = doc.createParagraph();
    p.setAlignment(ParagraphAlignment.CENTER);

    XWPFRun r = p.createRun();
    r.addBreak();
    r.setText("Chart generated at: " + java.time.LocalDateTime.now());
    r.addBreak();

    try (InputStream is = new FileInputStream(imageFile)) {
      r.addPicture(
        is,
        XWPFDocument.PICTURE_TYPE_PNG,
        imageFile.getName(),
        Units.toEMU(500),
        Units.toEMU(300));
    }
  }
}