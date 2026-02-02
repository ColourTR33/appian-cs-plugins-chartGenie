package com.appiancs.plugins.chartgenie.base;

import java.io.Serializable;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancs.plugins.chartgenie.dto.ServiceResult;

/**
 * An abstract base class for all smart services in this plugin.
 * <p>
 * It centralises common functionality, such as error handling,
 * serialization safety, and standard output parameters.
 * </p>
 */
public abstract class BaseSmartService extends AppianSmartService implements Serializable {

  private static final long serialVersionUID = 1L;

  // -- Services --
  // Transient is required so Appian doesn't try to serialize these services
  // when the process pauses/checkpoints.
  protected final transient ContentService contentService;
  protected final transient Logger log;

  // -- Outputs --
  private boolean errorOccurred;
  private String errorMessage;

  /**
   * Constructor that initialises the base service dependencies.
   *
   * @param contentService
   *          The Appian ContentService (must not be null).
   */
  public BaseSmartService(ContentService contentService) {
    super();
    this.contentService = Objects.requireNonNull(contentService, "ContentService cannot be null");

    // Initialize the logger specifically for the SUBCLASS (e.g. GenerateChartReport)
    // This ensures logs show the correct class name, not "BaseSmartService".
    this.log = Logger.getLogger(this.getClass());

    // Initialize defaults
    this.errorOccurred = false;
    this.errorMessage = "";
  }

  /**
   * Standard handler for domain-specific ServiceResults.
   *
   * @param result
   *          The result object returned by the internal service layer.
   */
  protected void handleResult(ServiceResult<?> result) {
    if (result == null) {
      handleException(new IllegalStateException("Service returned null result."), "System Error");
      return;
    }

    if (result.isSuccess()) {
      this.errorOccurred = false;
      this.errorMessage = null;
      if (log.isDebugEnabled()) {
        log.debug("Operation completed successfully.");
      }
    } else {
      // Logic Failure (e.g., "Template not found") - Log as WARN, not ERROR
      log.warn("Business Logic Failure: " + result.getErrorMessage());
      this.errorOccurred = true;
      this.errorMessage = result.getErrorMessage();
    }
  }

  /**
   * Standard handler for unexpected Exceptions.
   *
   * @param exception
   *          The exception caught.
   * @param contextMessage
   *          A brief description of what was happening.
   */
  protected void handleException(Exception exception, String contextMessage) {
    String cleanMessage = exception.getMessage() != null ? exception.getMessage() : exception.toString();
    String finalMsg = String.format("%s: %s", contextMessage, cleanMessage);

    // Log full stack trace for Admins (ERROR level)
    log.error(finalMsg, exception);

    // Set user-friendly outputs for the Process Designer
    this.errorOccurred = true;
    this.errorMessage = finalMsg;
  }

  // -- Standard Appian Outputs --

  /**
   * Returns whether an error occurred during execution.
   * * @return true if an error occurred.
   */
  @Input(required = Required.OPTIONAL)
  public boolean isErrorOccurred() {
    return errorOccurred;
  }

  /**
   * Returns the error message if one exists.
   * * @return The error message string.
   */
  @Input(required = Required.OPTIONAL)
  public String getErrorMessage() {
    return errorMessage;
  }
}