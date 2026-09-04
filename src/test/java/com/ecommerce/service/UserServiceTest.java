package com.ecommerce.service;

import com.ecommerce.dto.UpdateUserProfileRequest;
import com.ecommerce.dto.UserDto;
import com.ecommerce.dto.UserProfileDto;
import com.ecommerce.exception.EntityNotFoundException;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    private User buildUser(UUID id, String username, String email, Role... roles) {
        User user = User.builder()
                .id(id)
                .username(username)
                .email(email)
                .password("encoded-password")
                .build();
        for (Role role : roles) {
            user.getUserRoles().add(UserRole.builder().user(user).role(role).build());
        }
        return user;
    }

    private Role buildRole(UUID id, String name) {
        return Role.builder().id(id).name(name).description(name + " role").build();
    }

    // ---------- getUserProfile ----------

    @Test
    void shouldGetUserProfile() {
        UUID userId = UUID.randomUUID();
        Role userRole = buildRole(UUID.randomUUID(), "USER");
        User user = buildUser(userId, "john", "john@example.com", userRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileDto profile = userService.getUserProfile(userId);

        assertEquals(userId, profile.getId());
        assertEquals("john", profile.getUsername());
        assertEquals("john@example.com", profile.getEmail());
        assertEquals(1, profile.getRoles().size());
        assertEquals("USER", profile.getRoles().getFirst().getName());
    }

    @Test
    void shouldThrowWhenGettingProfileOfMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getUserProfile(userId));
    }

    // ---------- updateUserProfile ----------

    @Test
    void shouldUpdateUserProfile() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "john", "john@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        UserProfileDto result = userService.updateUserProfile(userId, request);

        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void shouldRejectUpdateWhenNewEmailAlreadyTaken() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "john", "john@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .email("taken@example.com")
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUserProfile(userId, request));
        assertEquals("Email already exists", exception.getMessage());
    }

    @Test
    void shouldAllowUpdateKeepingSameEmail() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "john", "john@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .email("john@example.com")
                .firstName("John")
                .build();

        UserProfileDto result = userService.updateUserProfile(userId, request);

        assertEquals("john@example.com", result.getEmail());
        verify(userRepository, never()).existsByEmail(any());
    }

    // ---------- getAllUsers ----------

    @Test
    void shouldGetAllUsersPaged() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = buildUser(UUID.randomUUID(), "john", "john@example.com");
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user)));

        var page = userService.getAllUsers(pageable);

        assertEquals(1, page.getTotalElements());
        assertEquals("john", page.getContent().getFirst().getUsername());
    }

    // ---------- getUserById ----------

    @Test
    void shouldGetUserById() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "john", "john@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDto dto = userService.getUserById(userId);

        assertEquals(userId, dto.getId());
        assertEquals("john", dto.getUsername());
    }

    @Test
    void shouldThrowWhenGettingMissingUserById() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getUserById(userId));
    }

    // ---------- assignRoleToUser ----------

    @Test
    void shouldAssignRoleToUser() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = buildUser(userId, "john", "john@example.com");
        Role adminRole = buildRole(roleId, "ADMIN");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto dto = userService.assignRoleToUser(userId, roleId);

        assertEquals(1, dto.getRoles().size());
        assertEquals("ADMIN", dto.getRoles().getFirst().getName());
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectAssigningAlreadyAssignedRole() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Role adminRole = buildRole(roleId, "ADMIN");
        User user = buildUser(userId, "john", "john@example.com", adminRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(adminRole));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.assignRoleToUser(userId, roleId));
        assertEquals("Role already assigned to user", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenAssigningMissingRole() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = buildUser(userId, "john", "john@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.assignRoleToUser(userId, roleId));
    }

    @Test
    void shouldThrowWhenAssigningRoleToMissingUser() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.assignRoleToUser(userId, roleId));
        verify(roleRepository, never()).findById(any());
    }

    // ---------- removeRoleFromUser ----------

    @Test
    void shouldRemoveRoleFromUser() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Role adminRole = buildRole(roleId, "ADMIN");
        User user = buildUser(userId, "john", "john@example.com", adminRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto dto = userService.removeRoleFromUser(userId, roleId);

        assertTrue(dto.getRoles().isEmpty());
        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowWhenRemovingRoleUserDoesNotHave() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Role adminRole = buildRole(roleId, "ADMIN");
        User user = buildUser(userId, "john", "john@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(adminRole));

        assertThrows(EntityNotFoundException.class, () -> userService.removeRoleFromUser(userId, roleId));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRemovingMissingRole() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = buildUser(userId, "john", "john@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.removeRoleFromUser(userId, roleId));
    }

    // ---------- deleteUser ----------

    @Test
    void shouldDeleteUser() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "john", "john@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteUser(userId);

        verify(userRepository).delete(user);
    }

    @Test
    void shouldThrowWhenDeletingMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.deleteUser(userId));
        verify(userRepository, never()).delete(any());
    }
}
