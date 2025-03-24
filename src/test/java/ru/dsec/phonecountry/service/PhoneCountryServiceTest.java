package ru.dsec.phonecountry.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.dsec.phonecountry.config.PhoneCountryConfig;
import ru.dsec.phonecountry.exception.CountryNotFoundException;
import ru.dsec.phonecountry.exception.InvalidPhoneNumberException;
import ru.dsec.phonecountry.repository.CountryCodeRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneCountryServiceTest {

    @Mock
    private CountryCodeRepository repository;

    @Mock
    private PhoneCountryConfig config;

    @InjectMocks
    private PhoneCountryService service;

    @BeforeEach
    void setUp() {
        service.countryCodeMap.clear(); // Очищаем map перед каждым тестом
    }

    @Test
    void testCleanPhoneNumber_validInput() {
        String input = "+7 (123) 456-78-90";
        String expected = "71234567890";
        assertEquals(expected, service.cleanPhoneNumber(input));
    }

    @Test
    void testCleanPhoneNumber_onlyNonDigits() {
        String input = "abc-xyz";
        String expected = "";
        assertEquals(expected, service.cleanPhoneNumber(input));
    }

    @Test
    void testDetermineCountry_russia() {
        service.countryCodeMap.put("7", "Russia");
        service.countryCodeMap.put("76", "Kazakhstan");
        service.countryCodeMap.put("77", "Kazakhstan");

        String phoneNumber = "71423423412";
        String result = service.determineCountry(phoneNumber);
        assertEquals("Russia", result);
    }

    @Test
    void testDetermineCountry_kazakhstan() {
        service.countryCodeMap.put("7", "Russia");
        service.countryCodeMap.put("76", "Kazakhstan");
        service.countryCodeMap.put("77", "Kazakhstan");

        String phoneNumber = "77112227231";
        String result = service.determineCountry(phoneNumber);
        assertEquals("Kazakhstan", result);
    }

    @Test
    void testDetermineCountry_unitedStatesAndCanada() {
        service.countryCodeMap.put("1", "United States, Canada");
        service.countryCodeMap.put("1242", "Bahamas");

        String phoneNumber = "11165384765";
        String result = service.determineCountry(phoneNumber);
        assertEquals("United States, Canada", result);
    }

    @Test
    void testDetermineCountry_caribbeanNetherlands() {
        service.countryCodeMap.put("5993", "Caribbean Netherlands");
        service.countryCodeMap.put("5994", "Caribbean Netherlands");
        service.countryCodeMap.put("5997", "Caribbean Netherlands");

        String phoneNumber = "59971234567";
        String result = service.determineCountry(phoneNumber);
        assertEquals("Caribbean Netherlands", result);
    }

    @Test
    void testDetermineCountry_invalidPhoneNumber() {
        String phoneNumber = "abc";
        assertThrows(InvalidPhoneNumberException.class, () -> service.determineCountry(phoneNumber),
                "Expected InvalidPhoneNumberException for non-numeric input");
    }

    @Test
    void testDetermineCountry_countryNotFound() {
        service.countryCodeMap.put("7", "Russia");

        String phoneNumber = "9991234567";
        assertThrows(CountryNotFoundException.class, () -> service.determineCountry(phoneNumber),
                "Expected CountryNotFoundException for unknown code");
    }

    @Test
    void testLoadCountryCodes_integration() {
        when(config.getApiUrl()).thenReturn("https://en.wikipedia.org/wiki/List_of_telephone_country_codes");
        when(config.getTimeout()).thenReturn(10000);

        PhoneCountryService realService = new PhoneCountryService(repository, config);
        realService.loadCountryCodes();

        verify(repository, atLeastOnce()).deleteAll();
        verify(repository, atLeastOnce()).saveAll(anyList());

        assertTrue(realService.countryCodeMap.containsKey("1"));
        assertTrue(realService.countryCodeMap.containsKey("7"));
        assertTrue(realService.countryCodeMap.containsKey("77"));
        assertEquals("Russia", realService.countryCodeMap.get("7"));
    }
}