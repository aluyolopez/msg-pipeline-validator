package com.msgpipeline.validator.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO de respuesta del ValidatorController (perfil local) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateResponse {
    private String  messageId;
    private boolean valida;
    private String  motivo;
    private String  messageType;
}
