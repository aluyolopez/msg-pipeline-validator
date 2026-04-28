package com.msgpipeline.validator.adapter.out.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msgpipeline.validator.domain.model.MessagePayload;
import com.msgpipeline.validator.domain.port.out.MessageQueuePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/**
 * =========================================================================
 * CAPA: Infraestructura — Adaptador de Salida (Output Adapter)
 * ARQUITECTURA: Hexagonal (Ports & Adapters)
 * PERFIL: 'aws' (activo en AWS Lambda)
 * =========================================================================
 *
 * PATRÓN: Adapter (GoF — Gang of Four)
 *   Esta clase ADAPTA el contrato del dominio (MessageQueuePort)
 *   al API del AWS SDK v2 (SqsClient.sendMessage()).
 *   El dominio nunca importa SqsClient directamente.
 *
 * PATRÓN: Dependency Inversion (DIP — SOLID)
 *   La clase implementa MessageQueuePort (interfaz del dominio).
 *   El dominio define el contrato; la infraestructura lo cumple.
 *   ValidateMessageUseCase nunca importa SqsMessageQueueAdapter directamente.
 *
 * @Profile("aws"):
 *   SOLO se registra en el perfil 'aws'. En 'local', Spring usa InMemoryQueueAdapter.
 *   Mecanismo de Inversión de Control por perfil — el Use Case ejecuta el mismo
 *   código en ambos perfiles gracias a la abstracción MessageQueuePort.
 *
 * VARIABLE DE ENTORNO REQUERIDA EN LAMBDA:
 *   SQS_QUEUE_URL = https://sqs.us-east-1.amazonaws.com/{accountId}/msg-pipeline-queue
 *
 * CREDENCIALES:
 *   SqsClient se crea en SqsConfig.java con DefaultCredentialsProvider.
 *   En Lambda, usa el IAM Role automáticamente (sin access keys).
 * =========================================================================
 */
@Slf4j
@Component
@Profile("aws")
public class SqsMessageQueueAdapter implements MessageQueuePort {

    /** Cliente SQS inyectado desde SqsConfig (bean @Profile("aws")) */
    private final SqsClient sqsClient;

    /** Jackson para serializar el payload a JSON antes de enviarlo a SQS */
    private final ObjectMapper objectMapper;

    /** URL de la cola SQS — inyectada desde application.yml / variable de entorno */
    @Value("${app.aws.sqs-queue-url}")
    private String sqsQueueUrl;

    public SqsMessageQueueAdapter(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient    = sqsClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Serializa el payload a JSON y lo envía a la cola SQS.
     *
     * SQS SendMessage:
     *   - El body del mensaje SQS es un String (no JSON nativo).
     *   - Serializamos el MessagePayload a JSON String para que el
     *     Lambda Processor pueda deserializarlo con Jackson.
     *   - SQS garantiza entrega at-least-once (el procesador debe ser idempotente).
     *
     * FLUJO:
     *   MessagePayload → Jackson.writeValueAsString() → String JSON
     *   → SQS SendMessageRequest → SQS Queue → Lambda Processor (SqsHandler)
     *
     * @param payload Mensaje validado a encolar
     * @return        MessageId asignado por SQS (GUID único de la cola)
     */
    @Override
    public String enqueue(MessagePayload payload) {
        log.info("Encolando mensaje en SQS [id={}] [tipo={}] [cola={}]",
                payload.getMessageId(), payload.getMessageType(), sqsQueueUrl);

        try {
            // ── Serializar payload a JSON ─────────────────────────────────
            //
            // El body de SQS es un String. El Lambda Processor lo leerá con:
            //   objectMapper.readValue(sqsMessage.getBody(), MessagePayload.class)
            //
            String messageBody = objectMapper.writeValueAsString(payload);

            // ── Construir y enviar el mensaje SQS ────────────────────────
            //
            // SendMessageRequest usa el Builder Pattern del SDK v2.
            // queueUrl: URL completa de la cola (no el ARN).
            // messageBody: JSON serializado del payload.
            //
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(sqsQueueUrl)
                    .messageBody(messageBody)
                    .build();

            SendMessageResponse response = sqsClient.sendMessage(request);

            log.info("Mensaje encolado exitosamente [id={}] [sqsMessageId={}]",
                    payload.getMessageId(), response.messageId());

            return response.messageId();

        } catch (JsonProcessingException e) {
            log.error("Error serializando payload [id={}]: {}",
                    payload.getMessageId(), e.getMessage(), e);
            throw new RuntimeException(
                    "Error al serializar el payload para SQS [id=" + payload.getMessageId() + "]", e);
        } catch (Exception e) {
            log.error("Error enviando mensaje a SQS [id={}]: {}",
                    payload.getMessageId(), e.getMessage(), e);
            throw new RuntimeException(
                    "Error al encolar mensaje en SQS [id=" + payload.getMessageId() + "]", e);
        }
    }
}
