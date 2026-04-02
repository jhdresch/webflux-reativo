

package com.br.webfluxreativo.controller.handler;

import com.br.webfluxreativo.model.dtos.ValidationError;

import java.time.Instant;
import java.util.List;

/**
 * Error payload following RFC7807 (Problem Details) with optional validation details.
 */
public record ErrorResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        Instant timestamp,
        List<ValidationError> errors
) {

    public ErrorResponse(String type, String title, int status, String detail, String instance) {
        this(type, title, status, detail, instance, Instant.now(), null);
    }

    public ErrorResponse(String type, String title, int status, String detail, String instance, List<ValidationError> errors) {
        this(type, title, status, detail, instance, Instant.now(), errors);
    }

}

