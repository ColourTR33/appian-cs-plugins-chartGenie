package com.appiancs.plugins.chartgenie.base;

import java.io.Serializable;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancs.plugins.chartgenie.dto.ServiceResult;

public abstract class BaseSmartService extends AppianSmartService implements Serializable {

  private static final long serialVersionUID = 1L;

  protected final transient ContentService contentService;

  protected final transient Logger log;

  private boolean errorOccurred;
  private String errorMessage;

  public BaseSmartService(ContentService contentService) {
    super();
    this.contentService = Objects.requireNonNull(contentService, "ContentService cannot be null");

    // FIXED: Correct SLF4J initialization for subclasses
    this.log = LoggerFactory.getLogger(this.getClass());

    this.errorOccurred = false;
    this.errorMessage = "";
  }

  protected void handleResult(ServiceResult<?> result) {
    if (result == null) {
      handleException(new IllegalStateException("Service returned null result."), "System Error");
      return;
    }

    if (result.isSuccess()) {
      this.errorOccurred = false;
      this.errorMessage = null;
      // FIXED: SLF4J uses curly braces {} for parameters, which is more efficient
      log.debug("Operation completed successfully.");
    } else {
      log.warn("Business Logic Failure: {}", result.getErrorMessage());
      this.errorOccurred = true;
      this.errorMessage = result.getErrorMessage();
    }
  }

  protected void handleException(Exception exception, String contextMessage) {
    // SECURITY FIX: Sanitize user input to prevent CWE-117/93 log injection
    String sanitizedContext = sanitizeForLogging(contextMessage);
    String cleanMessage = exception.getMessage() != null ? sanitizeForLogging(exception.getMessage())
      : exception.getClass().getSimpleName();

    // Use parameterized logging to prevent injection
    log.error("Error in {}: {}", sanitizedContext, cleanMessage, exception);

    // Store sanitized message for output
    String finalMsg = String.format("%s: %s", sanitizedContext, cleanMessage);
    this.errorOccurred = true;
    this.errorMessage = finalMsg;
  }

  /**
   * Sanitizes input for logging to prevent CWE-117 (Log Injection) and CWE-93 (CRLF Injection) attacks.
   * Removes control characters, line breaks, and other potentially dangerous characters.
   * 
   * @param input
   *          The input string to sanitize
   * @return Sanitized string safe for logging
   */
  protected String sanitizeForLogging(String input) {
    if (input == null) {
      return "null";
    }

    // Limit input length to prevent log flooding
    if (input.length() > 500) {
      input = input.substring(0, 500) + "[TRUNCATED]";
    }

    // Remove CRLF characters and control characters that could be used for log injection
    return input.replaceAll("[\\r\\n\\t]", "_")
      .replaceAll("[\\p{Cntrl}]", "")
      .replaceAll("[\\x00-\\x1F\\x7F]", "")
      .trim();
  }

  @Input(required = Required.OPTIONAL)
  public boolean isErrorOccurred() {
    return errorOccurred;
  }

  @Input(required = Required.OPTIONAL)
  public String getErrorMessage() {
    return errorMessage;
  }
}