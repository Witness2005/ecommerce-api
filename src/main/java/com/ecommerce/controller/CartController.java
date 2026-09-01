package com.ecommerce.controller;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.CartDto;
import com.ecommerce.dto.CartItemDto;
import com.ecommerce.dto.UpdateCartItemRequest;
import com.ecommerce.model.User;
import com.ecommerce.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<CartDto> addToCart(@AuthenticationPrincipal User user,
                                              @Valid @RequestBody AddToCartRequest request) {
        CartDto cart = cartService.addToCart(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cart);
    }

    @GetMapping
    public ResponseEntity<CartDto> getCart(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cartService.getCart(user.getId()));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartItemDto> updateCartItem(@AuthenticationPrincipal User user,
                                                        @PathVariable UUID itemId,
                                                        @Valid @RequestBody UpdateCartItemRequest request) {
        CartItemDto updated = cartService.updateCartItem(user.getId(), itemId, request.getQuantity());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeFromCart(@AuthenticationPrincipal User user,
                                                @PathVariable UUID itemId) {
        cartService.removeFromCart(user.getId(), itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal User user) {
        cartService.clearCart(user.getId());
        return ResponseEntity.noContent().build();
    }
}
