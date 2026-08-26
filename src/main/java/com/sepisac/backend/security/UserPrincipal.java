package com.sepisac.backend.security;

import com.sepisac.backend.model.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String username;
    private final String password;
    private final UUID companyId;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean isActive;

    public UserPrincipal(UUID id,
                         String email,
                         String username,
                         String password,
                         UUID companyId,
                         Collection<? extends GrantedAuthority> authorities,
                         boolean isActive) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.companyId = companyId;
        this.authorities = authorities != null ? authorities : Collections.emptyList();
        this.isActive = isActive;
    }

    public static UserPrincipal create(UserEntity user) {
        String roleName = (user.getRole() != null && user.getRole().getName() != null)
                ? user.getRole().getName()
                : "USER";

        String formattedRole = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(formattedRole));

        UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;
        boolean active = user.getIsActive() != null && user.getIsActive();

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getPasswordHash(),
                companyId,
                authorities,
                active
        );
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getRole() {
        if (authorities != null) {
            for (GrantedAuthority authority : authorities) {
                if (authority != null && authority.getAuthority() != null) {
                    return authority.getAuthority();
                }
            }
        }
        return "ROLE_USER";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
