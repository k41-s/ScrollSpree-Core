package com.k41s.scrollspree_core.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class DashboardController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/logs")
    public String logs() {
        return "logs";
    }
}