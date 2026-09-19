package com.k41s.scrollspree_core.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DashboardController {
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/admin/logs")
    public String logs() {
        return "logs";
    }
}