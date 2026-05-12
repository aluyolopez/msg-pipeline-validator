package com.msgpipeline.validator.application.validation.strategy;

import com.msgpipeline.validator.domain.model.MessagePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * =========================================================================
 * CLASE: PushMessageValidator -- Estrategia para tipo PUSH_NOTIFICATION
 * CAPA: Aplicacion -- Strategy Implementation
 * =========================================================================
 *
 * REGLAS PARA PUSH_NOTIFICATION:
 *   [R1] channel debe ser PUSH
 *   [R2] content obligatorio, max 256 chars
 * =========================================================================
 */
@Slf4j
public class PushMessageValidator implements ValidationStrategy {

    private static final Set<String> CANALES_VALIDOS = Set.of("PUSH");
    private static final int CONTENT_MAX = 256;

    @Override
    public ValidationResult validate(MessagePayload payload) {
        log.debug("Validando PUSH_NOTIFICATION [id={}]", payload.getMessageId());
        ValidationResult result = ValidationResult.valid();

        // [R1] channel PUSH
        if (payload.getChannel() == null || payload.getChannel().isBlank()) {
            result.addError("El canal es obligatorio para PUSH_NOTIFICATION");
        } else if (!CANALES_VALIDOS.contains(payload.getChannel().toUpperCase())) {
            result.addError("Canal no valido: '" + payload.getChannel() + "'. Valor permitido: PUSH");
        }

        // [R2] content max 256 chars
        if (payload.getContent() == null || payload.getContent().isBlank()) {
            result.addError("El contenido de la notificacion push es obligatorio");
        } else if (payload.getContent().length() > CONTENT_MAX) {
            result.addError("El contenido PUSH no puede superar " + CONTENT_MAX
                    + " caracteres (tiene " + payload.getContent().length() + ")");
        }

        return result;
    }

    @Override public boolean supports(String t) { return "PUSH_NOTIFICATION".equals(t) || "PUSH".equals(t); }
    @Override public String getNombre() { return "PushMessageValidator"; }
}
