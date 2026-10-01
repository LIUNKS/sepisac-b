package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta tras alternar el estado del segundo factor de autenticación")
public record Toggle2FaResponseDTO(
        @Schema(description = "Estado actual de activación del 2FA", example = "true")
        boolean twoFactorEnabled,
        @Schema(description = "Mensaje informativo del cambio realizado", example = "Autenticación de dos factores activada")
        String message
) {
}
