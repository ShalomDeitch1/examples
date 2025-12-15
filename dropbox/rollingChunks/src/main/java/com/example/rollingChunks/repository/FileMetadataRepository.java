package com.example.rollingChunks.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.rollingChunks.model.FileMetadata;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
}
