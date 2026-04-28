package com.msgpipeline.validator;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msgpipeline.validator.config.ValidatorApplication;
import com.msgpipeline.validator.domain.model.MessagePayload;
import com.msgpipeline.validator.domain.port.in.ValidateMessagePort;
import com.msgpipeline.validator.domain.port.in.ValidateMessagePort.ValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

/**
 * =========================================================================
 * CLASE: ValidatorHandler — Lambda Entry Point (Handler de AWS Lambda)
 * CAPA: Infraestructura — Adaptador de Entrada (Input Adapter)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║  FLUJO DE SESIÓN 04:                                                ║
 * ║                                                                      ║
 * ║  API Gateway  →  ValidatorHandler  →  ValidateMessageUseCase         ║
 * ║  (POST /messages)  (este Lambda)       (Strategy + Factory)          ║
 * ║                          │                     │                     ║
 * ║                          ↓             Si válido → SQS Queue         ║
 * ║                    202 Accepted        Si inválido → 400 Bad Request  ║
 * ║                                                                      ║
 * ║  → Lambda Processor lee de SQS → DynamoDB + SNS (notificación email) ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * PATRÓN: Adapter (Input/Driving Adapter)
 *   Adapta el contrato de AWS Lambda (RequestHandler<APIGatewayProxy...>)
 *   al contrato de negocio (ValidateMessagePort).
 *   Lambda solo sabe de APIGatewayProxyRequestEvent.
 *   El negocio solo sabe de MessagePayload.
 *   Este Handler hace la conversión entre ambos mundos.
 *
 * PATRÓN: Singleton (el contexto Spring es singleton por Lambda container)
 *   Los campos static se inicializan UNA VEZ en el cold start.
 *   Las invocaciones warm reutilizan el contexto Spring ya inicializado.
 *
 * DIFERENCIA con ValidatorController:
 *   ValidatorHandler: recibe APIGatewayProxyRequestEvent (Lambda en AWS)
 *   ValidatorController: recibe HTTP Request (Spring MVC local)
 *   Ambos llaman al mismo ValidateMessagePort (mismo Use Case).
 *
 * HANDLER A CONFIGURAR EN LAMBDA:
 *   com.msgpipeline.validator.ValidatorHandler::handleRequest
 *
 * VARIABLES DE ENTORNO REQUERIDAS:
 *   SQS_QUEUE_URL = https://sqs.us-east-1.amazonaws.com/{accountId}/msg-pipeline-queue
 *   AWS_REGION    = us-east-1 (Lambda la inyecta automáticamente)
 *
 * COMPILAR Y DESPLEGAR:
 *   ./gradlew clean buildZip
 *   → Genera: build/distributions/msg-pipeline-validator-lambda.zip
 *   → Subir el ZIP a la consola Lambda
 * =========================================================================
 */
