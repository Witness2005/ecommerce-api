package com.ecommerce.controller;

import com.ecommerce.dto.AssignRoleRequest;
import com.ecommerce.dto.RoleDto;
import com.ecommerce.dto.UpdateUserProfileRequest;
import com.ecommerce.dto.UserDto;
import com.ecommerce.dto.UserProfileDto;
import com.ecommerce.exception.EntityNotFoundException;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import com.ecommerce.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private static final Role USER_ROLE = Role.builder()
            .id(UUID.randomUUID()).name("USER").description("Default role").build();
    private static final Role ADMIN_ROLE = Role.builder()
            .id(UUID.randomUUID()).name("ADMIN").description("Admin role").build();

    private User principal(Role... roles) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("testuser@example.com")
                .password("encoded-password")
                .build();
        for (Role role : roles) {
            user.getUserRoles().add(UserRole.builder().user(user).role(role).build());
        }
        return user;
    }

    private UserDto sampleUserDto(UUID id) {
        return UserDto.builder()
                .id(id)
                .username("testuser")
                .email("testuser@example.com")
                .roles(List.of(RoleDto.builder().id(USER_ROLE.getId()).name("USER").build()))
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ---------- GET /api/users/profile ----------

    @Test
    void getProfile_authenticated_returnsOk() throws Exception {
        User me = principal(USER_ROLE);
        UserProfileDto profile = UserProfileDto.builder()
                .id(me.getId()).username("testuser").email("testuser@example.com")
                .roles(List.of()).createdAt(LocalDateTime.now()).build();
        when(userService.getUserProfile(me.getId())).thenReturn(profile);

        mockMvc.perform(get("/api/users/profile").with(user(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void getProfile_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isForbidden());
    }

    // ---------- PUT /api/users/profile ----------

    @Test
    void updateProfile_validBody_returnsOk() throws Exception {
        User me = principal(USER_ROLE);
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .email("new@example.com").firstName("New").lastName("Name").build();
        UserProfileDto updated = UserProfileDto.builder()
                .id(me.getId()).username("testuser").email("new@example.com")
                .firstName("New").lastName("Name").roles(List.of()).createdAt(LocalDateTime.now()).build();
        when(userService.updateUserProfile(eq(me.getId()), any())).thenReturn(updated);

        mockMvc.perform(put("/api/users/profile").with(user(me))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void updateProfile_invalidEmail_returnsBadRequest() throws Exception {
        User me = principal(USER_ROLE);
        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .email("not-an-email").build();

        mockMvc.perform(put("/api/users/profile").with(user(me))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /api/users ----------

    @Test
    void getAllUsers_asAdmin_returnsOk() throws Exception {
        when(userService.getAllUsers(any())).thenReturn(new PageImpl<>(List.of(sampleUserDto(UUID.randomUUID()))));

        mockMvc.perform(get("/api/users").with(user(principal(ADMIN_ROLE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("testuser"));
    }

    @Test
    void getAllUsers_asRegularUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users").with(user(principal(USER_ROLE))))
                .andExpect(status().isForbidden());
    }

    // ---------- GET /api/users/{userId} ----------

    @Test
    void getUserById_asAdmin_returnsOk() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(userService.getUserById(targetId)).thenReturn(sampleUserDto(targetId));

        mockMvc.perform(get("/api/users/{userId}", targetId).with(user(principal(ADMIN_ROLE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetId.toString()));
    }

    @Test
    void getUserById_notFound_returnsNotFound() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(userService.getUserById(targetId))
                .thenThrow(new EntityNotFoundException("User not found: " + targetId));

        mockMvc.perform(get("/api/users/{userId}", targetId).with(user(principal(ADMIN_ROLE))))
                .andExpect(status().isNotFound());
    }

    // ---------- POST /api/users/{userId}/roles ----------

    @Test
    void assignRole_asAdmin_returnsOk() throws Exception {
        UUID targetId = UUID.randomUUID();
        AssignRoleRequest request = AssignRoleRequest.builder().roleId(ADMIN_ROLE.getId()).build();
        when(userService.assignRoleToUser(targetId, ADMIN_ROLE.getId())).thenReturn(sampleUserDto(targetId));

        mockMvc.perform(post("/api/users/{userId}/roles", targetId).with(user(principal(ADMIN_ROLE)))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void assignRole_missingRoleId_returnsBadRequest() throws Exception {
        UUID targetId = UUID.randomUUID();

        mockMvc.perform(post("/api/users/{userId}/roles", targetId).with(user(principal(ADMIN_ROLE)))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignRole_asRegularUser_returnsForbidden() throws Exception {
        UUID targetId = UUID.randomUUID();
        AssignRoleRequest request = AssignRoleRequest.builder().roleId(ADMIN_ROLE.getId()).build();

        mockMvc.perform(post("/api/users/{userId}/roles", targetId).with(user(principal(USER_ROLE)))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ---------- DELETE /api/users/{userId}/roles/{roleId} ----------

    @Test
    void removeRole_asAdmin_returnsOk() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(userService.removeRoleFromUser(targetId, ADMIN_ROLE.getId())).thenReturn(sampleUserDto(targetId));

        mockMvc.perform(delete("/api/users/{userId}/roles/{roleId}", targetId, ADMIN_ROLE.getId())
                        .with(user(principal(ADMIN_ROLE))))
                .andExpect(status().isOk());
    }

    @Test
    void removeRole_notAssigned_returnsNotFound() throws Exception {
        UUID targetId = UUID.randomUUID();
        when(userService.removeRoleFromUser(targetId, ADMIN_ROLE.getId()))
                .thenThrow(new EntityNotFoundException("User does not have role: " + ADMIN_ROLE.getId()));

        mockMvc.perform(delete("/api/users/{userId}/roles/{roleId}", targetId, ADMIN_ROLE.getId())
                        .with(user(principal(ADMIN_ROLE))))
                .andExpect(status().isNotFound());
    }

    // ---------- DELETE /api/users/{userId} ----------

    @Test
    void deleteUser_asAdmin_returnsNoContent() throws Exception {
        UUID targetId = UUID.randomUUID();

        mockMvc.perform(delete("/api/users/{userId}", targetId).with(user(principal(ADMIN_ROLE))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_asRegularUser_returnsForbidden() throws Exception {
        UUID targetId = UUID.randomUUID();

        mockMvc.perform(delete("/api/users/{userId}", targetId).with(user(principal(USER_ROLE))))
                .andExpect(status().isForbidden());
    }
}
