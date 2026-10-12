package com.garmentqc.system.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.garmentqc.system.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

        private final SecretKey key;
        private final long expirationMs;

        public JwtService(
                        @Value("${app.jwt.secret}") String secret,
                        @Value("${app.jwt.expiration-ms}") long expirationMs) {

                byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);

                if (secretBytes.length < 32) {
                        throw new IllegalStateException(
                                        "JWT_SECRET minimal 32 karakter (256 bit)");
                }

                this.key = Keys.hmacShaKeyFor(secretBytes);
                this.expirationMs = expirationMs;
        }

        public String generateToken(User user) {

                return Jwts.builder()
                                .subject(user.getUsername())
                                .claim("role", user.getRole().name())
                                .claim(
                                                "section",
                                                user.getSection() != null
                                                                ? user.getSection().name()
                                                                : null)
                                .issuedAt(new Date())
                                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                                .signWith(key)
                                .compact();
        }

        public String extractUsername(String token) {
                return extractAllClaims(token).getSubject();
        }

        public boolean isTokenValid(String token, User user) {

                String username = extractUsername(token);

                return username.equals(user.getUsername())
                                && !isTokenExpired(token);
        }

        private boolean isTokenExpired(String token) {
                return extractAllClaims(token)
                                .getExpiration()
                                .before(new Date());
        }

        private Claims extractAllClaims(String token) {
                return Jwts.parser()
                                .verifyWith(key)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();
        }
}