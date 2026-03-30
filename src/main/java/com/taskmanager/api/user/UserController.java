package com.taskmanager.api.user;

import com.taskmanager.api.common.exception.ErrorResponse;
import com.taskmanager.api.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;
    private final AuthenticatedUser authenticatedUser;

    public UserController(UserService userService, AuthenticatedUser authenticatedUser) {
        this.userService = userService;
        this.authenticatedUser = authenticatedUser;
    }

    @Operation(summary = "Get current user", description = "Returns the profile of the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        User user = authenticatedUser.getCurrentUser();
        if (user == null) {
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    "Not authenticated",
                    "/api/users/me"
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @Operation(summary = "Get user by ID", description = "Returns a user profile (only own profile or admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found or not authorized")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable UUID id) {
        User currentUser = authenticatedUser.getCurrentUser();

        if (currentUser == null) {
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    "Not authenticated",
                    "/api/users/" + id
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        
        // Check if requesting own profile or is admin
        if (!currentUser.getId().equals(id) && !currentUser.getRoles().contains(Role.ADMIN)) {
            return ResponseEntity.notFound().build(); // Return 404 to prevent ID enumeration
        }
        
        return userService.findById(id)
                .filter(User::isActive)
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete current user (soft delete)", 
               description = "Soft deletes the authenticated user's account. The account will be marked as DELETED but data is preserved.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User account deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteCurrentUser() {
        User user = authenticatedUser.getCurrentUser();
        if (user == null) {
            ErrorResponse error = new ErrorResponse(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    "Not authenticated",
                    "/api/users/me"
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        
        userService.softDelete(user.getId());
        return ResponseEntity.noContent().build();
    }
}
