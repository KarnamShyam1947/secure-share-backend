package com.secure_share.dto;

import lombok.Getter;

@Getter
public class WebsocketUserInfo {
    private final String username;
    private final String userId;
    private final long joinedAt;
    private final String walletAddress;

    public WebsocketUserInfo(String username, String userId, String walletAddress) {
        this.username = username;
        this.userId = userId;
        this.walletAddress = walletAddress;
        this.joinedAt = System.currentTimeMillis();
    }   
}