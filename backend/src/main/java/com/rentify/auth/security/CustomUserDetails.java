package com.rentify.auth.security;

import com.rentify.user.User;
import com.rentify.user.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final UserRole role;
    private final boolean isSuspended;
    private final User user;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.isSuspended = user.isSuspended();
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isSuspended() {
        return isSuspended;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String authority = role == UserRole.ADMIN ? "ROLE_ADMIN" : "ROLE_STUDENT";
        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isSuspended;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !isSuspended;
    }
}
