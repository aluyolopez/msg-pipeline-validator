package com.msgpipeline.validator.application.validation.strategy;

import com.msgpipeline.validator.domain.model.MessagePayload;

/**
 * =========================================================================
 * CAPA: Aplicación — Estrategia de Validación (Strategy Interface)
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * PATRÓN: Strategy (GoF — Gang of Four)
 *   Define una familia de algoritmos de validación, encapsula cada uno
 *   y los hace intercambiables. Las estrategias varían independientemente
 *   de los clientes que las usan (ValidateMessageUseCase).
 *
 * PRINCIPIO: Interface Segregation (ISP — SOLID)
 *   Interfaz pequeña y cohesiva — solo dos métodos específicos.
 *
 * PRINCIPIO: Open/Closed (OCP — SOLID)
 *   Para agregar un nuevo tipo de mensaje:
 *   → Crear nueva clase que implemente ValidationStrategy
 *   → Registrarla en ValidatorFactory
 *   → NO modificar ValidateMessageUseCase ni esta interfaz
 *
 * PRINCIPIO: Liskov Substitution (LSP — SOLID)
 *   Cualquier implementación puede sustituir a esta interfaz
 *   sin alterar el comportamiento correcto del sistema.
 *
 * IMPLEMENTACIONES ACTUALES (Sesión 04):
 *   - NotificationValidator : messageType = "NOTIFICATION"
 *   - RecordValidator       : messageType = "RECORD"
 *
 * FUTURAS EXTENSIONES (sin modificar código existente):
 *   - AuditValidator        : messageType = "AUDIT"
 *   - AlertValidator        : messageType = "ALERT"
 * =========================================================================
 */
public interface ValidationStrategy {

    /**
     * Ejecuta las reglas de validación específicas del tipo de mensaje.
     *
     * @param payload Mensaje a validar (con todos sus campos)
     * @return        ValidationResult con el estado y los errores encontrados
     */
    ValidationResult validate(MessagePayload payload);

    /**
     * Indica si esta estrategia puede manejar el tipo de mensaje dado.
     * Usado por ValidatorFactory para seleccionar la estrategia correcta.
     *
     * @param messageType Tipo de mensaje (NOTIFICATION, RECORD, etc.)
     * @return            true si esta estrategia maneja ese tipo
     */
    boolean supports(String messageType);

    /**
     * Nombre descriptivo de la estrategia (para logging y debugging).
     *
     * @return Nombre de la estrategia (ej: "NotificationValidator")
     */
    String getNombre();
}
