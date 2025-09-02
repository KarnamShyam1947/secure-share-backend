package com.secure_share.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    private int id;
    private String name;
    private String role;
    private String email;
    private String phoneNumber;
    private String walletAddress;  
    
    private String message;

    private String jwtToken; 
    private String refreshToken;
}
