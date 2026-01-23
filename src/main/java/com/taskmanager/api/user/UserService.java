package com.taskmanager.api.user;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    @Transactional
    public User register(String email, String rawPassword) {
        String normalized = email.trim().toLowerCase();

        if (userRepository.existsByEmail(normalized)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setEmail(normalized);
        user.setPasswordHash(encoder.encode(rawPassword));
        user.getRoles().add(Role.USER);
        user.setStatus(UserStatus.ACTIVE);

        return userRepository.save(user);
    }

    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findActiveByEmail(String email) {
        return userRepository.findActiveByEmail(email);
    }

    public Optional<User> findActiveById(UUID id) {
        return userRepository.findById(id)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE);
    }

    @Transactional
    public void softDelete(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.softDelete();
            userRepository.save(user);
        });
    }

    public boolean verifyPassword(User user, String rawPassword) {
        return encoder.matches(rawPassword, user.getPasswordHash());
    }
}
