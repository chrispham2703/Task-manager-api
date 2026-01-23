package com.taskmanager.api.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder encoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private final UUID testUserId = UUID.randomUUID();
    private final String testEmail = "test@example.com";
    private final String testPassword = "password123";
    private final String hashedPassword = "$2a$10$hashedpassword";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail(testEmail);
        testUser.setPasswordHash(hashedPassword);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.getRoles().add(Role.USER);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register new user successfully")
        void shouldRegisterNewUser() {
            // Given
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(encoder.encode(testPassword)).thenReturn(hashedPassword);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                return user;
            });

            // When
            User result = userService.register(testEmail, testPassword);

            // Then
            assertThat(result.getEmail()).isEqualTo(testEmail.toLowerCase());
            assertThat(result.getPasswordHash()).isEqualTo(hashedPassword);
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(result.getRoles()).contains(Role.USER);

            verify(userRepository).existsByEmail(testEmail.toLowerCase());
            verify(encoder).encode(testPassword);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should normalize email to lowercase")
        void shouldNormalizeEmail() {
            // Given
            String upperCaseEmail = "TEST@EXAMPLE.COM";
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(encoder.encode(anyString())).thenReturn(hashedPassword);
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            User result = userService.register(upperCaseEmail, testPassword);

            // Then
            assertThat(result.getEmail()).isEqualTo(upperCaseEmail.toLowerCase());
        }

        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowWhenEmailExists() {
            // Given
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> userService.register(testEmail, testPassword))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email already exists");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUserWhenFound() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            Optional<User> result = userService.findById(testUserId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo(testEmail);
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            // When
            Optional<User> result = userService.findById(testUserId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActiveById")
    class FindActiveById {

        @Test
        @DisplayName("should return active user")
        void shouldReturnActiveUser() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            Optional<User> result = userService.findActiveById(testUserId);

            // Then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("should return empty for deleted user")
        void shouldReturnEmptyForDeletedUser() {
            // Given
            testUser.setStatus(UserStatus.DELETED);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            Optional<User> result = userService.findActiveById(testUserId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("verifyPassword")
    class VerifyPassword {

        @Test
        @DisplayName("should return true for correct password")
        void shouldReturnTrueForCorrectPassword() {
            // Given
            when(encoder.matches(testPassword, hashedPassword)).thenReturn(true);

            // When
            boolean result = userService.verifyPassword(testUser, testPassword);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for wrong password")
        void shouldReturnFalseForWrongPassword() {
            // Given
            when(encoder.matches("wrongPassword", hashedPassword)).thenReturn(false);

            // When
            boolean result = userService.verifyPassword(testUser, "wrongPassword");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("softDelete")
    class SoftDelete {

        @Test
        @DisplayName("should soft delete user")
        void shouldSoftDeleteUser() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            // When
            userService.softDelete(testUserId);

            // Then
            assertThat(testUser.getStatus()).isEqualTo(UserStatus.DELETED);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should do nothing when user not found")
        void shouldDoNothingWhenUserNotFound() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

            // When
            userService.softDelete(testUserId);

            // Then
            verify(userRepository, never()).save(any());
        }
    }
}
