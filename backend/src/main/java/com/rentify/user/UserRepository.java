package com.rentify.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRole(UserRole role);
    List<User> findAllByOrderByCreatedAtDesc();
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByIsSuspendedFalse();
    long countByIsSuspendedTrue();
}
