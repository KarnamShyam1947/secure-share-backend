package com.secure_share.services;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.secure_share.dto.request.LoginRequest;
import com.secure_share.dto.request.RegisterRequest;
import com.secure_share.dto.request.SetPasswordRequest;
import com.secure_share.entities.UserEntity;
import com.secure_share.exceptions.OTPExpiredException;
import com.secure_share.exceptions.UserNotFoundException;
import com.secure_share.repositories.UserRepository;
import com.secure_share.utils.DateTimeUtils;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Cacheable(value = "userById", key = "#id")
     public UserEntity getUserById(int id) throws UserNotFoundException {
        Optional<UserEntity> userEntity = userRepository.findById(id);

        if(userEntity.isEmpty())
            throw new UserNotFoundException();

        return userEntity.get();
    }

    @Cacheable(value = "userByEmail", key = "#email")
    public UserEntity getUserByEmail(String email) throws UserNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(email);

        if(userEntity == null)
            throw new UserNotFoundException(email);

        return userEntity;
    }
    
    @Cacheable(value = "userByToken", key = "#token")
    public UserEntity getUserByToken(String token) throws UserNotFoundException {
        UserEntity userEntity = userRepository.findByToken(token);

        if(userEntity == null)
            throw new UserNotFoundException(token);

        return userEntity;
    }
    
    public UserEntity registerUser(RegisterRequest registerRequest) throws UserNotFoundException {
        UserEntity userEntity = userRepository.findByEmail(registerRequest.getEmail());

        if(userEntity != null)
            return null;

        UserEntity newUser = new UserEntity();
        BeanUtils.copyProperties(registerRequest, newUser);
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setRole("USER");
        newUser.setToken(UUID.randomUUID().toString());
        newUser.setExpirationDate(DateTimeUtils.addHours(1));

        UserEntity save = userRepository.save(newUser);

        return save;
    }

    public UserEntity loginUser(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        return userRepository.findByEmail(loginRequest.getEmail());

    }
    
    @CachePut(value = "userByEmail", key = "#result.email")
    public UserEntity forgotPassword(String email) throws UserNotFoundException {
        UserEntity userByEmail = getUserByEmail(email);

        userByEmail.setToken(UUID.randomUUID().toString());
        userByEmail.setExpirationDate(DateTimeUtils.addHours(1));
        UserEntity newUser = userRepository.save(userByEmail);

        return newUser;
    }

    @CachePut(value = "userById", key = "#result.id")
    public UserEntity setUserPassword(SetPasswordRequest setPasswordRequest) throws UserNotFoundException, OTPExpiredException {
        UserEntity userByToken = getUserByToken(setPasswordRequest.getToken());

        if (DateTimeUtils.isTokenInTime(userByToken.getExpirationDate())) {
            userByToken.setPassword(passwordEncoder.encode(setPasswordRequest.getPassword()));
            userByToken.setExpirationDate(null);
            userByToken.setToken(null);

            userRepository.save(userByToken);
        }

        return userByToken;
    }

    @CachePut(value = "userByToken", key = "#token")
    public UserEntity activateUser(String token) throws UserNotFoundException, OTPExpiredException {
        UserEntity userByToken = getUserByToken(token);

        if (DateTimeUtils.isTokenInTime(userByToken.getExpirationDate())) {
            userByToken.setActive(true);
            userByToken.setToken(null);
            userByToken.setExpirationDate(null);
            return userRepository.save(userByToken);
        }

        return null;
    }
}
