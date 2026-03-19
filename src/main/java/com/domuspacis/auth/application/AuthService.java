package com.domuspacis.auth.application;

import com.domuspacis.aop.annotation.Audited;
import com.domuspacis.aop.annotation.SensitiveParam;
import com.domuspacis.auth.domain.User;
import com.domuspacis.auth.domain.UserRole;
import com.domuspacis.auth.infrastructure.UserRepository;
import com.domuspacis.auth.interfaces.dto.*;
import com.domuspacis.shared.exception.BusinessRuleViolationException;
import com.domuspacis.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ── all your existing methods unchanged ──────────────────────────────────

    @Audited("CREATE_USER")
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleViolationException("Email already registered: " + request.email());
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.CUSTOMER)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .isActive(true)
                .build();
        userRepository.save(user);
        log.info("New customer registered: {}", user.getEmail());
        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new AuthResponse(token, refreshToken, user.getEmail(), user.getRole().name(), jwtService.getExpirationMillis());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));
        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        log.info("User logged in: {}", user.getEmail());
        return new AuthResponse(token, refreshToken, user.getEmail(), user.getRole().name(), jwtService.getExpirationMillis());
    }

    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new BusinessRuleViolationException("Invalid or expired refresh token");
        }
        String newToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        return new AuthResponse(newToken, newRefreshToken, user.getEmail(), user.getRole().name(), jwtService.getExpirationMillis());
    }

    @Audited("CREATE_USER")
    public UserResponse createAdminUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleViolationException("Email already exists: " + request.email());
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .isActive(true)
                .build();
        userRepository.save(user);
        return toUserResponse(user);
    }

    @Audited("CHANGE_USER_ROLE")
    public UserResponse assignRole(UUID userId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setRole(newRole);
        userRepository.save(user);
        return toUserResponse(user);
    }

    @Audited("DEACTIVATE_USER")
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User deactivated: {}", user.getEmail());
    }

    @Audited("RESET_USER_PASSWORD")
    public void initiatePasswordReset(String email) {
        log.info("Password reset initiated for: {}", email);
    }

    @Audited("RESET_USER_PASSWORD")
    public void resetPassword(UUID userId, @SensitiveParam String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ── 3 NEW METHODS ────────────────────────────────────────────────────────

    @Audited("LIST_USERS")
    @Transactional(readOnly = true)
    public UserPageResponse getUsers(String search, UserRole role, Boolean isActive, Pageable pageable) {
        Specification<User> spec = Specification.where(null);

        if (search != null && !search.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("lastName")),  "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("email")),     "%" + search.toLowerCase() + "%")
            ));
        }
        if (role     != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("role"),     role));
        if (isActive != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("isActive"), isActive));

        Page<User> page = userRepository.findAll(spec, pageable);
        List<UserResponse> content = page.getContent().stream()
                .map(this::toUserResponse)
                .toList();

        return new UserPageResponse(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Audited("UPDATE_USER")
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Guard against stealing another user's email
        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleViolationException("Email already in use: " + request.email());
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        if (request.role() != null) user.setRole(request.role());

        userRepository.save(user);
        log.info("User updated: {}", user.getEmail());
        return toUserResponse(user);
    }

    @Audited("ACTIVATE_USER")
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setIsActive(true);
        userRepository.save(user);
        log.info("User re-activated: {}", user.getEmail());
    }

    // ── shared private helper ─────────────────────────────────────────────────

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getRole().name(), user.getIsActive());
    }
}