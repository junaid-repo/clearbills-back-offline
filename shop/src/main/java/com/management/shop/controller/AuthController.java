package com.management.shop.controller;

import com.management.shop.dto.*;
import com.management.shop.service.AuthService;
import com.management.shop.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {


    private final AuthenticationManager authenticationManager;
    private final AuthService serv;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager,
                          AuthService serv,
                          JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.serv = serv;
        this.jwtService = jwtService;
    }

    @PostMapping("/auth/authenticate")
    public String authenticateAndGetToken(@RequestBody AuthRequest authRequest, HttpServletResponse response) {

        String token =serv.authAndsetCookies(authRequest, response);
        return token;

    }

    @PostMapping("/auth/validate-contact")
    public ValidateContactResponse validateContact(@RequestBody ValidateContactRequest userInfo) {
        System.out.println("Entered validateContact with payload  " + userInfo);
        return serv.validateContact(userInfo);
    }

    @PostMapping("/auth/forgot-password")
    public ValidateContactResponse forgotPassword(@RequestBody ForgotPassRequest forgotPassRequest) {
        System.out.println("Entered forgotPassword with payload  " + forgotPassRequest);
        return serv.forgotPaswrod(forgotPassRequest);
    }

    @PostMapping("/auth/update-password")
    public ValidateContactResponse confirmOtpAndUpdatePassword(@RequestBody UpdatePasswordRequest updatePassRequest) {
        System.out.println("Entered confirmOtpAndUpdatePassword with payload  " + updatePassRequest);
        return serv.confirmOtpAndUpdatePassword(updatePassRequest);
    }

    @PostMapping("/auth/register/newuser")
    public RegisterResponse addNewThirdPartyUser(@RequestBody RegisterRequest userInfo) {
        System.out.println("Entered addNewThirdPartyUser with payload  " + userInfo);
        return serv.registerNewUser(userInfo);
    }
}
