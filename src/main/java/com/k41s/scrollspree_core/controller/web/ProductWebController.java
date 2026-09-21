package com.k41s.scrollspree_core.controller.web;

import com.k41s.scrollspree_core.exceptions.ResourceNotFoundException;
import com.k41s.scrollspree_core.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ProductWebController {

    private final ProductService productService;

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable int id, Model model) {
        try {
            model.addAttribute("product", productService.getActiveProductById(id));
            return "product-detail";
        } catch (ResourceNotFoundException ex) {
            return "redirect:/";
        }
    }

    @GetMapping("/cart")
    public String cart() {
        return "cart";
    }
}