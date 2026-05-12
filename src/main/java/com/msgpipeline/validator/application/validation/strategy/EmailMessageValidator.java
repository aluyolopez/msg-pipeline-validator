package com.msgpipeline.validator.application.validation.strategy;

import com.msgpipeline.validator.domain.model.MessagePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * =========================================================================
 * CLASE: EmailMessageValidator -- Estrategia para tipo EMAIL
 * CAPA: Aplicacion -- Strategy Implementation
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * PATRON: Strategy (implementacion concreta para EMAIL)
 *
 * REGLAS DE NEGOCIO PARA EMAIL:
 *   [R1] recipientEmail obligatorio con formato valido (regex)
 *   [R2] channel debe ser EMAIL
 *   [R3] content obligatorio, minimo 10 chars, maximo 1000 chars
 * =========================================================================
 */
@Slf4j
public class EmailMessageValidator implements ValidationStrategy {

    private static final Set<String>   CANALES_VALIDOS = Set.of("EMAIL");
    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int CONTENT_MIN = 10;
    private static final int CONTENT_MAX = 1000;

    @Override
    public ValidationResult validate(MessagePayload payload) {
        log.debug("Validando EMAIL [id={}]", payload.getMessageId());
        ValidationResult result = ValidationResult.valid();

        // [R1] recipientEmail obligatorio y con formato valido
        if (payload.getRecipientEmail() == null || payload.getRecipientEmail().isBlank()) {
            result.addError("El email del destinatario es obligatorio para mensajes EMAIL");
        } else if (!EMAIL_REGEX.matcher(payload.getRecipientEmail()).matches()) {
            result.addError("El email del destinatario no tiene formato valido: '"
                    + payload.getRecipientEmail() + "'");
        }

        // [R2] channel EMAIL
        if (payload.getChannel() == null || payload.getChannel().isBlank()) {
            result.addError("El canal es obligatorio para mensajes EMAIL");
        } else if (!CANALES_VALIDOS.contains(payload.getChannel().toUpperCase())) {
            result.addError("Canal no valido: '" + payload.getChannel()
                    + "'. Valor permitido: EMAIL");
        }

        // [R3] content con longitudes minima y maxima
        validateContent(payload, result, CONTENT_MIN, CONTENT_MAX);

        return result;
    }

    @Override public boolean supports(String messageType) { return "EMAIL".equals(messageType); }
    @Override public String getNombre() { return "EmailMessageValidator"; }

    private void validateContent(MessagePayload p, ValidationResult r, int min, int max) {
        if (p.getContent() == null || p.getContent().isBlank()) {
            r.addError("El contenido del mensaje es obligatorio");
        } else {
            int len = p.getContent().length();
            if (len < min) r.addError("El contenido debe tener al menos " + min + " caracteres (tiene " + len + ")");
            if (len > max) r.addError("El contenido no puede superar " + max + " caracteres (tiene " + len + ")");
        }
    }
}
