package com.msgpipeline.validator.application.validation.factory;

import com.msgpipeline.validator.application.validation.strategy.NotificationValidator;
import com.msgpipeline.validator.application.validation.strategy.RecordValidator;
import com.msgpipeline.validator.application.validation.strategy.ValidationStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * =========================================================================
 * CAPA: Aplicación — Fábrica de Estrategias de Validación
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * PATRÓN: Factory Method (GoF — Gang of Four)
 *   Encapsula la lógica de creación/selección de ValidationStrategy según
 *   el tipo de mensaje. El Use Case no sabe cuántas estrategias existen
 *   ni cómo se seleccionan — delega completamente en esta fábrica.
 *
 * PATRÓN: Registry (variante del Factory)
 *   Mantiene un registro (STRATEGIES) de todas las estrategias disponibles.
 *   Busca la estrategia correcta consultando strategy.supports(messageType).
 *   Esto es más flexible que un switch/case tradicional.
 *
 * PRINCIPIO: Open/Closed (OCP — SOLID)
 *   Para agregar un nuevo tipo de mensaje:
 *   → Crear nueva clase que implemente ValidationStrategy (Open for extension)
 *   → Agregar la instancia a la lista STRATEGIES (mínima modificación)
 *   → NO modificar getStrategy() ni ValidateMessageUseCase (Closed for modification)
 *
 * PRINCIPIO: Dependency Inversion (DIP — SOLID)
 *   ValidateMessageUseCase depende de ValidationStrategy (abstracción).
 *   ValidatorFactory crea las instancias concretas (NotificationValidator, etc.)
 *   El Use Case nunca importa las implementaciones concretas directamente.
 *
 * THREAD SAFETY:
 *   STRATEGIES es una List inmutable (List.of()) — segura para acceso
 *   concurrente sin sincronización explícita.
 *   Las instancias de estrategia son stateless (sin estado mutable) →
 *   son seguras para compartirse entre múltiples invocaciones Lambda.
 * =========================================================================
 */
@Slf4j
public class ValidatorFactory {

    /**
     * Registro de todas las estrategias de validación disponibles.
     *
     * PATRÓN: Registry — lista centralizada de implementaciones.
     * Las instancias son STATELESS → se pueden compartir sin problemas
     * de concurrencia (múltiples hilos o invocaciones Lambda simultáneas).
     *
     * EXTENSIÓN (Sesiones futuras):
     *   List.of(
     *       new NotificationValidator(),
     *       new RecordValidator(),
     *       new AuditValidator(),   ← Sesión 05
     *       new AlertValidator()    ← Sesión 06
     *   )
     */
    private static final List<ValidationStrategy> STRATEGIES = List.of(
            new NotificationValidator(),
            new RecordValidator()
    );

    /**
     * Obtiene la estrategia de validación apropiada para el tipo de mensaje.
     *
     * Algoritmo:
     *   1. Normaliza el tipo de mensaje (trim + uppercase)
     *   2. Itera sobre STRATEGIES buscando la que soporte el tipo
     *   3. Retorna la primera que lo soporte (supports() retorna true)
     *   4. Lanza IllegalArgumentException si ninguna estrategia lo soporta
     *
     * @param messageType Tipo de mensaje (NOTIFICATION, RECORD, etc.)
     * @return            Estrategia de validación correspondiente
     * @throws IllegalArgumentException si el tipo no está soportado
     */
    public static ValidationStrategy getStrategy(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            throw new IllegalArgumentException(
                    "El tipo de mensaje no puede ser nulo o vacío");
        }

        String tipoNormalizado = messageType.trim().toUpperCase();
        log.debug("Buscando estrategia para tipo: {}", tipoNormalizado);

        return STRATEGIES.stream()
                .filter(strategy -> strategy.supports(tipoNormalizado))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Tipo de mensaje no soportado: {}", tipoNormalizado);
                    return new IllegalArgumentException(
                            "Tipo de mensaje no soportado: '" + messageType
                            + "'. Tipos válidos: NOTIFICATION, RECORD");
                });
    }
}
