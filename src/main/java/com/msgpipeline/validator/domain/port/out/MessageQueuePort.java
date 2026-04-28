package com.msgpipeline.validator.domain.port.out;

import com.msgpipeline.validator.domain.model.MessagePayload;

/**
 * =========================================================================
 * CAPA: Dominio — Puerto de Salida (Output Port)
 * ARQUITECTURA: Hexagonal (Ports & Adapters)
 * =========================================================================
 *
 * Define cómo el dominio envía mensajes validados al sistema de colas.
 * El dominio NO sabe si la cola es SQS, RabbitMQ, Kafka o un Map en memoria.
 * Solo conoce este contrato.
 *
 * PATRÓN: Repository / Gateway (Output Port)
 *   El dominio declara QUÉ necesita (encolar un mensaje).
 *   La infraestructura implementa el CÓMO (SQS SDK, InMemory).
 *
 * PATRÓN: Dependency Inversion Principle (DIP — SOLID)
 *   UseCase → MessageQueuePort ← SqsMessageQueueAdapter
 *                              ← InMemoryQueueAdapter
 *   Sin DIP: UseCase → SqsMessageQueueAdapter (acoplamiento fuerte)
 *   Con DIP: UseCase depende de la abstracción, no de la implementación.
 *
 * IMPLEMENTACIONES DISPONIBLES:
 *   @Profile("aws")   → SqsMessageQueueAdapter (SQS real en AWS Lambda)
 *   @Profile("local") → InMemoryQueueAdapter   (memoria para desarrollo local)
 *
 * PRINCIPIO: Interface Segregation (ISP — SOLID)
 *   Interfaz mínima y específica — solo la operación necesaria.
 * =========================================================================
 */
public interface MessageQueuePort {

    /**
     * Encola un mensaje validado para procesamiento asíncrono.
     *
     * En producción (perfil 'aws'): envía el mensaje a la cola SQS.
     * En desarrollo (perfil 'local'): almacena en memoria para verificación.
     *
     * @param payload Mensaje validado a encolar (con messageId asignado)
     * @return        ID de la cola del mensaje encolado (SQS MessageId o simulado)
     */
    String enqueue(MessagePayload payload);
}
