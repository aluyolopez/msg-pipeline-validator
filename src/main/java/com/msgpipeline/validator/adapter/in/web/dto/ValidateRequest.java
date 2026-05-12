package com.msgpipeline.validator.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de entrada para el ValidatorController (perfil local) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateRequest {
    private String messageId;

    @NotBlank(message = "El tipo de mensaje es obligatorio")
    private String messageType;

    private String channel;
    private String recipientEmail;

    @NotBlank(message = "El contenido es obligatorio")
    private String content;

    private String userEmail;
}
