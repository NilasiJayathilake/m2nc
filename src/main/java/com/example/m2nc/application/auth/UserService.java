package com.example.m2nc.application.auth;

import com.example.m2nc.domain.user.User;
import com.example.m2nc.domain.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean emailExists(String normalizedEmail) {
        return userRepository.existsByEmailIgnoreCase(normalizedEmail);
    }

    public Optional<User> findByEmail(String normalizedEmail) {
        return userRepository.findByEmailIgnoreCase(normalizedEmail);
    }

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}
