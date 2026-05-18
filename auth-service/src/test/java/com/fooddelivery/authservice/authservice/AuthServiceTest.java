package com.fooddelivery.authservice.authservice;

import com.fooddelivery.authservice.dto.LoginRequest;
import com.fooddelivery.authservice.dto.LoginResponse;
import com.fooddelivery.authservice.dto.RegisterRequest;
import com.fooddelivery.authservice.entity.Role;
import com.fooddelivery.authservice.entity.UserEntity;
import com.fooddelivery.authservice.repository.UserRepository;
import com.fooddelivery.authservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "expirationMs", 86400000L);
    }

    // --- login ---

    @Test
    void login_shouldReturnLoginResponseWhenCredentialsAreValid() {
        UserEntity user = new UserEntity("alice", "encoded-password", Role.ROLE_USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawpassword", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken("alice", "ROLE_USER")).thenReturn("mocked-token");

        LoginResponse response = authService.login(new LoginRequest("alice", "rawpassword"));

        assertThat(response.token()).isEqualTo("mocked-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(86400L);
    }

    @Test
    void login_shouldThrowBadCredentialsExceptionWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "pass")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_shouldThrowBadCredentialsExceptionWhenPasswordDoesNotMatch() {
        UserEntity user = new UserEntity("alice", "encoded-password", Role.ROLE_USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrongpass")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    // --- register ---

    @Test
    void register_shouldSaveUserWithEncodedPasswordAndDefaultRole() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("rawpassword")).thenReturn("encoded-password");

        authService.register(new RegisterRequest("alice", "rawpassword"));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void register_shouldThrowIllegalArgumentExceptionWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice", "password123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username already exists");

        verify(userRepository, never()).save(any());
    }

    // --- promoteToAdmin ---

    @Test
    void promoteToAdmin_shouldUpdateUserRoleToAdmin() {
        UserEntity user = new UserEntity("alice", "encoded-password", Role.ROLE_USER);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        authService.promoteToAdmin("alice");

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
    }

    @Test
    void promoteToAdmin_shouldThrowIllegalArgumentExceptionWhenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.promoteToAdmin("ghost"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}