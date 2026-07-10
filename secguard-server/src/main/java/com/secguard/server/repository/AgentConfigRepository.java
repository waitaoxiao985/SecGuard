package com.secguard.server.repository;

import com.secguard.server.entity.AgentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentConfigRepository extends JpaRepository<AgentConfig, Long> {

    Optional<AgentConfig> findByAgentId(Long agentId);
}
