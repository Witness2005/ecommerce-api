package com.ecommerce.service;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.CartDto;
import com.ecommerce.dto.CartItemDto;
import com.ecommerce.exception.EntityNotFoundException;
import com.ecommerce.mapper.CartMapper;
import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    public CartDto addToCart(UUID userId, AddToCartRequest request) {
        UUID productId = parseProductId(request.getProductId());
        Product product = findProductById(productId);
        Cart cart = findOrCreateCart(userId);

        CartItem existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            existingItem.setPrice(product.getPrice());
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .price(product.getPrice())
                    .build();
            cart.getCartItems().add(newItem);
        }

        recalculateTotal(cart);
        cart = cartRepository.save(cart);
        log.info("Product {} added to cart of user {}", productId, userId);

        return cartMapper.toDto(cart);
    }

    @Transactional(readOnly = true)
    public CartDto getCart(UUID userId) {
        return cartMapper.toDto(findOrCreateCart(userId));
    }

    public CartItemDto updateCartItem(UUID userId, UUID itemId, Integer quantity) {
        Cart cart = findCartByUserId(userId);
        CartItem item = findItemInCart(cart, itemId);

        item.setQuantity(quantity);
        recalculateTotal(cart);
        cartRepository.save(cart);
        log.info("Cart item {} updated to quantity {} for user {}", itemId, quantity, userId);

        return cartMapper.toDto(item);
    }

    public void removeFromCart(UUID userId, UUID itemId) {
        Cart cart = findCartByUserId(userId);
        CartItem item = findItemInCart(cart, itemId);

        cart.getCartItems().remove(item);
        recalculateTotal(cart);
        cartRepository.save(cart);
        log.info("Cart item {} removed for user {}", itemId, userId);
    }

    public void clearCart(UUID userId) {
        Cart cart = findCartByUserId(userId);
        cart.getCartItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        cartRepository.save(cart);
        log.info("Cart cleared for user {}", userId);
    }

    private Cart findOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = findUserById(userId);
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private Cart findCartByUserId(UUID userId) {
        findUserById(userId);
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found for user: " + userId));
    }

    private CartItem findItemInCart(Cart cart, UUID itemId) {
        return cart.getCartItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Cart item not found: " + itemId));
    }

    private void recalculateTotal(Cart cart) {
        BigDecimal total = cart.getCartItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalPrice(total);
    }

    private Product findProductById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    private UUID parseProductId(String productId) {
        try {
            return UUID.fromString(productId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid product id: " + productId);
        }
    }
}
