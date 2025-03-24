package ru.dsec.phonecountry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "phone.country")
@Getter
@Setter
public class PhoneCountryConfig {
    private String apiUrl;
    private String apiKey;
    private int timeout;
}