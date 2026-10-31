package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByAudienceInOrderByPinnedDescCreatedAtDesc(Collection<Announcement.Audience> audiences);

    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.course LEFT JOIN FETCH a.author " +
           "WHERE a.course.courseId = :courseId " +
           "AND (a.expiresAt IS NULL OR a.expiresAt > :now) " +
           "ORDER BY a.pinned DESC, a.createdAt DESC")
    List<Announcement> findActiveByCourseId(@Param("courseId") Long courseId, @Param("now") LocalDateTime now);

    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.course LEFT JOIN FETCH a.author " +
           "WHERE ((a.course IS NULL AND a.audience IN :audiences) OR (a.course.courseId IN :enrolledCourseIds)) " +
           "AND (a.expiresAt IS NULL OR a.expiresAt > :now) " +
           "ORDER BY a.pinned DESC, a.createdAt DESC")
    List<Announcement> findActiveForStudent(@Param("audiences") Collection<Announcement.Audience> audiences,
                                           @Param("enrolledCourseIds") Collection<Long> enrolledCourseIds,
                                           @Param("now") LocalDateTime now);

    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.course LEFT JOIN FETCH a.author " +
           "WHERE (a.course IS NULL AND a.audience IN :audiences) " +
           "AND (a.expiresAt IS NULL OR a.expiresAt > :now) " +
           "ORDER BY a.pinned DESC, a.createdAt DESC")
    List<Announcement> findActiveGlobal(@Param("audiences") Collection<Announcement.Audience> audiences,
                                        @Param("now") LocalDateTime now);

    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.course LEFT JOIN FETCH a.author " +
           "WHERE ((a.course IS NULL AND a.audience IN :audiences) OR (a.course.courseId IN :taughtCourseIds)) " +
           "AND (a.expiresAt IS NULL OR a.expiresAt > :now) " +
           "ORDER BY a.pinned DESC, a.createdAt DESC")
    List<Announcement> findActiveForTeacher(@Param("audiences") Collection<Announcement.Audience> audiences,
                                           @Param("taughtCourseIds") Collection<Long> taughtCourseIds,
                                           @Param("now") LocalDateTime now);

    @Query("SELECT a FROM Announcement a LEFT JOIN FETCH a.course LEFT JOIN FETCH a.author " +
           "WHERE (:courseId IS NULL OR a.course.courseId = :courseId) " +
           "AND (a.expiresAt IS NULL OR a.expiresAt > :now) " +
           "ORDER BY a.pinned DESC, a.createdAt DESC")
    List<Announcement> findActiveForAdmin(@Param("courseId") Long courseId,
                                         @Param("now") LocalDateTime now);
}
