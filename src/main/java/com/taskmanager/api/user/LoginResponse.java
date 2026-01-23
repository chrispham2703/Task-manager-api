package com.taskmanager.api.user;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    public static LoginResponse of(String accessToken, long expiresInMs, User user) {
        return new LoginResponse(
                accessToken,
                "Bearer",
                expiresInMs / 1000, // convert to seconds
                UserResponse.from(user)
        );
    }
}
