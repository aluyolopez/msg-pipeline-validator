package com.msgpipeline.validator.application.validation.strategy;

import com.msgpipeline.validator.domain.model.MessagePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * =========================================================================
 * CAPA: Aplicación — Estrategia de Validación para NOTIFICATION
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * PATRÓN: Strategy (implementación concreta)
 *   Implementa ValidationStrategy con las reglas específicas de negocio
 *   para mensajes de tipo NOTIFICATION.
 *
 * PRINCIPIO: Single Responsibility (SRP — SOLID)
 *   Solo valida mensajes de tipo NOTIFICATION.
 *   No gestiona persistencia, no conoce SQS, no interactúa con Lambda.
 *
 * PRINCIPIO: Liskov Substitution (LSP — SOLID)
 *   Sustituye correctamente a ValidationStrategy en cualquier contexto.
 *   ValidateMessageUseCase usa esta clase exactamente igual que RecordValidator.
 *
 * REGLAS DE NEGOCIO PARA NOTIFICATION:
 *   ✅ recipientEmail obligatorio y formato válido (contiene '@' y '.')
 *   ✅ channel obligatorio: EMAIL | SMS | PUSH
 *   ✅ content obligatorio, mínimo 10 caracteres, máximo 1000 caracteres
 *   ✅ messageType debe ser "NOTIFICATION" (verificado por supports())
 *
 * EXTENSIÓN (sin modificar esta clase — OCP):
 *   Para agregar una nueva regla: crear una nueva estrategia que delegue
 *   en esta, o agregar aquí si es una regla adicional de NOTIFICATION.
 * =========================================================================
 */
@Slf4j
public class NotificationValidator implements ValidationStrategy {

    /** Canales de entrega válidos para mensajes NOTIFICATION */
    private static final Set<String> CANALES_VALIDOS = Set.of("EMAIL", "SMS", "PUSH");

    /** Longitud mínima del contenido del mensaje */
    private static final int CONTENT_MIN_LENGTH = 10;

    /** Longitud máxima del contenido del mensaje NOTIFICATION */
    private static final int CONTENT_MAX_LENGTH = 1000;

    @Override
    public ValidationResult validate(MessagePayload payload) {
        log.debug("Validando mensaje NOTIFICATION [id={}]", payload.getMessageId());

        List<String> errors = new ArrayList<>();

        // ── Regla 1: recipientEmail obligatorio y formato válido ──────────
        // Validación básica de formato: debe contener '@' y '.' al menos.
        // Para validación más estricta usar jakarta.validation.constraints.Email.
        if (payload.getRecipientEmail() == null || payload.getRecipientEmail().isBlank()) {
            errors.add("El email del destinatario es obligatorio para mensajes NOTIFICATION");
        } else if (!payload.getRecipientEmail().contains("@") ||
                   !payload.getRecipientEmail().contains(".")) {
            errors.add("El email del destinatario no tiene un formato válido: '"
                    + payload.getRecipientEmail() + "'");
        }

        // ── Regla 2: channel obligatorio y dentro del conjunto válido ─────
        if (payload.getChannel() == null || payload.getChannel().isBlank()) {
            errors.add("El canal es obligatorio para mensajes NOTIFICATION");
        } else if (!CANALES_VALIDOS.contains(payload.getChannel().toUpperCase())) {
            errors.add("Canal no válido: '" + payload.getChannel()
                    + "'. Valores permitidos: " + CANALES_VALIDOS);
        }

        // ── Regla 3: content obligatorio con longitudes mínima y máxima ──
        if (payload.getContent() == null || payload.getContent().isBlank()) {
            errors.add("El contenido del mensaje es obligatorio");
        } else {
            int len = payload.getContent().length();
            if (len < CONTENT_MIN_LENGTH) {
                errors.add("El contenido debe tener al menos " + CONTENT_MIN_LENGTH
                        + " caracteres (actual: " + len + ")");
            } else if (len > CONTENT_MAX_LENGTH) {
                errors.add("El contenido no puede exceder " + CONTENT_MAX_LENGTH
                        + " caracteres (actual: " + len + ")");
            }
        }

        if (errors.isEmpty()) {
            log.info("Validación NOTIFICATION exitosa [id={}] [canal={}]",
                    payload.getMessageId(), payload.getChannel());
            return ValidationResult.ok();
        }

        log.warn("Validación NOTIFICATION fallida [id={}] [errores={}]",
                payload.getMessageId(), errors);
        return ValidationResult.errors(errors);
    }

    @Override
    public boolean supports(String messageType) {
        // Comparación insensible a mayúsculas para robustez
        return "NOTIFICATION".equalsIgnoreCase(messageType);
    }

    @Override
    public String getNombre() {
        return "NotificationValidator";
    }
}
