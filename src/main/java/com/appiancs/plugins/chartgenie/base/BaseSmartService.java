package com.appiancs.plugins.chartgenie.base;

import java.util.Objects;

import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancs.plugins.chartgenie.dto.ServiceResult;

/**
 * An abstract base class for all smart services in this plugin.
 * It centralises common functionality, such as error handling,
 * and standard output parameters (errorOccurred, errorMessage).
 */

public abstract class BaseSmartService extends AppianSmartService {

  // These outputs are common to all smart services
  protected boolean errorOccurred = false;
  protected String errorMessage = "";

  protected final transient ContentService cs;
  private final Logger log;

  public BaseSmartService(ContentService cs, Logger log) {
    super();
    this.cs = Objects.requireNonNull(cs, "ContentService cannot be null");
    this.log = log;
  }

  protected void handleResult(ServiceResult<?> result) {
      if (result == null) {
          handleError(new IllegalStateException("Service returned null result."), "Unknown Error");
          return;
      }

      if (result.isSuccess()) {
          this.errorOccurred = false;
          this.errorMessage = null;
      } else {
          // Log as warning so it shows in logs but doesn't crash the node
          log.warn("Logic Failure: " + result.getErrorMessage());
          this.errorOccurred = true;
          this.errorMessage = result.getErrorMessage();
      }
  }

    protected void handleError(Exception e, String contextMessage) {
        String finalMsg = contextMessage + ": " + e.getMessage();
        log.error(finalMsg, e);
        this.errorOccurred = true;
        this.errorMessage = finalMsg;
    }

    @Name("ErrorOccurred")
    public boolean isErrorOccurred() {
        return errorOccurred;
    }

    @Name("ErrorMessage")
    public String getErrorMessage() {
        return errorMessage;
    }

}
