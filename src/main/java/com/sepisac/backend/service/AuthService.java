package com.sepisac.backend.service;

import com.sepisac.backend.dto.AuthResponseDTO;
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

    public AuthResponseDTO login(LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(userPrincipal);

        return new AuthResponseDTO(
                token,
                "Bearer",
                userPrincipal.getEmail(),
                userPrincipal.getUsername(),
                userPrincipal.getRole(),
                userPrincipal.getCompanyId()
        );
    }
}
