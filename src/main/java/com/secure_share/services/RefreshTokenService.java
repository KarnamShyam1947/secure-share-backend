package com.secure_share.services;

import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.secure_share.entities.RefreshTokenEntity;
import com.secure_share.exceptions.OTPExpiredException;
import com.secure_share.repositories.RefreshTokenRepository;
import com.secure_share.utils.DateTimeUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${application.jwt.refresh.expiration}")
    private long refreshTokenExpirationTime;

    public RefreshTokenEntity createRefreshToken(int userId) {

        RefreshTokenEntity refreshTokenEntityByUserId = refreshTokenRepository.findByUserId(userId);
        
        if(refreshTokenEntityByUserId != null) {
            refreshTokenEntityByUserId.setExpireDate(new Date(System.currentTimeMillis() + refreshTokenExpirationTime));
            return refreshTokenRepository.save(refreshTokenEntityByUserId);
        }

        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity
            .builder()
            .userId(userId)
            .refreshToken(UUID.randomUUID().toString())
            .expireDate(new Date(System.currentTimeMillis() + refreshTokenExpirationTime))
            .build();

        return refreshTokenRepository.save(refreshTokenEntity);
    }

    @Transactional
    public RefreshTokenEntity refreshToken(String token) {
        RefreshTokenEntity byRefreshToken = refreshTokenRepository.findByRefreshToken(token);
        int userId = byRefreshToken.getId();

        try {
            DateTimeUtils.isTokenInTime(byRefreshToken.getExpireDate());
            byRefreshToken.setExpireDate(new Date(System.currentTimeMillis() + refreshTokenExpirationTime));
            return refreshTokenRepository.save(byRefreshToken);
        } catch (OTPExpiredException e) {
            RefreshTokenEntity newRefreshToken = createRefreshToken(userId);
            refreshTokenRepository.delete(byRefreshToken);

            return newRefreshToken;
        }
    }

}
