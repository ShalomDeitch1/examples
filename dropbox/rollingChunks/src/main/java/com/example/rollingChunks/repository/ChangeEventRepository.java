package com.example.rollingChunks.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.rollingChunks.model.ChangeEvent;
import com.example.rollingChunks.model.ChangeEventType;

public interface ChangeEventRepository extends JpaRepository<ChangeEvent, Long> {

    boolean existsByEventTypeAndVersionId(ChangeEventType eventType, UUID versionId);

    @Query("select e from ChangeEvent e where e.id > :afterId order by e.id asc")
    List<ChangeEvent> findAfter(@Param("afterId") long afterId, Pageable pageable);
}
