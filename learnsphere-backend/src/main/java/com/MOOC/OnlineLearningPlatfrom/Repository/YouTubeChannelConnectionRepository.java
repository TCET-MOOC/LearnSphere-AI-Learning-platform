package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.YouTubeChannelConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface YouTubeChannelConnectionRepository extends JpaRepository<YouTubeChannelConnection, Long> {

    Optional<YouTubeChannelConnection> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
