package com.msgpipeline.validator;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.msgpipeline.validator.config.ValidatorApplication;
import com.msgpipeline.validator.domain.model.MessagePayload;
import com.msgpipeline.validator.domain.port.in.ValidateMessagePort;
import com.msgpipeline.validator.domain.port.in.ValidateMessagePort.ValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * =========================================================================
 * CLASE: ValidatorHandler -- Lambda Entry Point (Step Functions Task)
 * CAPA: Infraestructura -- Adaptador de Entrada (Input Adapter)
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * DIFERENCIA CLAVE CON SESION 04:
 *   Sesion 04: Recibe APIGatewayProxyRequestEvent (HTTP de API Gateway)
 *   Sesion 07: Recibe Map<String,Object> de Step Functions (Task State)
 *              Retorna Map enriquecido con campo 'validacion'
 *
 * FLUJO SESION 07:
 *   Step Functions (estado ValidarMensaje, tipo Task)
 *   --> invoca Lambda Validator con el input del workflow
 *   --> ValidatorHandler::handleRequest(Map event, Context context)
 *   --> Retorna Map original + {validacion: {valida: true/false, motivo: "..."}}
 *   --> Step Functions evalua $.validacion.valida en el Choice state EvaluarValidacion
 *
 * INPUT DE STEP FUNCTIONS:
 *   {
 *     "messageId": "uuid...",
 *     "messageType": "EMAIL",
 *     "channel": "EMAIL",
 *     "recipientEmail": "estudiante01@test.com",
 *     "content": "Mensaje de prueba sesion 07",
 *     "userEmail": "estudiante01@test.com",
 *     "submittedAt": "2026-...",
 *     "source": "msg-pipeline.orchestrator"
 *   }
 *
 * OUTPUT (mapa enriquecido para ResultSelector de Step Functions):
 *   {
 *     ...todos los campos del input...
 *     "validacion": {
 *       "valida": true,
 *       "motivo": "Mensaje valido -- tipo: EMAIL"
 *     }
 *   }
 *
 * ResultSelector en Step Functions extrae:
 *   $.Payload.messageId, $.Payload.messageType, $.Payload.validacion, etc.
 *
 * HANDLER: com.msgpipeline.validator.ValidatorHandler::handleRequest
 * TIMEOUT: 15s | MEMORIA: 512 MB
 *
 * IMPORTANTE -- WebApplicationType.SERVLET:
 *   Requerido en Spring Boot 3.5. NONE causa ClassCastException en runtime.
 * =========================================================================
 */
@Slf4j
public class ValidatorHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    // -- Bloque static -- Cold Start -----------------------------------------
    private static final ValidateMessagePort validateMessagePort;

    static {
        log.info("ValidatorHandler -- Cold Start (Sesion 07)");
        log.info("Trigger: Step Functions Task (ValidarMensaje)");
        log.info("Patron: Strategy + Factory para validaciones intercambiables");

        // WebApplicationType.SERVLET: OBLIGATORIO en Spring Boot 3.5
        ConfigurableApplicationContext context = new SpringApplicationBuilder(ValidatorApplication.class)
                .web(WebApplicationType.SERVLET)
                .profiles("aws")
                .run();

        validateMessagePort = context.getBean(ValidateMessagePort.class);
        log.info("Contexto Spring inicializado. Puerto de entrada listo.");
    }

    /** Constructor publico sin argumentos -- OBLIGATORIO para AWS Lambda */
    public ValidatorHandler() { }

    /**
     * handleRequest -- Invocado por Step Functions como Task state ValidarMensaje.
     *
     * Retorna el mapa de entrada ENRIQUECIDO con el campo 'validacion'.
     * Step Functions usa ResultSelector para extraer los campos del Payload.
     */
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        log.info("Evento Step Functions recibido [requestId={}] [tiempoRestante={}ms]",
                context.getAwsRequestId(), context.getRemainingTimeInMillis());

        try {
            // -- Extraer campos del input de Step Functions -----------------
            String messageId      = (String) event.getOrDefault("messageId",     "");
            String messageType    = (String) event.getOrDefault("messageType",   "");
            String channel        = (String) event.getOrDefault("channel",       "");
            String recipientEmail = (String) event.getOrDefault("recipientEmail","");
            String content        = (String) event.getOrDefault("content",       "");
            String userEmail      = (String) event.getOrDefault("userEmail",     "");

            log.info("Validando [messageId={}] [tipo={}] [canal={}]",
                    messageId, messageType, channel);

            // -- Construir entidad de dominio --------------------------------
            MessagePayload payload = MessagePayload.builder()
                    .messageId(messageId)
                    .messageType(messageType)
                    .channel(channel)
                    .recipientEmail(recipientEmail)
                    .content(content)
                    .userEmail(userEmail)
                    .build();

            // -- Ejecutar el caso de uso (Factory + Strategy) ---------------
            ValidationResponse response = validateMessagePort.validate(payload);

            // -- Construir el mapa de salida enriquecido --------------------
            // Retornamos TODOS los campos del input original mas 'validacion'
            // Step Functions usa $.validacion.valida en el Choice state
            Map<String, Object> output = new HashMap<>(event);
            Map<String, Object> validacion = new HashMap<>();
            validacion.put("valida",  response.isValida());
            validacion.put("motivo",  response.getMotivo());
            output.put("validacion", validacion);

            log.info("Validacion completada [messageId={}] [valida={}] [motivo={}]",
                    messageId, response.isValida(), response.getMotivo());

            return output;

        } catch (Exception e) {
            log.error("Error en ValidatorHandler [requestId={}]: {}",
                    context.getAwsRequestId(), e.getMessage(), e);

            // En caso de error, retornar validacion=false para enrutar a ColaDeError
            Map<String, Object> output = new HashMap<>(event);
            Map<String, Object> validacion = new HashMap<>();
            validacion.put("valida", false);
            validacion.put("motivo", "Error interno en validacion: " + e.getMessage());
            output.put("validacion", validacion);
            return output;
        }
    }
}
