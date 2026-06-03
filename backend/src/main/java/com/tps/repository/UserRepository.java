package com.tps.repository;

/**
 * 文件说明：数据访问层，负责声明实体查询与持久化接口。
 */

import com.tps.entity.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByStudentId(String studentId);
    boolean existsByPhone(String phone);
    boolean existsByStudentId(String studentId);
    long countByStatus(User.UserStatus status);
}
