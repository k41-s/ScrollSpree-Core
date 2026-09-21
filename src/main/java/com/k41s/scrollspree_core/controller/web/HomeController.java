package com.k41s.scrollspree_core.controller.web;

import com.k41s.scrollspree_core.dtos.ProductDTO;
import com.k41s.scrollspree_core.services.CategoryService;
import com.k41s.scrollspree_core.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping("/")
    public String index(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<ProductDTO> productPage = productService.getActiveProducts(search, categoryId, pageable);

        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("categories", categoryService.getAll());

        model.addAttribute("currentSearch", search);
        model.addAttribute("currentCategoryId", categoryId);
        model.addAttribute("currentPage", page);
        model.addAttribute("currentSortBy", sortBy);
        model.addAttribute("currentDirection", direction);

        return "index";
    }
}