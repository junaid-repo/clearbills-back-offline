package com.management.shop.service;

import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.errors.MailjetSocketTimeoutException;
import com.management.shop.dto.*;
import com.management.shop.entity.RegisterUserOTPEntity;
import com.management.shop.entity.UserInfo;
import com.management.shop.entity.UserSubscriptions;
import com.management.shop.repository.UserInfoRepository;
import com.management.shop.repository.UserOtpRepo;
import com.management.shop.repository.UserSubscriptionsRepository;
import com.management.shop.util.AccountEmailTemplate;
import com.management.shop.util.OTPSender;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserInfoRepository userinfoRepo;

    @Autowired
    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    @Autowired
    private Environment environment;

    @Autowired
    private OTPSender otpSender;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Autowired
    private UserOtpRepo otpRepo;

    @Autowired
    private AccountEmailTemplate emailTemplateUtil;

    @Autowired
    UserSubscriptionsRepository subsRepo;
    public AuthService(AuthenticationManager authenticationManager,

                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Value("${app.username}")
    private String appUsername;

    public String extractUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("Current user: " + username);
        //  username="junaid1";
        return username;
    }

    public String authAndsetCookies(AuthRequest authRequest, HttpServletResponse response){
        var regRequest= RegisterRequest.builder().password(authRequest.getPassword()).fullName("app user").email("na@na.com").phone("8888888888").password(authRequest.getPassword()).confirmPassword(authRequest.getPassword()).build();


        String userSource= null;
        try {
            userSource = userinfoRepo.findByUsername(authRequest.getUsername()).get().getSource();
        } catch (Exception e) {
            registerNewUser(regRequest);
            userSource = userinfoRepo.findByUsername(authRequest.getUsername()).get().getSource();
        }

        boolean isUserActive = checkUserStatus(authRequest.getUsername());
        if(userSource.equals("email")||authRequest.getUsername().equals("junaid1")) {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));
            System.out.println("The authentication object is --> " + authentication);
            if (authentication.isAuthenticated() && isUserActive) {
                String token = jwtService.generateToken(authRequest.getUsername());

                Cookie cookie = new Cookie("jwt", token);
                if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
                    cookie.setHttpOnly(true);       // ✅ Prevent JS access
                    cookie.setSecure(true);         // ✅ Required for HTTPS
                    cookie.setPath("/");            // ✅ Makes cookie accessible for all paths
                    cookie.setMaxAge(3600);         // ✅ 1 hour
                    cookie.setDomain(".clearbills.info"); // ✅ Share across subdomains
// Note: cookie.setSameSite("None"); is not available directly in Servlet Cookie API

                    response.addHeader("Set-Cookie",
                            "jwt=" + token + "; Path=/; HttpOnly; Secure; SameSite=None; Domain=.clearbills.info; Max-Age=36000");
                } else {
                    cookie.setHttpOnly(true);      // Prevent JS access
                    cookie.setSecure(true);       // Don't require HTTPS in dev
                    cookie.setPath("/");           // Available on all paths
                    cookie.setMaxAge(3600);        // 1 hour
                    cookie.setDomain("localhost"); // Or remove for simpler case

                    response.addCookie(cookie);
                }
                System.out.println("The generated token --> "+token);
                return token;
            }
        }
        else if(userSource.equals("google")){
            return "Please login using google login";
        }
        else {
            throw new UsernameNotFoundException("invalid user request !");
        }
        return null;
    }

    public boolean checkUserStatus(String username) {
        // TODO Auto-generated method stub
        return userinfoRepo.findByUsername(username).get().getIsActive();
    }

    public ValidateContactResponse validateContact(ValidateContactRequest userInfo) {

        List<UserInfo> res = userinfoRepo.validateContact(userInfo.getEmail(), userInfo.getPhone(), true);


        if (res.size() > 0) {

            return ValidateContactResponse.builder().status(false).message("Email/Phone already registered").build();
        }
        return ValidateContactResponse.builder().status(true).message("Email/Phone already registered").build();


    }

    public ValidateContactResponse forgotPaswrod(ForgotPassRequest forgotPassRequest) {
        List<UserInfo> res = userinfoRepo.validateUser(forgotPassRequest.getEmailId(), forgotPassRequest.getUserId(), true);


        if (res.size() > 0) {
            System.out.println(res.get(0));
            Random random = new Random();
            int otp = 100000 + random.nextInt(900000);
            var otpVerifyReq = OtpVerifyRequest.builder().otp(String.valueOf(otp)).username(res.get(0).getUsername()).build();


            RegisterUserOTPEntity res2 = otpRepo.getByUsername(res.get(0).getUsername());
            if (res2 != null) {
                otpRepo.removeOldOTP(res.get(0).getUsername());
            }

            String htmlContent=emailTemplateUtil.generateForgetPasswordHtml(res.get(0).getUsername(), res.get(0).getName(), String.valueOf(otp), String.valueOf(20));

            try {
                otpSender.sendEmail(res.get(0).getEmail(), "support@clearbill.store", res.get(0).getName(), "Clear Bill",
                        "OTP for resetting you password", htmlContent);
            } catch (MailjetException | MailjetSocketTimeoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            var regsiterUserTemp = RegisterUserOTPEntity.builder().username(res.get(0).getUsername())
                    .createdDate(LocalDateTime.now()).otp(String.valueOf(otp)).status("fresh").retries(0).build();
            otpRepo.save(regsiterUserTemp);


            return ValidateContactResponse.builder().status(true).message("OTP sent to your email Id").build();
        }
        return ValidateContactResponse.builder().status(false).message("No user found with provided details").build();

    }

    public ValidateContactResponse confirmOtpAndUpdatePassword(UpdatePasswordRequest updatePassRequest) {
        List<UserInfo> userInfo = userinfoRepo.validateUser(updatePassRequest.getEmailId(), updatePassRequest.getUserId(), true);

        if (userInfo.size() > 0) {
            RegisterUserOTPEntity otpedUser = otpRepo.getLatestOtp(userInfo.get(0).getUsername());

            if (otpedUser != null) {
                if (otpedUser.getOtp().equals(updatePassRequest.getOtp())) {

                    LocalDateTime updatedAt=LocalDateTime.now();

                    updatePassword(UserInfo.builder().username(userInfo.get(0).getUsername()).password(updatePassRequest.getNewPassword()).updatedAt(updatedAt).build());

                    return ValidateContactResponse.builder().status(true).message("Your password has been updated successfully").build();
                } else {

                    return ValidateContactResponse.builder().status(false).message("Your otp doesn't matched please re-enter").build();

                }
            }

        }
        return null;

    }
    public String updatePassword(UserInfo userInfo) {

        UserInfo userRes = userinfoRepo.findByUsername(userInfo.getUsername()).get();
        userRes.setPassword(passwordEncoder.encode(userInfo.getPassword()));
        userRes.setUpdatedAt(LocalDateTime.now());
        userinfoRepo.save(userRes);

        return "success";
    }

    public RegisterResponse registerNewUser(RegisterRequest regRequest) {

        ValidateContactResponse validateContactResponse=    validateContact(ValidateContactRequest.builder().phone(regRequest.getPhone()).email(regRequest.getEmail()).build());





        var userInfo = UserInfo.builder().email(regRequest.getEmail()).isActive(true).name(regRequest.getFullName())
                .password(regRequest.getPassword()).phoneNumber(regRequest.getPhone())
                .source("email")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        userInfo.setRoles("USER");
        userInfo.setPassword(passwordEncoder.encode(userInfo.getPassword()));
        UserInfo res = userinfoRepo.save(userInfo);
        if (res.getId() > 0) {

            UserSubscriptions userSub= subsRepo.findLatestActiveOrUpcomingByUsername(appUsername);

            if(userSub!=null){
                userInfo.setRoles("ROLE_PREMIUM");
            }
            String username = appUsername;
            userInfo.setUsername(username);
            res = userinfoRepo.save(userInfo);


            //  return RegisterResponse.builder().username(res.getUsername()).build();

        }

        return RegisterResponse.builder().message("User created successfully. Please verify the OTP sent to your email to activate your account.").success(true).username(res.getUsername()).build();
    }

}
