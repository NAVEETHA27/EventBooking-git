package com.eventbooking.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that an event date is not in the past.
 */
@Documented
@Constraint(validatedBy = EventDateValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEventDate {
    String message() default "Event date must be today or in the future";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
