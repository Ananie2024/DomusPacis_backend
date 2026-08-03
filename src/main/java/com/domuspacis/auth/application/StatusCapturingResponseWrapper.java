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
    public void sendRedirect(String location) {
        // A redirect is a 302 (Found).  With a proper AuthenticationEntryPoint
        // configured, Spring Security no longer uses sendRedirect for auth
        // failures — it writes a JSON 401 directly.  Capture the real status.
        this.httpStatus = HttpServletResponse.SC_FOUND;
        try {
            super.sendRedirect(location);
        } catch (java.io.IOException e) {
            // Ignore – we only care about capturing the status
        }
    }

    @Override
    public int getStatus() {
        return this.httpStatus;
    }
}