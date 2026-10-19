package com.k41s.scrollspree_core.controller.web;

import com.k41s.scrollspree_core.dtos.AuthenticatedUserDTO;
import com.k41s.scrollspree_core.dtos.LoginDTO;
import com.k41s.scrollspree_core.dtos.RegisterUserDTO;
import com.k41s.scrollspree_core.enums.Role;
import com.k41s.scrollspree_core.services.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthWebController {
    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginDto", new LoginDTO());
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(@Valid @ModelAttribute("loginDto") LoginDTO loginDto,
                              BindingResult bindingResult,
                              HttpServletResponse response,
                              Model model) {
        if (bindingResult.hasErrors()) {
            return "login";
        }
        try {
            AuthenticatedUserDTO authUser = authService.login(loginDto);
            setJwtCookie(response, authUser.getToken());
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDto", new RegisterUserDTO());
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("registerDto") RegisterUserDTO registerDto,
                                 BindingResult bindingResult,
                                 HttpServletResponse response,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            registerDto.setRole(Role.USER);
            AuthenticatedUserDTO authUser = authService.registerAndLogin(registerDto);
            setJwtCookie(response, authUser.getToken());
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        Cookie jwtCookie = new Cookie("jwt", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(jwtCookie);
    }
}
