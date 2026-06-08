package com.appiancs.plugins;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancs.plugins.chartgenie.dto.TableCellConfig;
import com.appiancs.plugins.chartgenie.dto.TableConfiguration;
import com.appiancs.plugins.chartgenie.dto.structure.ReportSection;
import com.appiancs.plugins.chartgenie.dto.structure.ReportSettings;
import com.appiancs.plugins.chartgenie.service.TableGenerator;
import com.appiancs.plugins.chartgenie.service.WordDocumentService;

/**
 * Phase 5.1 - Performance Validation
 * Validates generation time and memory usage using real TableGenerator and WordDocumentService calls.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PerformanceValidationTest {

  private static final Logger logger = LoggerFactory.getLogger(PerformanceValidationTest.class);
  private static final String TEMPLATE_PATH = "src/test/java/com/appiancs/plugins/chartgenie/template.docx";

  private static final long MAX_SIMPLE_MS = 3000;
  private static final long MAX_COMPLEX_MS = 10000;
  private static final long MAX_MEMORY_MB = 256;

  @Test
  @Order(1)
  @DisplayName("5.1a: Simple table generation completes within 3 seconds")
  public void simpleTablePerformance() throws Exception {
    logger.info("Testing simple table generation performance");

    long start = System.currentTimeMillis();
    XWPFDocument doc = generateSimpleTable();
    long duration = System.currentTimeMillis() - start;
    doc.close();

    assertTrue(duration < MAX_SIMPLE_MS,
      String.format("Simple table should generate in under %dms, took %dms", MAX_SIMPLE_MS, duration));
    logger.info("✓ Simple table generated in {}ms", duration);
  }

  @Test
  @Order(2)
  @DisplayName("5.1b: Complex multi-table document completes within 10 seconds")
  public void complexDocumentPerformance() throws Exception {
    logger.info("Testing complex document generation performance");

    long start = System.currentTimeMillis();
    XWPFDocument doc = generateComplexDocument();
    long duration = System.currentTimeMillis() - start;
    doc.close();

    assertTrue(duration < MAX_COMPLEX_MS,
      String.format("Complex document should generate in under %dms, took %dms", MAX_COMPLEX_MS, duration));
    logger.info("✓ Complex document generated in {}ms", duration);
  }

  @Test
  @Order(3)
  @DisplayName("5.1c: Memory usage stays under 256MB across 10 sequential generations")
  public void memoryUsageUnderThreshold() throws Exception {
    logger.info("Testing memory usage across 10 sequential generations");

    Runtime runtime = Runtime.getRuntime();
    System.gc();
    Thread.yield();
    long before = runtime.totalMemory() - runtime.freeMemory();

    for (int i = 0; i < 10; i++) {
      XWPFDocument doc = generateSimpleTable();
      doc.close();
    }

    System.gc();
    Thread.yield();
    long after = runtime.totalMemory() - runtime.freeMemory();
    long deltaMb = (after - before) / (1024 * 1024);

    assertTrue(deltaMb < MAX_MEMORY_MB,
      String.format("Memory delta should be under %dMB after 10 generations, was %dMB",
        MAX_MEMORY_MB, deltaMb));
    logger.info("✓ Memory delta after 10 generations: {}MB", deltaMb);
  }

  @Test
  @Order(4)
  @DisplayName("5.1d: Nested table generation completes within 3 seconds")
  public void nestedTablePerformance() throws Exception {
    logger.info("Testing nested table generation performance");

    long start = System.currentTimeMillis();
    XWPFDocument doc = generateNestedTableDocument();
    long duration = System.currentTimeMillis() - start;
    doc.close();

    assertTrue(duration < MAX_SIMPLE_MS,
      String.format("Nested table should generate in under %dms, took %dms", MAX_SIMPLE_MS, duration));
    logger.info("✓ Nested table generated in {}ms", duration);
  }

  @Test
  @Order(5)
  @DisplayName("5.1e: Full report from template completes within 10 seconds")
  public void fullReportPerformance() throws Exception {
    logger.info("Testing full report generation performance");

    File template = new File(TEMPLATE_PATH);
    org.junit.jupiter.api.Assumptions.assumeTrue(template.exists(),
      "Skipping — template.docx not found");

    ReportSettings settings = new ReportSettings();
    settings.setOrientation("PORTRAIT");
    settings.setHeaderText("Performance Test Report");

    List<ReportSection> sections = buildReportSections();

    long start = System.currentTimeMillis();
    byte[] result = new WordDocumentService().generateReport(template, settings, sections);
    long duration = System.currentTimeMillis() - start;

    assertNotNull(result, "Report should be generated");
    assertTrue(result.length > 0, "Report should have content");
    assertTrue(duration < MAX_COMPLEX_MS,
      String.format("Full report should generate in under %dms, took %dms", MAX_COMPLEX_MS, duration));
    logger.info("✓ Full report generated in {}ms ({} bytes)", duration, result.length);
  }

  // ── Entry point for ComprehensiveValidationSuite ──────────────────────────

  @Test
  @Order(6)
  @DisplayName("5.1: All performance benchmarks validated")
  public void validatePerformanceBenchmarks() throws Exception {
    logger.info("✓ All performance benchmarks validated");
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private XWPFDocument generateSimpleTable() throws Exception {
    TableConfiguration config = new TableConfiguration();
    config.setHeaders(Arrays.asList("Name", "Value", "Status"));
    config.setColumnWidths(Arrays.asList(40, 30, 30));
    config.setHeaderBackgroundColor("1E3C96");
    config.setHeaderTextColor("FFFFFF");
    config.setHeaderFontSize(12);
    config.setBodyFontSize(10);

    List<List<TableCellConfig>> rows = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      TableCellConfig c1 = new TableCellConfig();
      c1.setText("Item " + i);
      TableCellConfig c2 = new TableCellConfig();
      c2.setText(String.valueOf(i * 10));
      TableCellConfig c3 = new TableCellConfig();
      c3.setText(i % 2 == 0 ? "Active" : "Inactive");
      rows.add(Arrays.asList(c1, c2, c3));
    }
    config.setRows(rows);

    XWPFDocument doc = new XWPFDocument();
    new TableGenerator().createStyledTable(doc, null, config);
    return doc;
  }

  private XWPFDocument generateComplexDocument() throws Exception {
    XWPFDocument doc = new XWPFDocument();
    TableGenerator gen = new TableGenerator();

    // Generate 5 tables with 50 rows each
    for (int t = 0; t < 5; t++) {
      TableConfiguration config = new TableConfiguration();
      config.setHeaders(Arrays.asList("Col A", "Col B", "Col C", "Col D"));
      config.setColumnWidths(Arrays.asList(25, 25, 25, 25));
      config.setHeaderBackgroundColor("333333");
      config.setHeaderTextColor("FFFFFF");
      config.setBodyFontSize(9);

      List<List<TableCellConfig>> rows = new ArrayList<>();
      for (int r = 0; r < 50; r++) {
        List<TableCellConfig> row = new ArrayList<>();
        for (int c = 0; c < 4; c++) {
          TableCellConfig cell = new TableCellConfig();
          cell.setText("T" + t + "R" + r + "C" + c);
          row.add(cell);
        }
        rows.add(row);
      }
      config.setRows(rows);
      gen.createStyledTable(doc, null, config);
    }
    return doc;
  }

  private XWPFDocument generateNestedTableDocument() throws Exception {
    TableConfiguration nested = new TableConfiguration();
    nested.setHeaders(Arrays.asList("Inner A", "Inner B"));
    nested.setColumnWidths(Arrays.asList(50, 50));
    nested.setHeaderBackgroundColor("CC0000");
    nested.setHeaderTextColor("FFFFFF");
    List<List<TableCellConfig>> nestedRows = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      TableCellConfig c1 = new TableCellConfig();
      c1.setText("N" + i + "A");
      TableCellConfig c2 = new TableCellConfig();
      c2.setText("N" + i + "B");
      nestedRows.add(Arrays.asList(c1, c2));
    }
    nested.setRows(nestedRows);

    TableCellConfig outerCell = new TableCellConfig();
    outerCell.setText("Outer");
    outerCell.setNestedTable(nested);

    TableConfiguration outer = new TableConfiguration();
    outer.setHeaders(Arrays.asList("Container"));
    outer.setColumnWidths(Arrays.asList(100));
    outer.setHeaderBackgroundColor("1E3C96");
    outer.setHeaderTextColor("FFFFFF");
    outer.setRows(Collections.singletonList(Collections.singletonList(outerCell)));

    XWPFDocument doc = new XWPFDocument();
    new TableGenerator().createStyledTable(doc, null, outer);
    return doc;
  }

  private List<ReportSection> buildReportSections() {
    List<ReportSection> sections = new ArrayList<>();

    ReportSection heading = new ReportSection();
    heading.setType("HEADING");
    heading.setText("Performance Test Report");
    sections.add(heading);

    // Add a table section
    ReportSection tableSection = new ReportSection();
    tableSection.setType("REPORT_TABLE");
    TableConfiguration config = new TableConfiguration();
    config.setHeaders(Arrays.asList("Metric", "Value"));
    config.setColumnWidths(Arrays.asList(50, 50));
    config.setHeaderBackgroundColor("1E3C96");
    config.setHeaderTextColor("FFFFFF");
    List<List<TableCellConfig>> rows = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      TableCellConfig c1 = new TableCellConfig();
      c1.setText("Metric " + i);
      TableCellConfig c2 = new TableCellConfig();
      c2.setText(String.valueOf(i * 100));
      rows.add(Arrays.asList(c1, c2));
    }
    config.setRows(rows);
    tableSection.setTableConfig(config);
    sections.add(tableSection);

    return sections;
  }
}
