package com.fooddelivery.authservice.dto;

public record LoginResponse(String token, String tokenType, long expiresIn) {

    public static LoginResponse of(String token, long expiresInMs) {
        return new LoginResponse(token, "Bearer", expiresInMs / 1000);
    }
}