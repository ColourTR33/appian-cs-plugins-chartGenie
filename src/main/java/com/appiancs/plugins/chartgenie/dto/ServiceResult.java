package com.appiancs.plugins.chartgenie.dto;

/**
 * Generic result wrapper.
 * T is the type of the main output (e.g., Long for DocId, String for Text).
 */
public class ServiceResult<T> {
    private final T data;
    private final boolean success;
    private final String errorMessage;

    // Success Constructor
    public ServiceResult(T data) {
        this.data = data;
        this.success = true;
        this.errorMessage = null;
    }

    // Failure Constructor
    public ServiceResult(String errorMessage) {
        this.data = null;
        this.success = false;
        this.errorMessage = errorMessage;
    }

    public T getData() { return data; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
}
