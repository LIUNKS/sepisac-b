package com.sepisac.backend.dto;

public record AuthResult(
        String token,
        AuthResponseDTO responseDTO
) {
}
