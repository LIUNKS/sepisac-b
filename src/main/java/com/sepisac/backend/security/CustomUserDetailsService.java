package com.sepisac.backend.security;

import com.sepisac.backend.model.UserEntity;
import com.sepisac.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String emailOrUsername) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmail(emailOrUsername)
                .or(() -> userRepository.findByUsername(emailOrUsername))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con: " + emailOrUsername));

        return UserPrincipal.create(user);
    }
}
