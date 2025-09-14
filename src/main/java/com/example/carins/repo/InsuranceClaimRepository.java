package com.example.carins.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.carins.model.InsuranceClaim;

public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, Long> {
    List<InsuranceClaim> findByCarIdOrderByClaimDateAsc(Long carId);
}

