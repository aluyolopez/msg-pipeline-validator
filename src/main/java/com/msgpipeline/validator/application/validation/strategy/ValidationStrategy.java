package com.msgpipeline.validator.application.validation.strategy;

import com.msgpipeline.validator.domain.model.MessagePayload;

/**
 * =========================================================================
 * INTERFAZ: ValidationStrategy -- Estrategia de Validacion
 * CAPA: Aplicacion -- Strategy Interface
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * PATRON: Strategy (GoF)
 *   Familia de algoritmos de validacion intercambiables segun messageType.
 *
 * PRINCIPIO OCP (SOLID): Para nuevo tipo de mensaje:
 *   1. Crear nueva clase que implemente ValidationStrategy
 *   2. Registrarla en ValidatorFactory
 *   3. NO modificar ValidateMessageUseCase ni esta interfaz
 *
 * IMPLEMENTACIONES (Sesion 07):
 *   - EmailMessageValidator    --> messageType = EMAIL
 *   - SmsMessageValidator      --> messageType = SMS
 *   - PushMessageValidator     --> messageType = PUSH_NOTIFICATION
 * =========================================================================
 */
public interface ValidationStrategy {

    /**
     * Ejecuta las reglas de validacion del tipo de mensaje.
     *
     * @param payload Mensaje con todos sus campos
     * @return        ValidationResult con estado y errores
     */
    ValidationResult validate(MessagePayload payload);

    /**
     * Indica si esta estrategia maneja el tipo de mensaje dado.
     * Usado por ValidatorFactory para seleccionar la estrategia.
     *
     * @param messageType Tipo: EMAIL | SMS | PUSH_NOTIFICATION
     * @return            true si esta estrategia lo maneja
     */
    boolean supports(String messageType);

    /** Nombre descriptivo de la estrategia para logging */
    String getNombre();
}
