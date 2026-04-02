package com.br.webfluxreativo.model.dtos;

/**
 * Represents a field-level validation error.
 */
public record ValidationError(String field, String message) {

}

