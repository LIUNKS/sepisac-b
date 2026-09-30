package com.sepisac.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Petición para validar el código de autenticación de dos factores (2FA)")
public class Verify2FaRequestDTO {

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe tener un formato válido")
    @Schema(description = "Correo electrónico del usuario", example = "usuario@sepisac.com")
    private String email;

    @NotBlank(message = "El código 2FA es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "El código 2FA debe constar de 6 dígitos numéricos")
    @Schema(description = "Código numérico de 6 dígitos enviado al correo", example = "123456")
    private String code;

    public Verify2FaRequestDTO() {
    }

    public Verify2FaRequestDTO(String email, String code) {
        this.email = email;
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
