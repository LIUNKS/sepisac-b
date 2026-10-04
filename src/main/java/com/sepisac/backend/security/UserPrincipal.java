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
    private final String fullName;
    private final UUID companyId;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean isActive;
    private final boolean twoFactorEnabled;

    public UserPrincipal(UUID id,
                         String email,
                         String username,
                         String password,
                         UUID companyId,
                         Collection<? extends GrantedAuthority> authorities,
                         boolean isActive) {
        this(id, email, username, password, null, companyId, authorities, isActive, false);
    }

    public UserPrincipal(UUID id,
                         String email,
                         String username,
                         String password,
                         String fullName,
                         UUID companyId,
                         Collection<? extends GrantedAuthority> authorities,
                         boolean isActive) {
        this(id, email, username, password, fullName, companyId, authorities, isActive, false);
    }

    public UserPrincipal(UUID id,
                         String email,
                         String username,
                         String password,
                         UUID companyId,
                         Collection<? extends GrantedAuthority> authorities,
                         boolean isActive,
                         boolean twoFactorEnabled) {
        this(id, email, username, password, null, companyId, authorities, isActive, twoFactorEnabled);
    }

    public UserPrincipal(UUID id,
                         String email,
                         String username,
                         String password,
                         String fullName,
                         UUID companyId,
                         Collection<? extends GrantedAuthority> authorities,
                         boolean isActive,
                         boolean twoFactorEnabled) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.companyId = companyId;
        this.authorities = authorities != null ? authorities : Collections.emptyList();
        this.isActive = isActive;
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public static UserPrincipal create(UserEntity user) {
        String roleName = (user.getRole() != null && user.getRole().getName() != null)
                ? user.getRole().getName().toUpperCase()
                : "USER";

        String formattedRole = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(formattedRole));

        UUID companyId = user.getCompany() != null ? user.getCompany().getId() : null;
        boolean active = user.getIsActive() != null && user.getIsActive();
        boolean twoFactor = Boolean.TRUE.equals(user.getTwoFactorEnabled());

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                companyId,
                authorities,
                active,
                twoFactor
        );
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
    
    public String getFullName() {
        return fullName;
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
