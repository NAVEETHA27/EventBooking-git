package com.eventbooking.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

/**
 * Implementation for @ValidEventDate — rejects past dates.
 */
public class EventDateValidator implements ConstraintValidator<ValidEventDate, LocalDate> {

    @Override
    public boolean isValid(LocalDate date, ConstraintValidatorContext context) {
        if (date == null) return true; // let @NotNull handle null checks separately
        return !date.isBefore(LocalDate.now());
    }
}
