package com.sepisac.backend.service;

import com.sepisac.backend.dto.AuthResponseDTO;
import com.sepisac.backend.dto.AuthResult;
import com.sepisac.backend.dto.LoginRequestDTO;
import com.sepisac.backend.dto.Toggle2FaResponseDTO;
import com.sepisac.backend.dto.Verify2FaRequestDTO;
import com.sepisac.backend.exception.BusinessRuleException;
import com.sepisac.backend.model.UserEntity;
import com.sepisac.backend.repository.UserRepository;
import com.sepisac.backend.security.JwtTokenProvider;
import com.sepisac.backend.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(JwtTokenProvider jwtTokenProvider,
                       AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       EmailService emailService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public AuthResult login(LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UserEntity user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            String code = String.format("%06d", secureRandom.nextInt(1_000_000));
            user.setTwoFactorCode(code);
            user.setTwoFactorExpiresAt(OffsetDateTime.now().plusMinutes(5));
            userRepository.save(user);

            emailService.send2FaCode(user.getEmail(), code);

            return new AuthResult(null, AuthResponseDTO.twoFactorRequired(user.getEmail()));
        }

        String token = jwtTokenProvider.generateToken(userPrincipal);
        AuthResponseDTO responseDTO = new AuthResponseDTO(
                userPrincipal.getEmail(),
                userPrincipal.getUsername(),
                userPrincipal.getFullName(),
                userPrincipal.getRole(),
                userPrincipal.getCompanyId(),
                false,
                false
        );

        return new AuthResult(token, responseDTO);
    }

    @Transactional
    public AuthResult verify2Fa(Verify2FaRequestDTO request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new BusinessRuleException("El usuario no tiene la autenticación de dos factores habilitada");
        }

        if (user.getTwoFactorCode() == null || !user.getTwoFactorCode().equals(request.getCode())) {
            throw new BadCredentialsException("Código 2FA incorrecto");
        }

        if (user.getTwoFactorExpiresAt() == null || user.getTwoFactorExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BadCredentialsException("El código 2FA ha expirado");
        }

        user.setTwoFactorCode(null);
        user.setTwoFactorExpiresAt(null);
        userRepository.save(user);

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String token = jwtTokenProvider.generateToken(userPrincipal);

        AuthResponseDTO responseDTO = new AuthResponseDTO(
                userPrincipal.getEmail(),
                userPrincipal.getUsername(),
                userPrincipal.getFullName(),
                userPrincipal.getRole(),
                userPrincipal.getCompanyId(),
                false,
                true
        );

        return new AuthResult(token, responseDTO);
    }

    @Transactional
    public Toggle2FaResponseDTO toggle2Fa(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));

        boolean newState = !Boolean.TRUE.equals(user.getTwoFactorEnabled());
        user.setTwoFactorEnabled(newState);
        if (!newState) {
            user.setTwoFactorCode(null);
            user.setTwoFactorExpiresAt(null);
        }
        userRepository.save(user);

        String message = newState ? "Autenticación de dos factores activada" : "Autenticación de dos factores desactivada";
        return new Toggle2FaResponseDTO(newState, message);
    }
}
