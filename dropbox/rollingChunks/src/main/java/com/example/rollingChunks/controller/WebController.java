package com.example.rollingChunks.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
public class WebController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/files")
    public String files() {
        return "files";
    }

    @GetMapping("/files/{id}")
    public String file(@PathVariable UUID id) {
        return "file";
    }
}
