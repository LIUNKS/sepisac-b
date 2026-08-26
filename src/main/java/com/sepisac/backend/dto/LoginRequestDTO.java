package com.sepisac.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequestDTO {

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El formato de email no es válido")
    private String email;

    @NotBlank(message = "La contraseña no puede estar vacía")
    private String password;

    public LoginRequestDTO() {
    }

    public LoginRequestDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
