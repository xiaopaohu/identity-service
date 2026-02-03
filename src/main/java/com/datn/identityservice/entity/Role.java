package com.datn.identityservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Role {
    @Id
    @GeneratedValue
    @org.hibernate.annotations.UuidGenerator
    UUID id;

    @Column(unique = true, nullable = false, length = 50)
    String name;

    @OneToMany(mappedBy = "role")
    Set<UserRole> userRoles;
}