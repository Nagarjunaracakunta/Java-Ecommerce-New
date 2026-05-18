package com.fooddelivery.authservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public UserEntity() {}

    public UserEntity(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public Long getId() { return id; }

    public String getUsername() { return username; }

    public String getPassword() { return password; }

    public Role getRole() { return role; }
}