package com.msgpipeline.validator.application.validation.factory;

import com.msgpipeline.validator.application.validation.strategy.EmailMessageValidator;
import com.msgpipeline.validator.application.validation.strategy.PushMessageValidator;
import com.msgpipeline.validator.application.validation.strategy.SmsMessageValidator;
import com.msgpipeline.validator.application.validation.strategy.ValidationStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * =========================================================================
 * CLASE: ValidatorFactory -- Fabrica de Estrategias de Validacion
 * CAPA: Aplicacion -- Factory
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * PATRON: Factory Method (GoF)
 *   Encapsula la seleccion de ValidationStrategy segun el tipo de mensaje.
 *   ValidateMessageUseCase no sabe cuantas estrategias existen.
 *
 * PATRON: Registry (variante del Factory)
 *   STRATEGIES contiene el registro de todas las estrategias disponibles.
 *   Selecciona la correcta consultando strategy.supports(messageType).
 *
 * PRINCIPIO OCP (SOLID): Para agregar nuevo tipo:
 *   1. Crear nueva clase que implemente ValidationStrategy
 *   2. Agregar instancia a la lista STRATEGIES
 *   3. NO modificar getStrategy() ni ValidateMessageUseCase
 *
 * THREAD SAFETY: STRATEGIES es inmutable (List.of) -- segura para Lambda.
 *
 * TIPOS SOPORTADOS EN SESION 07:
 *   EMAIL | SMS | PUSH_NOTIFICATION
 * =========================================================================
 */
@Slf4j
public class ValidatorFactory {

    /**
     * Registro de todas las estrategias de validacion disponibles.
     * STATELESS: las estrategias no tienen estado -- compartibles en Lambda.
     */
    private static final List<ValidationStrategy> STRATEGIES = List.of(
            new EmailMessageValidator(),
            new SmsMessageValidator(),
            new PushMessageValidator()
    );

    /**
     * Obtiene la estrategia para el tipo de mensaje dado.
     *
     * @param messageType EMAIL | SMS | PUSH_NOTIFICATION
     * @return            Estrategia correspondiente
     * @throws IllegalArgumentException si el tipo no esta soportado
     */
    public static ValidationStrategy getStrategy(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            throw new IllegalArgumentException("El tipo de mensaje no puede ser nulo o vacio");
        }

        String tipo = messageType.trim().toUpperCase();
        log.debug("Buscando estrategia para tipo: {}", tipo);

        return STRATEGIES.stream()
                .filter(s -> s.supports(tipo))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Tipo no soportado: {}", tipo);
                    return new IllegalArgumentException(
                            "Tipo no soportado: '" + messageType
                            + "'. Tipos validos: EMAIL, SMS, PUSH_NOTIFICATION");
                });
    }
}
