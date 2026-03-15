package com.ashu.MediSmart.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret-key}")
    private String secretKeyString;

    private final long EXPIRATION_TIME = 1000 * 60 * 60; // 1 hour

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        // Ensure the key is at least 32 bytes for HS256
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("Secret key must be at least 32 characters long");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    //  Extract username (email in our case)
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    //  Extract role (optional helper)
    public String extractRole(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    //  Extract expiration
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    //  Generic claim extractor
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ✅ Generate token
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate token
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }
}