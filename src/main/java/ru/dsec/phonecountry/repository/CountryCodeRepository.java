package ru.dsec.phonecountry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.dsec.phonecountry.model.CountryCode;

import java.util.Optional;

@Repository
public interface CountryCodeRepository extends JpaRepository<CountryCode, Long> {
    Optional<CountryCode> findByCode(String code);
}
