package com.ecommerce.service;

import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    void shouldRegisterUserSuccessfully() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        Role userRole = Role.builder().name("USER").description("Default role").build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(roleRepository.findByName("USER")).thenReturn(java.util.Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getToken());
        assertEquals("newuser", response.getUsername());
        assertEquals("new@example.com", response.getEmail());
        assertEquals("Bearer", response.getType());
        assertEquals(1, response.getRoles().size());
        assertEquals("ROLE_USER", response.getRoles().get(0));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRejectRegistrationWhenUsernameAlreadyExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );
        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
    }

    @Test
    void shouldRejectRegistrationWhenPasswordsDoNotMatch() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("different123");

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );
        assertEquals("Passwords do not match", exception.getMessage());
        verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
    }

    @Test
    void shouldLoginSuccessfully() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");

        Role userRole = Role.builder().name("USER").description("Default role").build();
        User user = User.builder()
                .username("existinguser")
                .email("existing@example.com")
                .password("encoded-password")
                .build();
        user.getUserRoles().add(UserRole.builder().user(user).role(userRole).build());

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(user)).thenReturn("fake-jwt-token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getToken());
        assertEquals("existinguser", response.getUsername());
        assertEquals("existing@example.com", response.getEmail());
        assertEquals("Bearer", response.getType());
        assertEquals(1, response.getRoles().size());
        assertEquals("ROLE_USER", response.getRoles().get(0));
    }

    @Test
    void shouldRejectLoginWithBadCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("existinguser");
        request.setPassword("wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        com.ecommerce.exception.AuthenticationException exception = assertThrows(
                com.ecommerce.exception.AuthenticationException.class,
                () -> authService.login(request)
        );
        assertEquals("Invalid username or password", exception.getMessage());
        verify(jwtTokenProvider, org.mockito.Mockito.never()).generateToken(any());
    }
}
