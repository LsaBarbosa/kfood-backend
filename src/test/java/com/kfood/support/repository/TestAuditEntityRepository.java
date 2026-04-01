package com.kfood.support.repository;

import com.kfood.support.entity.TestAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestAuditEntityRepository extends JpaRepository<TestAuditEntity, Long> {}
