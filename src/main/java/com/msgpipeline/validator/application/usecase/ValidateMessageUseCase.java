package com.msgpipeline.validator.application.usecase;

import com.msgpipeline.validator.application.validation.factory.ValidatorFactory;
import com.msgpipeline.validator.application.validation.strategy.ValidationResult;
import com.msgpipeline.validator.application.validation.strategy.ValidationStrategy;
import com.msgpipeline.validator.domain.model.MessagePayload;
import com.msgpipeline.validator.domain.port.in.ValidateMessagePort;
import com.msgpipeline.validator.domain.port.out.MessageQueuePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * =========================================================================
 * CAPA: Aplicación — Caso de Uso (Use Case / Application Service)
 * ARQUITECTURA: Hexagonal + Clean Architecture
 * =========================================================================
 *
 * RESPONSABILIDAD ÚNICA (SRP — SOLID):
 *   Orquesta el flujo completo de validación de un mensaje.
 *   No sabe de SQS, no sabe de API Gateway, no sabe de HTTP.
 *   Solo coordina estrategias de validación y el puerto de cola.
 *
 * FLUJO DE PROCESAMIENTO:
 *   1. Enriquecer payload → asignar messageId y createdAt si no vienen
 *   2. Obtener estrategia → ValidatorFactory.getStrategy(messageType) [Factory]
 *   3. Ejecutar validación → strategy.validate(payload) [Strategy]
 *   4. Si VÁLIDO  → encolar en SQS via MessageQueuePort → retornar accepted
 *   5. Si INVÁLIDO → retornar rejected con el error de validación
 *
 * PATRÓN: Use Case (Application Service)
 *   Coordina entidades del dominio para completar un caso de uso de negocio.
 *   En DDD se llamaría "Application Service".
 *
 * PATRÓN: Dependency Injection (@RequiredArgsConstructor + @Service)
 *   Spring inyecta MessageQueuePort. La implementación varía por perfil:
 *     - Perfil 'aws'   → SqsMessageQueueAdapter (SQS real)
 *     - Perfil 'local' → InMemoryQueueAdapter   (memoria para desarrollo)
 *   El Use Case ejecuta el mismo código en ambos perfiles.
 *
 * PATRÓN: Factory + Strategy (delegados a ValidatorFactory)
 *   El Use Case no implementa las reglas de validación — las delega.
 *   Conoce la existencia de ValidatorFactory pero no de las estrategias concretas.
 * =========================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidateMessageUseCase implements ValidateMessagePort {

    /**
     * Puerto de salida — Spring inyecta la implementación correcta según el perfil.
     * Perfil 'aws'   → SqsMessageQueueAdapter
     * Perfil 'local' → InMemoryQueueAdapter
     * El Use Case no sabe cuál es — Dependency Inversion en acción.
     */
    private final MessageQueuePort messageQueuePort;

    @Override
    public ValidationResponse validate(MessagePayload payload) {
        log.info("Iniciando validación [tipo={}]", payload.getMessageType());

        // ── Paso 1: Enriquecer el payload ────────────────────────────────
        //
        // Si el cliente no envió messageId, lo generamos aquí.
        // UUID v4: universalmente único, sin coordinación entre nodos.
        // Esto garantiza que cada mensaje tiene identidad única en DynamoDB.
        //
        enrichPayload(payload);

        log.info("Payload enriquecido [id={}] [tipo={}] [canal={}]",
                payload.getMessageId(), payload.getMessageType(), payload.getChannel());

        // ── Paso 2: Seleccionar estrategia (Factory Pattern) ─────────────
        //
        // ValidatorFactory examina el messageType y retorna la estrategia
        // correcta. Lanza IllegalArgumentException si el tipo no es soportado.
        // El Use Case no sabe si la estrategia es NotificationValidator o RecordValidator.
        //
        ValidationStrategy strategy;
        try {
            strategy = ValidatorFactory.getStrategy(payload.getMessageType());
            log.info("Estrategia seleccionada: {}", strategy.getNombre());
        } catch (IllegalArgumentException e) {
            log.warn("Tipo de mensaje no soportado: {}", payload.getMessageType());
            return ValidationResponse.rejected(e.getMessage());
        }

        // ── Paso 3: Ejecutar validación (Strategy Pattern) ───────────────
        //
        // strategy.validate() aplica las reglas de negocio específicas del tipo.
        // El Use Case no conoce las reglas concretas — principio Open/Closed.
        //
        ValidationResult resultado = strategy.validate(payload);

        if (!resultado.isValid()) {
            // ── Paso 4a: Validación fallida ───────────────────────────────
            //
            // Retornamos el primer error encontrado en la respuesta HTTP.
            // Para un listado completo, resultado.getErrors() contiene todos.
            //
            String errorMsg = resultado.getFirstError();
            log.warn("Validación fallida [id={}] [estrategia={}] [error={}]",
                    payload.getMessageId(), strategy.getNombre(), errorMsg);
            return ValidationResponse.rejected(errorMsg);
        }

        // ── Paso 4b: Validación exitosa — encolar en SQS ─────────────────
        //
        // La validación pasó. Encolamos el mensaje en SQS para que el
        // Lambda Processor lo consuma de forma asíncrona.
        // API Gateway retornará 202 Accepted al cliente — procesamiento asíncrono.
        //
        String queueId = messageQueuePort.enqueue(payload);

        log.info("Mensaje encolado exitosamente [id={}] [queueId={}]",
                payload.getMessageId(), queueId);

        return ValidationResponse.accepted(payload.getMessageId());
    }

    // ── Métodos auxiliares privados ───────────────────────────────────────

    /**
     * Enriquece el payload con valores generados si los campos no vienen en el request.
     *
     * messageId  → UUID v4 si no viene (garantiza unicidad global)
     * createdAt  → ISO-8601 timestamp si no viene (registra el momento de creación)
     * messageType → normalizado a UPPERCASE para comparaciones insensibles
     *
     * @param payload Payload a enriquecer (modificado in-place)
     */
    private void enrichPayload(MessagePayload payload) {
        if (payload.getMessageId() == null || payload.getMessageId().isBlank()) {
            payload.setMessageId(UUID.randomUUID().toString());
        }
        if (payload.getCreatedAt() == null || payload.getCreatedAt().isBlank()) {
            payload.setCreatedAt(Instant.now().toString());
        }
        if (payload.getMessageType() != null) {
            payload.setMessageType(payload.getMessageType().trim().toUpperCase());
        }
    }
}
