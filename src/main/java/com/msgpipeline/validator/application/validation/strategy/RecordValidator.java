package com.msgpipeline.validator.application.validation.strategy;

import com.msgpipeline.validator.domain.model.MessagePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================
 * CAPA: Aplicación — Estrategia de Validación para RECORD
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * PATRÓN: Strategy (implementación concreta)
 *   Implementa ValidationStrategy con las reglas específicas de negocio
 *   para mensajes de tipo RECORD (registros de auditoría/trazabilidad).
 *
 * PRINCIPIO: Single Responsibility (SRP — SOLID)
 *   Solo valida mensajes de tipo RECORD.
 *   Las reglas de RECORD son independientes de las de NOTIFICATION.
 *
 * REGLAS DE NEGOCIO PARA RECORD:
 *   ✅ content obligatorio (registro que describe el evento)
 *   ✅ content máximo 5000 caracteres (más largo que NOTIFICATION)
 *   ✅ priority obligatorio, rango 1-5 (1=baja, 5=crítica)
 *   ✅ recipientEmail NO es obligatorio (RECORD no tiene destinatario externo)
 *
 * DIFERENCIAS vs NotificationValidator:
 *   - RECORD: prioridad obligatoria (1-5), sin validación de email
 *   - NOTIFICATION: email y canal obligatorios, sin prioridad
 *   El Strategy Pattern encapsula estas diferencias en clases separadas.
 *
 * EXTENSIÓN (OCP — Open/Closed):
 *   Para agregar tipo "AUDIT": crear AuditValidator implements ValidationStrategy
 *   sin modificar RecordValidator ni ValidatorFactory (solo agregar al registro).
 * =========================================================================
 */
@Slf4j
public class RecordValidator implements ValidationStrategy {

    /** Longitud máxima del contenido para mensajes RECORD */
    private static final int CONTENT_MAX_LENGTH = 5000;

    /** Prioridad mínima permitida */
    private static final int PRIORITY_MIN = 1;

    /** Prioridad máxima permitida */
    private static final int PRIORITY_MAX = 5;

    @Override
    public ValidationResult validate(MessagePayload payload) {
        log.debug("Validando mensaje RECORD [id={}]", payload.getMessageId());

        List<String> errors = new ArrayList<>();

        // ── Regla 1: content obligatorio con longitud máxima ─────────────
        // Los RECORD son registros de auditoría — pueden ser más extensos.
        // La longitud máxima (5000) es mayor que en NOTIFICATION (1000).
        if (payload.getContent() == null || payload.getContent().isBlank()) {
            errors.add("El contenido del registro es obligatorio para mensajes RECORD");
        } else if (payload.getContent().length() > CONTENT_MAX_LENGTH) {
            errors.add("El contenido del registro excede el límite de "
                    + CONTENT_MAX_LENGTH + " caracteres (actual: "
                    + payload.getContent().length() + ")");
        }

        // ── Regla 2: priority obligatoria y dentro del rango válido ──────
        // La prioridad determina la urgencia del registro en sistemas de auditoría.
        // Rango 1-5: 1=baja, 2=normal, 3=alta, 4=urgente, 5=crítica.
        if (payload.getPriority() == null) {
            errors.add("La prioridad es obligatoria para mensajes RECORD");
        } else if (payload.getPriority() < PRIORITY_MIN || payload.getPriority() > PRIORITY_MAX) {
            errors.add("La prioridad debe estar entre " + PRIORITY_MIN + " y " + PRIORITY_MAX
                    + " (valor recibido: " + payload.getPriority() + ")");
        }

        if (errors.isEmpty()) {
            log.info("Validación RECORD exitosa [id={}] [prioridad={}]",
                    payload.getMessageId(), payload.getPriority());
            return ValidationResult.ok();
        }

        log.warn("Validación RECORD fallida [id={}] [errores={}]",
                payload.getMessageId(), errors);
        return ValidationResult.errors(errors);
    }

    @Override
    public boolean supports(String messageType) {
        return "RECORD".equalsIgnoreCase(messageType);
    }

    @Override
    public String getNombre() {
        return "RecordValidator";
    }
}
