package ru.dsec.phonecountry.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import ru.dsec.phonecountry.config.PhoneCountryConfig;
import ru.dsec.phonecountry.exception.CountryNotFoundException;
import ru.dsec.phonecountry.exception.InvalidPhoneNumberException;
import ru.dsec.phonecountry.model.CountryCode;
import ru.dsec.phonecountry.repository.CountryCodeRepository;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for determining the country by phone number.
 * Loads country codes from Wikipedia and stores them in the database.
 */
@Service
@Slf4j
public class PhoneCountryService {

    private static final Pattern SUBCODE_PATTERN = Pattern.compile("\\(([^)]+)\\)");
    final Map<String, String> countryCodeMap = new HashMap<>();
    private final CountryCodeRepository repository;
    private final PhoneCountryConfig config;

    public PhoneCountryService(CountryCodeRepository repository, PhoneCountryConfig config) {
        this.repository = repository;
        this.config = config;
    }

    /**
     * Loads country codes from Wikipedia and stores them in the database.
     * This method runs once after the service is initialized.
     */
    @PostConstruct
    public void loadCountryCodes() {
        try {
            log.info("Starting to load country codes from Wikipedia...");
            repository.deleteAll();
            log.info("Cleared the country codes repository");

            Document doc = Jsoup.connect(config.getApiUrl())
                    .timeout(config.getTimeout())
                    .get();
            log.info("Successfully fetched data from {}", config.getApiUrl());

            Element alphabeticalOrderHeader = doc.getElementById("Alphabetical_order");
            if (alphabeticalOrderHeader == null) {
                log.error("Could not find 'Alphabetical order' section");
                return;
            }

            Element parent = alphabeticalOrderHeader.parent();
            Element table = parent != null ? parent.nextElementSibling() : null;
            while (table != null && !table.tagName().equals("table")) {
                table = table.nextElementSibling();
            }

            if (table == null || !table.hasClass("wikitable")) {
                log.error("Could not find the country codes table");
                return;
            }

            Map<String, List<String>> codeToCountries = new HashMap<>(250);
            for (Element row : table.select("tbody > tr")) {
                Elements cells = row.select("td");
                if (cells.size() >= 2) {
                    String countryName = cells.get(0).text().trim();
                    String codeText = cells.get(1).text().trim();

                    if (!codeText.isEmpty()) {
                        codeText = codeText.split("\\[")[0].trim(); // Remove references
                        if (!codeText.startsWith("+")) {
                            codeText = "+" + codeText;
                        }

                        // Extract base code and subcodes
                        String baseCode = codeText.split("\\s+")[0]; // E.g., "+7" or "+599"
                        Matcher subcodeMatcher = SUBCODE_PATTERN.matcher(codeText);
                        if (subcodeMatcher.find()) {
                            String subCodes = subcodeMatcher.group(1); // E.g., "6, 7" or "3, 4, 7"
                            for (String subCode : subCodes.split(",\\s*")) {
                                String fullCode = baseCode + subCode.trim();
                                codeToCountries.computeIfAbsent(fullCode, k -> new ArrayList<>()).add(countryName);
                            }
                        } else {
                            codeToCountries.computeIfAbsent(baseCode, k -> new ArrayList<>()).add(countryName);
                        }
                    }
                }
            }

            List<CountryCode> codes = new ArrayList<>(codeToCountries.size());
            for (Map.Entry<String, List<String>> entry : codeToCountries.entrySet()) {
                String code = entry.getKey();
                String combinedCountries = String.join(", ", entry.getValue());
                CountryCode countryCode = new CountryCode();
                countryCode.setCode(code);
                countryCode.setCountryName(combinedCountries);
                codes.add(countryCode);
                countryCodeMap.put(code.substring(1), combinedCountries);
            }

            log.info("Found {} unique country codes", codes.size());
            repository.saveAll(codes);
            log.info("Successfully loaded country codes into the database");
        } catch (IOException e) {
            log.error("Failed to load country codes", e);
        }
    }

    /**
     * Cleans a phone number by removing all non-numeric characters.
     *
     * @param phoneNumber The input phone number.
     * @return A string containing only digits.
     */
    public String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.error("Phone number is null or empty");
            throw new InvalidPhoneNumberException("Phone number cannot be empty");
        }

        // Разрешаем только числа, пробелы, скобки, дефисы и плюс, но плюс должен быть только в начале
        if (!phoneNumber.matches("\\+?[0-9()\\-\\s]+")) {
            log.error("Invalid phone number format: {}", phoneNumber);
            throw new InvalidPhoneNumberException("Phone number contains invalid characters");
        }

        // Удаляем все символы, кроме цифр и "+"
        String cleanedNumber = phoneNumber.replaceAll("[^\\d+]", "");

        // Проверяем длину номера (7-15 цифр по E.164)
        String digitsOnly = cleanedNumber.replace("+", "");
        if (digitsOnly.length() < 7 || digitsOnly.length() > 15) {
            log.error("Phone number length out of bounds: {}", cleanedNumber);
            throw new InvalidPhoneNumberException("Phone number length must be between 7 and 15 digits");
        }

        log.debug("Cleaned phone number: {} -> {}", phoneNumber, cleanedNumber);
        return cleanedNumber;
    }

    /**
     * Determines the country corresponding to the given phone number.
     *
     * @param phoneNumber The phone number to analyze.
     * @return The country name associated with the phone number.
     * @throws InvalidPhoneNumberException If the phone number is empty or invalid.
     * @throws CountryNotFoundException    If the country cannot be determined.
     */
    public String determineCountry(String phoneNumber) {
        log.debug("Determining country for phone number: {}", phoneNumber);
        String cleanNumber = cleanPhoneNumber(phoneNumber);

        if (cleanNumber.isEmpty()) {
            log.error("Invalid phone number provided: {}", phoneNumber);
            throw new InvalidPhoneNumberException("Invalid phone number format");
        }

        Optional<String> bestMatch = countryCodeMap.entrySet().stream()
                .filter(entry -> cleanNumber.startsWith(entry.getKey()) || cleanNumber.startsWith("+" + entry.getKey()))
                .max(Comparator.comparingInt(entry -> entry.getKey().length()))
                .map(Map.Entry::getValue);

        if (bestMatch.isPresent()) {
            log.info("Found country {} for phone number {}", bestMatch.get(), phoneNumber);
            return bestMatch.get();
        }

        log.error("Country not found for phone number: {}", phoneNumber);
        throw new CountryNotFoundException("Country not found for phone number: " + phoneNumber);
    }
}
