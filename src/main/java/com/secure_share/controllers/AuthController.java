package com.secure_share.controllers;

import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.secure_share.config.custom.CustomUserDetails;
import com.secure_share.dto.request.ForgotPasswordRequest;
import com.secure_share.dto.request.LoginRequest;
import com.secure_share.dto.request.RegisterRequest;
import com.secure_share.dto.request.SetPasswordRequest;
import com.secure_share.dto.response.LoginResponse;
import com.secure_share.dto.response.RegisterResponse;
import com.secure_share.dto.response.UserResponse;
import com.secure_share.entities.RefreshTokenEntity;
import com.secure_share.entities.UserEntity;
import com.secure_share.exceptions.AuthorizationHeaderMissingException;
import com.secure_share.exceptions.OTPExpiredException;
import com.secure_share.exceptions.UserAlreadyExistsException;
import com.secure_share.exceptions.UserNotFoundException;
import com.secure_share.services.AuthService;
import com.secure_share.services.JwtService;
import com.secure_share.services.RefreshTokenService;
import com.secure_share.utils.CookieUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RefreshTokenService refreshTokenService;
    private final AuthService authService;
    private final JwtService jwtService;

    @Value("${application.authentication.type}")
    private String authType;
 
    @Value("${application.cookie.expiration}")
    private int expiry;

    @Value("${application.cookie.refresh.expiration}")
    private int refreshExpiry;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) throws UserAlreadyExistsException, UserNotFoundException {
        UserEntity newUser = authService.registerUser(registerRequest);

        if (newUser == null) 
            throw new UserAlreadyExistsException("user already exists with email : " + registerRequest.getEmail());
        
        RegisterResponse registerResponse = new RegisterResponse();
        BeanUtils.copyProperties(newUser, registerResponse);
        registerResponse.setMessage("User registered successfully.");

        return ResponseEntity
                .status(HttpStatus.CREATED.value())
                .body(registerResponse); 
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletResponse httpServletResponse
    ) {
        UserEntity loginUser = authService.loginUser(loginRequest);
        String jwtToken = jwtService.generateJwtToken(new CustomUserDetails(loginUser));
        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(loginUser.getId());

        LoginResponse loginResponse = new LoginResponse();
        BeanUtils.copyProperties(loginUser, loginResponse);
        loginResponse.setMessage("User logged in successfully");

        if("AUTHORIZATION_HEADER".equals(authType)) {
            loginResponse.setJwtToken(jwtToken);
            loginResponse.setRefreshToken(refreshToken.getRefreshToken());
        }
        else {
            Cookie jwtCookie = CookieUtils.generateCookie("jwtToken", jwtToken, true, expiry);
            httpServletResponse.addCookie(jwtCookie);

            Cookie refreshCookie = CookieUtils.generateCookie("refreshToken", refreshToken.getRefreshToken(), true, refreshExpiry);
            httpServletResponse.addCookie(refreshCookie);
        }
        
        return ResponseEntity
                .status(HttpStatus.OK.value())
                .body(loginResponse); 
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(
        @RequestBody(required = false) Map<String, String> refreshTokenRequest,
        HttpServletResponse httpServletResponse,
        HttpServletRequest httpServletRequest
    ) throws UserNotFoundException, AuthorizationHeaderMissingException {
        
        String refreshToken = null;

        if(refreshTokenRequest != null && "AUTHORIZATION_HEADER".equals(authType)) 
            refreshToken = refreshTokenRequest.get("refresh-token");

        
        else {
            Cookie[] cookies = httpServletRequest.getCookies();
            
            if (cookies == null) 
                throw new AuthorizationHeaderMissingException("Didn't receive any cookies with the request", httpServletRequest.getServletPath());
    
            for (Cookie cookie : cookies) 
                if ("refreshToken".equals(cookie.getName())) 
                    refreshToken = cookie.getValue();
        }

        System.out.println("\n\n" + refreshToken + "\n\n");
        
        if (refreshToken == null) 
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing refresh token from the request body");

        RefreshTokenEntity refreshTokenEntity = refreshTokenService.refreshToken(refreshToken);
        UserEntity userById = authService.getUserById(refreshTokenEntity.getUserId());
        String jwtToken = jwtService.generateJwtToken(new CustomUserDetails(userById));

        LoginResponse loginResponse = new LoginResponse();
        BeanUtils.copyProperties(userById, loginResponse);
        loginResponse.setMessage("Refresh Token is generate successfully");

         if("AUTHORIZATION_HEADER".equals(authType)) {
            loginResponse.setJwtToken(jwtToken);
            loginResponse.setRefreshToken(refreshTokenEntity.getRefreshToken());
        }
        else {
            Cookie jwtCookie = CookieUtils.generateCookie("jwtToken", jwtToken, true, expiry);
            httpServletResponse.addCookie(jwtCookie);

            Cookie refreshCookie = CookieUtils.generateCookie("refreshToken", refreshTokenEntity.getRefreshToken(), true, refreshExpiry);
            httpServletResponse.addCookie(refreshCookie);
        }

        return ResponseEntity
                .status(HttpStatus.OK.value())
                .body(loginResponse); 
    }

    @GetMapping("/type")
    public ResponseEntity<?> type() {
        return ResponseEntity
                .status(HttpStatus.OK.value())
                .body(Map.of(
                    "type", authType
                ));
    }

    @PostMapping("/resend-activation")
    public ResponseEntity<Map<String, Object>> resendActivation(
        @RequestParam(required = true) String email
    ) throws UserNotFoundException {
        UserEntity user = authService.forgotPassword(email);
        return ResponseEntity
                .status(HttpStatus.OK.value())
                .body(Map.of(
                    "message", "Password reset link was sent to your mail",
                    "user", UserResponse.entityToResponse(user)
                ));
    }
    
    @PostMapping("/forgot-password")
    public Object forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) throws UserNotFoundException {
        UserEntity user = authService.forgotPassword(forgotPasswordRequest.getEmail());
        return ResponseEntity
                .status(HttpStatus.OK.value())
                .body(Map.of(
                    "message", "Password reset link was sent to your mail",
                    "user", UserResponse.entityToResponse(user)
                ));
    }
   
    @PostMapping("/set-password")
    public Object setPassword(@Valid @RequestBody SetPasswordRequest setPasswordRequest) throws UserNotFoundException, OTPExpiredException {
        UserEntity user = authService.setUserPassword(setPasswordRequest);
        return ResponseEntity
                .status(HttpStatus.OK.value())
                .body(Map.of(
                    "message", "Password reset successful",
                    "user", UserResponse.entityToResponse(user)
                ));
    }

    @PutMapping("/activate/{token}")
    public ResponseEntity<?> activateUser(
        @PathVariable String token
    ) throws UserNotFoundException, OTPExpiredException {

        UserEntity activateUser = authService.activateUser(token);

        return ResponseEntity
                .status(HttpStatus.OK.value())
                .body(Map.of(
                    "message", "User activated successfully",
                    "user", UserResponse.entityToResponse(activateUser)
                ));

    }
    
    @GetMapping("/user/{id}")
    public ResponseEntity<UserResponse> getUserById(
        @PathVariable int id
    ) throws UserNotFoundException {
        UserEntity userByEmail = authService.getUserById(id);
        UserResponse userResponse = UserResponse.entityToResponse(userByEmail);

        return ResponseEntity
                .status(HttpStatus.OK.value())
                .body(userResponse);
    }    
}
