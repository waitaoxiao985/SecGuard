package com.secguard.server.repository;

import com.secguard.server.entity.CveEntryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * CVE 漏洞库 Repository
 */
@Repository
public interface CveEntryRepository extends JpaRepository<CveEntryEntity, Long> {

    /** 按 CVE ID 查找 */
    Optional<CveEntryEntity> findByCveId(String cveId);

    /** 按产品名模糊搜索（不区分大小写） */
    Page<CveEntryEntity> findByAffectedProductContainingIgnoreCase(String product, Pageable pageable);

    /** 按产品名精确查找所有 CVE 条目 */
    List<CveEntryEntity> findByAffectedProductIgnoreCase(String product);

    /** 按严重等级统计数量 */
    @Query("SELECT e.severity, COUNT(e) FROM CveEntryEntity e GROUP BY e.severity")
    List<Object[]> countBySeverityGrouped();

    /** 检查某 CVE 是否已存在 */
    boolean existsByCveId(String cveId);

    /** 按产品名模糊搜索并返回全部（用于匹配） */
    @Query("SELECT e FROM CveEntryEntity e WHERE LOWER(e.affectedProduct) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<CveEntryEntity> searchByProductKeyword(@Param("keyword") String keyword);
}
