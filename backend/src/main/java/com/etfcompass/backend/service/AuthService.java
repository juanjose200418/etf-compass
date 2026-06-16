package com.etfcompass.backend.service;

import com.etfcompass.backend.domain.AppUser;
import com.etfcompass.backend.dto.auth.AuthResponse;
import com.etfcompass.backend.dto.auth.ForgotPasswordRequest;
import com.etfcompass.backend.dto.auth.LoginRequest;
import com.etfcompass.backend.dto.auth.RegisterRequest;
import com.etfcompass.backend.dto.auth.ResetPasswordRequest;
import com.etfcompass.backend.dto.auth.UserResponse;
import com.etfcompass.backend.exception.BadRequestException;
import com.etfcompass.backend.exception.NotFoundException;
import java.security.SecureRandom;
import java.time.Instant;
import com.etfcompass.backend.repository.AppUserRepository;
import com.etfcompass.backend.config.SecurityProperties;
import com.etfcompass.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final String INVALID_RESET_CODE_MESSAGE = "Codigo invalido o caducado";

  private final AppUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final UserDetailsService userDetailsService;
  private final JwtService jwtService;
  private final PasswordResetDeliveryService passwordResetDeliveryService;
  private final SecurityProperties securityProperties;
  private final SecureRandom secureRandom = new SecureRandom();

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new BadRequestException("Email is already registered");
    }
    var user = new AppUser(email, passwordEncoder.encode(request.password()), request.displayName().trim());
    userRepository.save(user);
    return tokenFor(user.getEmail());
  }

  public AuthResponse login(LoginRequest request) {
    String email = normalizeEmail(request.email());
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
    return tokenFor(email);
  }

  @Transactional
  public void requestPasswordResetCode(ForgotPasswordRequest request) {
    String email = normalizeEmail(request.email());
    userRepository.findByEmailIgnoreCase(email).ifPresent(this::issuePasswordResetCode);
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    String email = normalizeEmail(request.email());
    AppUser user = userRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new BadRequestException(INVALID_RESET_CODE_MESSAGE));

    validateResetCode(user, request.code().trim());
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    clearPasswordResetCode(user);
  }

  public UserResponse me(String email) {
    return userRepository.findByEmailIgnoreCase(email)
        .map(this::toUserResponse)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }

  private AuthResponse tokenFor(String email) {
    var userDetails = userDetailsService.loadUserByUsername(email);
    var user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
    String token = jwtService.generateToken(userDetails);
    return new AuthResponse(token, "Bearer", jwtService.getExpiresInSeconds(), toUserResponse(user));
  }

  private void issuePasswordResetCode(AppUser user) {
    String code = generateResetCode();
    user.setPasswordResetCodeHash(passwordEncoder.encode(code));
    user.setPasswordResetCodeExpiresAt(Instant.now().plusSeconds(securityProperties.passwordResetCodeExpirationMinutes() * 60));

    try {
      passwordResetDeliveryService.sendPasswordResetCode(
          user.getEmail(),
          user.getDisplayName(),
          code,
          securityProperties.passwordResetCodeExpirationMinutes());
    } catch (RuntimeException ex) {
      throw new BadRequestException("No se pudo enviar el correo de recuperacion. Intentalo otra vez.");
    }
  }

  private void validateResetCode(AppUser user, String code) {
    if (user.getPasswordResetCodeHash() == null || user.getPasswordResetCodeExpiresAt() == null) {
      throw new BadRequestException(INVALID_RESET_CODE_MESSAGE);
    }
    if (user.getPasswordResetCodeExpiresAt().isBefore(Instant.now())) {
      clearPasswordResetCode(user);
      throw new BadRequestException(INVALID_RESET_CODE_MESSAGE);
    }
    if (!passwordEncoder.matches(code, user.getPasswordResetCodeHash())) {
      throw new BadRequestException(INVALID_RESET_CODE_MESSAGE);
    }
  }

  private void clearPasswordResetCode(AppUser user) {
    user.setPasswordResetCodeHash(null);
    user.setPasswordResetCodeExpiresAt(null);
  }

  private String generateResetCode() {
    return String.valueOf(100000 + secureRandom.nextInt(900000));
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase();
  }

  private UserResponse toUserResponse(AppUser user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRoles());
  }
}
