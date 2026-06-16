package com.etfcompass.backend.security;

import com.etfcompass.backend.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

  private final SecurityProperties properties;

  public String generateToken(UserDetails userDetails) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(getExpiresInSeconds());
    return Jwts.builder()
        .subject(userDetails.getUsername())
        .claim("roles", userDetails.getAuthorities().stream().map(Object::toString).toList())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(key())
        .compact();
  }

  public String extractUsername(String token) {
    return claims(token).getSubject();
  }

  public boolean isValid(String token, UserDetails userDetails) {
    return extractUsername(token).equalsIgnoreCase(userDetails.getUsername())
        && claims(token).getExpiration().after(new Date());
  }

  public long getExpiresInSeconds() {
    return properties.accessTokenExpirationMinutes() * 60;
  }

  private Claims claims(String token) {
    return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey key() {
    byte[] bytes = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(bytes);
  }
}
