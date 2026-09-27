package com.sepisac.backend.service;

import com.sepisac.backend.dto.AuthResponseDTO;
import com.sepisac.backend.dto.AuthResult;
import com.sepisac.backend.dto.LoginRequestDTO;
import com.sepisac.backend.security.JwtTokenProvider;
import com.sepisac.backend.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(JwtTokenProvider jwtTokenProvider,
                       AuthenticationManager authenticationManager) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    public AuthResult login(LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(userPrincipal);

        AuthResponseDTO responseDTO = new AuthResponseDTO(
                userPrincipal.getEmail(),
                userPrincipal.getUsername(),
                userPrincipal.getRole(),
                userPrincipal.getCompanyId()
        );

        return new AuthResult(token, responseDTO);
    }
}
