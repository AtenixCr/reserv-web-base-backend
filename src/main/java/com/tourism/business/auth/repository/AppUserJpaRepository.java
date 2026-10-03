package com.tourism.business.auth.repository;
import com.tourism.business.auth.entity.AppUserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AppUserJpaRepository extends JpaRepository<AppUserEntity,Long> {
    Optional<AppUserEntity> findByUsername(String username);
    boolean existsByIdAndRoleAndActiveTrue(Long id,String role);
}
