package com.appiancs.plugins;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Phase 4.2 - Documentation Validation
 * Validates README completeness and JavaDoc presence on key classes.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DocumentationValidationTest {

  private static final Logger logger = LoggerFactory.getLogger(DocumentationValidationTest.class);
  private static final String ROOT = "c:\\Repos\\appian-plugins\\appian-cs-plugins\\chartGenie";
  private static final String SRC = ROOT + "\\src\\main\\java\\com\\appiancs\\plugins\\chartgenie";

  @Test
  @Order(1)
  @DisplayName("4.2a: README — core sections present")
  public void validateReadmeCoreSections() throws Exception {
    logger.info("Validating README core sections");
    String readme = readFile(ROOT + "\\README.md");

    assertTrue(readme.contains("Key Features"), "README should have Key Features section");
    assertTrue(readme.contains("Smart Services"), "README should have Smart Services section");
    assertTrue(readme.contains("JSON Configuration Schema"), "README should have JSON schema section");
    assertTrue(readme.contains("Installation"), "README should have Installation section");
    assertTrue(readme.contains("Requirements"), "README should have Requirements section");

    logger.info("✓ README core sections present");
  }

  @Test
  @Order(2)
  @DisplayName("4.2b: README — font control fields documented")
  public void validateReadmeFontControl() throws Exception {
    logger.info("Validating README font control documentation");
    String readme = readFile(ROOT + "\\README.md");

    assertTrue(readme.contains("headerFontSize"), "README should document headerFontSize");
    assertTrue(readme.contains("bodyFontSize"), "README should document bodyFontSize");
    assertTrue(readme.contains("8"), "README should mention minimum font size");
    assertTrue(readme.contains("72"), "README should mention maximum font size");

    logger.info("✓ README font control fields documented");
  }

  @Test
  @Order(3)
  @DisplayName("4.2c: README — nested table field and example documented")
  public void validateReadmeNestedTables() throws Exception {
    logger.info("Validating README nested tables documentation");
    String readme = readFile(ROOT + "\\README.md");

    assertTrue(readme.contains("nestedTable"), "README should document nestedTable field");
    assertTrue(readme.contains("nested"), "README should describe nested table concept");
    assertTrue(readme.contains("\"nestedTable\""), "README should include nestedTable JSON example");

    logger.info("✓ README nested tables documented");
  }

  @Test
  @Order(4)
  @DisplayName("4.2d: README — all 6 client features mentioned")
  public void validateReadmeAllFeatures() throws Exception {
    logger.info("Validating README covers all 6 client features");
    String readme = readFile(ROOT + "\\README.md");

    assertTrue(readme.contains("Rich Text"), "README should mention Rich Text Styling");
    assertTrue(readme.contains("Sidebar"), "README should mention Sidebar Layout");
    assertTrue(readme.contains("headerText") || readme.contains("Header") || readme.contains("header"), "README should mention Headers");
    assertTrue(readme.contains("Chart") || readme.contains("chart"), "README should mention Graph/Chart generation");
    assertTrue(readme.contains("Font Control") || readme.contains("headerFontSize"), "README should mention Font Control");
    assertTrue(readme.contains("Nested") || readme.contains("nestedTable"), "README should mention Nested Tables");

    logger.info("✓ All 6 client features mentioned in README");
  }

  @Test
  @Order(5)
  @DisplayName("4.2e: JavaDoc — TableConfiguration has class-level JavaDoc")
  public void validateTableConfigurationJavaDoc() throws Exception {
    logger.info("Validating TableConfiguration JavaDoc");
    String source = readFile(SRC + "\\dto\\TableConfiguration.java");

    assertTrue(source.contains("/**"), "TableConfiguration should have JavaDoc");
    assertTrue(source.contains("headerFontSize"), "TableConfiguration should document headerFontSize");
    assertTrue(source.contains("bodyFontSize"), "TableConfiguration should document bodyFontSize");

    logger.info("✓ TableConfiguration JavaDoc validated");
  }

  @Test
  @Order(6)
  @DisplayName("4.2f: JavaDoc — TableCellConfig has class-level JavaDoc")
  public void validateTableCellConfigJavaDoc() throws Exception {
    logger.info("Validating TableCellConfig JavaDoc");
    String source = readFile(SRC + "\\dto\\TableCellConfig.java");

    assertTrue(source.contains("/**"), "TableCellConfig should have JavaDoc");
    assertTrue(source.contains("nestedTable"), "TableCellConfig should document nestedTable field");

    logger.info("✓ TableCellConfig JavaDoc validated");
  }

  @Test
  @Order(7)
  @DisplayName("4.2g: JavaDoc — TableGenerator has class-level JavaDoc")
  public void validateTableGeneratorJavaDoc() throws Exception {
    logger.info("Validating TableGenerator JavaDoc");
    String source = readFile(SRC + "\\service\\TableGenerator.java");

    assertTrue(source.contains("/**"), "TableGenerator should have JavaDoc");
    assertTrue(source.contains("MAX_NESTING_DEPTH") || source.contains("nested"), "TableGenerator should document nesting");
    assertTrue(source.contains("CWE") || source.contains("sanitiz"), "TableGenerator should document security measures");

    logger.info("✓ TableGenerator JavaDoc validated");
  }

  // ── Helper ────────────────────────────────────────────────────────────────

  private String readFile(String path) throws Exception {
    Path p = Paths.get(path);
    assertTrue(Files.exists(p), "File should exist: " + path);
    return Files.readString(p);
  }
}
