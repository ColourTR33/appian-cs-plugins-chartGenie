package com.appiancs.plugins.chartgenie;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.dto.TableCellConfig;
import com.appiancs.plugins.chartgenie.dto.TableConfiguration;
import com.appiancs.plugins.chartgenie.service.TableGenerator;

/**
 * Phase 1.1 - Font Control Feature Validation
 * Validates that headerFontSize and bodyFontSize are correctly applied to table cells.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FontControlValidationTest {

  private static final Logger logger = LoggerFactory.getLogger(FontControlValidationTest.class);
  private TableGenerator tableGenerator;

  @BeforeEach
  public void setup() {
    tableGenerator = new TableGenerator();
  }

  // ── Helper ────────────────────────────────────────────────────────────────

  private TableConfiguration buildConfig(Integer headerFontSize, Integer bodyFontSize, String cellText) {
    TableConfiguration config = new TableConfiguration();
    config.setHeaders(Arrays.asList("Column A", "Column B"));
    config.setColumnWidths(Arrays.asList(50, 50));
    config.setHeaderBackgroundColor("1E3C96");
    config.setHeaderTextColor("FFFFFF");
    config.setHeaderFontSize(headerFontSize);
    config.setBodyFontSize(bodyFontSize);

    TableCellConfig cell1 = new TableCellConfig();
    cell1.setText(cellText != null ? cellText : "Body text");
    TableCellConfig cell2 = new TableCellConfig();
    cell2.setText("More text");
    config.setRows(Collections.singletonList(Arrays.asList(cell1, cell2)));
    return config;
  }

  private XWPFDocument generateDoc(TableConfiguration config) throws Exception {
    XWPFDocument doc = new XWPFDocument();
    tableGenerator.createStyledTable(doc, null, config);
    return doc;
  }

  // ── Tests ─────────────────────────────────────────────────────────────────

  @Test
  @Order(1)
  @DisplayName("1.1a: Header font size is applied to header row runs")
  public void headerFontSizeApplied() throws Exception {
    logger.info("Testing header font size application");

    XWPFDocument doc = generateDoc(buildConfig(18, null, null));
    XWPFTable table = doc.getTables().get(0);
    XWPFTableRow headerRow = table.getRow(0);

    boolean fontSizeFound = false;
    for (XWPFTableCell cell : headerRow.getTableCells()) {
      for (XWPFParagraph p : cell.getParagraphs()) {
        for (XWPFRun run : p.getRuns()) {
          if (run.getFontSize() == 18) {
            fontSizeFound = true;
          }
        }
      }
    }

    assertTrue(fontSizeFound, "Header row should have font size 18");
    logger.info("✓ Header font size 18 applied correctly");
    doc.close();
  }

  @Test
  @Order(2)
  @DisplayName("1.1b: Body font size is applied to data row runs")
  public void bodyFontSizeApplied() throws Exception {
    logger.info("Testing body font size application");

    XWPFDocument doc = generateDoc(buildConfig(null, 11, "Plain body text"));
    XWPFTable table = doc.getTables().get(0);
    XWPFTableRow dataRow = table.getRow(1);

    boolean fontSizeFound = false;
    for (XWPFTableCell cell : dataRow.getTableCells()) {
      for (XWPFParagraph p : cell.getParagraphs()) {
        for (XWPFRun run : p.getRuns()) {
          if (run.getFontSize() == 11) {
            fontSizeFound = true;
          }
        }
      }
    }

    assertTrue(fontSizeFound, "Body row should have font size 11");
    logger.info("✓ Body font size 11 applied correctly");
    doc.close();
  }

  @Test
  @Order(3)
  @DisplayName("1.1c: Font size below minimum (8) is clamped to 8")
  public void fontSizeClampsToMinimum() throws Exception {
    logger.info("Testing font size minimum clamp");

    XWPFDocument doc = generateDoc(buildConfig(2, null, null));
    XWPFTable table = doc.getTables().get(0);
    XWPFTableRow headerRow = table.getRow(0);

    for (XWPFTableCell cell : headerRow.getTableCells()) {
      for (XWPFParagraph p : cell.getParagraphs()) {
        for (XWPFRun run : p.getRuns()) {
          int size = run.getFontSize();
          if (size > 0) {
            assertTrue(size >= 8, "Font size should be clamped to minimum 8, got: " + size);
          }
        }
      }
    }

    logger.info("✓ Font size clamped to minimum 8");
    doc.close();
  }

  @Test
  @Order(4)
  @DisplayName("1.1d: Font size above maximum (72) is clamped to 72")
  public void fontSizeClampsToMaximum() throws Exception {
    logger.info("Testing font size maximum clamp");

    XWPFDocument doc = generateDoc(buildConfig(null, 999, "Body text"));
    XWPFTable table = doc.getTables().get(0);
    XWPFTableRow dataRow = table.getRow(1);

    for (XWPFTableCell cell : dataRow.getTableCells()) {
      for (XWPFParagraph p : cell.getParagraphs()) {
        for (XWPFRun run : p.getRuns()) {
          int size = run.getFontSize();
          if (size > 0) {
            assertTrue(size <= 72, "Font size should be clamped to maximum 72, got: " + size);
          }
        }
      }
    }

    logger.info("✓ Font size clamped to maximum 72");
    doc.close();
  }

  @Test
  @Order(5)
  @DisplayName("1.1e: Null font sizes do not throw and use document defaults")
  public void nullFontSizesAreIgnored() throws Exception {
    logger.info("Testing null font sizes are safely ignored");

    assertDoesNotThrow(() -> {
      XWPFDocument doc = generateDoc(buildConfig(null, null, null));
      assertFalse(doc.getTables().isEmpty(), "Table should still be generated");
      doc.close();
    });

    logger.info("✓ Null font sizes handled safely");
  }

  @Test
  @Order(6)
  @DisplayName("1.1f: Both header and body font sizes applied together")
  public void bothFontSizesAppliedTogether() throws Exception {
    logger.info("Testing both header and body font sizes together");

    XWPFDocument doc = generateDoc(buildConfig(16, 10, "Body text"));
    XWPFTable table = doc.getTables().get(0);

    // Check header row
    boolean headerSizeFound = false;
    for (XWPFTableCell cell : table.getRow(0).getTableCells()) {
      for (XWPFParagraph p : cell.getParagraphs()) {
        for (XWPFRun run : p.getRuns()) {
          if (run.getFontSize() == 16)
            headerSizeFound = true;
        }
      }
    }

    // Check body row
    boolean bodySizeFound = false;
    for (XWPFTableCell cell : table.getRow(1).getTableCells()) {
      for (XWPFParagraph p : cell.getParagraphs()) {
        for (XWPFRun run : p.getRuns()) {
          if (run.getFontSize() == 10)
            bodySizeFound = true;
        }
      }
    }

    assertTrue(headerSizeFound, "Header font size 16 should be applied");
    assertTrue(bodySizeFound, "Body font size 10 should be applied");
    logger.info("✓ Both header (16pt) and body (10pt) font sizes applied correctly");
    doc.close();
  }

  @Test
  @Order(7)
  @DisplayName("1.1g: Font size works with HTML rich text in body cells")
  public void fontSizeWithHtmlRichText() throws Exception {
    logger.info("Testing font size with HTML rich text content");

    XWPFDocument doc = generateDoc(buildConfig(null, 12, "<b>Bold text</b> and <em>italic</em>"));
    XWPFTable table = doc.getTables().get(0);
    XWPFTableRow dataRow = table.getRow(1);

    boolean fontSizeFound = false;
    for (XWPFTableCell cell : dataRow.getTableCells()) {
      for (XWPFParagraph p : cell.getParagraphs()) {
        for (XWPFRun run : p.getRuns()) {
          if (run.getFontSize() == 12) {
            fontSizeFound = true;
          }
        }
      }
    }

    assertTrue(fontSizeFound, "Body font size 12 should be applied to HTML-rendered runs");
    logger.info("✓ Font size 12 applied to HTML rich text runs");
    doc.close();
  }
}
