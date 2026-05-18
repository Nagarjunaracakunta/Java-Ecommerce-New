package com.fooddelivery.authservice.repository;

import com.fooddelivery.authservice.entity.Role;
import com.fooddelivery.authservice.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    // --- findByUsername ---

    @Test
    void findByUsername_shouldReturnUserWhenExists() {
        userRepository.save(new UserEntity("alice", "encoded-pass", Role.ROLE_USER));

        Optional<UserEntity> result = userRepository.findByUsername("alice");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
        assertThat(result.get().getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void findByUsername_shouldReturnEmptyWhenNotExists() {
        Optional<UserEntity> result = userRepository.findByUsername("ghost");
        assertThat(result).isEmpty();
    }

    // --- existsByUsername ---

    @Test
    void existsByUsername_shouldReturnTrueWhenExists() {
        userRepository.save(new UserEntity("alice", "encoded-pass", Role.ROLE_USER));
        assertThat(userRepository.existsByUsername("alice")).isTrue();
    }

    @Test
    void existsByUsername_shouldReturnFalseWhenNotExists() {
        assertThat(userRepository.existsByUsername("ghost")).isFalse();
    }

    // --- uniqueness constraint ---

    @Test
    void save_shouldThrowWhenDuplicateUsernameIsInserted() {
        userRepository.save(new UserEntity("alice", "encoded-pass-1", Role.ROLE_USER));

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(new UserEntity("alice", "encoded-pass-2", Role.ROLE_USER));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}