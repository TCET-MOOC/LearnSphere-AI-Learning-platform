package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByResourceTypeAndResourceIdOrderByIdAsc(String resourceType, Long resourceId);

    /** Latest entries for one resource, newest first (admin Inspect, W7-12). */
    List<AuditLog> findTop10ByResourceTypeAndResourceIdOrderByIdDesc(String resourceType, Long resourceId);
}
