package com.interview.application.service;

import com.interview.application.dto.LoginCommand;
import com.interview.application.dto.LoginResult;
import com.interview.application.dto.RegisterCommand;
import com.interview.domain.model.User;
import com.interview.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthApplicationService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResult login(LoginCommand command) {
        User user = userRepository.findByUsername(command.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
        if (user.status() == null || user.status() != 1 || !passwordEncoder.matches(command.password(), user.passwordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        return new LoginResult(tokenService.generateToken(user.id(), user.username()), "Bearer");
    }

    @Transactional
    public LoginResult register(RegisterCommand command) {
        if (userRepository.findByUsername(command.username()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(command.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = userRepository.save(
                command.username(),
                command.email(),
                passwordEncoder.encode(command.password())
        );
        return new LoginResult(tokenService.generateToken(user.id(), user.username()), "Bearer");
    }
}
