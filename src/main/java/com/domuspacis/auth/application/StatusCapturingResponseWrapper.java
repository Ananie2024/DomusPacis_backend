package com.domuspacis.auth.application;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * Wraps an {@link HttpServletResponse} to capture the HTTP status code
 * set by downstream filters/servlets.
 */
public class StatusCapturingResponseWrapper extends HttpServletResponseWrapper {

    private int httpStatus = SC_OK;

    public StatusCapturingResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void setStatus(int status) {
        this.httpStatus = status;
        super.setStatus(status);
    }

    @Override
    public void sendError(int status) {
        this.httpStatus = status;
        try {
            super.sendError(status);
        } catch (java.io.IOException e) {
            // Ignore – we only care about capturing the status
        }
    }

    @Override
    public void sendError(int status, String message) {
        this.httpStatus = status;
        try {
            super.sendError(status, message);
        } catch (java.io.IOException e) {
            // Ignore – we only care about capturing the status
        }
    }

    @Override
    public int getStatus() {
        return this.httpStatus;
    }
}