package com.taskmanager.api.user;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn,
        UserResponse user
) {
    public static LoginResponse of(String accessToken, String refreshToken, 
                                   long accessExpiresInMs, long refreshExpiresInMs, User user) {
        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                accessExpiresInMs / 1000, // convert to seconds
                refreshExpiresInMs / 1000,
                UserResponse.from(user)
        );
    }

}
