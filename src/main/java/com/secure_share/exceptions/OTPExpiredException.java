package com.secure_share.exceptions;

public class OTPExpiredException extends Exception {
    public OTPExpiredException() {
        super("your otp have expired");
    }
    
    public OTPExpiredException(String msg) {
        super(msg);
    }
}
