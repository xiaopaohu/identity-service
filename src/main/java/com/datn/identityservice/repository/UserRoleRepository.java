package com.datn.identityservice.repository;

import com.datn.identityservice.entity.UserRole;
import com.datn.identityservice.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}
