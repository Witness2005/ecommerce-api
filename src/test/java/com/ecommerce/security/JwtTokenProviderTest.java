package com.ecommerce.security;

import com.ecommerce.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void shouldGenerateValidToken() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();

        // Act
        String token = jwtTokenProvider.generateToken(user);

        // Assert
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("testuser", jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    void shouldRejectExpiredToken() {
        // Arrange: forzamos una expiración de -1 segundo para generar un token ya expirado
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", -1000L);
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .build();

        // Act
        String expiredToken = jwtTokenProvider.generateToken(user);

        // Assert
        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }
}
