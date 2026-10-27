package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByFamilyId(String familyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.status = com.MOOC.OnlineLearningPlatfrom.Entity.RefreshToken.Status.REVOKED WHERE r.familyId = :familyId")
    int revokeFamily(@Param("familyId") String familyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.status = com.MOOC.OnlineLearningPlatfrom.Entity.RefreshToken.Status.REVOKED WHERE r.user.userId = :userId AND r.status = com.MOOC.OnlineLearningPlatfrom.Entity.RefreshToken.Status.ACTIVE")
    int revokeAllActiveForUser(@Param("userId") Long userId);
}
