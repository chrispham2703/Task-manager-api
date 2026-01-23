package com.taskmanager.api.user;

import com.taskmanager.api.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for user registration, login, and token refresh")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "Register a new user", description = "Creates a new user account and returns access/refresh tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(createLoginResponse(user));
    }

    @Operation(summary = "Login", description = "Authenticates user and returns access/refresh tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findActiveByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !userService.verifyPassword(user, request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(createLoginResponse(user));
    }

    @Operation(summary = "Refresh token", description = "Uses refresh token to get new access/refresh token pair")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userService.findActiveById(userId).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(createLoginResponse(user));
    }

    private LoginResponse createLoginResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());
        return LoginResponse.of(
                accessToken,
                refreshToken,
                jwtUtil.getAccessTokenExpirationMs(),
                jwtUtil.getRefreshTokenExpirationMs(),
                user
        );
    }
}
