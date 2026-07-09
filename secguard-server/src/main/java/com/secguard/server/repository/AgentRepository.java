package com.secguard.server.repository;

import com.secguard.common.enums.AgentStatus;
import com.secguard.server.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByAgentKey(String agentKey);

    List<Agent> findByStatus(AgentStatus status);

    @Query("SELECT a FROM Agent a WHERE a.lastKeepalive < :threshold")
    List<Agent> findStaleAgents(LocalDateTime threshold);

    long countByStatus(AgentStatus status);
}
