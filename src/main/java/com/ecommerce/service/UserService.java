package com.ecommerce.service;

import com.ecommerce.dto.UpdateUserProfileRequest;
import com.ecommerce.dto.UserDto;
import com.ecommerce.dto.UserProfileDto;
import com.ecommerce.exception.EntityNotFoundException;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(UUID userId) {
        return userMapper.toProfileDto(findEntityById(userId));
    }

    public UserProfileDto updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        User user = findEntityById(userId);

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        log.info("Profile updated for user {}", userId);
        return userMapper.toProfileDto(user);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        return userMapper.toDto(findEntityById(userId));
    }

    public UserDto assignRoleToUser(UUID userId, UUID roleId) {
        User user = findEntityById(userId);
        Role role = findRoleById(roleId);

        boolean alreadyAssigned = user.getUserRoles().stream()
                .anyMatch(userRole -> userRole.getRole().getId().equals(roleId));
        if (alreadyAssigned) {
            throw new IllegalArgumentException("Role already assigned to user");
        }

        user.getUserRoles().add(UserRole.builder().user(user).role(role).build());
        userRepository.save(user);
        log.info("Role {} assigned to user {}", role.getName(), userId);

        return userMapper.toDto(user);
    }

    public UserDto removeRoleFromUser(UUID userId, UUID roleId) {
        User user = findEntityById(userId);
        findRoleById(roleId);

        UserRole userRole = user.getUserRoles().stream()
                .filter(ur -> ur.getRole().getId().equals(roleId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("User does not have role: " + roleId));

        user.getUserRoles().remove(userRole);
        userRepository.save(user);
        log.info("Role {} removed from user {}", roleId, userId);

        return userMapper.toDto(user);
    }

    public void deleteUser(UUID userId) {
        User user = findEntityById(userId);
        userRepository.delete(user);
        log.info("User deleted: {}", userId);
    }

    private User findEntityById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    private Role findRoleById(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));
    }
}
