package com.rentify.auth;

import com.rentify.auth.dto.AuthResponse;
import com.rentify.auth.dto.LoginRequest;
import com.rentify.auth.dto.RegisterRequest;
import com.rentify.auth.security.JwtService;
import com.rentify.exception.ConflictException;
import com.rentify.exception.ForbiddenException;
import com.rentify.exception.NotFoundException;
import com.rentify.exception.UnauthorizedException;
import com.rentify.user.User;
import com.rentify.user.UserRepository;
import com.rentify.user.UserRole;
import com.rentify.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email is already registered");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = new User(
                request.name().trim(),
                normalizedEmail,
                hashedPassword,
                request.campus() != null ? request.campus().trim() : ""
        );
        user.setRole(UserRole.STUDENT);

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getId());

        return new AuthResponse(UserResponse.fromEntity(savedUser), token);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (user.isSuspended()) {
            throw new ForbiddenException("Your account has been suspended by an administrator.");
        }

        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(UserResponse.fromEntity(user), token);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return UserResponse.fromEntity(user);
    }
}
