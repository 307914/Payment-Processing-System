package com.payments.service;

import com.payments.config.JwtConfig;
import com.payments.dto.request.LoginRequest;
import com.payments.dto.request.LogoutRequest;
import com.payments.dto.request.RefreshTokenRequest;
import com.payments.dto.request.RegisterRequest;
import com.payments.dto.response.AuthResponse;
import com.payments.dto.response.UserResponse;
import com.payments.entity.RefreshToken;
import com.payments.entity.User;
import com.payments.exception.BusinessException;
import com.payments.repository.RefreshTokenRepository;
import com.payments.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.emailTaken(request.getEmail());
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);

        return UserResponse.from(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(BusinessException::invalidCredentials);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.invalidCredentials();
        }

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        String refreshTokenValue = jwtService.generateRefreshToken(user.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtConfig.getRefreshTokenExpiry()))
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getAccessTokenExpiry())
                .build();
    }

    @Transactional
    public void logout(String accessToken, LogoutRequest request) {
        long remainingExpiry = jwtService.getRemainingExpiry(accessToken);
        tokenBlacklistService.blacklist(accessToken, remainingExpiry);

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(BusinessException::tokenExpired);

        if (refreshToken.isRevoked()) {
            refreshTokenRepository.revokeAllByUser(refreshToken.getUser(), Instant.now());
            throw BusinessException.tokenBlacklisted();
        }

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(BusinessException::tokenExpired);

        if (refreshToken.isRevoked()) {
            refreshTokenRepository.revokeAllByUser(refreshToken.getUser(), Instant.now());
            throw BusinessException.tokenBlacklisted();
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw BusinessException.tokenExpired();
        }

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        String newRefreshTokenValue = jwtService.generateRefreshToken(user.getId());

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(newRefreshTokenValue)
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtConfig.getRefreshTokenExpiry()))
                .build();

        refreshTokenRepository.save(newRefreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getAccessTokenExpiry())
                .build();
    }
}
