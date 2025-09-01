package com.secure_share.listeners;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.secure_share.dto.event.UserEvent;
import com.secure_share.enums.UserEventType;
import com.secure_share.services.EmailService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserAccountActivationListener {

    private final EmailService emailService;
    
    @Async
    @EventListener
    public void listenUserActivation(UserEvent userEvent) {

        if (UserEventType.ACCOUNT_ACTIVATION.equals(userEvent.getEventType())) 
            emailService.sendEmail(
                userEvent.getUserEntity(), 
                "SecureShare: User account activation email", 
                "activate.ftl",
                "verify"
            );
        
        if (UserEventType.PASSWORD_RESET.equals(userEvent.getEventType())) 
            emailService.sendEmail(
                userEvent.getUserEntity(), 
                "SecureShare: password reset instructions for your account", 
                "password.ftl",
                "set-password"
            );
            
        

    }
    
}
