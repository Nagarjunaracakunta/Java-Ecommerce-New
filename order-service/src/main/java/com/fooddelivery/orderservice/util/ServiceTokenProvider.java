package com.fooddelivery.orderservice.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class ServiceTokenProvider {

    private static final long TTL_MS = 60_000;

    @Value("${jwt.secret}")
    private String secret;

    public String token() {
        return Jwts.builder()
                .subject("order-service")
                .claim("role", "ROLE_SERVICE")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + TTL_MS))
                .signWith(signingKey())
                .compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}