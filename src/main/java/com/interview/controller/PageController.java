package com.interview.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/questions/{id}")
    public String question() {
        return "forward:/question.html";
    }

    @GetMapping("/admin")
    public String admin() {
        return "forward:/admin.html";
    }

}
