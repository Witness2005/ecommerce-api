package com.ecommerce.service;

import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.exception.AuthenticationException;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String DEFAULT_ROLE_NAME = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        // 1. Validaciones
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // 2. Crear usuario
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        // 3. Asignar el rol por defecto (USER)
        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE_NAME)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(DEFAULT_ROLE_NAME)
                                .description("Default role granted to new users")
                                .build()));
        user.getUserRoles().add(UserRole.builder().user(user).role(defaultRole).build());

        userRepository.save(user);
        log.info("New user registered: {}", request.getUsername());

        // 4. Generar token
        String token = jwtTokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            // 1. Autenticar (verifica username + password)
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // 2. Obtener usuario
            User user = (User) auth.getPrincipal();

            // 3. Generar token
            String token = jwtTokenProvider.generateToken(user);

            log.info("User logged in: {}", request.getUsername());

            return AuthResponse.builder()
                    .token(token)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .roles(user.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList()))
                    .build();

        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Invalid username or password");
        }
    }
}
