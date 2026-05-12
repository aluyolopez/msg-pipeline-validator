package com.msgpipeline.validator.domain.port.in;

import com.msgpipeline.validator.domain.model.MessagePayload;
import lombok.Builder;
import lombok.Value;

/**
 * =========================================================================
 * INTERFAZ: ValidateMessagePort -- Puerto de Entrada del Validator
 * CAPA: Dominio -- Puerto de Entrada (Input Port)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * Define el contrato del caso de uso de validacion.
 * ValidatorHandler (Lambda/Step Functions) y ValidatorController (local)
 * usan esta interfaz para interactuar con la logica de negocio.
 *
 * DIAGRAMA:
 *   [Step Functions Task]    [HTTP Request local]
 *          |                         |
 *   [ValidatorHandler]     [ValidatorController]
 *          |                         |
 *          +----------+-------------+
 *                     |
 *           [ValidateMessagePort]  <-- esta interfaz
 *                     |
 *           [ValidateMessageUseCase]
 *                     |
 *           [ValidatorFactory -> Strategy]
 *
 * PRINCIPIO DIP (SOLID): adaptadores dependen de esta abstraccion.
 * =========================================================================
 */
public interface ValidateMessagePort {

    /**
     * Valida el mensaje segun su tipo usando Strategy + Factory.
     *
     * RESPONSABILIDADES:
     *   1. Normalizar messageType (trim + uppercase)
     *   2. Obtener estrategia: ValidatorFactory.getStrategy(messageType)
     *   3. Ejecutar validacion: strategy.validate(payload)
     *   4. Retornar ValidationResponse con {valida, motivo}
     *
     * @param payload Mensaje del input de Step Functions
     * @return        Resultado de la validacion
     */
    ValidationResponse validate(MessagePayload payload);

    /**
     * Value Object con el resultado de la validacion.
     *
     * PATRON: Value Object (DDD) -- inmutable, sin identidad propia.
     * Step Functions usa $.validacion.valida en el Choice state.
     */
    @Value
    @Builder
    class ValidationResponse {
        boolean valida;
        String  motivo;

        /** Factory method para resultado valido */
        public static ValidationResponse valido(String motivo) {
            return ValidationResponse.builder().valida(true).motivo(motivo).build();
        }

        /** Factory method para resultado invalido */
        public static ValidationResponse invalido(String motivo) {
            return ValidationResponse.builder().valida(false).motivo(motivo).build();
        }
    }
}
