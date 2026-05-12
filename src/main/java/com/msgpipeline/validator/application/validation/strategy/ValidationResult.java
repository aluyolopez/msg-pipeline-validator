package com.msgpipeline.validator.application.validation.strategy;

import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================
 * CLASE: ValidationResult -- Resultado de Validacion
 * CAPA: Aplicacion -- Value Object
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Encapsula el resultado de una ValidationStrategy.
 * Puede contener multiples errores de validacion.
 * =========================================================================
 */
public class ValidationResult {

    private final List<String> errors;

    private ValidationResult(List<String> errors) { this.errors = errors; }

    public static ValidationResult valid() { return new ValidationResult(new ArrayList<>()); }

    public static ValidationResult invalid(String error) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return new ValidationResult(errors);
    }

    public void addError(String error) { errors.add(error); }

    public boolean isValid() { return errors.isEmpty(); }

    public String getFirstError() { return errors.isEmpty() ? null : errors.get(0); }

    public List<String> getErrors() { return List.copyOf(errors); }
}
