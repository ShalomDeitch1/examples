package com.example.ticketmaster.locking.postgresannotations;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SeatInventoryRepository extends JpaRepository<SeatInventory, SeatInventoryId> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SeatInventory s where s.id = :id")
    Optional<SeatInventory> findByIdForUpdate(@Param("id") SeatInventoryId id);
}
