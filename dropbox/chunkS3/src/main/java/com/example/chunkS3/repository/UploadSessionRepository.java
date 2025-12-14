package com.example.chunkS3.repository;

import com.example.chunkS3.model.FileStatus;
import com.example.chunkS3.model.UploadSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {

    @Query("""
            select s from UploadSession s
            join s.version v
            join v.parts p
            where p.hash = :hash
              and s.status in :activeStatuses
            """)
    List<UploadSession> findActiveSessionsExpectingHash(@Param("hash") String hash,
                                                       @Param("activeStatuses") Collection<FileStatus> activeStatuses);

    Optional<UploadSession> findByVersion_Id(UUID versionId);
}
