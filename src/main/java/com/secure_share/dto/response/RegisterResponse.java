package com.secure_share.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private String role;
    private String email;
    private String lastName;
    private String firstName;
    private String phoneNumber;

    private String message;
}
