package com.taskmanager.api.user;

import com.taskmanager.api.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthenticatedUser authenticatedUser;

    public UserController(UserService userService, AuthenticatedUser authenticatedUser) {
        this.userService = userService;
        this.authenticatedUser = authenticatedUser;
    }

    /**
     * Get current authenticated user's profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        User user = authenticatedUser.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Get user by ID (only for admin or self)
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        User currentUser = authenticatedUser.getCurrentUser();
        
        // Users can only view their own profile (unless admin)
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
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
}
