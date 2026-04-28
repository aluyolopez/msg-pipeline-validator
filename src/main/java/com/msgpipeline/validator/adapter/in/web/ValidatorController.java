package com.msgpipeline.validator.adapter.in.web;

import com.msgpipeline.validator.adapter.in.web.dto.ValidateRequest;
import com.msgpipeline.validator.adapter.in.web.dto.ValidateResponse;
import com.msgpipeline.validator.adapter.out.queue.InMemoryQueueAdapter;
import com.msgpipeline.validator.domain.model.MessagePayload;
import com.msgpipeline.validator.domain.port.in.ValidateMessagePort;
import com.msgpipeline.validator.domain.port.in.ValidateMessagePort.ValidationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * =========================================================================
 * CAPA: Infraestructura — Adaptador de Entrada Web (Input Adapter)
 * ARQUITECTURA: Hexagonal
 * PERFIL: 'local' (solo disponible en desarrollo local)
 * =========================================================================
 *
 * PATRÓN: Adapter (Input/Driving Adapter)
 *   Adapta solicitudes HTTP al contrato definido por ValidateMessagePort.
 *   Convierte:
 *     ValidateRequest (DTO HTTP) → MessagePayload (dominio)
 *     ValidationResponse (dominio) → ValidateResponse (DTO HTTP)
 *
 * PROPÓSITO:
 *   Permite probar el validador localmente via Swagger UI sin necesitar
 *   API Gateway configurado en AWS. Simula el payload que llegaría desde
 *   API Gateway en producción.
 *
 * @Profile("local"):
 *   Este bean SOLO existe en el perfil 'local'. En Lambda (perfil 'aws')
 *   Spring no registra este controlador — el ValidatorHandler gestiona
 *   las peticiones directamente desde API Gateway.
 *
 * ENDPOINTS:
 *   POST /api/v1/validator/validate → valida y encola el mensaje
 *   GET  /api/v1/validator/queue    → lista mensajes encolados en memoria
 *
 * Swagger UI: http://localhost:8081/swagger-ui.html
 * =========================================================================
 */
@Slf4j
@RestController
@Profile("local")
@RequestMapping("/api/v1/validator")
@RequiredArgsConstructor
@Tag(name = "Validador de Mensajes",
     description = "Endpoints para probar localmente el validador de mensajes. " +
                   "Simula el flujo: API Gateway → ValidatorHandler → SQS. " +
                   "⚠️ Solo disponible en perfil 'local' (no existe en Lambda)")
public class ValidatorController {

    /** Puerto de entrada — el mismo que usa ValidatorHandler en Lambda */
    private final ValidateMessagePort validateMessagePort;

    /** Repositorio en memoria — solo disponible en perfil 'local' */
    private final InMemoryQueueAdapter inMemoryQueueAdapter;

    /**
     * POST /api/v1/validator/validate
     *
     * Simula la validación que hace el Lambda Validator cuando recibe
     * una solicitud desde API Gateway. Usa el mismo Use Case que Lambda.
     */
    @PostMapping("/validate")
    @Operation(
        summary = "Validar y encolar mensaje (simula API Gateway → Lambda Validator)",
        description = "Valida el mensaje según su tipo (Strategy Pattern) y, si es válido, " +
                      "lo encola en la cola en memoria (equivalente al SQS en producción). " +
                      "202 = válido y encolado. 400 = error de validación."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Mensaje válido — aceptado para procesamiento asíncrono"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos — ver campo 'message' en la respuesta"),
        @ApiResponse(responseCode = "500", description = "Error interno inesperado")
    })
    public ResponseEntity<ValidateResponse> validate(
            @Valid @RequestBody ValidateRequest request) {

        log.info("Solicitud HTTP recibida [tipo={}] [canal={}]",
                request.getMessageType(), request.getChannel());

        // ── Convertir DTO → Entidad de Dominio ───────────────────────────
        //
        // El controlador NO pasa el DTO directamente al Use Case.
        // Convierte al modelo del dominio. Desacopla HTTP del negocio.
        //
        MessagePayload payload = MessagePayload.builder()
                .messageType(request.getMessageType())
                .channel(request.getChannel())
                .recipientEmail(request.getRecipientEmail())
                .content(request.getContent())
                .priority(request.getPriority())
                .build();

        // ── Ejecutar el caso de uso ───────────────────────────────────────
        ValidationResponse validationResponse = validateMessagePort.validate(payload);

        if (!validationResponse.isAccepted()) {
            // Error de validación de negocio → 400 Bad Request
            log.warn("Validación rechazada [error={}]", validationResponse.getErrorMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ValidateResponse.builder()
                            .success(false)
                            .messageType(request.getMessageType())
                            .message(validationResponse.getErrorMessage())
                            .build());
        }

        // ── Construir respuesta exitosa ───────────────────────────────────
        log.info("Validación exitosa [messageId={}]", validationResponse.getMessageId());

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ValidateResponse.builder()
                        .success(true)
                        .messageId(validationResponse.getMessageId())
                        .messageType(request.getMessageType())
                        .message("Mensaje aceptado para procesamiento asíncrono " +
                                 "(perfil local — cola en memoria)")
                        .build());
    }

    /**
     * GET /api/v1/validator/queue
     *
     * Lista todos los mensajes "encolados" en memoria durante la sesión actual.
     * Útil para verificar que la validación funcionó correctamente.
     * Los datos se pierden al reiniciar la aplicación.
     */
    @GetMapping("/queue")
    @Operation(
        summary = "Listar mensajes en cola (solo local)",
        description = "Retorna todos los mensajes validados y 'encolados' en memoria. " +
                      "Simula la inspección de la cola SQS. Los datos se pierden al reiniciar."
    )
    @ApiResponse(responseCode = "200", description = "Lista de mensajes en cola")
    public ResponseEntity<List<MessagePayload>> getQueue() {
        List<MessagePayload> messages = inMemoryQueueAdapter.findAll();
        log.info("Consultando cola en memoria. Total: {}", messages.size());
        return ResponseEntity.ok(messages);
    }
}
