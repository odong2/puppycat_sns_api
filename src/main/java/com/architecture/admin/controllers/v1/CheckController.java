package com.architecture.admin.controllers.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/check")
public class CheckController {
    @GetMapping("")
    public String check() {
        return "API ok";
    }
}
