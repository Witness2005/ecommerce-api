package com.ecommerce.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50)
    private String username;

    @Column(nullable = false, unique = true)
    @NotBlank
    @Email
    private String email;

    @Column(nullable = false)
    @NotBlank
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserRole> userRoles = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Roles asignados al usuario (a través de UserRole)
    public List<Role> getRoles() {
        return userRoles.stream()
                .map(UserRole::getRole)
                .collect(Collectors.toList());
    }

    // UserDetails implementation (para Spring Security)
    // Cada rol aporta una authority "ROLE_<nombre>" (para hasRole(...)) más una
    // authority por cada Authority asociada al rol (para hasAuthority(...)).
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getRoles().stream()
                .flatMap(role -> {
                    List<GrantedAuthority> roleAuthorities = new ArrayList<>();
                    roleAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
                    role.getAuthorities().forEach(authority ->
                            roleAuthorities.add(new SimpleGrantedAuthority(authority.getName())));
                    return roleAuthorities.stream();
                })
                .collect(Collectors.toList());
    }

    // isAccountNonExpired(), isAccountNonLocked(), isCredentialsNonExpired() e isEnabled()
    // no se sobrescriben: UserDetails ya las implementa por defecto devolviendo true.
}
