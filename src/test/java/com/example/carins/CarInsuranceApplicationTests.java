package com.example.carins;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.carins.model.InsurancePolicy;
import com.example.carins.repo.InsurancePolicyRepository;
import com.example.carins.service.CarService;

@SpringBootTest
class CarInsuranceApplicationTests {

    @Autowired
    CarService service;

    @Autowired
    jakarta.validation.Validator validator;

    @Autowired
    InsurancePolicyRepository policyRepo;

    @Test
    void insuranceValidityBasic() {
        assertTrue(service.isInsuranceValid(1L, LocalDate.parse("2024-06-01")));
        assertTrue(service.isInsuranceValid(1L, LocalDate.parse("2025-06-01")));
        assertFalse(service.isInsuranceValid(2L, LocalDate.parse("2025-02-01")));
    }

    @Test
    void policyWithoutEndDateShouldFail() {
        var car = policyRepo.findAll().get(0).getCar();

        InsurancePolicy p = new InsurancePolicy();
        p.setCar(car);
        p.setStartDate(LocalDate.of(2025, 1, 5));
        

        var violations = validator.validate(p);

        violations.forEach(v ->
            System.out.println(v.getPropertyPath() + " -> " + v.getMessage())
        );

        assertFalse(violations.isEmpty(), "Expected validation errors for missing endDate");
    }

     @Test
    void allPoliciesShouldHaveEndDate() {
        var policies = policyRepo.findAll();
        assertFalse(policies.isEmpty(), "No policies in DB");
        for (var p : policies) {
            assertNotNull(p.getEndDate(), "Policy " + p.getId() + " should have endDate");
        }
    }

    @Test
    void policyWithEndDateBeforeStartDateShouldFail() {
        var car = policyRepo.findAll().get(0).getCar();

        InsurancePolicy p = new InsurancePolicy();
        p.setCar(car);
        p.setStartDate(LocalDate.of(2025, 1, 10));
        p.setEndDate(LocalDate.of(2025, 1, 5));

        var violations = validator.validate(p);

        violations.forEach(v ->
            System.out.println(v.getPropertyPath() + " -----------> " + v.getMessage()+ "<---------------")
        );

        assertFalse(violations.isEmpty(), "Expected validation errors for endDate before startDate");
    }
    @Test
    void policyWithValidDatesShouldPass() {
        var car = policyRepo.findAll().get(0).getCar();

        InsurancePolicy validPolicy = new InsurancePolicy();
        validPolicy.setCar(car);
        validPolicy.setStartDate(LocalDate.of(2025, 1, 5));
        validPolicy.setEndDate(LocalDate.of(2025, 1, 10));

        var violations = validator.validate(validPolicy);
        assertTrue(violations.isEmpty(), "No validation errors expected for valid dates");
    }

    
}
