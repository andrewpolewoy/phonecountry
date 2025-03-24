package ru.dsec.phonecountry.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.dsec.phonecountry.model.dto.CountryResponse;
import ru.dsec.phonecountry.model.dto.PhoneRequest;
import ru.dsec.phonecountry.service.PhoneCountryService;

@RestController
@RequestMapping("/api/phone")
public class PhoneCountryController {

    private final PhoneCountryService service;

    public PhoneCountryController(PhoneCountryService service) {
        this.service = service;
    }

    @PostMapping(value = "/country")
    @Operation(summary = "Determine country by phone number")
    public ResponseEntity<CountryResponse> getCountry(@RequestBody PhoneRequest phoneRequest) {
        String country = service.determineCountry(phoneRequest.phoneNumber());
        return ResponseEntity.ok(new CountryResponse(country));
    }
}
