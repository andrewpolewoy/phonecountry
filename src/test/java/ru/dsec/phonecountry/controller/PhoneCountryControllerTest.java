package ru.dsec.phonecountry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.dsec.phonecountry.exception.CountryNotFoundException;
import ru.dsec.phonecountry.exception.InvalidPhoneNumberException;
import ru.dsec.phonecountry.model.dto.CountryResponse;
import ru.dsec.phonecountry.model.dto.ErrorResponse;
import ru.dsec.phonecountry.model.dto.PhoneRequest;
import ru.dsec.phonecountry.service.PhoneCountryService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PhoneCountryController.class)
class PhoneCountryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PhoneCountryService service;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reset(service); // Сбрасываем состояние мока перед каждым тестом
    }

    @Test
    void testGetCountry_successRussia() throws Exception {
        String phoneNumber = "71423423412";
        when(service.determineCountry(phoneNumber)).thenReturn("Russia");

        String requestJson = objectMapper.writeValueAsString(new PhoneRequest(phoneNumber));
        String responseJson = objectMapper.writeValueAsString(new CountryResponse("Russia"));

        mockMvc.perform(post("/api/phone/country")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().json(responseJson));

        verify(service, times(1)).determineCountry(phoneNumber);
    }

    @Test
    void testGetCountry_successKazakhstan() throws Exception {
        String phoneNumber = "77112227231";
        when(service.determineCountry(phoneNumber)).thenReturn("Kazakhstan");

        String requestJson = objectMapper.writeValueAsString(new PhoneRequest(phoneNumber));
        String responseJson = objectMapper.writeValueAsString(new CountryResponse("Kazakhstan"));

        mockMvc.perform(post("/api/phone/country")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().json(responseJson));

        verify(service, times(1)).determineCountry(phoneNumber);
    }

    @Test
    void testGetCountry_successUnitedStatesAndCanada() throws Exception {
        String phoneNumber = "11165384765";
        when(service.determineCountry(phoneNumber)).thenReturn("United States, Canada");

        String requestJson = objectMapper.writeValueAsString(new PhoneRequest(phoneNumber));
        String responseJson = objectMapper.writeValueAsString(new CountryResponse("United States, Canada"));

        mockMvc.perform(post("/api/phone/country")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().json(responseJson));

        verify(service, times(1)).determineCountry(phoneNumber);
    }

    @Test
    void testGetCountry_invalidPhoneNumber() throws Exception {
        String phoneNumber = "abc";
        when(service.determineCountry(phoneNumber))
                .thenThrow(new InvalidPhoneNumberException("Invalid phone number format"));

        String requestJson = objectMapper.writeValueAsString(new PhoneRequest(phoneNumber));
        String responseJson = objectMapper.writeValueAsString(new ErrorResponse("Invalid phone number format"));

        mockMvc.perform(post("/api/phone/country")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(responseJson));

        verify(service, times(1)).determineCountry(phoneNumber);
    }

    @Test
    void testGetCountry_countryNotFound() throws Exception {
        String phoneNumber = "9991234567";
        when(service.determineCountry(phoneNumber))
                .thenThrow(new CountryNotFoundException("Country not found for phone number: " + phoneNumber));

        String requestJson = objectMapper.writeValueAsString(new PhoneRequest(phoneNumber));
        String responseJson = objectMapper.writeValueAsString(
                new ErrorResponse("Country not found for phone number: " + phoneNumber));

        mockMvc.perform(post("/api/phone/country")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(content().json(responseJson));

        verify(service, times(1)).determineCountry(phoneNumber);
    }
}