package ru.dsec.phonecountry.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

import java.io.IOException;

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
        String expected = "+71234567890"; // Учитываем, что метод оставляет +
        assertEquals(expected, service.cleanPhoneNumber(input));
    }

    @Test
    void testCleanPhoneNumber_onlyNonDigits() {
        String input = "abc-xyz";
        assertThrows(InvalidPhoneNumberException.class, () -> service.cleanPhoneNumber(input),
                "Expected InvalidPhoneNumberException for non-numeric input");
    }

    @Test
    void testCleanPhoneNumber_nullInput() {
        assertThrows(InvalidPhoneNumberException.class, () -> service.cleanPhoneNumber(null),
                "Expected InvalidPhoneNumberException for null input");
    }

    @Test
    void testCleanPhoneNumber_tooShort() {
        String input = "+123";
        assertThrows(InvalidPhoneNumberException.class, () -> service.cleanPhoneNumber(input),
                "Expected InvalidPhoneNumberException for too short number");
    }

    @Test
    void testCleanPhoneNumber_tooLong() {
        String input = "+1234567890123456";
        assertThrows(InvalidPhoneNumberException.class, () -> service.cleanPhoneNumber(input),
                "Expected InvalidPhoneNumberException for too long number");
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
    void testLoadCountryCodes_noWikipediaAccess() throws IOException {
        when(config.getApiUrl()).thenReturn("https://en.wikipedia.org/wiki/List_of_telephone_country_codes");
        when(config.getTimeout()).thenReturn(10000);

        try (var mockedJsoup = mockStatic(Jsoup.class)) {
            // Мокаем вызов Jsoup.connect() с конкретными параметрами
            mockedJsoup.when(() -> Jsoup.connect("https://en.wikipedia.org/wiki/List_of_telephone_country_codes"))
                    .thenReturn(mock(org.jsoup.Connection.class));

            // Мокаем вызов get() на Connection, чтобы выбросить IOException
            org.jsoup.Connection connection = mock(org.jsoup.Connection.class);
            when(connection.timeout(10000)).thenReturn(connection);
            when(connection.get()).thenThrow(new IOException("Network error"));
            mockedJsoup.when(() -> Jsoup.connect("https://en.wikipedia.org/wiki/List_of_telephone_country_codes"))
                    .thenReturn(connection);

            service.loadCountryCodes();

            verify(repository, times(1)).deleteAll();
            verify(repository, never()).saveAll(anyList());
            assertTrue(service.countryCodeMap.isEmpty());
        }
    }

    @Test
    void testLoadCountryCodes_emptyTable() throws IOException {
        when(config.getApiUrl()).thenReturn("https://en.wikipedia.org/wiki/List_of_telephone_country_codes");
        when(config.getTimeout()).thenReturn(10000);

        // Мокаем Document и его методы для симуляции пустой таблицы
        Document mockDoc = mock(Document.class);
        when(mockDoc.getElementById("Alphabetical_order")).thenReturn(null);

        try (var mockedJsoup = mockStatic(Jsoup.class)) {
            // Мокаем вызов Jsoup.connect() с конкретными параметрами
            mockedJsoup.when(() -> Jsoup.connect("https://en.wikipedia.org/wiki/List_of_telephone_country_codes"))
                    .thenReturn(mock(org.jsoup.Connection.class));

            // Мокаем вызов get() на Connection, чтобы вернуть mockDoc
            org.jsoup.Connection connection = mock(org.jsoup.Connection.class);
            when(connection.timeout(10000)).thenReturn(connection);
            when(connection.get()).thenReturn(mockDoc);
            mockedJsoup.when(() -> Jsoup.connect("https://en.wikipedia.org/wiki/List_of_telephone_country_codes"))
                    .thenReturn(connection);

            service.loadCountryCodes();

            verify(repository, times(1)).deleteAll();
            verify(repository, never()).saveAll(anyList());
            assertTrue(service.countryCodeMap.isEmpty());
        }
    }
}