@Slf4j
public class ValidatorHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    // ── Bloque static — Inicialización en el Cold Start ──────────────────
    //
    // COLD START: primera vez que Lambda ejecuta esta función.
    //   - La JVM se inicia
    //   - Lambda crea UNA instancia de ValidatorHandler
    //   - El bloque 'static' se ejecuta UNA sola vez
    //   - El contexto Spring arranca con perfil 'aws' y WebApplicationType.NONE
    //
    // WARM START: invocaciones subsiguientes.
    //   - El bloque 'static' NO se vuelve a ejecutar
    //   - El contexto Spring se REUTILIZA → mucho más rápido (sin cold start)
    //
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ValidateMessagePort validateMessagePort;

    static {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  ValidatorHandler — Inicialización Cold Start (Sesión 04)   ║");
        log.info("║  Patrón: Strategy + Factory para validaciones intercambiables║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        // ── Inicializar contexto Spring SIN servidor web ─────────────────
        //
        // SpringApplicationBuilder: versión flexible de SpringApplication.
        // .web(WebApplicationType.NONE): NO arranca Tomcat/Netty.
        //   Solo crea el contenedor IoC con todos los beans de validación.
        // .profiles("aws"): activa el perfil 'aws':
        //   → SqsConfig se crea (SqsClient bean)
        //   → SqsMessageQueueAdapter se registra (en vez de InMemoryQueueAdapter)
        //   → ValidatorController NO se crea (@Profile("local"))
        //   → Swagger UI NO se activa
        //
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ValidatorApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("aws")
                .run();

        validateMessagePort = context.getBean(ValidateMessagePort.class);

        log.info("Contexto Spring inicializado. Puerto de entrada listo.");
        log.info("Cola SQS: {}",
                context.getEnvironment().getProperty("app.aws.sqs-queue-url", "NO CONFIGURADO"));
    }

    /**
     * Constructor público sin argumentos — OBLIGATORIO para AWS Lambda.
     * Lambda instancia el handler via reflexión sin argumentos.
     * No ponemos @Component ni @Service porque Lambda gestiona el ciclo de vida.
     */
    public ValidatorHandler() {
        // Constructor explícito requerido por AWS Lambda runtime
    }

    /**
     * handleRequest — Método invocado por Lambda para cada solicitud de API Gateway.
     *
     * API Gateway → Lambda Integration (proxy):
     *   API Gateway envuelve la solicitud HTTP en un APIGatewayProxyRequestEvent.
     *   El body del evento es el JSON del mensaje a validar.
     *   Lambda retorna APIGatewayProxyResponseEvent → API Gateway lo convierte a HTTP.
     *
     * Códigos de respuesta:
     *   202 Accepted   → mensaje válido, encolado en SQS para procesamiento
     *   400 Bad Request → mensaje inválido, con descripción del error
     *   500 Internal   → error inesperado (serialización, SDK, etc.)
     *
     * @param event   Evento de API Gateway con el body del mensaje
     * @param context Contexto Lambda (requestId, tiempo restante, etc.)
     * @return        Respuesta HTTP que API Gateway devolverá al cliente
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {

        log.info("Solicitud recibida desde API Gateway [requestId={}] [path={}]",
                context.getAwsRequestId(), event.getPath());

        try {
            // ── Paso 1: Verificar que hay body ────────────────────────────
            if (event.getBody() == null || event.getBody().isBlank()) {
                return response(400, false, null, null,
                        "El body de la solicitud no puede estar vacío");
            }

            // ── Paso 2: Deserializar el body → MessagePayload ─────────────
            //
            // El body de la solicitud HTTP (JSON) se convierte al modelo de dominio.
            // @JsonIgnoreProperties(ignoreUnknown=true) en MessagePayload garantiza
            // que campos adicionales no rompan la deserialización.
            //
            MessagePayload payload = objectMapper.readValue(event.getBody(), MessagePayload.class);

            log.info("Payload deserializado [tipo={}] [canal={}]",
                    payload.getMessageType(), payload.getChannel());

            // ── Paso 3: Ejecutar el caso de uso ───────────────────────────
            //
            // El Use Case orquesta: enriquecimiento → Factory → Strategy → enqueue
            // Retorna ValidationResponse con el resultado.
            //
            ValidationResponse validationResponse = validateMessagePort.validate(payload);

            if (!validationResponse.isAccepted()) {
                // ── Paso 4a: Validación rechazada → 400 ──────────────────
                log.warn("Solicitud rechazada [error={}]", validationResponse.getErrorMessage());
                return response(400, false, null, null,
                        validationResponse.getErrorMessage());
            }

            // ── Paso 4b: Validación exitosa → 202 ────────────────────────
            //
            // 202 Accepted: el servidor ACEPTÓ la solicitud pero el procesamiento
            // ocurrirá de forma asíncrona (Lambda Processor leerá de SQS).
            // El cliente no espera el resultado del procesamiento.
            //
            log.info("Solicitud aceptada [messageId={}]", validationResponse.getMessageId());
            return response(202, true,
                    validationResponse.getMessageId(),
                    payload.getMessageType(),
                    "Mensaje aceptado para procesamiento asíncrono");

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Error deserializando el body: {}", e.getMessage());
            return response(400, false, null, null,
                    "El body no es un JSON válido: " + e.getOriginalMessage());
        } catch (Exception e) {
            log.error("Error inesperado en ValidatorHandler [requestId={}]: {}",
                    context.getAwsRequestId(), e.getMessage(), e);
            return response(500, false, null, null,
                    "Error interno en el servicio de validación");
        }
    }

    // ── Métodos privados ──────────────────────────────────────────────────

    /**
     * Construye la APIGatewayProxyResponseEvent con el body JSON y headers CORS.
     *
     * CORS headers (Access-Control-Allow-Origin):
     *   Necesarios si el API Gateway no tiene CORS habilitado.
     *   Para el curso, API Gateway gestiona CORS — los incluimos por completitud.
     *
     * @param statusCode  Código HTTP de respuesta (202, 400, 500)
     * @param success     Indica si la operación fue exitosa
     * @param messageId   ID del mensaje (null si success=false)
     * @param messageType Tipo de mensaje (null si success=false)
     * @param message     Mensaje descriptivo del resultado
     * @return            Respuesta formateada para API Gateway
     */
    private APIGatewayProxyResponseEvent response(
            int statusCode, boolean success, String messageId, String messageType, String message) {
        try {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("success", success);
            if (messageId != null)   body.put("messageId", messageId);
            if (messageType != null) body.put("messageType", messageType);
            body.put("message", message);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withBody(objectMapper.writeValueAsString(body))
                    .withHeaders(Map.of(
                            "Content-Type", "application/json",
                            "Access-Control-Allow-Origin", "*"
                    ));
        } catch (Exception e) {
            log.error("Error construyendo respuesta HTTP: {}", e.getMessage(), e);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("{\"success\":false,\"message\":\"Error interno construyendo respuesta\"}");
        }
    }
}
