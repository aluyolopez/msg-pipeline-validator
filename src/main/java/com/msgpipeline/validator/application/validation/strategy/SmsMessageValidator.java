package com.msgpipeline.validator.application.validation.strategy;

import com.msgpipeline.validator.domain.model.MessagePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * =========================================================================
 * CLASE: SmsMessageValidator -- Estrategia para tipo SMS
 * CAPA: Aplicacion -- Strategy Implementation
 * =========================================================================
 *
 * REGLAS PARA SMS:
 *   [R1] channel debe ser SMS
 *   [R2] content obligatorio, max 160 chars (limite SMS estandar)
 * =========================================================================
 */
@Slf4j
public class SmsMessageValidator implements ValidationStrategy {

    private static final Set<String> CANALES_VALIDOS = Set.of("SMS");
    private static final int CONTENT_MAX_SMS = 160;

    @Override
    public ValidationResult validate(MessagePayload payload) {
        log.debug("Validando SMS [id={}]", payload.getMessageId());
        ValidationResult result = ValidationResult.valid();

        // [R1] channel SMS
        if (payload.getChannel() == null || payload.getChannel().isBlank()) {
            result.addError("El canal es obligatorio para mensajes SMS");
        } else if (!CANALES_VALIDOS.contains(payload.getChannel().toUpperCase())) {
            result.addError("Canal no valido: '" + payload.getChannel() + "'. Valor permitido: SMS");
        }

        // [R2] content max 160 chars
        if (payload.getContent() == null || payload.getContent().isBlank()) {
            result.addError("El contenido SMS es obligatorio");
        } else if (payload.getContent().length() > CONTENT_MAX_SMS) {
            result.addError("El contenido SMS no puede superar " + CONTENT_MAX_SMS
                    + " caracteres (tiene " + payload.getContent().length() + ")");
        }

        return result;
    }

    @Override public boolean supports(String messageType) { return "SMS".equals(messageType); }
    @Override public String getNombre() { return "SmsMessageValidator"; }
}
