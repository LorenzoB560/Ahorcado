package com.lbodaszsservidor.ahorcado;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PrincipalController {

    @GetMapping("/")
    public String devuelveAhorcado() {
        return "ahorcado";
    }
}
