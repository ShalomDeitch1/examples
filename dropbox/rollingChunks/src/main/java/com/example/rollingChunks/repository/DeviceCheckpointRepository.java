package com.example.rollingChunks.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.rollingChunks.model.DeviceCheckpoint;

public interface DeviceCheckpointRepository extends JpaRepository<DeviceCheckpoint, String> {}
