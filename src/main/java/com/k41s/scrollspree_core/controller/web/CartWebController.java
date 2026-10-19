package com.k41s.scrollspree_core.controller.web;

import com.k41s.scrollspree_core.dtos.CartDTO;
import com.k41s.scrollspree_core.security.CustomUserDetails;
import com.k41s.scrollspree_core.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartWebController {

    private final CartService cartService;

    @GetMapping
    public String viewCart(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        CartDTO cart = cartService.getCartForUser(userDetails.user().getId());
        model.addAttribute("cart", cart);
        return "cart";
    }

    @PostMapping("/add")
    public String addItem(@AuthenticationPrincipal CustomUserDetails userDetails,
                          @RequestParam Integer productId,
                          @RequestParam(defaultValue = "1") Integer quantity) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        cartService.addItemToCart(userDetails.user().getId(), productId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateQuantity(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @RequestParam Integer productId,
                                 @RequestParam Integer quantity) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        if (quantity <= 0) {
            cartService.removeItemFromCart(userDetails.user().getId(), productId);
        } else {
            cartService.updateItemQuantity(userDetails.user().getId(), productId, quantity);
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeItem(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam Integer productId) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        cartService.removeItemFromCart(userDetails.user().getId(), productId);
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }
        cartService.clearCart(userDetails.user().getId());
        return "redirect:/cart";
    }
}