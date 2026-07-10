package com.secguard.server.repository;

import com.secguard.common.enums.AlertStatus;
import com.secguard.common.enums.Severity;
import com.secguard.server.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警 Repository
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    Page<Alert> findByAgentId(Long agentId, Pageable pageable);

    long countBySeverity(Severity severity);

    long countByStatus(AlertStatus status);

    long countByCreatedAtAfter(LocalDateTime since);

    @Query("SELECT a.severity, COUNT(a) FROM Alert a GROUP BY a.severity")
    List<Object[]> countBySeverityGrouped();

    @Query("SELECT a.status, COUNT(a) FROM Alert a GROUP BY a.status")
    List<Object[]> countByStatusGrouped();
}
