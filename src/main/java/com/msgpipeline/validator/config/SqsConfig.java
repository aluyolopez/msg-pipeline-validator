package com.msgpipeline.validator.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * =========================================================================
 * CAPA: Infraestructura — Configuración de Beans AWS
 * ARQUITECTURA: Hexagonal
 * PERFIL: 'aws' (solo se carga en AWS Lambda)
 * =========================================================================
 *
 * PATRÓN: Factory Method (via @Bean)
 *   Cada método @Bean es una "fábrica" gestionada por Spring.
 *   Spring llama al método UNA VEZ y guarda la instancia creada.
 *   Todos los beans que necesiten SqsClient reciben LA MISMA instancia.
 *
 * PATRÓN: Singleton (implícito en Spring)
 *   Por defecto, todos los @Bean son singletons en Spring.
 *   El SqsClient se crea ONCE → reutilizado en todas las invocaciones Lambda.
 *   CRÍTICO para el rendimiento en Lambda (warm start sin recrear el cliente).
 *
 * @Profile("aws"):
 *   SOLO activo en el perfil 'aws'. En 'local', NO se crea SqsClient.
 *   Evita errores de conexión cuando no hay credenciales AWS disponibles.
 *
 * CREDENCIALES — DefaultCredentialsProvider:
 *   Busca credenciales en este orden automáticamente:
 *     1. Variables de entorno: AWS_ACCESS_KEY_ID + AWS_SECRET_ACCESS_KEY
 *     2. Perfil AWS: ~/.aws/credentials (para desarrollo)
 *     3. IAM Role del Lambda → AUTOMÁTICO en AWS (sin credenciales explícitas)
 *
 *   En Lambda, SIEMPRE usa el IAM Role (opción 3).
 *   Rol requerido: msg-pipeline-lambda-role con AmazonSQSFullAccess.
 * =========================================================================
 */
@Slf4j
@Configuration
@Profile("aws")
@RequiredArgsConstructor
public class SqsConfig {

    private final AppConfig appConfig;

    /**
     * Crea y configura el cliente SQS.
     *
     * REUTILIZACIÓN en Lambda (Warm Start):
     *   Este bean se crea en el COLD START (primera invocación).
     *   En warm starts, Lambda reutiliza el contexto Spring existente.
     *   El SqsClient NO se recrea — mejora significativamente el rendimiento.
     *
     * @return SqsClient configurado para la región del curso (us-east-1)
     */
    @Bean
    public SqsClient sqsClient() {
        String region = appConfig.getAws().getRegion();

        log.info("Inicializando SqsClient [region={}] [cola={}]",
                region, appConfig.getAws().getSqsQueueUrl());

        SqsClient client = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        log.info("SqsClient inicializado exitosamente");
        return client;
    }
}
