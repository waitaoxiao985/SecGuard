package com.secguard.server.repository;

import com.secguard.common.enums.AgentStatus;
import com.secguard.server.entity.Agent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByAgentKey(String agentKey);

    /** 去重查询：同 IP + 同 hostname */
    Optional<Agent> findByIpAndHostname(String ip, String hostname);

    // ===== 分页查询 =====

    Page<Agent> findByStatus(AgentStatus status, Pageable pageable);

    @Query("SELECT a FROM Agent a WHERE " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%',:kw,'%')) OR " +
           "a.ip LIKE CONCAT('%',:kw,'%') OR " +
           "LOWER(a.hostname) LIKE LOWER(CONCAT('%',:kw,'%'))")
    Page<Agent> findByKeyword(@Param("kw") String keyword, Pageable pageable);

    @Query("SELECT a FROM Agent a WHERE a.status = :status AND (" +
           "LOWER(a.name) LIKE LOWER(CONCAT('%',:kw,'%')) OR " +
           "a.ip LIKE CONCAT('%',:kw,'%') OR " +
           "LOWER(a.hostname) LIKE LOWER(CONCAT('%',:kw,'%')))")
    Page<Agent> findByStatusAndKeyword(
            @Param("status") AgentStatus status,
            @Param("kw") String keyword,
            Pageable pageable);

    // ===== 离线检测 =====

    /** 查找心跳超时的 ACTIVE Agent */
    @Query("SELECT a FROM Agent a WHERE a.status = 'ACTIVE' AND a.lastKeepalive < :threshold")
    List<Agent> findStaleActiveAgents(@Param("threshold") LocalDateTime threshold);

    // ===== 统计 =====

    long countByStatus(AgentStatus status);

    @Query("SELECT a.status, COUNT(a) FROM Agent a GROUP BY a.status")
    List<Object[]> countByStatusGrouped();
}
