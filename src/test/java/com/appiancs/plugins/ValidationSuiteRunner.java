package com.appiancs.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test Runner for ChartGenie Comprehensive Validation Suite.
 * Usage (from IDE or Gradle):
 * gradle test --tests com.appiancs.plugins.ComprehensiveValidationSuite
 * gradle test --tests com.appiancs.plugins.chartgenie.FontControlValidationTest
 * Or run individual phase classes directly via Gradle test filter:
 * gradle test --tests com.appiancs.plugins.MemoryBankValidationTest
 * gradle test --tests com.appiancs.plugins.SecurityValidationTest
 * gradle test --tests com.appiancs.plugins.IntegrationValidationTest
 * gradle test --tests com.appiancs.plugins.DocumentationValidationTest
 * gradle test --tests com.appiancs.plugins.PerformanceValidationTest
 * gradle test --tests com.appiancs.plugins.EndToEndValidationTest
 */
public class ValidationSuiteRunner {

  private static final Logger logger = LoggerFactory.getLogger(ValidationSuiteRunner.class);

  public static void main(String[] args) {
    logger.info("=== ChartGenie Validation Suite Runner ===");
    logger.info("Run tests via Gradle:");
    logger.info("  All phases:    gradlew test --tests com.appiancs.plugins.ComprehensiveValidationSuite");
    logger.info("  Font Control:  gradlew test --tests com.appiancs.plugins.chartgenie.FontControlValidationTest");
    logger.info("  Memory Bank:   gradlew test --tests com.appiancs.plugins.MemoryBankValidationTest");
    logger.info("  Security:      gradlew test --tests com.appiancs.plugins.SecurityValidationTest");
    logger.info("  Integration:   gradlew test --tests com.appiancs.plugins.IntegrationValidationTest");
    logger.info("  Documentation: gradlew test --tests com.appiancs.plugins.DocumentationValidationTest");
    logger.info("  Performance:   gradlew test --tests com.appiancs.plugins.PerformanceValidationTest");
    logger.info("  End-to-End:    gradlew test --tests com.appiancs.plugins.EndToEndValidationTest");
  }
}
