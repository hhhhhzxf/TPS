package com.tps.repository;

import com.tps.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByStudentId(String studentId);
    boolean existsByPhone(String phone);
    boolean existsByStudentId(String studentId);
}
