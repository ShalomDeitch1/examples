package com.example.directS3.repository;

import com.example.directS3.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
    java.util.Optional<FileMetadata> findFirstByS3Key(String s3Key);
}
