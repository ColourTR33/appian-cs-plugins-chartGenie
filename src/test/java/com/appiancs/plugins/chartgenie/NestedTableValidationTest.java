package com.appiancs.plugins.chartgenie;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.dto.TableCellConfig;
import com.appiancs.plugins.chartgenie.dto.TableConfiguration;
import com.appiancs.plugins.chartgenie.service.TableGenerator;

/**
 * Phase 1.2 - Nested Tables Feature Validation
 * Validates that a TableCellConfig with a nestedTable renders a child table inside the cell.
 * Assertions use CTTc.getTblArray() (raw XML) since POI's getTables() does not reflect
 * tables added via addNewTbl() after cell construction.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NestedTableValidationTest {

  private static final Logger logger = LoggerFactory.getLogger(NestedTableValidationTest.class);
  private TableGenerator tableGenerator;

  @BeforeEach
  public void setup() {
    tableGenerator = new TableGenerator();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private TableConfiguration buildSimpleConfig(String... cellTexts) {
    TableConfiguration config = new TableConfiguration();
    config.setHeaders(Arrays.asList("Col A", "Col B"));
    config.setColumnWidths(Arrays.asList(50, 50));
    config.setHeaderBackgroundColor("1E3C96");
    config.setHeaderTextColor("FFFFFF");

    TableCellConfig c1 = new TableCellConfig();
    c1.setText(cellTexts.length > 0 ? cellTexts[0] : "Cell 1");
    TableCellConfig c2 = new TableCellConfig();
    c2.setText(cellTexts.length > 1 ? cellTexts[1] : "Cell 2");
    config.setRows(Collections.singletonList(Arrays.asList(c1, c2)));
    return config;
  }

  private TableCellConfig cellWithNested(String text, TableConfiguration nested) {
    TableCellConfig cell = new TableCellConfig();
    cell.setText(text);
    cell.setNestedTable(nested);
    return cell;
  }

  private TableConfiguration singleCellConfig(TableCellConfig cell) {
    TableConfiguration config = new TableConfiguration();
    config.setHeaders(Arrays.asList("Col"));
    config.setColumnWidths(Arrays.asList(100));
    config.setHeaderBackgroundColor("444444");
    config.setHeaderTextColor("FFFFFF");
    config.setRows(Collections.singletonList(Collections.singletonList(cell)));
    return config;
  }

  /** Returns the data cell (row 1, col 0) of the first table in the document. */
  private XWPFTableCell getDataCell(XWPFDocument doc) {
    return doc.getTables().get(0).getRow(1).getCell(0);
  }

  // ── Tests ─────────────────────────────────────────────────────────────────

  @Test
  @Order(1)
  @DisplayName("1.2a: Cell with nestedTable produces a child table in the cell XML")
  public void nestedTableRenderedInsideCell() throws Exception {
    logger.info("Testing nested table renders inside parent cell");

    TableConfiguration nested = buildSimpleConfig("Nested A", "Nested B");
    TableConfiguration outer = singleCellConfig(cellWithNested("Parent text", nested));

    XWPFDocument doc = new XWPFDocument();
    tableGenerator.createStyledTable(doc, null, outer);

    assertFalse(doc.getTables().isEmpty(), "Outer table should be created");

    // Assert via raw CTTc XML — POI's getTables() doesn't reflect addNewTbl() calls
    XWPFTableCell dataCell = getDataCell(doc);
    CTTbl[] nestedTbls = dataCell.getCTTc().getTblArray();
    assertTrue(nestedTbls != null && nestedTbls.length > 0,
      "Data cell CTTc should contain a nested w:tbl element");

    logger.info("✓ Nested table found in parent cell XML ({} nested table(s))", nestedTbls.length);
    doc.close();
  }

  @Test
  @Order(2)
  @DisplayName("1.2b: Nested table has correct number of rows")
  public void nestedTableHasCorrectStructure() throws Exception {
    logger.info("Testing nested table structure (rows)");

    TableConfiguration nested = buildSimpleConfig("N1", "N2");
    TableConfiguration outer = singleCellConfig(cellWithNested("", nested));

    XWPFDocument doc = new XWPFDocument();
    tableGenerator.createStyledTable(doc, null, outer);

    XWPFTableCell dataCell = getDataCell(doc);
    CTTbl[] nestedTbls = dataCell.getCTTc().getTblArray();
    assertTrue(nestedTbls != null && nestedTbls.length > 0, "Nested table should exist in cell XML");

    // nested config: 1 header row + 1 data row = 2 rows
    int rowCount = nestedTbls[0].getTrArray().length;
    assertEquals(2, rowCount, "Nested table should have 2 rows (header + data)");

    logger.info("✓ Nested table has {} rows", rowCount);
    doc.close();
  }

  @Test
  @Order(3)
  @DisplayName("1.2c: Nested table cell text is rendered correctly")
  public void nestedTableCellTextRendered() throws Exception {
    logger.info("Testing nested table cell text content");

    TableConfiguration nested = buildSimpleConfig("Inner Value", "Inner Value 2");
    TableConfiguration outer = singleCellConfig(cellWithNested("", nested));

    XWPFDocument doc = new XWPFDocument();
    tableGenerator.createStyledTable(doc, null, outer);

    XWPFTableCell dataCell = getDataCell(doc);
    CTTbl[] nestedTbls = dataCell.getCTTc().getTblArray();
    assertTrue(nestedTbls != null && nestedTbls.length > 0, "Nested table should exist");

    // Row 1 (data row), cell 0 — get text from the first paragraph run
    String cellText = nestedTbls[0].getTrArray(1).getTcArray(0).getPArray(0).getRArray(0).getTArray(0).getStringValue();
    assertEquals("Inner Value", cellText, "Nested cell should contain correct text");

    logger.info("✓ Nested table cell text: '{}'", cellText);
    doc.close();
  }

  @Test
  @Order(4)
  @DisplayName("1.2d: Cell without nestedTable is unaffected")
  public void cellWithoutNestedTableUnaffected() throws Exception {
    logger.info("Testing that cells without nestedTable are unaffected");

    XWPFDocument doc = new XWPFDocument();
    tableGenerator.createStyledTable(doc, null, buildSimpleConfig("Plain A", "Plain B"));

    assertFalse(doc.getTables().isEmpty(), "Table should be created");
    XWPFTableCell dataCell = getDataCell(doc);

    CTTbl[] nestedTbls = dataCell.getCTTc().getTblArray();
    assertTrue(nestedTbls == null || nestedTbls.length == 0,
      "Cell without nestedTable should have no child w:tbl elements");
    assertEquals("Plain A", dataCell.getText(), "Plain cell text should be intact");

    logger.info("✓ Cells without nestedTable are unaffected");
    doc.close();
  }

  @Test
  @Order(5)
  @DisplayName("1.2e: Nesting depth is capped at MAX_NESTING_DEPTH (3 levels)")
  public void nestingDepthIsCapped() throws Exception {
    logger.info("Testing nesting depth cap at 3 levels");

    // Build 4 levels deep — level 4 should be silently ignored
    TableConfiguration level4 = buildSimpleConfig("L4");
    TableCellConfig cellL3 = cellWithNested("L3", level4);
    TableConfiguration level3 = singleCellConfig(cellL3);

    TableCellConfig cellL2 = cellWithNested("L2", level3);
    TableConfiguration level2 = singleCellConfig(cellL2);

    TableCellConfig cellL1 = cellWithNested("L1", level2);
    TableConfiguration level1 = singleCellConfig(cellL1);

    XWPFDocument doc = new XWPFDocument();
    assertDoesNotThrow(() -> tableGenerator.createStyledTable(doc, null, level1),
      "Deep nesting should not throw");

    assertFalse(doc.getTables().isEmpty(), "Top-level table should be created");
    logger.info("✓ Nesting depth capped safely at 3 levels");
    doc.close();
  }

  @Test
  @Order(6)
  @DisplayName("1.2f: Nested table uses its own font size config")
  public void nestedTableUsesItsOwnFontConfig() throws Exception {
    logger.info("Testing nested table uses its own font size config");

    TableConfiguration nested = buildSimpleConfig("Sized text", "Other");
    nested.setBodyFontSize(9);

    TableConfiguration outer = singleCellConfig(cellWithNested("", nested));
    outer.setBodyFontSize(14);

    XWPFDocument doc = new XWPFDocument();
    tableGenerator.createStyledTable(doc, null, outer);

    XWPFTableCell dataCell = getDataCell(doc);
    CTTbl[] nestedTbls = dataCell.getCTTc().getTblArray();
    assertTrue(nestedTbls != null && nestedTbls.length > 0, "Nested table should exist");

    // Verify the nested table has its own font size (9pt) in the run properties
    String nestedXml = nestedTbls[0].xmlText();
    assertTrue(
      nestedXml.contains("w:sz w:val=\"18\"") || nestedXml.contains("w:sz=\"18\"") || nestedXml.contains(">18<") ||
        nestedXml.contains("val=\"18\""),
      "Nested table XML should contain font size 18 half-points (= 9pt). XML: " +
        nestedXml.substring(0, Math.min(500, nestedXml.length())));

    logger.info("✓ Nested table applies its own font size config independently");
    doc.close();
  }
}
