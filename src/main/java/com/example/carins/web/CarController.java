package com.example.carins.web;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.carins.model.Car;
import com.example.carins.repo.CarRepository;
import com.example.carins.repo.InsuranceClaimRepository;
import com.example.carins.repo.InsurancePolicyRepository;
import com.example.carins.service.CarService;
import com.example.carins.web.dto.CarDto;
import com.example.carins.web.dto.InsuranceClaimDto;
import com.example.carins.web.dto.PolicyHistoryDto;
import com.example.carins.web.dto.ClaimHistoryDto;

import jakarta.validation.Valid;
@RestController
@RequestMapping("/api")
public class CarController {

    private final CarService service;
    private final CarRepository carRepo;
    private final InsuranceClaimRepository claimRepo;
    private final InsurancePolicyRepository policyRepo;

    public CarController(CarService service, CarRepository carRepo, InsuranceClaimRepository claimRepo, InsurancePolicyRepository policyRepo) {
        this.service = service;
        this.carRepo = carRepo;
        this.claimRepo = claimRepo;
        this.policyRepo = policyRepo;
    }

    @GetMapping("/cars")
    public List<CarDto> getCars() {
        return service.listCars().stream().map(this::toDto).toList();
    }

    @GetMapping("/cars/{carId}/insurance-valid")
    public ResponseEntity<?> isInsuranceValid(
            @PathVariable Long carId,
            @RequestParam String date) {

        var car = carRepo.findById(carId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Car not found"));

        // Validate date format
        LocalDate d;
        try {
            d = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format, expected YYYY-MM-DD");
        }

        // Reject impossible dates
        if (d.isBefore(LocalDate.of(1900, 1, 1)) || d.isAfter(LocalDate.of(2100, 12, 31))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date is out of supported range (1900-2100)");
        }

        boolean valid = service.isInsuranceValid(car.getId(), d);
        return ResponseEntity.ok(new InsuranceValidityResponse(carId, d.toString(), valid));
    }

    @PostMapping("/cars/{carId}/claims")
    public ResponseEntity<?> createClaim(
            @PathVariable Long carId,
            @Valid @RequestBody com.example.carins.model.InsuranceClaim claim) {

        var car = carRepo.findById(carId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Car not found"));

        claim.setCar(car);
        var saved = claimRepo.save(claim);

        URI location = URI.create("/api/cars/" + carId + "/claims/" + saved.getId());
        return ResponseEntity.created(location).body(toClaimDto(saved));
    }


    @GetMapping("/cars/{carId}/history")
    public ResponseEntity<?> getCarHistory(@PathVariable Long carId) {
        var car = carRepo.findById(carId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Car not found"));

        var policies = policyRepo.findByCarIdOrderByStartDateAsc(carId)
                .stream()
                .map(p -> new PolicyHistoryDto("policy", p.getStartDate(), p.getEndDate(), p.getProvider()))
                .toList();

        var claims = claimRepo.findByCarIdOrderByClaimDateAsc(carId)
                .stream()
                .map(c -> new ClaimHistoryDto("claim", c.getClaimDate(), c.getAmount(), c.getDescription()))
                .toList();

        
        var history = new ArrayList<Object>();
        history.addAll(policies);
        history.addAll(claims);

        history.sort((a, b) -> {
            LocalDate dateA = (a instanceof PolicyHistoryDto dto) ? dto.startDate() : ((ClaimHistoryDto) a).claimDate();
            LocalDate dateB = (b instanceof PolicyHistoryDto dto) ? dto.startDate() : ((ClaimHistoryDto) b).claimDate();
            return dateA.compareTo(dateB);
        });

        return ResponseEntity.ok(history);
    }
    private CarDto toDto(Car c) {
        var o = c.getOwner();
        return new CarDto(c.getId(), c.getVin(), c.getMake(), c.getModel(), c.getYearOfManufacture(),
                o != null ? o.getId() : null,
                o != null ? o.getName() : null,
                o != null ? o.getEmail() : null);
    }

    public record InsuranceValidityResponse(Long carId, String date, boolean valid) {}

    private InsuranceClaimDto toClaimDto(com.example.carins.model.InsuranceClaim claim) {
    return new InsuranceClaimDto(
            claim.getId(),
            claim.getClaimDate(),
            claim.getDescription(),
            claim.getAmount()
    );
}


}

