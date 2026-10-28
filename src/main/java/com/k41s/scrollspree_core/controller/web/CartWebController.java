package com.k41s.scrollspree_core.controller.web;

import com.k41s.scrollspree_core.dtos.CartDTO;
import com.k41s.scrollspree_core.dtos.CartItemDTO;
import com.k41s.scrollspree_core.dtos.ProductDTO;
import com.k41s.scrollspree_core.security.CustomUserDetails;
import com.k41s.scrollspree_core.services.CartService;
import com.k41s.scrollspree_core.services.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartWebController {

    private final CartService cartService;
    private final ProductService productService;

    private static final String GUEST_CART_SESSION_KEY = "guestCart";

    @GetMapping
    public String viewCart(@AuthenticationPrincipal CustomUserDetails userDetails, HttpSession session, Model model) {
        CartDTO cart;
        if (userDetails != null) {
            cart = cartService.getCartForUser(userDetails.user().getId());
        } else {
            cart = getOrCreateSessionCart(session);
        }
        model.addAttribute("cart", cart);
        return "cart";
    }

    @PostMapping("/add")
    public String addItem(@AuthenticationPrincipal CustomUserDetails userDetails,
                          @RequestParam Integer productId,
                          @RequestParam(defaultValue = "1") Integer quantity,
                          HttpSession session) {
        if (userDetails != null) {
            cartService.addItemToCart(userDetails.user().getId(), productId, quantity);
        } else {
            addOrUpdateSessionCart(session, productId, quantity);
        }
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateQuantity(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @RequestParam Integer productId,
                                 @RequestParam Integer quantity,
                                 HttpSession session) {
        if (userDetails != null) {
            if (quantity <= 0) {
                cartService.removeItemFromCart(userDetails.user().getId(), productId);
            } else {
                cartService.updateItemQuantity(userDetails.user().getId(), productId, quantity);
            }
        } else {
            updateSessionItemQuantity(session, productId, quantity);
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeItem(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam Integer productId,
                             HttpSession session) {
        if (userDetails != null) {
            cartService.removeItemFromCart(userDetails.user().getId(), productId);
        } else {
            removeSessionItem(session, productId);
        }
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(@AuthenticationPrincipal CustomUserDetails userDetails, HttpSession session) {
        if (userDetails != null) {
            cartService.clearCart(userDetails.user().getId());
        } else {
            session.removeAttribute(GUEST_CART_SESSION_KEY);
        }
        return "redirect:/cart";
    }

    // --- Helper Methods for Guest Session Cart Management ---

    private CartDTO getOrCreateSessionCart(HttpSession session) {
        CartDTO cart = (CartDTO) session.getAttribute(GUEST_CART_SESSION_KEY);
        if (cart == null) {
            cart = new CartDTO();
            cart.setItems(new ArrayList<>());
            session.setAttribute(GUEST_CART_SESSION_KEY, cart);
        }
        return cart;
    }

    private void addOrUpdateSessionCart(HttpSession session, Integer productId, int quantity) {
        CartDTO cart = getOrCreateSessionCart(session);

        CartItemDTO existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            ProductDTO product = productService.getActiveProductById(productId);
            CartItemDTO newItem = new CartItemDTO();
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
        }
        session.setAttribute(GUEST_CART_SESSION_KEY, cart);
    }

    private void updateSessionItemQuantity(HttpSession session, Integer productId, int quantity) {
        CartDTO cart = getOrCreateSessionCart(session);
        if (quantity <= 0) {
            removeSessionItem(session, productId);
            return;
        }
        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> item.setQuantity(quantity));
        session.setAttribute(GUEST_CART_SESSION_KEY, cart);
    }

    private void removeSessionItem(HttpSession session, Integer productId) {
        CartDTO cart = getOrCreateSessionCart(session);
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        session.setAttribute(GUEST_CART_SESSION_KEY, cart);
    }
}