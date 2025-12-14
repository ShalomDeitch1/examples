package com.example.chunkS3.repository;

import com.example.chunkS3.model.FileVersion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileVersionRepository extends JpaRepository<FileVersion, UUID> {

    List<FileVersion> findByFileIdOrderByCreatedAtDesc(UUID fileId);

    Optional<FileVersion> findByIdAndFileId(UUID id, UUID fileId);
}
