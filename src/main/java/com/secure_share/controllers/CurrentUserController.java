package com.secure_share.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.secure_share.config.custom.CustomUserDetails;
import com.secure_share.dto.request.ChangePasswordRequest;
import com.secure_share.dto.response.UserResponse;
import com.secure_share.entities.UserEntity;
import com.secure_share.exceptions.AuthorizationHeaderMissingException;
import com.secure_share.repositories.UserRepository;
import com.secure_share.utils.CookieUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/current-user")
public class CurrentUserController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = authentication.getPrincipal();
        UserEntity currentUser = null;

        if (principal instanceof CustomUserDetails)
            currentUser = ((CustomUserDetails) principal).getUserEntity();

        
        return ResponseEntity.ok(UserResponse.entityToResponse(currentUser));
 
    }
    
    @PostMapping("/change-password")
    public Object changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var principal = authentication.getPrincipal();
        UserEntity userEntity = ((CustomUserDetails) principal).getUserEntity();

        if (passwordEncoder.matches(changePasswordRequest.getOldPassword(), userEntity.getPassword())) {
            userEntity.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
            userRepository.save(userEntity);

            return ResponseEntity
                    .ok()
                    .body(Map.of(
                        "message", "Password updated successfully"
                    ));
        }
        
        return ResponseEntity
                .badRequest()
                .body(Map.of(
                    "error", "Old Password was not matching"
                ));
 
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<?,?>> logOut(
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse
    ) throws AuthorizationHeaderMissingException {

        Cookie[] cookies = httpServletRequest.getCookies();
            
        if (cookies == null) 
            throw new AuthorizationHeaderMissingException("Didn't receive any cookies with the request", httpServletRequest.getServletPath());

        Cookie jwtCookie = CookieUtils.generateCookie("jwtToken", "", true, 0);
        httpServletResponse.addCookie(jwtCookie);

        Cookie refreshCookie = CookieUtils.generateCookie("refreshToken", "", true, 0);
        httpServletResponse.addCookie(refreshCookie);

        return ResponseEntity
                    .status(HttpStatus.OK.value())
                    .body(Map.of(
                        "message", "logged out successfully..."
                    ));
    }
    
}
