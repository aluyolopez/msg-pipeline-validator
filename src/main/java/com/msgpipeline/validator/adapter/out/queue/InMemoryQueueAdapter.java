package com.msgpipeline.validator.adapter.out.queue;

import com.msgpipeline.validator.domain.model.MessagePayload;
import com.msgpipeline.validator.domain.port.out.MessageQueuePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * =========================================================================
 * CAPA: Infraestructura — Adaptador de Salida (Output Adapter)
 * ARQUITECTURA: Hexagonal
 * PERFIL: 'local' (activo en desarrollo local con macOS)
 * =========================================================================
 *
 * PATRÓN: Test Double / Null Object (In-Memory Queue)
 *   Implementación alternativa de MessageQueuePort que almacena los mensajes
 *   en memoria (ConcurrentHashMap). No requiere conexión a AWS ni SQS real.
 *   Permite desarrollar y probar localmente sin credenciales AWS.
 *
 * PATRÓN: Strategy (intercambiable con SqsMessageQueueAdapter)
 *   Ambas implementaciones cumplen el mismo contrato (MessageQueuePort).
 *   Spring selecciona la implementación correcta según el perfil activo.
 *   ValidateMessageUseCase ejecuta exactamente el mismo código en ambos perfiles.
 *
 * PATRÓN: Singleton (implícito por @Component)
 *   Spring crea UNA sola instancia. ConcurrentHashMap garantiza
 *   acceso seguro en entornos multi-hilo de Spring MVC.
 *
 * @Profile("local"):
 *   SOLO activo en el perfil 'local'. En 'aws', Spring usa SqsMessageQueueAdapter.
 *
 * NOTA: Los datos se pierden al reiniciar la aplicación.
 *   Esto es comportamiento esperado — es un repositorio de prueba.
 * =========================================================================
 */
@Slf4j
@Component
@Profile("local")
public class InMemoryQueueAdapter implements MessageQueuePort {

    /**
     * Almacenamiento en memoria: messageId → MessagePayload
     * ConcurrentHashMap: thread-safe para múltiples solicitudes HTTP concurrentes.
     * Los datos se pierden al reiniciar (comportamiento esperado en local).
     */
    private final Map<String, MessagePayload> queue = new ConcurrentHashMap<>();

    /**
     * Simula el envío a SQS almacenando el mensaje en memoria.
     * Genera un ID simulado con el prefijo "local-" para distinguirlo
     * de los IDs reales de SQS en logs y respuestas.
     *
     * @param payload Mensaje validado a "encolar"
     * @return        ID simulado del mensaje (prefijo "local-" + UUID)
     */
    @Override
    public String enqueue(MessagePayload payload) {
        // ID simulado que imita el formato de SQS MessageId
        String simulatedQueueId = "local-" + UUID.randomUUID();

        queue.put(payload.getMessageId(), payload);

        log.info("[IN-MEMORY QUEUE] Mensaje encolado [id={}] [tipo={}] [queueId={}] | Total en cola: {}",
                payload.getMessageId(),
                payload.getMessageType(),
                simulatedQueueId,
                queue.size());

        return simulatedQueueId;
    }

    /**
     * Retorna todos los mensajes "encolados" en memoria.
     * Usado por ValidatorController GET /api/v1/validator/queue (solo perfil 'local').
     * Permite verificar que los mensajes se validaron y encolaron correctamente.
     *
     * @return Lista inmutable de mensajes en cola
     */
    public List<MessagePayload> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(queue.values()));
    }

    /**
     * Retorna la cantidad de mensajes en la cola en memoria.
     * Útil para verificar el estado de la cola en pruebas locales.
     *
     * @return Número de mensajes en la cola
     */
    public int size() {
        return queue.size();
    }
}
