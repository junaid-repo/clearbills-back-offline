package com.management.shop.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class ReactForwardController implements ErrorController {

    // Handle all 404s (paths not found) by returning index.html
    // This allows React Router to handle the URL on the client side
    @RequestMapping(value = {
            "/{path:(?!h2-console|api)[^\\.]*}",
            "/**/{path:(?!h2-console|api)[^\\.]*}"
    })
    public String redirect(HttpServletRequest request) {
        // If we are here, it means the path is NOT h2-console, NOT api, and NOT a file.
        // So it must be a React Route -> Forward to index.html
        return "forward:/index.html";
    }


}