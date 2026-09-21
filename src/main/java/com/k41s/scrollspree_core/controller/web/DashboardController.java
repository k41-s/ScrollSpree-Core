package com.k41s.scrollspree_core.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/admin/logs")
    public String logs() {
        return "logs";
    }
}