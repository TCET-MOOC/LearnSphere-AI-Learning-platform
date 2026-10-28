package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.MediaPublishJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaPublishJobRepository extends JpaRepository<MediaPublishJob, Long> {

    List<MediaPublishJob> findByContentIdAndStatusIn(Long contentId, Collection<String> statuses);

    Optional<MediaPublishJob> findByTeacherIdAndContentIdAndIdempotencyKey(Long teacherId, Long contentId, String idempotencyKey);

    List<MediaPublishJob> findByTeacherId(Long teacherId);
}
