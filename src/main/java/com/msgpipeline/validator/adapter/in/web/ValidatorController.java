package com.msgpipeline.validator.adapter.in.web;

import com.msgpipeline.validator.adapter.in.web.dto.ValidateRequest;
import com.msgpipeline.validator.adapter.in.web.dto.ValidateResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * =========================================================================
 * CLASE: ValidatorController -- Adaptador de Entrada HTTP (Local)
 * CAPA: Infraestructura -- Adaptador de Entrada
 * ARQUITECTURA: Hexagonal
 * =========================================================================
 *
 * @Profile("local"): Solo activo en desarrollo local.
 * En Lambda, el adaptador de entrada es ValidatorHandler (Step Functions Task).
 *
 * Simula el input que Step Functions envia al Lambda Validator.
 * Swagger UI: http://localhost:8081/swagger-ui.html
 * =========================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/validator")
@Profile("local")
@RequiredArgsConstructor
@Tag(name = "Validador S7",
     description = "Simula ValidatorHandler de Step Functions. Tipos: EMAIL, SMS, PUSH_NOTIFICATION")
public class ValidatorController {

    private final ValidateMessagePort validateMessagePort;

    @Operation(
            summary = "Validar mensaje (Sesion 07)",
            description = "Strategy + Factory segun messageType. " +
                          "Retorna {valida: true/false, motivo}. " +
                          "En AWS este endpoint es invocado por Step Functions Task."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validacion ejecutada"),
            @ApiResponse(responseCode = "400", description = "Request invalido"),
            @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @PostMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(
            @Valid @RequestBody ValidateRequest request) {

        log.info("POST /api/v1/validator/validate [tipo={}] [canal={}]",
                request.getMessageType(), request.getChannel());

        MessagePayload payload = MessagePayload.builder()
                .messageId(request.getMessageId() != null ?
                        request.getMessageId() : UUID.randomUUID().toString())
                .messageType(request.getMessageType())
                .channel(request.getChannel())
                .recipientEmail(request.getRecipientEmail())
                .content(request.getContent())
                .userEmail(request.getUserEmail())
                .build();

        ValidationResponse response = validateMessagePort.validate(payload);

        return ResponseEntity.ok(ValidateResponse.builder()
                .messageId(payload.getMessageId())
                .valida(response.isValida())
                .motivo(response.getMotivo())
                .messageType(payload.getMessageType())
                .build());
    }
}
