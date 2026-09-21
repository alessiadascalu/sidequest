package dev.sidequest.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.ZoneId;

public class ValidZoneIdValidator implements ConstraintValidator<ValidZoneId, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null/blank le tratează @NotBlank; aici verificăm doar formatul.
        return value == null || value.isBlank() || ZoneId.getAvailableZoneIds().contains(value);
    }
}
