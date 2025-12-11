package com.example.chunkS3.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.chunkS3.model.FileMetadata;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
}
