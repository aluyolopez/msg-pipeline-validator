package com.msgpipeline.validator.application.usecase;

import com.msgpipeline.validator.application.validation.factory.ValidatorFactory;
import com.msgpipeline.validator.application.validation.strategy.ValidationResult;
import com.msgpipeline.validator.application.validation.strategy.ValidationStrategy;
import com.msgpipeline.validator.domain.model.MessagePayload;
import com.msgpipeline.validator.domain.port.in.ValidateMessagePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * =========================================================================
 * CLASE: ValidateMessageUseCase -- Caso de Uso de Validacion
 * CAPA: Aplicacion -- Caso de Uso (Application Service)
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * RESPONSABILIDAD (SRP): Orquesta la validacion del mensaje.
 * No sabe de Step Functions, API Gateway ni AWS.
 *
 * FLUJO SESION 07:
 *   ValidatorHandler --> validate(payload)
 *   --> Paso 1: Normalizar messageType (trim + uppercase)
 *   --> Paso 2: Factory: ValidatorFactory.getStrategy(messageType)
 *   --> Paso 3: Strategy: strategy.validate(payload)
 *   --> Retornar ValidationResponse {valida, motivo}
 *
 * PATRONES:
 *   - Use Case (Application Service)
 *   - Factory: selecciona la estrategia correcta
 *   - Strategy: aplica las reglas de negocio especificas del tipo
 *   - DIP: no depende de implementaciones concretas
 * =========================================================================
 */
@Slf4j
@Service
public class ValidateMessageUseCase implements ValidateMessagePort {

    @Override
    public ValidationResponse validate(MessagePayload payload) {
        log.info("Iniciando validacion [messageId={}] [tipo={}]",
                payload.getMessageId(), payload.getMessageType());

        // -- Paso 1: Normalizar messageType ---------------------------------
        if (payload.getMessageType() != null) {
            payload.setMessageType(payload.getMessageType().trim().toUpperCase());
        }

        // -- Paso 2: Seleccionar estrategia (Factory Pattern) ---------------
        ValidationStrategy strategy;
        try {
            strategy = ValidatorFactory.getStrategy(payload.getMessageType());
            log.info("Estrategia seleccionada: {}", strategy.getNombre());
        } catch (IllegalArgumentException e) {
            log.warn("Tipo no soportado: {}", payload.getMessageType());
            return ValidationResponse.invalido(e.getMessage());
        }

        // -- Paso 3: Ejecutar validacion (Strategy Pattern) -----------------
        ValidationResult resultado = strategy.validate(payload);

        if (!resultado.isValid()) {
            String errorMsg = resultado.getFirstError();
            log.warn("Validacion fallida [id={}] [estrategia={}] [error={}]",
                    payload.getMessageId(), strategy.getNombre(), errorMsg);
            return ValidationResponse.invalido(errorMsg);
        }

        log.info("Validacion exitosa [messageId={}] [estrategia={}]",
                payload.getMessageId(), strategy.getNombre());

        return ValidationResponse.valido("Mensaje valido -- tipo: " + payload.getMessageType());
    }
}
