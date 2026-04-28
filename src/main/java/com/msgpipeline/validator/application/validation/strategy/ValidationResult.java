package com.msgpipeline.validator.application.validation.strategy;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * =========================================================================
 * CAPA: Aplicación — Resultado de Validación (Value Object)
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * PATRÓN: Value Object (DDD — Domain Driven Design)
 *   Objeto inmutable que no tiene identidad propia.
 *   Dos ValidationResult con los mismos valores son equivalentes.
 *   No cambia de estado — se crea uno nuevo en cada validación.
 *
 * PRINCIPIO: Single Responsibility (SRP — SOLID)
 *   Solo representa el resultado de una validación.
 *   No ejecuta lógica de negocio ni interactúa con AWS.
 *
 * USO:
 *   // Resultado exitoso
 *   return ValidationResult.ok();
 *
 *   // Resultado con un error
 *   return ValidationResult.error("El email es obligatorio");
 *
 *   // Resultado con múltiples errores
 *   List<String> errores = new ArrayList<>();
 *   errores.add("El email es obligatorio");
 *   errores.add("El contenido debe tener al menos 10 caracteres");
 *   return ValidationResult.errors(errores);
 *
 *   // Combinar dos resultados (patrón Composite)
 *   return result1.merge(result2);
 * =========================================================================
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationResult {

    /** true si el mensaje pasó todas las validaciones */
    private final boolean valid;

    /** Lista de errores de validación (vacía si valid=true) */
    private final List<String> errors;

    // ── Factory Methods ───────────────────────────────────────────────────

    /**
     * Resultado exitoso — sin errores de validación.
     *
     * @return ValidationResult válido
     */
    public static ValidationResult ok() {
        return new ValidationResult(true, Collections.emptyList());
    }

    /**
     * Resultado con un único error de validación.
     *
     * @param error Descripción del error encontrado
     * @return      ValidationResult inválido con un error
     */
    public static ValidationResult error(String error) {
        return new ValidationResult(false, List.of(error));
    }

    /**
     * Resultado con múltiples errores de validación.
     * Útil cuando una sola estrategia valida múltiples campos.
     *
     * @param errors Lista de errores encontrados
     * @return       ValidationResult inválido con todos los errores
     */
    public static ValidationResult errors(List<String> errors) {
        return new ValidationResult(false, Collections.unmodifiableList(new ArrayList<>(errors)));
    }

    // ── Métodos de utilidad ───────────────────────────────────────────────

    /**
     * Combina dos resultados de validación.
     *
     * PATRÓN: Composite — combina resultados parciales en uno final.
     * Reglas:
     *   - Si ambos son válidos → resultado válido
     *   - Si alguno falla → resultado inválido con TODOS los errores acumulados
     *
     * Útil para combinar resultados de múltiples validaciones independientes.
     *
     * @param other Otro resultado de validación a combinar
     * @return      Resultado combinado
     */
    public ValidationResult merge(ValidationResult other) {
        if (this.valid && other.valid) {
            return ValidationResult.ok();
        }
        List<String> allErrors = new ArrayList<>(this.errors);
        allErrors.addAll(other.errors);
        return ValidationResult.errors(allErrors);
    }

    /**
     * Retorna el primer error de la lista, o null si no hay errores.
     * Conveniente para respuestas que solo reportan un error a la vez.
     *
     * @return Primer mensaje de error, o null si valid=true
     */
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }

    @Override
    public String toString() {
        return valid
                ? "ValidationResult{OK}"
                : "ValidationResult{FAIL, errors=" + errors + "}";
    }
}
