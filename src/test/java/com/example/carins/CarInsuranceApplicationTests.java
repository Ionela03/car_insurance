package com.example.carins;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.carins.model.Car;
import com.example.carins.model.InsuranceClaim;
import com.example.carins.model.InsurancePolicy;
import com.example.carins.repo.CarRepository;
import com.example.carins.repo.InsuranceClaimRepository;
import com.example.carins.repo.InsurancePolicyRepository;
import com.example.carins.service.CarService;
import com.example.carins.web.CarController;

@SpringBootTest
class CarInsuranceApplicationTests {

    @Autowired
    CarService service;

    @Autowired
    jakarta.validation.Validator validator;

    @Autowired
    InsurancePolicyRepository policyRepo;

    @Autowired
    CarRepository carRepo;

    @Autowired
    InsuranceClaimRepository claimRepo;

    @Autowired
    CarController controller;
    
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


     @Test
    void claimWithValidDataShouldPass() {
        Car car = carRepo.findAll().get(0);

        InsuranceClaim claim = new InsuranceClaim();
        claim.setCar(car);
        claim.setClaimDate(LocalDate.now());
        claim.setDescription("Minor accident");
        claim.setAmount(BigDecimal.valueOf(1200));

        var violations = validator.validate(claim);
        assertTrue(violations.isEmpty(), "Valid claim should not have validation errors");

        claimRepo.save(claim);
        assertNotNull(claim.getId(), "Claim should be saved and have an ID");
    }

    @Test
    void claimWithoutDateShouldFail() {
        Car car = carRepo.findAll().get(0);

        InsuranceClaim claim = new InsuranceClaim();
        claim.setCar(car);
        claim.setDescription("Missing date");
        claim.setAmount(BigDecimal.valueOf(1200));

        var violations = validator.validate(claim);
        assertFalse(violations.isEmpty(), "Claim without date should fail validation");
    }

    @Test
    void claimWithNegativeAmountShouldFail() {
        Car car = carRepo.findAll().get(0);

        InsuranceClaim claim = new InsuranceClaim();
        claim.setCar(car);
        claim.setClaimDate(LocalDate.now());
        claim.setDescription("Invalid amount");
        claim.setAmount(BigDecimal.valueOf(-50));

        var violations = validator.validate(claim);
        assertFalse(violations.isEmpty(), "Claim with negative amount should fail validation");
    }

    @Test
    void claimWithoutDescriptionShouldFail() {
        Car car = carRepo.findAll().get(0);

        InsuranceClaim claim = new InsuranceClaim();
        claim.setCar(car);
        claim.setClaimDate(LocalDate.now());
        claim.setAmount(BigDecimal.valueOf(1200));

        var violations = validator.validate(claim);
        assertFalse(violations.isEmpty(), "Claim without description should fail validation");
    }
    
    
    @Test
    void insuranceValidShouldReturn404ForUnknownCar() {
        var ex = assertThrows(ResponseStatusException.class, () ->
            controller.isInsuranceValid(999L, "2025-01-01")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void insuranceValidShouldReturn400ForInvalidDateFormat() {
        var carId = carRepo.findAll().get(0).getId();
        var ex = assertThrows(ResponseStatusException.class, () ->
            controller.isInsuranceValid(carId, "not-a-date")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void insuranceValidShouldReturn400ForTooOldDate() {
        var carId = carRepo.findAll().get(0).getId();
        var ex = assertThrows(ResponseStatusException.class, () ->
            controller.isInsuranceValid(carId, "1800-01-01")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void insuranceValidShouldReturn400ForFutureDate() {
        var carId = carRepo.findAll().get(0).getId();
        var ex = assertThrows(ResponseStatusException.class, () ->
            controller.isInsuranceValid(carId, "2200-01-01")
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }


}
