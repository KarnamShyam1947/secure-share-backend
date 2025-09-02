package com.secure_share.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.secure_share.config.custom.CookieAuthEntryPoint;
import com.secure_share.config.custom.CustomAuthEntryPoint;
import com.secure_share.config.custom.CustomUserDetailService;
import com.secure_share.filters.JwtAuthFilter;
import com.secure_share.filters.JwtCookieFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final CustomUserDetailService customUserDetailService;
    private final CustomAuthEntryPoint customAuthEntryPoint;
    private final CookieAuthEntryPoint cookieAuthEntryPoint;
    private final JwtCookieFilter jwtCookieFilter;
    private final JwtAuthFilter jwtAuthFilter;

    @Value("${application.cors.allowedMethods}")
    private List<String> allowedMethods;
    
    @Value("${application.cors.allowedOrigins}")
    private List<String> allowedOrigins;

    @Value("${application.cors.allowedHeaders}")
    private List<String> allowedHeaders;
   
    @Value("${application.cors.isCredentialsAllowed}")
    private boolean isCredentialsAllowed;
    
    private String[] permittedUrls = {
        "/auth/**",
        "/login/**",
        "/signal/**",
        "/oauth2/**",
        "/springwolf/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/api/v1/auth/**",
        "/authentication-service/**",
    };

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(customUserDetailService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());

        return daoAuthenticationProvider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

     @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(allowedMethods);
        configuration.setAllowedHeaders(allowedHeaders);
        configuration.setAllowCredentials(isCredentialsAllowed);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "application.authentication.type", havingValue = "AUTHORIZATION_HEADER", matchIfMissing = true)
    SecurityFilterChain authHeaderSecurityFilterChain(HttpSecurity security) throws Exception {
        security.csrf(
                AbstractHttpConfigurer::disable
        );

        security.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        security.authorizeHttpRequests(
                authorizer -> authorizer
                                .requestMatchers(permittedUrls).permitAll()
                                .anyRequest().authenticated()
        );

        security.sessionManagement(
                session -> session
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        security.addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        security.exceptionHandling(
            exception -> exception
                            .authenticationEntryPoint(customAuthEntryPoint)
        );


        return security.build();
    }
    
    @Bean
    @Order(2)
    @ConditionalOnProperty(name = "application.authentication.type", havingValue = "HTTP_COOKIE")
    SecurityFilterChain httpCookieSecurityFilterChain(HttpSecurity security) throws Exception {

        security.csrf(
                AbstractHttpConfigurer::disable
        );

        security.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        security.authorizeHttpRequests(
                authorizer -> authorizer
                                .requestMatchers(permittedUrls).permitAll()
                                .anyRequest().authenticated()
        );

        security.sessionManagement(
                session -> session
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        security.addFilterBefore(
                jwtCookieFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        security.exceptionHandling(
            exception -> exception
                            .authenticationEntryPoint(cookieAuthEntryPoint)
        );


        return security.build();
    }

}
