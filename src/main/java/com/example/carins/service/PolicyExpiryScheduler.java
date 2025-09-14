package com.example.carins.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.carins.repo.InsurancePolicyRepository;

@Service
public class PolicyExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PolicyExpiryScheduler.class);
    private final InsurancePolicyRepository policyRepo;

    
    private final Set<Long> notifiedPolicies = new HashSet<>();

    public PolicyExpiryScheduler(InsurancePolicyRepository policyRepo) {
        this.policyRepo = policyRepo;
    }

    @Scheduled(fixedRate = 5_000) 
    public void checkForExpiredPolicies() {
        LocalDate today = LocalDate.now();
        var expiredPolicies = policyRepo.findByEndDate(today);

        expiredPolicies.forEach(policy -> {
            if (!notifiedPolicies.contains(policy.getId())) {
                log.info("Policy {} for car {} expired on {}",
                        policy.getId(),
                        policy.getCar().getId(),
                        policy.getEndDate());
                notifiedPolicies.add(policy.getId());
            }
        });
    }
}