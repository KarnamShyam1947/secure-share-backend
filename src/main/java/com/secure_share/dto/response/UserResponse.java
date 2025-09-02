package com.secure_share.dto.response;

import org.springframework.beans.BeanUtils;

import com.secure_share.entities.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private int id;
    private String role;
    private String email;
    private String lastName;
    private String firstName;
    private String phoneNumber;
    private String walletAddress;

    public static UserResponse entityToResponse(UserEntity entity) {
        UserResponse userResponse = new UserResponse();
        BeanUtils.copyProperties(entity, userResponse);

        return userResponse;
    }
    
    public static UserEntity responseToEntity(UserResponse userResponse) {
        UserEntity userEntity = new UserEntity();
        BeanUtils.copyProperties(userResponse, userEntity);

        return userEntity;
    }
}
