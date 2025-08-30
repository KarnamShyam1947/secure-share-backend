package com.secure_share.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.secure_share.entities.RefreshTokenEntity;


@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Integer>{
    RefreshTokenEntity findByRefreshToken(String refreshToken);
    RefreshTokenEntity findByUserId(int userId);
}
