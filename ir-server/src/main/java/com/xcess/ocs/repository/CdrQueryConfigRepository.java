package com.xcess.ocs.repository;

import com.xcess.ocs.entity.CdrQueryConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CdrQueryConfigRepository extends JpaRepository<CdrQueryConfig, Long> {
}
