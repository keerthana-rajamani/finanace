package com.examly.springapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {"/dashboard", "/budget", "/goals", "/ai", "/bills", "/tax", "/login", "/register"})
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
