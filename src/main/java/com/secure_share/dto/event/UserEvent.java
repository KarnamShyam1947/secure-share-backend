package com.secure_share.dto.event;

import com.secure_share.entities.UserEntity;
import com.secure_share.enums.UserEventType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private UserEventType eventType;
    private UserEntity userEntity;
}
